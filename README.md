# Chahina'z POS — Phase 2 backend foundation

This is a **new, separate** POS/backend project. The existing customer website has not been modified. The architecture is in [ARCHITECTURE.md](ARCHITECTURE.md); deployment planning is in [DEPLOYMENT.md](DEPLOYMENT.md).

## Current scope

Spring Boot 4.1.1 / Java 21, PostgreSQL, Flyway migrations, employee roles, bcrypt password hashing, session login, CSRF, catalogue, inventory movements, image storage, and audit records. This is **not yet a usable store POS**. Sales, checkout, payments, shifts, reports, and the frontend are outside Phase 2.

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

## Phase 2 catalogue and inventory

Flyway V2 adds `category`, `product`, and `inventory_movement`. Each product has unique SKU, optional unique barcode, category, USD `numeric(12,2)` cost and selling prices, stock, threshold, publication status, flags, and an optional image key. Products start as `DRAFT`, even with initial stock. Only active `PUBLISHED` products in active categories appear in the public API. There is no automatic website sync yet.

`POST /api/management/products` accepts `{ "product": { "sku": "ABC-1", "barcode": null, "name": "Item", "description": "", "categoryId": "<uuid>", "costPriceUsd": 0.75, "sellingPriceUsd": 2.50, "lowStockThreshold": 2, "featured": false, "newArrival": false }, "initialStock": 5 }`. `PUT /api/management/products/{id}` accepts the inner `product` object without stock. Stock changes use dedicated transaction endpoints and each creates a movement and audit event. Initial stock creates an `INITIAL_STOCK` movement when greater than zero. A pessimistic database lock prevents concurrent adjustments from silently overwriting stock; negative results are rejected. The `SALE` movement type is reserved for the later sales phase and has no management endpoint.

Management endpoints require ADMIN or MANAGER: `GET/POST /api/management/categories`, `PUT /api/management/categories/{id}`, `GET/POST /api/management/products`, `GET/PUT /api/management/products/{id}`, `PATCH /api/management/products/{id}/publication` with JSON string `"DRAFT"`, `"PUBLISHED"`, or `"INACTIVE"`, `PATCH /api/management/products/{id}/active` with JSON boolean, `POST /api/management/products/{id}/image` with multipart field `file`, `POST /api/management/products/{id}/restock` with positive `quantity`, `POST /api/management/products/{id}/adjust` with signed `quantityChange` and required `reason`, `GET /api/management/products/{id}/movements`, `GET /api/management/products/low-stock`, and `GET /api/management/products/out-of-stock`. Categories are deactivated rather than deleted. Management product responses include internal cost and stock details; do not expose these endpoints to customers.

Anonymous read-only endpoints are `GET /api/public/categories`, `GET /api/public/products`, `GET /api/public/products/{id}`, and `GET /api/public/images/{key}`. Public product DTOs contain ID, SKU, name, description, category ID/name, selling price, image URL, in-stock boolean, featured, and new-arrival flags. They exclude cost, exact stock, employee, audit, and movement data. Teller (as well as ADMIN/MANAGER) can read `GET /api/pos/categories` and `GET /api/pos/products`; this view includes active in-store products regardless of website publication, without internal costs. No POS write or sale endpoint exists.

Images accept JPEG or PNG up to 5 MB. The server checks actual image content and dimensions, scales large images to 1600 pixels on the longest side, re-encodes them, and saves UUID filenames under `POS_IMAGE_DIR` (default `./data/product-images`). The database stores only the image key. Public image URLs are served only while a published active product in an active category references the key. `data/` and `.env` are Git-ignored. For a different local path, set `POS_IMAGE_DIR` in `.env`; use an external directory for persistent deployments. Storage can later be replaced with object storage without changing the product schema. Replaced local image files are retained; an image cleanup policy is future work.

Run `mvn clean verify` with Docker Desktop running. The Phase 2 integration tests use disposable PostgreSQL containers and verify V2 migration, constraints, roles, publication, public DTOs, stock transactions, images, and audit records. Run the app with `. .\scripts\Load-DevEnv.ps1` followed by `mvn spring-boot:run`. Login throttling, password reset, production image storage/backups, and deployment hardening remain before daily store use.
