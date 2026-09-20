# Chahina'z retail architecture

## System boundary and frontend choice

One centrally hosted Spring Boot REST service and one PostgreSQL database own products, inventory, sales, employees, register sessions, and reporting. The Windows store laptop uses a touchscreen web POS, later packaged as a desktop launcher. The manager interface uses the same API. The existing Cloudflare customer website remains a separate project and is not changed in this phase.

Use React + TypeScript + Vite for the POS and manager frontend. It supports fast touch interactions, predictable state, and a static build without making the business logic depend on the browser. No frontend exists yet; Phase 1 focuses on the backend. The browser cannot be trusted to authorize or calculate a completed sale.

Planned folders: `backend/src/main/java` for feature-based API, services, domain and repositories; `backend/src/main/resources/db/migration` for Flyway; `pos-web/` for the future teller and management UI; `docs/` for operations. The current Phase 1 prototype keeps the backend at the repository root and can move it under `backend/` when the frontend is introduced.

## Data model and relationships

| Entity | Key fields and relationships |
| --- | --- |
| Employee | UUID, username, bcrypt hash, role, enabled; referenced by sales, shifts, adjustments and audit events |
| Setting | Key/value, editor and time; business values require typed validation before production |
| Category | UUID, name, display order, active |
| Product | UUID, SKU, optional barcode, category, name, description, cost and sell prices, low stock, publication state, flags, image key, timestamps |
| Inventory balance | Product, on-hand quantity and version; locked when posting movements |
| Inventory movement | Product, signed quantity, reason, actor, reference, timestamp; append only |
| Register/shift | Terminal, teller, opening, expected, counted and difference by currency |
| Sale | UUID, receipt number, teller, shift, timestamp, totals, rate snapshot, status |
| Sale line | Sale, product reference, name/SKU/category and price snapshots, quantity, discount, line total |
| Payment | Sale, method, USD/LBP received, verification state, change by currency |
| Refund | Original sale/line, quantity, amount, reason, approver, inventory decision |
| Audit event | Actor, action, entity, ID, safe before/after values, timestamp; no secrets |

Foreign keys preserve history. Products with sales are archived, never physically deleted. Sale snapshots preserve historic names, prices, discounts and rates. A unique transaction UUID and receipt number provide idempotency and traceability.

## APIs

Private, session-authenticated examples: `GET /api/auth/me`; `POST /login`; `POST /logout`; `/api/admin/employees`; `/api/admin/settings`; later `/api/management/categories`, `/api/management/products`, `/api/management/products/{id}/image`, `/api/management/inventory/movements`, `/api/pos/catalogue`, `/api/pos/shifts`, `/api/pos/sales`, `/api/pos/sales/{id}/receipt`, `/api/management/refunds`, `/api/management/reports`, and `/api/management/exports/*.xlsx`. DTOs validate input; services own transactions and permissions.

Future public read-only endpoints: `GET /api/public/products`, `GET /api/public/products/{id}`, `GET /api/public/categories`, and immutable image URLs. Public DTOs include only ID, name, description, selling price, category, image URL, availability/stock status, featured and new-arrival flags. Draft/inactive products, cost, exact sensitive inventory, suppliers, employees, audit data and margins stay private. Public endpoints should use an explicit publication predicate, cache controls, pagination, and a safe CORS allowlist for the Cloudflare website.

## Authentication and permissions

Backend-managed login sessions use bcrypt password hashes, HTTP-only SameSite cookies, CSRF protection and HTTPS in production. Secure cookies are enabled in production. Phase 1 uses Spring Security form login. Add explicit login throttling/lockout before production. No default employee password is committed; first admin credentials come from environment variables and must be rotated after setup.

| Operation | Admin | Manager | Teller | Public |
| --- | --- | --- | --- | --- |
| Employees, roles, system settings | Yes | No | No | No |
| Product/create/publish | Yes | Yes | No | No |
| Inventory adjustments, refunds, reports | Yes | Yes | Limited by later policy | No |
| Normal sales and own shift | Yes | Yes | Yes | No |
| Published product read | Yes | Yes | Yes | Yes, safe DTO only |

Authorization lives in API/service checks, not just hidden controls. Manager publishing may later become a configurable permission. Critical changes append audit events in the same database transaction.

## Images

Store originals/optimized variants in private object storage such as Cloudflare R2; keep object keys and metadata in PostgreSQL. Validate MIME by decoding, dimensions and byte limit, strip metadata, generate optimized WebP/JPEG variants, scan uploads, and serve public variants from a controlled CDN hostname. Never trust client filenames or expose an upload bucket. A missing image uses a placeholder. Image replacement changes the key; old objects are retained temporarily for rollback/cleanup.

## Money and mixed cash

Use integer USD cents and integer LBP units for posted money; use `numeric(18,6)` for LBP-per-USD rate. Java uses `long` with checked arithmetic or `BigDecimal` for conversion; never `double`/`float`. Store the rate and rounding policy on every sale. Displayed USD equivalent is a calculation, not the source of record.

For mixed cash, convert the USD balance into LBP at the captured rate using `BigDecimal`, add received LBP, and compare exact values before rounding. If short, reject checkout and show remaining due. If overpaid, calculate change; whole USD dollars are offered where feasible, with LBP remainder. LBP notes are configurable (initially 5k–100k). If exact physical change is impossible, show an explicit cash-rounding adjustment and require teller acceptance under policy. Never silently discard a remainder. Tests must cover exact, short, mixed, overpaid, very large and rounding edge cases.

## Inventory and atomic completion

Product balances live centrally. `POST /api/pos/sales` takes a client-generated UUID idempotency key. Within one database transaction it locks relevant inventory rows in deterministic order, verifies stock and session, records sale/lines/payment/rate snapshots, appends inventory movements and audit, updates shift cash effects, and commits. Unique keys make retries safe. Failure rolls back everything. Two terminals racing for the last item cannot both sell it. Browser cart activity never deducts stock. Future website reservations need their own expiry/commit mechanism.

## Registers, reports and XLSX

Each sale belongs to an open register shift and teller. Expected USD/LBP starts with opening cash and changes only through recorded cash sales, cash refunds and cash in/out movements. Whish is reported separately and does not enter physical cash. At close, store counted values and differences by currency without overwriting expectations. Managers review immutable reconciliation history.

Reports aggregate posted sales, refunds, discounts, shift events and movements with explicit timezone `Asia/Beirut` and date bounds. No synthetic data appears in production reports. Product performance accounts for launch date. Apache POI will generate real `.xlsx` workbooks, streamed where needed, with separate detailed sales, summary, inventory, low stock, performance, reconciliation and employee metrics exports.

## Deployment, backup, outage and terminals

For production, host the central API and managed PostgreSQL on continuously running infrastructure; the development PC is not needed. The store laptop gets a packaged shortcut/launcher to the HTTPS POS frontend. A local PostgreSQL on one laptop would not satisfy shared website and multi-terminal stock without unsafe sync.

V1 is online-first: show connection state, stop new sale completion while disconnected, and never pretend a sale posted. A future offline design needs local durable queue, unique IDs, idempotent server replay, conflict policy, inventory allocation and careful recovery testing. Do not ship partial offline sync. Multiple terminals share the central API/database and row locking.

Take encrypted scheduled PostgreSQL backups plus tested point-in-time recovery where supported; retain offsite copies with an explicit retention schedule. Verify restores to a separate database regularly. Flyway migrations are additive and backed up before upgrades; never reset production data. Store-laptop replacement only re-installs the client and reconnects to the central service.

## Phases and hard-to-reverse decisions

1. Foundation: Spring Boot, PostgreSQL, migrations, sessions, roles, employees, typed settings and audit. Current work has started here.
2. Catalogue: categories, products, inventory, publishing, image pipeline and safe public DTOs.
3. Core POS: teller UI, cash/Whish, exact change, atomic sales, receipts.
4. Operations: shifts, hold/resume, refunds, voids, discounts and complete audit.
5. Management: real dashboards, history, reports and XLSX.
6. Deployment: hardened hosting, backup/restore drills, Windows installer and upgrades.
7. Website integration: only after explicit access/authorization; replace demo data with public API while preserving the existing website.

Hard-to-reverse decisions: central database placement, monetary units and exchange-rate snapshot rules, sale/receipt identity, inventory concurrency, public/private DTO boundary, object key lifecycle and any offline conflict policy. Decide these before live data exists. Business-specific settings remain configurable and typed.
