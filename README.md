# Chahina'z POS — Phase 1 foundation

This is a **new, separate** POS/backend project. The existing customer website has not been modified. The architecture is in [ARCHITECTURE.md](ARCHITECTURE.md); deployment planning is in [DEPLOYMENT.md](DEPLOYMENT.md).

## Current scope

Spring Boot 4.1.1 / Java 21, PostgreSQL, Flyway V1 migration, employee roles, bcrypt password hashing, session login, CSRF, Admin-only employee/settings endpoints, and audit records for those changes. This is **not yet a usable store POS**. Product management, sale posting, inventory, register shifts, reports and frontend are planned later phases.

## Development requirements

- JDK 21 and Maven 3.6.3+
- Docker Desktop with the Linux container engine running

In PowerShell in this project directory, run `Copy-Item .env.example .env`, then `notepad .env`. Replace both example passwords with different random values. The Admin password must have at least 16 characters. `.env` is ignored by Git. Compose reads it automatically; Spring does not, so run `. .\scripts\Load-DevEnv.ps1` in each PowerShell window before launching Spring Boot. The first Admin is created only when the employee table is empty. Set `POS_SECURE_COOKIE=true` behind HTTPS in production.

## Commands

```powershell
java -version
mvn -version
docker version
docker compose version
docker compose --env-file .env up -d --wait postgres
docker compose --env-file .env ps
docker inspect --format '{{.State.Health.Status}}' chahinaz-pos-postgres
. .\scripts\Load-DevEnv.ps1
mvn test
mvn clean verify
mvn spring-boot:run
```

The integration tests use disposable PostgreSQL containers and do not alter the development database. To inspect the Flyway migration after application startup:

```powershell
docker exec chahinaz-pos-postgres psql -U chahinaz -d chahinaz_pos -c "SELECT version, description, success FROM flyway_schema_history;"
```

The local database is `chahinaz_pos`, exposed only at `127.0.0.1:5432` by container `chahinaz-pos-postgres`. To stop it, run `docker compose --env-file .env stop postgres`; to see logs, run `docker compose --env-file .env logs postgres --tail 30`. The named volume preserves data when stopped.

Flyway runs before JPA validation. Visit `/login` for the initial form login. After login, `GET /api/auth/me` returns the signed-in user. Admin calls use the session cookie and CSRF token from the `XSRF-TOKEN` cookie; send it in `X-XSRF-TOKEN` for modifying requests. Use a REST client or the future admin UI. `POST /api/admin/employees` accepts `username`, `displayName`, `password`, and `role`. `PATCH /api/admin/employees/{id}/enabled` accepts a JSON boolean. `GET /api/admin/settings` lists settings; `PUT /api/admin/settings/{key}` updates one. Only allowlisted non-secret settings are accepted; monetary settings need stronger approval controls before store deployment.

## Limitations before Phase 1 can be called complete

Phase 1 is complete only after the full tests, clean build, live PostgreSQL startup, Flyway and application startup pass. Login throttling, password reset, stronger monetary setting controls, and production hardening remain before daily store use. Do not deploy to a store yet.
