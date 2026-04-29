# LMI CRM — Backend Deployment Guide (EC2, No Docker)

## Overview

This guide deploys the Spring Boot backend to a single AWS EC2 instance.
No Docker. No Kubernetes. No domain name required for v1.

The app will be accessible at: `http://<ec2-public-ip>:8080`

---

## How Config Files Work (Read This First)

Spring Boot loads config in layers. When you start the app with `--spring.profiles.active=prod`,
it loads TWO files in order:

```
1. application.properties        (base config — inside the JAR, committed to git)
2. application-prod.properties   (prod secrets — lives ONLY on the server, never in git)
```

The second file OVERRIDES the first. This is how secrets stay off git.

Your local setup uses the same pattern:
- `application.properties` (base)
- `application-local.properties` (your local secrets — gitignored)

On the server, you will manually create `application-prod.properties` with real values.
It never touches your repo.

```
Your Laptop                          EC2 Server
-----------                          ----------
application.properties     →  JAR → application.properties  (baked into jar)
application-local.properties         application-prod.properties  ← you create this on server
(stays here, gitignored)             (stays here, never in git)
```

---

## Prerequisites

- AWS account with access to EC2
- Your terminal (to SSH into the server)
- The project builds locally (`./mvnw clean package -DskipTests` succeeds)

---

## Phase 1 — Prepare the Code (Do This on Your Laptop)

### Step 1.1 — Verify application.properties has no secrets

The file at `src/main/resources/application.properties` should look exactly like this.
Open it and confirm:

```properties
spring.application.name=crm

# JPA
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=false
spring.jpa.open-in-view=false

# CORS
app.cors.allowed-origins=

# JWT
jwt.expiry.hours=24
```

No passwords. No JWT secret. No database URL. Good.

### Step 1.2 — Verify @EnableScheduling is present

Open `src/main/java/com/lmi/crm/CrmApplication.java` and confirm it looks like this:

```java
@SpringBootApplication
@EnableScheduling
public class CrmApplication {
    public static void main(String[] args) {
        SpringApplication.run(CrmApplication.class, args);
    }
}
```

If `@EnableScheduling` is missing, add it. Without it, all scheduled jobs
(protection expiry, task reminders, etc.) will silently never run.

### Step 1.3 — Build the JAR

Run this from the project root:

```bash
./mvnw clean package -DskipTests
```

What this does:
- `clean` — deletes the old `target/` folder so you start fresh
- `package` — compiles code, runs any tests, then packages everything into one fat JAR
- `-DskipTests` — skips test execution (you have no tests yet)

When it finishes, you should see:
```
BUILD SUCCESS
```

Your JAR is at:
```
target/crm-0.0.1-SNAPSHOT.jar
```

### Step 1.4 — Verify the JAR works locally

Run it locally against your local profile to confirm nothing broke:

```bash
java -jar target/crm-0.0.1-SNAPSHOT.jar --spring.profiles.active=local
```

- It should start without errors
- Visit `http://localhost:8080/swagger-ui/index.html` — you should see the API docs
- Hit Ctrl+C to stop it once confirmed

---

## Phase 2 — Provision EC2 Instance (AWS Console)

### Step 2.1 — Launch an EC2 instance

1. Go to AWS Console → EC2 → Launch Instance
2. Settings:
   - **Name**: `lmi-crm-backend`
   - **AMI**: Amazon Linux 2023 (free tier eligible, comes with useful tools)
   - **Instance type**: `t3.small` (recommended for 60-70 users — t2.micro will struggle)
   - **Key pair**: Create a new key pair → name it `lmi-crm-key` → Download the `.pem` file
     - SAVE THIS FILE. You cannot download it again. Put it in `~/.ssh/lmi-crm-key.pem`
   - **Storage**: 20 GB gp3 (default 8 GB is fine but 20 GB gives room for logs)

3. **Network settings** — this is important. Configure Security Group:

   | Type | Protocol | Port | Source | Why |
   |------|----------|------|--------|-----|
   | SSH | TCP | 22 | My IP | So only you can SSH in |
   | Custom TCP | TCP | 8080 | 0.0.0.0/0 | So the app is reachable from internet |

   Do NOT open port 8080 to everywhere permanently in production — but for v1 this is fine.

4. Click **Launch Instance**

5. Wait ~2 minutes. Go to EC2 → Instances → find your instance → copy the **Public IPv4 address**.
   It will look like: `54.xxx.xxx.xxx`

### Step 2.2 — Fix the SSH key permissions

On your laptop:

```bash
chmod 400 ~/.ssh/lmi-crm-key.pem
```

Without this, SSH will refuse to connect (security requirement).

---

## Phase 3 — Set Up the Server (SSH into EC2)

### Step 3.1 — Connect to your EC2 instance

```bash
ssh -i ~/.ssh/lmi-crm-key.pem ec2-user@<your-ec2-ip>
```

Replace `<your-ec2-ip>` with the actual IP from Step 2.1.

You should see an Amazon Linux prompt: `[ec2-user@ip-xxx ~]$`

### Step 3.2 — Install Java 17

The server is a clean OS — nothing is installed. Run:

```bash
sudo dnf install java-17-amazon-corretto -y
```

Verify it installed:

```bash
java -version
```

You should see something like: `openjdk version "17.x.x"`

### Step 3.3 — Create the app directory

```bash
mkdir -p /home/ec2-user/app
```

This is where your JAR and config file will live.

---

## Phase 4 — Deploy the JAR and Config

### Step 4.1 — Copy the JAR to the server

Run this FROM YOUR LAPTOP (not inside the SSH session):

```bash
scp -i ~/.ssh/lmi-crm-key.pem \
  target/crm-0.0.1-SNAPSHOT.jar \
  ec2-user@<your-ec2-ip>:/home/ec2-user/app/
```

This copies your JAR from your laptop to the server over SSH.

Verify it arrived — back in your SSH session:

```bash
ls -lh /home/ec2-user/app/
```

You should see `crm-0.0.1-SNAPSHOT.jar` (~64 MB).

### Step 4.2 — Create application-prod.properties ON THE SERVER

This is the critical step. You are creating the secrets file directly on the server.
It never touches your laptop or your git repo.

In your SSH session:

```bash
nano /home/ec2-user/app/application-prod.properties
```

Paste the following, replacing every `<value>` with your actual values:

```properties
# Database — same Supabase credentials you use locally (it's cloud, accessible from anywhere)
spring.datasource.url=jdbc:postgresql://aws-1-ap-southeast-2.pooler.supabase.com:5432/postgres?user=postgres.pitjixglbimefmncrihg&password=<your-db-password>
spring.datasource.username=postgres.pitjixglbimefmncrihg
spring.datasource.password=<your-db-password>
spring.datasource.driver-class-name=org.postgresql.Driver
spring.datasource.hikari.maximum-pool-size=10
spring.datasource.hikari.minimum-idle=2
spring.datasource.hikari.connection-timeout=20000
spring.datasource.hikari.idle-timeout=300000
spring.datasource.hikari.max-lifetime=600000

# Mail (Mailtrap is fine for v1)
spring.mail.host=sandbox.smtp.mailtrap.io
spring.mail.port=587
spring.mail.username=<your-mailtrap-username>
spring.mail.password=<your-mailtrap-password>
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true

# JWT — use the same secret you had before (or generate a new one)
jwt.secret=15d0eca6799a5c5e8ab4caa8fd598963e633fc04ae28f32d3a6cec05b62e77aa
jwt.expiry.hours=24

# App URLs — use your EC2 public IP for now
app.base-url=http://<your-ec2-ip>:8080
app.frontend-url=http://localhost:3000
app.mlo-user-id=
app.cors.allowed-origins=http://localhost:3000,http://localhost:5173

# Logging — write logs to a file on the server
logging.level.root=WARN
logging.level.com.lmi.crm=INFO
logging.file.name=/home/ec2-user/app/logs/crm.log
logging.logback.rollingpolicy.max-history=30
```

Save and exit nano: `Ctrl+O` → Enter → `Ctrl+X`

Why this file lives next to the JAR:
Spring Boot automatically finds `application-prod.properties` in the same directory
as the JAR when you start it. No extra config needed.

### Step 4.3 — Create the logs directory

```bash
mkdir -p /home/ec2-user/app/logs
```

---

## Phase 5 — Run the Application

### Step 5.1 — First run (foreground, to check for errors)

Run this in your SSH session:

```bash
cd /home/ec2-user/app
java -jar crm-0.0.1-SNAPSHOT.jar --spring.profiles.active=prod
```

Watch the output. You should see Spring Boot startup logs ending with:
```
Started CrmApplication in X.XXX seconds
```

If it fails, the error will be printed here. Common issues:
- `Connection refused` on DB — Supabase URL or password wrong in prod properties
- `Port 8080 already in use` — something else running on that port (unlikely on fresh server)
- `Could not resolve placeholder` — a `${VAR}` in application-prod.properties was left blank

### Step 5.2 — Verify the app is reachable

From your laptop browser:

```
http://<your-ec2-ip>:8080/swagger-ui/index.html
```

You should see the Swagger UI with all your API endpoints.

Hit Ctrl+C to stop the app for now. We'll make it run permanently in the next step.

### Step 5.3 — Run as a background service (so it survives SSH disconnect)

Create a systemd service so the app:
- Starts automatically when the server reboots
- Restarts automatically if it crashes
- Runs in the background without needing your SSH session open

```bash
sudo nano /etc/systemd/system/crm.service
```

Paste this:

```ini
[Unit]
Description=LMI CRM Backend
After=network.target

[Service]
User=ec2-user
WorkingDirectory=/home/ec2-user/app
ExecStart=java -jar /home/ec2-user/app/crm-0.0.1-SNAPSHOT.jar --spring.profiles.active=prod
SuccessExitStatus=143
Restart=on-failure
RestartSec=10
StandardOutput=append:/home/ec2-user/app/logs/crm.log
StandardError=append:/home/ec2-user/app/logs/crm.log

[Install]
WantedBy=multi-user.target
```

Save and exit: `Ctrl+O` → Enter → `Ctrl+X`

Enable and start the service:

```bash
sudo systemctl daemon-reload
sudo systemctl enable crm
sudo systemctl start crm
```

Check it's running:

```bash
sudo systemctl status crm
```

You should see `Active: active (running)`.

---

## Phase 6 — Verify Everything Works

### Check the app is running

```bash
sudo systemctl status crm
```

### Watch live logs

```bash
tail -f /home/ec2-user/app/logs/crm.log
```

### Test an endpoint from your laptop

```bash
curl http://<your-ec2-ip>:8080/api/auth/setup/validate-token?token=test
```

You should get a JSON response (even if it's an error — that means the app is running).

### Check it survives a reboot

```bash
sudo reboot
```

Wait 30 seconds, then SSH back in and check:

```bash
sudo systemctl status crm
```

---

## How to Redeploy (When You Push New Code)

Every time you update the code:

1. On your laptop — build a new JAR:
   ```bash
   ./mvnw clean package -DskipTests
   ```

2. Copy the new JAR to the server:
   ```bash
   scp -i ~/.ssh/lmi-crm-key.pem \
     target/crm-0.0.1-SNAPSHOT.jar \
     ec2-user@<your-ec2-ip>:/home/ec2-user/app/
   ```

3. SSH into the server and restart the service:
   ```bash
   ssh -i ~/.ssh/lmi-crm-key.pem ec2-user@<your-ec2-ip>
   sudo systemctl restart crm
   sudo systemctl status crm
   ```

The `application-prod.properties` stays on the server untouched. You only replace the JAR.

---

## File Layout on the Server

```
/home/ec2-user/app/
├── crm-0.0.1-SNAPSHOT.jar          ← deployed from your laptop via scp
├── application-prod.properties      ← created manually on server, NEVER in git
└── logs/
    └── crm.log                      ← rolling log file
```

---

## Security Notes for v1

- The `.pem` key file is your only way into the server — don't lose it
- `application-prod.properties` contains all secrets — it lives only on the server
- Port 22 (SSH) is restricted to your IP in the security group — good
- Port 8080 is open to the internet — fine for v1, restrict later when you add a domain + HTTPS

---

## What's NOT Covered Here (Future Steps)

- HTTPS / SSL certificate (needed before real users — use AWS Certificate Manager + ALB, or Certbot)
- Custom domain (point your domain's A record to the EC2 IP)
- Real email provider (replace Mailtrap with SendGrid or AWS SES for production email)
- Frontend deployment
