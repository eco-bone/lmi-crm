# LMI CRM — Redeploy Guide

This guide explains how to push code changes live using our GitHub Actions deployment pipeline.
You only need this after the one-time server setup in `DEPLOYMENT.md` is done.

---

## The Mental Model (Read This Once)

Your code travels through three places:

```
Your laptop (source code)  →  GitHub (shared copy)  →  EC2 server (running app)
src/main/java/...              main branch               crm-0.0.1-SNAPSHOT.jar
```

When you push to GitHub, the shared copy updates. But **the running server has no idea
any of this happened** — it's still running the old JAR.

To update the server, three things must happen:

1. **Build** — compile the latest source into a fresh JAR file
2. **Copy** — upload the new JAR to the EC2 server
3. **Restart** — stop the old app, start the new one

Previously, you did these three steps manually from your laptop. Now **GitHub Actions does
all three automatically** when you trigger a deployment from the GitHub UI.

---

## How Deployment Works Now

The file `.github/workflows/deploy.yml` defines our deployment pipeline. It runs on
GitHub's servers (not yours), triggered manually from the GitHub UI.

Here is what happens under the hood when you trigger a deploy:

```
GitHub Actions Runner (ubuntu-latest)
│
├── 1. Checkout code         ← pulls your latest commit from the branch
├── 2. Set up Java 17        ← installs Amazon Corretto JDK (same as production)
├── 3. Build JAR             ← runs: ./mvnw clean package -DskipTests
│                               produces: target/crm-0.0.1-SNAPSHOT.jar
│
├── 4. Copy JAR to EC2       ← uses scp over SSH to upload the JAR to:
│                               ec2-user@<EC2_HOST>:/home/ec2-user/app/
│
└── 5. Restart app on EC2    ← SSH in and runs:
                                sudo systemctl restart crm
                                sudo systemctl status crm
```

The EC2 host and SSH private key are stored as **GitHub Secrets** (not in the code).
You never touch them during a normal deploy.

---

## Prerequisites (One-Time Check)

Before your first deploy, confirm these exist:

- [ ] Your code is committed and pushed to GitHub
- [ ] The two GitHub Secrets are configured in the repo:
  - `EC2_SSH_KEY` — the private key (contents of `lmi-crm-backend-new-key.pem`)
  - `EC2_HOST` — the EC2 public IP address (e.g. `43.204.19.53`)
- [ ] The EC2 instance is running (check AWS Console → EC2 → Instances)
- [ ] The `crm` systemd service is set up on the server (covered in `DEPLOYMENT.md`)

> **Note on the EC2 IP:** if the instance was stopped and restarted in AWS, the public IP
> changes. Update the `EC2_HOST` secret in GitHub, and update `app.base-url` in
> `application-prod.properties` on the server (see "If the EC2 IP Changed" at the bottom).

---

## The Redeploy Workflow

### Step 1 — Commit and push your changes

On your laptop, in the project folder:

```bash
git add .
git commit -m "your descriptive commit message"
git push
```

The workflow runs against whatever is currently on GitHub. **If you forget to push,
the deploy will use the old code.**

### Step 2 — Trigger the deployment on GitHub

1. Go to the GitHub repository in your browser
2. Click the **Actions** tab
3. In the left sidebar, click **"Deploy to EC2"**
4. Click the **"Run workflow"** button (top right of the list)
5. Leave the branch as `main` (or select the branch you want to deploy)
6. Click the green **"Run workflow"** button

The workflow will appear in the list with a yellow spinning indicator — it's running.

### Step 3 — Watch the deployment

Click on the running workflow to see live logs for each step:

- **Checkout code** — should be instant
- **Set up Java 17** — ~15 seconds
- **Build JAR** — ~30–60 seconds; watch for `BUILD SUCCESS`
- **Copy JAR to EC2** — ~15–30 seconds (54 MB upload)
- **Restart app on EC2** — ~10 seconds; watch for `Active: active (running)`

If any step turns red, the deploy stopped there. Click the step to read the error.

### Step 4 — Verify the new code is live

From your browser:
```
http://<EC2_HOST>:8080/swagger-ui/index.html
```

Or from your terminal:
```bash
curl -I http://43.204.19.53:8080/swagger-ui/index.html
```

You should get `HTTP/1.1 200 OK`. If you added a new endpoint, it should appear in Swagger.

---

## TL;DR — The 3 Actions You Need

```
1. git push                          (your laptop)
2. GitHub → Actions → Deploy to EC2 → Run workflow
3. Watch the steps go green
```

Total time: ~2–3 minutes.

---

## What Each Piece Is, Briefly

### What is GitHub Actions?
A CI/CD service built into GitHub. You write a YAML file describing steps to run, and
GitHub runs them on a clean Linux machine whenever you trigger it. Our workflow file is
`.github/workflows/deploy.yml`.

### What are GitHub Secrets?
Encrypted variables stored in the repo settings, injected into the workflow at runtime.
We use two:
- `EC2_SSH_KEY` — the private SSH key that lets GitHub connect to your EC2 server
- `EC2_HOST` — the server's public IP address

They are never visible in logs or in the code. To view or update them:
GitHub → Settings → Secrets and variables → Actions.

### What is `workflow_dispatch`?
The trigger type in our `deploy.yml`. It means the workflow only runs when you manually
click "Run workflow" in the GitHub UI — it does NOT auto-deploy on every push. This is
intentional: you decide when to deploy.

### What is `systemd` / `crm.service`?
The process manager running on the EC2 server. It keeps the app alive, restarts it on
crash, and starts it when the server boots. The workflow SSH's in and runs
`sudo systemctl restart crm` to swap the old JAR for the new one.

### What is the JAR file?
A single packaged file containing all compiled Java code, all dependencies, and the
embedded Tomcat web server. Spring Boot bundles everything into one file. Your JAR is ~54 MB.

### What is `application-prod.properties`?
A config file that lives ONLY on the server (never in git). It holds production secrets:
database password, mail credentials, JWT secret, public URL. The app reads it on startup
when `--spring.profiles.active=prod` is set. You do not touch this file during a normal
deploy — only when secrets or the EC2 IP change.

---

## Manual Deploy (Fallback — If GitHub Actions Is Down)

If GitHub Actions is unavailable and you need to deploy immediately:

```bash
# On your laptop, in the project folder
./mvnw clean package -DskipTests
scp -i ~/.ssh/lmi-crm-backend-new-key.pem target/crm-0.0.1-SNAPSHOT.jar ec2-user@43.204.19.53:/home/ec2-user/app/
ssh -i ~/.ssh/lmi-crm-backend-new-key.pem ec2-user@43.204.19.53

# On the server
sudo systemctl restart crm && sudo systemctl status crm
```

This is exactly what the workflow does — just run manually from your laptop.

---

## Common Things You'll Want to Do On the Server

SSH in first:
```bash
ssh -i ~/.ssh/lmi-crm-backend-new-key.pem ec2-user@43.204.19.53
```

| What | Command |
|---|---|
| Watch live logs | `tail -f /home/ec2-user/app/logs/crm.log` |
| See last 100 log lines | `tail -100 /home/ec2-user/app/logs/crm.log` |
| Check if app is running | `sudo systemctl status crm` |
| Stop the app | `sudo systemctl stop crm` |
| Start the app | `sudo systemctl start crm` |
| See crash logs (startup failures) | `sudo journalctl -u crm -n 100 --no-pager` |

---

## Troubleshooting

### The "Build JAR" step fails with BUILD FAILURE

Your code has a compilation error. Click the step in GitHub Actions to read the error —
it will show the file name and line number. Fix the code, push again, and re-trigger.

### The "Copy JAR to EC2" step fails with "Permission denied (publickey)"

The `EC2_SSH_KEY` secret is wrong or malformed. Check:
- GitHub → Settings → Secrets → `EC2_SSH_KEY` — re-paste the full contents of the `.pem` file
- `EC2_HOST` secret matches the current public IP in AWS Console

### The "Restart app on EC2" step shows `Active: failed`

The new JAR started but crashed. SSH into the server and check the logs:

```bash
tail -200 /home/ec2-user/app/logs/crm.log
sudo journalctl -u crm -n 200 --no-pager
```

Common causes:
- DB credentials in `application-prod.properties` are wrong → app can't connect → exits
- A code change introduced a bug that crashes on startup
- Port 8080 is already in use (rare)

To roll back, SSH in and restore the backup (if you made one), then restart:
```bash
cp /home/ec2-user/app/crm-0.0.1-SNAPSHOT.jar.backup /home/ec2-user/app/crm-0.0.1-SNAPSHOT.jar
sudo systemctl restart crm
```

### The browser shows old behavior after deploy

- Hard-refresh: `Cmd+Shift+R` (Mac) or `Ctrl+Shift+R` (Windows)
- Confirm restart happened: `sudo systemctl status crm` — "Active since" timestamp should be recent
- Confirm the JAR was updated: on the server, `ls -lh /home/ec2-user/app/` — check the modified time

### Can't reach the server at all

- AWS Console → EC2 → confirm the instance is in "running" state
- Confirm the public IP hasn't changed (stop/start changes it unless you have an Elastic IP)
- If your laptop's IP changed, update the security group: AWS Console → EC2 → Security Groups →
  your SG → Edit inbound rules → SSH rule → Source → "My IP" → Save

---

## Optional: Backup Before Deploying

For large or risky changes, SSH in before triggering the deploy and save a backup:

```bash
cp /home/ec2-user/app/crm-0.0.1-SNAPSHOT.jar /home/ec2-user/app/crm-0.0.1-SNAPSHOT.jar.backup
```

If the deploy breaks the app, roll back instantly:
```bash
cp /home/ec2-user/app/crm-0.0.1-SNAPSHOT.jar.backup /home/ec2-user/app/crm-0.0.1-SNAPSHOT.jar
sudo systemctl restart crm
```

---

## If the EC2 IP Changed

If AWS Console shows a different IP than `43.204.19.53`:

1. **Update the GitHub Secret:** GitHub → Settings → Secrets and variables → Actions →
   `EC2_HOST` → Update with the new IP

2. **Update `application-prod.properties` on the server** so email links use the correct URL:

   ```bash
   ssh -i ~/.ssh/lmi-crm-backend-new-key.pem ec2-user@<NEW-IP>
   nano /home/ec2-user/app/application-prod.properties
   ```

   Change:
   ```
   app.base-url=http://43.204.19.53:8080
   ```
   to:
   ```
   app.base-url=http://<NEW-IP>:8080
   ```

   Save (`Ctrl+O` → Enter → `Ctrl+X`), then restart:
   ```bash
   sudo systemctl restart crm
   ```

**To avoid this permanently:** attach an Elastic IP to the instance (AWS Console → EC2 →
Elastic IPs → Allocate → Associate). It's free while the instance is running and the IP
never changes again.

---

## Quick Reference Card

| What | Where | How |
|---|---|---|
| Push code | Your laptop | `git push` |
| Trigger deploy | GitHub → Actions → Deploy to EC2 | Click "Run workflow" |
| Watch deploy logs | GitHub Actions UI | Click the running workflow |
| SSH into server | Your laptop | `ssh -i ~/.ssh/lmi-crm-backend-new-key.pem ec2-user@43.204.19.53` |
| Restart app | On the server | `sudo systemctl restart crm` |
| Check app status | On the server | `sudo systemctl status crm` |
| Watch live logs | On the server | `tail -f /home/ec2-user/app/logs/crm.log` |
| Verify app is up | Your laptop / browser | `http://43.204.19.53:8080/swagger-ui/index.html` |
| Update secrets | GitHub → Settings → Secrets | `EC2_SSH_KEY`, `EC2_HOST` |

---

## When NOT to Use This Guide

- **Database schema changes that delete data** — JPA's `ddl-auto=update` won't drop columns.
  If you remove a column from an entity, the column stays in the DB. Plan migrations separately.
- **Changing secrets** — edit `application-prod.properties` on the server and restart. No new JAR needed.
- **Changing the EC2 instance type / size** — that's an AWS Console operation, not a redeploy.
- **Adding a new config value the app reads** — it must go in `application-prod.properties` on the
  server, not just the Java code. The JAR does not contain production config.
