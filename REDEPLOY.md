# LMI CRM — Redeploy Guide (For Pushing Code Changes Live)

This guide is for someone new to Linux, AWS, and deployment. It explains
what happens when you push new code, and exactly what to do to make those
changes go live on the server.

You only need this guide AFTER the initial deployment is done (`DEPLOYMENT.md`
covers that one-time setup). This is the everyday workflow.

---

## The Mental Model (Read This Once)

Your code lives in three places:

```
Your laptop (source code)        →  GitHub (shared copy)        →  EC2 server (running app)
src/main/java/...                   main / deployment branch       crm-0.0.1-SNAPSHOT.jar
```

When you write code, it changes the source on your laptop. When you push
to GitHub, the shared copy updates. **But the running server has no idea
any of this happened.** It's still running the old JAR file you copied
weeks ago.

To update the running server, three things must happen:

1. **Build** — turn your latest source code into a fresh JAR file (on your laptop)
2. **Copy** — send the new JAR to the EC2 server (overwriting the old one)
3. **Restart** — tell the server to stop the old running app and start the new one

That's the whole loop. Everything below is just the exact commands to do those three steps.

---

## What You Need (One-Time Setup Check)

You should already have these from the initial deployment:

- [ ] SSH key file at `~/.ssh/lmi-crm-backend-new-key.pem`
- [ ] EC2 public IP address (currently: `43.204.19.53` — check AWS console if unsure)
- [ ] Java 17 installed on your laptop (`java -version` should work)
- [ ] Maven wrapper in the project (`./mvnw -v` should work from project root)

If any of these are missing, go back to `DEPLOYMENT.md` and finish the initial setup first.

> **Note on the EC2 IP:** if the EC2 instance was stopped and started in AWS,
> the public IP changes. Always check AWS Console → EC2 → Instances → your
> instance → "Public IPv4 address" before redeploying. If it changed, you'll
> also need to update `app.base-url` in `application-prod.properties` on the
> server (see "If the EC2 IP changed" section at the bottom).

---

## The Redeploy Workflow

### Step 1 — Make sure your code changes are saved and committed

On your laptop, in the project folder:

```bash
cd /Users/shubhankarbhanot/lmi_crm/lmi-crm
git status
```

- If it shows changes you haven't committed → commit them first (`git add .` then `git commit -m "your message"`)
- If it says "nothing to commit, working tree clean" → you're good

You don't strictly need to push to GitHub before redeploying, but it's a
good habit. If something breaks on the server, having the exact code in
GitHub means you can roll back.

### Step 2 — Pull the latest code (if working with a teammate)

If your collaborator made changes, get them onto your laptop before building:

```bash
git pull
```

If you're the only one who changed code recently, you can skip this.

### Step 3 — Build a new JAR

Still in the project folder, run:

```bash
./mvnw clean package -DskipTests
```

What this does, in plain English:
- `clean` — wipes the old `target/` folder so we start fresh
- `package` — compiles all your Java code into one big JAR file
- `-DskipTests` — skips running tests (faster build; the project has none yet anyway)

Wait ~30 seconds. You want to see at the end:

```
[INFO] BUILD SUCCESS
```

If you see `BUILD FAILURE`, something in your code is broken. Read the error,
fix it, and run the build again. **Do not proceed until the build succeeds.**

The new JAR is now at `target/crm-0.0.1-SNAPSHOT.jar`.

### Step 4 — Copy the new JAR to the EC2 server

Run this from the project folder (the path `target/...` is relative to where you are):

```bash
scp -i ~/.ssh/lmi-crm-backend-new-key.pem target/crm-0.0.1-SNAPSHOT.jar ec2-user@43.204.19.53:/home/ec2-user/app/
```

What this does:
- `scp` = "secure copy" — copies a file over SSH
- `-i ~/.ssh/lmi-crm-backend-new-key.pem` — use this private key to authenticate
- `target/crm-0.0.1-SNAPSHOT.jar` — the local file (your new JAR)
- `ec2-user@43.204.19.53:/home/ec2-user/app/` — destination: the server, in the `app/` folder

You'll see a progress bar:
```
crm-0.0.1-SNAPSHOT.jar    100%   54MB   3.4MB/s   00:15
```

This **overwrites** the old JAR on the server with the new one. The server's
running app is still using the OLD one in memory though — that's why we need step 5.

### Step 5 — Restart the app on the server

SSH into the server:

```bash
ssh -i ~/.ssh/lmi-crm-backend-new-key.pem ec2-user@43.204.19.53
```

Once you're connected (you'll see the Amazon Linux logo and `[ec2-user@ip-xxx ~]$`), run:

```bash
sudo systemctl restart crm
sudo systemctl status crm
```

What happens:
- `restart crm` — systemd stops the old running app, then starts the new one (using the JAR you just uploaded)
- `status crm` — shows you whether the new app started successfully

You want to see in the status output:
```
Active: active (running)
```

If you see `Active: failed` — something's wrong. Skip to the **Troubleshooting** section.

### Step 6 — Verify the new code is live

From your laptop browser:
```
http://43.204.19.53:8080/swagger-ui/index.html
```

Or from your laptop terminal:
```bash
curl -I http://43.204.19.53:8080/swagger-ui/index.html
```

You should get `HTTP/1.1 200 OK` (or a redirect). If your code changes added
a new endpoint, you should see it in Swagger now.

### Step 7 — Exit the SSH session

```bash
exit
```

That's it. The new code is live. Total time: ~2 minutes.

---

## TL;DR — The 4 Commands You Actually Need

Once you've done this a few times, this is all you'll remember:

```bash
# On your laptop, in the project folder
./mvnw clean package -DskipTests
scp -i ~/.ssh/lmi-crm-backend-new-key.pem target/crm-0.0.1-SNAPSHOT.jar ec2-user@43.204.19.53:/home/ec2-user/app/
ssh -i ~/.ssh/lmi-crm-backend-new-key.pem ec2-user@43.204.19.53

# On the server
sudo systemctl restart crm && sudo systemctl status crm
```

---

## What Each Piece Is, Briefly

If you want to understand what you're touching:

### What is `systemd`?
The "manager" running on the EC2 server. It's responsible for keeping your
app alive — starting it when the server boots, restarting it if it crashes,
running it in the background. You set it up once in the initial deployment;
from now on you just tell it `start`, `stop`, `restart`, or `status`.

### What is the JAR file?
A single packaged file containing all your compiled Java code, all
dependencies, and the embedded Tomcat web server. Spring Boot bundles
everything into one file so you can run the whole app with just
`java -jar yourapp.jar`. Your JAR is ~54 MB.

### What is `application-prod.properties`?
A config file that lives ONLY on the server (never in git). It contains
your production secrets: database password, mail credentials, JWT secret,
public URL. When the app starts with `--spring.profiles.active=prod`,
Spring Boot automatically reads this file from the same folder as the JAR
and overrides anything in the base config.

You don't touch this file during a normal redeploy. You only edit it if
secrets change (new DB password, new mail provider, EC2 IP changed, etc.).

### What is the `crm.service` file?
A small text file at `/etc/systemd/system/crm.service` on the server that
tells systemd how to run your app. It contains the exact `java -jar` command,
which user to run as, what to do if it crashes, and where to put the logs.
You set this up once and basically never edit it again.

### What is `scp`?
"Secure copy." Copies a file from your laptop to the server (or back) over
the same encrypted SSH connection. Same auth as SSH, just for files.

### What is `ssh`?
Encrypted remote terminal. You type commands on your laptop, they run on
the EC2 server. The `.pem` key file is what proves you're allowed in.

---

## Common Things You'll Want to Do

### Watch the live logs while the app is running

SSH in, then:

```bash
tail -f /home/ec2-user/app/logs/crm.log
```

You'll see every log line as it's written. Useful when debugging — make
a request from your laptop, watch the log on the server. Press `Ctrl+C`
to stop watching (this only stops the `tail` command, the app keeps running).

### See the last 100 log lines without following

```bash
tail -100 /home/ec2-user/app/logs/crm.log
```

### Check if the app is running

```bash
sudo systemctl status crm
```

Look for `Active: active (running)`.

### Stop the app entirely (rare — you almost never need this)

```bash
sudo systemctl stop crm
```

### Start the app again after stopping

```bash
sudo systemctl start crm
```

### See if anything is wrong with the app

If `status` says `failed` or the app keeps crashing:

```bash
sudo journalctl -u crm -n 100 --no-pager
```

This shows the last 100 log lines from systemd's perspective — useful when
the app crashes during startup before it can write to its own log file.

---

## Troubleshooting

### The build fails with "BUILD FAILURE"

Your code has a compilation error. Read the error message — it'll tell you
the file and line number. Fix the code and run `./mvnw clean package -DskipTests` again.

### `scp` fails with "Permission denied (publickey)"

Either the key file path is wrong, the IP is wrong, or the username is
wrong. Verify:
- Key exists: `ls -la ~/.ssh/lmi-crm-backend-new-key.pem` should show `-r--------` permissions
- IP is current: check AWS Console → EC2 → Instances
- Username is `ec2-user` (not `ec2-useruser` or anything else)

### `scp` hangs forever

Your laptop's public IP probably changed and the security group is blocking you.
Fix: AWS Console → EC2 → Security Groups → your SG → Edit inbound rules → SSH rule → Source → re-select "My IP" → Save.

### `systemctl restart crm` then `status` shows "failed"

The new JAR has a problem. Check the logs:

```bash
tail -200 /home/ec2-user/app/logs/crm.log
sudo journalctl -u crm -n 200 --no-pager
```

Common causes:
- DB credentials in `application-prod.properties` are wrong → app can't connect → exits
- Port 8080 is already in use (rare; means the old process didn't die)
- Code change introduced a bug that crashes on startup

If it's a code bug, the safest fix is to redeploy the previous working JAR.
Keep a backup before each deploy if you're nervous (see below).

### The browser shows the OLD behavior even after redeploy

- Hard-refresh your browser (`Cmd+Shift+R` on Mac, `Ctrl+Shift+R` on Windows)
- Confirm the server actually restarted: `sudo systemctl status crm` — the "Active: active (running) since ..." timestamp should be very recent
- Confirm you actually copied the new JAR: on the server, `ls -lh /home/ec2-user/app/` — the JAR's modified time should be from your latest scp

### I can't SSH or reach the server at all

- Confirm the instance is running: AWS Console → EC2 → Instances → state should be "running"
- Confirm the public IP hasn't changed (if it did, update everywhere you use it)
- Confirm your laptop's IP hasn't changed (security group with "My IP" can break this — see above)

---

## Optional: Backup the Old JAR Before Deploying

If you want a safety net (recommended for big changes), SSH in BEFORE step 4 and run:

```bash
cp /home/ec2-user/app/crm-0.0.1-SNAPSHOT.jar /home/ec2-user/app/crm-0.0.1-SNAPSHOT.jar.backup
```

Now if the new deploy breaks the app, you can roll back instantly:

```bash
cp /home/ec2-user/app/crm-0.0.1-SNAPSHOT.jar.backup /home/ec2-user/app/crm-0.0.1-SNAPSHOT.jar
sudo systemctl restart crm
```

---

## If the EC2 IP Changed

If the AWS Console shows a different public IP than `43.204.19.53`, do this once:

1. **Update this guide** — replace every `43.204.19.53` with the new IP (or remember to use the new one in commands)

2. **Update `application-prod.properties` on the server** so generated email links use the new public address:

   ```bash
   ssh -i ~/.ssh/lmi-crm-backend-new-key.pem ec2-user@<NEW-IP>
   nano /home/ec2-user/app/application-prod.properties
   ```

   Change the line:
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

**To avoid this entirely:** attach an Elastic IP to the instance (AWS Console → EC2 → Elastic IPs → Allocate → Associate to your instance). It's free as long as the instance is running, and the IP never changes again.

---

## Quick Reference Card

| What | Command | Where |
|---|---|---|
| Build new JAR | `./mvnw clean package -DskipTests` | Your laptop, project folder |
| Upload JAR to server | `scp -i ~/.ssh/lmi-crm-backend-new-key.pem target/crm-0.0.1-SNAPSHOT.jar ec2-user@43.204.19.53:/home/ec2-user/app/` | Your laptop |
| SSH into server | `ssh -i ~/.ssh/lmi-crm-backend-new-key.pem ec2-user@43.204.19.53` | Your laptop |
| Restart app | `sudo systemctl restart crm` | On the server |
| Check app status | `sudo systemctl status crm` | On the server |
| Watch live logs | `tail -f /home/ec2-user/app/logs/crm.log` | On the server |
| Exit server | `exit` | On the server |
| Verify app is up | `curl -I http://43.204.19.53:8080/swagger-ui/index.html` | Your laptop |

---

## When NOT to Use This Guide

- **Database schema changes that delete data** — JPA's `ddl-auto=update` won't drop columns. If you remove a column from an entity, the column stays in the DB. Plan migrations separately.
- **Changing secrets** — edit `application-prod.properties` on the server and restart. No new JAR needed.
- **Changing the EC2 instance type / size** — that's an AWS Console operation; no redeploy.
- **Adding a new environment variable the app reads** — needs to go in `application-prod.properties`, not just the code.
