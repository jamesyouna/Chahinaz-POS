# Deployment plan — not yet production-ready

## Development PC

Install JDK 21, Maven and Docker Desktop. In PowerShell from this project folder:

```powershell
Copy-Item .env.example .env
notepad .env
. .\scripts\Load-DevEnv.ps1
docker compose --env-file .env up -d --wait postgres
docker compose --env-file .env ps
docker inspect --format '{{.State.Health.Status}}' chahinaz-pos-postgres
mvn test
mvn clean verify
mvn spring-boot:run
```

Replace both password placeholders in `.env` before starting. It is Git-ignored. For another PowerShell window, load it again with `. .\scripts\Load-DevEnv.ps1`. Check PostgreSQL and Flyway with:

```powershell
docker compose --env-file .env logs postgres --tail 30
docker exec chahinaz-pos-postgres psql -U chahinaz -d chahinaz_pos -c "SELECT version, description, success FROM flyway_schema_history;"
```

Database `chahinaz_pos` is bound to host port `5432` on `127.0.0.1`; container `chahinaz-pos-postgres` uses a persistent named volume. Stop with `docker compose --env-file .env stop postgres`; restart with `docker compose --env-file .env start postgres`. `docker compose --env-file .env down` removes the container but retains data. Do not use `down --volumes` as a routine stop command.

## Central production service

Host the Spring Boot API and PostgreSQL on always-on infrastructure reachable over HTTPS. Use managed PostgreSQL with private networking, a dedicated database user, encrypted secrets and automated backups. The development PC is not part of store operations. The customer website will later call only public API endpoints after separate website integration authorization. Keep the API and DB as one central source of truth.

## Store laptop

The future React POS build can be served by the API or as a separately hosted HTTPS frontend. Package a Windows launcher/shortcut that opens the POS URL in a locked-down browser profile; no IDE, Maven or npm is needed during normal operation. The backend and database stay central rather than on the laptop. A future installer must configure the service URL, install the launcher and provide update/repair shortcuts. Java is required on the backend host, not on the teller laptop when it only runs the web client.

## Updates and recovery

Before each update: back up PostgreSQL and verify the archive; rehearse the Flyway migration against a restored copy; then deploy the new JAR/frontend. Never run `DROP SCHEMA`, `ddl-auto=create` or demo seeding against production. Keep release artifacts and a rollback procedure. Restore a failed database to a new instance from encrypted backup/PITR, verify row counts and sample receipts, then repoint the API. Replacing a store laptop reinstalls only the client and does not erase central data.

## Backups

Use managed daily encrypted backups plus point-in-time recovery when available, retain offsite copies under a documented schedule, and test restoration regularly. Back up object-storage images separately with versioning. A backup is not considered valid until a restore succeeds in an isolated environment.

## Internet outage

V1 is online-first. The teller UI must visibly mark disconnection and block new sale completion. Offline sync is future work requiring durable local writes, idempotency, stock allocation and conflict resolution; no local-only sale should be silently treated as centrally posted.
