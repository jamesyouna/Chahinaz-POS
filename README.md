# Chahina'z POS — Phase 1 foundation

This is a **new, separate** POS/backend project. The existing customer website has not been modified. The architecture is in [ARCHITECTURE.md](ARCHITECTURE.md); deployment planning is in [DEPLOYMENT.md](DEPLOYMENT.md).

## Current scope

Spring Boot 4.1.1 / Java 21, PostgreSQL, Flyway V1 migration, employee roles, bcrypt password hashing, session login, CSRF, Admin-only employee/settings endpoints, and audit records for those changes. This is **not yet a usable store POS**. Product management, sale posting, inventory, register shifts, reports and frontend are planned later phases.

## Development requirements

- JDK 21 and Maven 3.6.3+ (workspace-local copies were used for the initial compile; they are not installed system-wide)
- PostgreSQL, with an empty `chahinaz_pos` database and a least-privilege application user

Set `POS_DB_URL`, `POS_DB_USER`, `POS_DB_PASSWORD`, `POS_BOOTSTRAP_ADMIN_USERNAME`, and a random `POS_BOOTSTRAP_ADMIN_PASSWORD` of at least 16 characters in your local environment. The `.env.example` is a template; Spring does not load it automatically. Never commit a filled `.env` file. The first admin is created only when the employee table is empty; no demo credentials are provided or seeded. Set `POS_SECURE_COOKIE=true` behind HTTPS in production.

## Commands

```text
mvn test
mvn package
java -jar target/pos-0.1.0-SNAPSHOT.jar
```

Flyway runs before JPA validation. Visit `/login` for the initial form login. After login, `GET /api/auth/me` returns the signed-in user. Admin calls use the session cookie and CSRF token from the `XSRF-TOKEN` cookie; send it in `X-XSRF-TOKEN` for modifying requests. Use a REST client or the future admin UI. `POST /api/admin/employees` accepts `username`, `displayName`, `password`, and `role`. `PATCH /api/admin/employees/{id}/enabled` accepts a JSON boolean. `GET/PUT /api/admin/settings/{key}` manages settings. Only allowlisted non-secret settings are accepted; monetary settings need stronger approval controls before store deployment.

## Limitations before Phase 1 can be called complete

The Java application compiled and an executable JAR was packaged with tests skipped. `mvn test` did not complete because the Windows sandbox caused `javac` to throw `AccessDeniedException` while closing dependency JAR files. PostgreSQL was not available, so database-backed tests could not run. The auth flow and migration still need integration verification. Login throttling, password reset, stronger monetary setting controls, and production hardening remain. Do not deploy to a store yet.
