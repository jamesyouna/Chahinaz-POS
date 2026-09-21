# Chahina'z POS production operation

## Architecture

Docker Desktop runs two restartable containers: PostgreSQL 16 with a named persistent volume, and the Spring Boot application. Spring Boot serves the compiled React files and all `/api` routes. PostgreSQL is never published to the LAN. The default application port binds only to `127.0.0.1`.

## Developer build

Prerequisites: Java 21, Maven 3.9+, Node 24/npm 11, Docker Desktop.

```cmd
cd /d C:\path\to\Chahinaz-POS
npm --prefix frontend test
mvn clean verify
docker compose --env-file .env.production -f compose.production.yaml build
```

Maven runs `npm ci` and `npm run build`, copies `frontend/dist` into `BOOT-INF/classes/static`, and produces `target/pos-0.1.0-SNAPSHOT.jar`. Generated `dist`, `node_modules`, logs, data and secrets stay outside Git.

## First store installation

1. Install Docker Desktop with WSL 2 and enable **Start Docker Desktop when you sign in**.
2. Copy the project release folder to `C:\ChahinazPOS`.
3. Copy `.env.production.example` to `.env.production`. Replace both password placeholders with different long random passwords. Keep this file local and out of OneDrive/Google Drive. Use `POS_DB_NAME=chahinaz_pos`, `POS_DB_USER=chahinaz_pos_app`, `POS_BOOTSTRAP_ADMIN_USERNAME=admin`, and a release image value such as `POS_VERSION=0.1.0`. The launcher rejects known test identities and placeholder credentials.
4. Run `docker compose --env-file .env.production -f compose.production.yaml build` once as the installer/developer.
5. Double-click `Start-ChahinazPOS.bat`. Flyway creates or upgrades the database without deleting existing data.
6. Sign in with the externally configured bootstrap Admin, create the permanent Admin account if required, then remove the bootstrap password from `.env.production` after confirming another Admin works. Existing bootstrap accounts are not recreated or reset.

PostgreSQL initialization variables only create a database and role when its data directory is empty. Changing those values cannot rename an existing database or role in an initialized volume; create the production database and role explicitly when promoting an existing test installation.
7. Test a sale, browser receipt printing, register close, backup, and a restore into `chahinaz_pos_restore`.
8. Optionally run `powershell -ExecutionPolicy Bypass -File scripts\Install-Automation.ps1 -BackupTime 02:00` from an Administrator PowerShell.

## Daily store use

1. Start the PC and Docker Desktop.
2. Double-click **Start-ChahinazPOS** if it did not open automatically.
3. Log in, open the register, sell normally, then close the register.
4. Use the browser print dialog for the 80mm receipt printer.

`Stop-ChahinazPOS.bat` stops the containers safely. It never deletes the database volume.

## Backups

`scripts\Backup-ChahinazPOS.ps1` uses PostgreSQL `pg_dump -Fc`, verifies the archive with `pg_restore -l`, and only then reports success. It retains 30 newest daily archives and 12 monthly archives. The first backup each month is copied to `monthly`. The scheduled task runs daily at the configured low-activity time.

Set `POS_BACKUP_DIR` to a local directory such as `C:\ChahinazPOS\Backups`. A separate OneDrive or Google Drive sync may copy that directory off-site. Never put `.env.production` in the synced folder. Cloud outages do not affect POS operation.

Manual verification:

```powershell
powershell -ExecutionPolicy Bypass -File scripts\Backup-ChahinazPOS.ps1
powershell -ExecutionPolicy Bypass -File scripts\Verify-Backup.ps1 -Backup C:\ChahinazPOS\Backups\daily\BACKUP.dump
```

## Disaster recovery

Test first with:

```powershell
powershell -ExecutionPolicy Bypass -File scripts\Restore-ChahinazPOS.ps1 -Backup C:\path\backup.dump -TargetDatabase chahinaz_pos_restore
```

The script requires typing `RESTORE`. Production replacement is blocked unless `-AllowProduction` is explicitly supplied. Stop the application and take another backup before an approved production restore. After restore, verify Flyway history, employee login, sales, inventory, registers and audit records before reopening the store.

## Logs and failures

Application logs are written to `logs\chahinaz-pos.log`, rotate at 10 MB, retain 30 files, and cap at 500 MB. Passwords, manager credentials and session IDs are not logged. The launcher detects missing Docker, an existing healthy instance, and startup timeout. Use `docker compose --env-file .env.production -f compose.production.yaml logs` for container failures.

`GET /health` returns only `{"status":"UP"}` after a successful database query. It exposes no credentials or internals.

## Trusted LAN terminals

The default is single-PC and loopback-only. A future trusted terminal may use the host PC's Spring Boot address after firewall, TLS and bind-address configuration. Never publish PostgreSQL port 5432. Cashier terminals connect only to the application. HTTPS must be configured before setting `POS_SECURE_COOKIE=true` for LAN use.

## Remove automation

Run `powershell -ExecutionPolicy Bypass -File scripts\Uninstall-Automation.ps1`. This removes scheduled tasks only; it preserves data, images, logs and backups.
