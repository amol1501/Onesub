# OneSub Local Testing Guide

This guide validates the complete OneSub stack locally before Azure deployment.

## Prerequisites

- Java 21
- Maven
- Node.js 20+
- PostgreSQL 16

## Start PostgreSQL

```bash
brew services start postgresql@16
pg_isready -h localhost -p 5432
```

Create the local database and role if they do not already exist:

```bash
psql -d postgres -c "CREATE DATABASE onesub;"
psql -d postgres -c "CREATE ROLE onesub LOGIN PASSWORD 'onesub_dev_password';"
psql -d postgres -c "ALTER DATABASE onesub OWNER TO onesub;"
```

## Start Backend

```bash
cd OneSub/onesub-backend
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

The backend runs at `http://localhost:8080`.
Flyway validates and migrates V1 and V2 against PostgreSQL.

## Start Frontend

```bash
cd OneSub/onesub-frontend
npm install
npm run dev -- --host 127.0.0.1
```

The frontend runs at `http://127.0.0.1:5173`.
Vite proxies `/api` to `http://localhost:8080`.

## Automated Validation

Backend:

```bash
cd OneSub/onesub-backend
./mvnw clean verify
```

Frontend:

```bash
cd OneSub/onesub-frontend
npm test
npm run lint
npm run build
```

## Manual Smoke Checklist

1. Open `http://127.0.0.1:5173/`.
2. Register a user or log in.
3. Confirm protected routes redirect unauthenticated users to `/login`.
4. Open the dashboard.
5. Open the merchants page and add a custom merchant.
6. Add a monthly subscription.
7. Verify the next renewal date.
8. Record a manual paid, pending, or failed payment.
9. Confirm dashboard monthly and annual totals.
10. Confirm reporting pages.
11. Pause, resume, or cancel tracking.
12. Verify notification preferences.
13. Attempt to access another user's IDs and confirm 403/404 responses.

## Local Smoke Test Result

Validation date: 2026-11-22

- PostgreSQL 16: accepting connections
- Flyway: V1 and V2 validated and up to date
- Backend: started successfully on port 8080
- Frontend: HTTP 200 on port 5173
- Vite API proxy: `/api/v1/auth/csrf` returned HTTP 200
- Unauthenticated protected API: HTTP 401
- Existing-user login through Vite: successful
- Protected `/api/v1/users/me`: successful
- Merchant catalogue: returned 6 entries in the test environment
- Dashboard with live write smoke:
  - 1 active subscription
  - USD 19.99 monthly recurring expenditure
- Monthly report: USD 19.99
- Manual payment: `PAID` and 19.99 USD
- Disposable smoke merchant, subscription, payment, and notifications were removed after testing

No Azure deployment has been performed.
