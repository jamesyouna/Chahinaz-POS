# Chahina'z POS — Phase 4 backend

This is a **new, separate** POS/backend project. The existing customer website has not been modified. The architecture is in [ARCHITECTURE.md](ARCHITECTURE.md); deployment planning is in [DEPLOYMENT.md](DEPLOYMENT.md).

## Current scope

Spring Boot 4.1.1 / Java 21, PostgreSQL, Flyway migrations, employee roles, bcrypt password hashing, session login, CSRF, catalogue, inventory, sales, register sessions, held sales, returns, controlled discounts, price overrides, manager approvals, and audit records. The final POS frontend and advanced reporting remain future work.

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

## Phase 3 sales engine

Flyway V3 adds `sale`, `sale_item`, and `payment`, a receipt number sequence, and a sale reference on inventory movements. `POST /api/pos/sales` creates an OPEN sale for the authenticated employee. Add items with `POST /api/pos/sales/{id}/items`, update with `PUT /api/pos/sales/{id}/items/{itemId}`, remove with `DELETE` at the same item URL, and complete with `POST /api/pos/sales/{id}/complete`. `GET /api/pos/sales/{id}` returns a receipt DTO; `GET /api/pos/sales?page=0&size=25` returns paginated history. Tellers see their own sales while managers and admins see all sales.

Items snapshot SKU, name, and unit price when first added. The server calculates every line and total. OPEN sales do not affect inventory. Completion reads `currency.usd_to_lbp`, snapshots it, locks all products in UUID order, verifies stock, records payments, deducts stock, creates linked `SALE` inventory movements, completes the sale, and audits it in one database transaction. Completed sales cannot be edited. The stable lock order and pessimistic product locks prevent two cashiers from selling the same final unit.

Completion accepts `paidUsd`, `paidLbp`, `whishUsd`, and `whishManuallyConfirmed`; omitted amounts are zero. USD and Whish accept at most two decimal places. LBP is an integer amount. The total USD value is converted to LBP using the transaction rate and rounded to the nearest 1 LBP with `HALF_UP`. Each USD payment component is converted the same way. Insufficient value is rejected. Change is deterministic: the largest whole USD amount is returned first, and the exact remaining integer LBP amount is returned as `changeLbp`. This does not assume that every possible remainder matches a physical denomination; denomination guidance belongs in the future cashier UI. Whish is stored as `WHISH_MANUAL` only after the cashier explicitly confirms it; no external verification is claimed.

Example completion request: `{ "paidUsd": 5.00, "paidLbp": 452500, "whishUsd": 0, "whishManuallyConfirmed": false }`. The receipt response includes receipt number, timestamps, cashier display name, immutable item snapshots, totals, exchange rate, payment components, change, and note.

## Phase 4 store operations

Flyway V4 adds register sessions, manager approvals, immutable return records, sale lifecycle details, discount and override snapshots, and return references on inventory movements. Existing V1–V3 migrations remain unchanged.

### Register lifecycle

All authenticated store roles can use `POST /api/pos/registers/open`, `GET /api/pos/registers/current`, `GET /api/pos/registers`, and `POST /api/pos/registers/{id}/close`. Only one register may be open per employee. Opening and closing cash must be nonnegative. Closing snapshots expected cash from completed cash sales minus cash refunds, actual cash, and the difference in USD and LBP. A teller needs an open register before completing a cash sale; Whish-only sales do not require a cash drawer.

Example requests:

```json
{"openingCashUsd":100.00,"openingCashLbp":5000000}
{"actualCashUsd":142.00,"actualCashLbp":6500000,"note":"Counted twice"}
```

### Sale lifecycle and receipts

An `OPEN` sale can be held at `POST /api/pos/sales/{id}/hold`, listed at `GET /api/pos/sales/held`, resumed at `POST /api/pos/sales/{id}/resume`, or voided with a reason at `POST /api/pos/sales/{id}/void`. Held sales reserve no stock and create no revenue. Only `OPEN` and `HELD` sales may be voided. A completed sale remains immutable and corrections use returns.

Receipts preserve the cashier name, item name and SKU, original and effective unit prices, line discounts, override approval, subtotal, combined discounts, final total, payment breakdown, exchange rate, change, void information, and linked returns.

### Discounts, overrides, and approvals

Apply a line discount with `POST /api/pos/sales/{saleId}/items/{itemId}/discount`, a whole-sale discount with `POST /api/pos/sales/{saleId}/discount`, and a price override with `POST /api/pos/sales/{saleId}/items/{itemId}/price-override`. Amounts use two-decimal USD arithmetic and cannot make a line or sale negative. Price overrides affect only the sale item and retain its original price.

Set `pos.teller_discount_max_percent` through the existing Admin settings endpoint to define teller authority from 0 through 100. Discounts above that percentage and all teller price overrides require a single-use approval created by a signed-in Manager or Admin at `POST /api/management/approvals`. The approval is bound to the requesting employee, operation, and sale; a client-supplied boolean cannot authorize an action.

Approval example:

```json
{"operationType":"PRICE_OVERRIDE","requestingUsername":"teller1","targetType":"sale","targetId":"00000000-0000-0000-0000-000000000000","reason":"Damaged package"}
```

### Returns and refunds

Managers and Admins create returns at `POST /api/management/sales/{saleId}/returns`. The original completed sale is never rewritten. Each return identifies its original sale items, quantities, processor, reason, refund method, and time. Refunds use the item total snapshot and the sale's exchange rate snapshot. Returned quantity cannot exceed the unreturned sold quantity. Restockable items restore stock and create linked `RETURN` inventory movements; damaged or otherwise non-restockable items do not change sellable stock.

Cash refunds require an open register. A Whish refund uses `WHISH_MANUAL` and requires `whishManuallyRecorded: true`; the system records the external action and does not claim automatic verification.

```json
{"items":[{"saleItemId":"00000000-0000-0000-0000-000000000000","quantity":1,"restockable":true}],"refundMethod":"USD_CASH","whishManuallyRecorded":false,"reason":"Customer return"}
```

Run `mvn clean verify` with Docker Desktop running to execute all PostgreSQL integration and security tests for Phases 1–4.

## Phase 5 management reporting

Manager and Admin reporting endpoints are under `/api/management/reports`; tellers are denied by the existing management security rule. All endpoints accept optional inclusive `from` and `to` ISO dates, default to the latest 30 store-local dates, and reject reversed ranges or ranges longer than 366 days. `POS_STORE_TIMEZONE` configures calendar boundaries and defaults to `Asia/Beirut`. Admins may also validate and store the `pos.store_timezone` business setting; restart the application after changing the runtime timezone.

Available reports are `/dashboard`, `/sales?grouping=hourly|daily|weekly|monthly`, `/products?sort=units|revenue|profit|lowest`, `/categories`, `/inventory`, `/payments`, `/registers`, `/returns`, `/discounts`, `/price-overrides`, `/voids`, and paginated `/inventory-movements?page=0&size=25`. Inventory accepts `stockStatus=out|low|approaching|in`, `categoryId`, and `active` filters. XLSX downloads are available at `/exports/sales`, `/exports/products`, `/exports/inventory`, `/exports/registers`, `/exports/returns`, `/exports/discounts`, and `/exports/overrides` with the same date parameters.

Metric definitions:

- Gross sales: completed-sale effective prices before discounts (`subtotal_usd`).
- Net sales: completed-sale final totals after discounts and before returns.
- Refunds: USD refunds plus LBP refunds converted using each original sale's exchange-rate snapshot.
- Net revenue after refunds: net sales minus refunds.
- COGS: item cost snapshots for sold quantities minus cost snapshots for returned quantities.
- Gross profit: net revenue after refunds minus adjusted COGS. Gross margin is gross profit divided by net revenue after refunds; it is null when the denominator is zero.
- Current inventory value: current product cost multiplied by current sellable stock. It is separate from historical COGS.
- Payment totals retain USD cash, LBP cash, and manually confirmed Whish in separate fields. LBP is never silently added to USD.

V5 snapshots product cost and category ID/name on every new sale item. This keeps profit and category history stable after catalogue changes. Existing rows are migration-filled from their current product/category values at upgrade time, which is the most accurate history available for sales created before snapshots existed. Returned revenue and cost are recognized in the report period containing the return.

Sale history now uses the application-owned page shape: `content`, `page`, `size`, `totalElements`, and `totalPages`. XLSX exports use Apache POI, numeric cells for amounts and counts, restrained formatting, and neutralize leading formula characters in user-controlled text.

## Phase 6 POS frontend

The React, TypeScript, and Vite application is in `frontend/`. It uses the Spring Boot session cookie and `XSRF-TOKEN` cookie; the centralized client sends credentials and `X-XSRF-TOKEN` on modifying requests. Passwords and authentication tokens are never stored in browser storage. HTTP 401 responses return the employee to login, 403 responses are presented as authorization errors, and network failures never produce a local completed sale.

Development commands, in separate terminals:

```powershell
. .\scripts\Load-DevEnv.ps1
mvn spring-boot:run
cd frontend
npm install
npm run dev
```

Vite proxies `/api`, `/login`, and `/logout` to `http://localhost:8080`, preserving same-origin cookie and CSRF behavior without enabling broad CORS. Production assets are generated with `npm run build` in `frontend/dist`; Phase 7 will package those assets with the backend so the store does not depend on the Vite development server.

The application provides a branded login, role-aware shell, touchscreen Sell workspace, backend-authoritative cart, held-sale resume flow, register opening and closing, USD/LBP/mixed/Whish checkout, receipt history and 80mm browser print styles, product management, inventory status, management KPIs, XLSX downloads, and Admin employee status controls. Teller navigation excludes cost, profit, reporting, catalogue management, and employee administration screens; backend authorization remains authoritative.

Frontend verification:

```powershell
cd frontend
npm test
npm run build
```
