# OneSub Backend Validation Report

Validation date: 2026-11-22

## Executive Summary

The OneSub Spring Boot backend builds successfully and passed the current Maven test suite.
PostgreSQL connectivity and both Flyway migrations were verified against PostgreSQL 16.
The critical authentication, merchant, subscription, payment, dashboard, reporting, notification,
and cross-user authorization paths were exercised through HTTP APIs.

Status: **PASS**

## Environment

- Java: OpenJDK 21
- Spring Boot: 3.5.7
- Maven: 3.10.1
- PostgreSQL: 16.16, native local installation
- Database: `onesub`
- Database role: `onesub`
- Test database: H2 PostgreSQL mode

### Docker Compose Limitation

Docker and Docker Compose were not available in the current shell.
An attempt to install Docker Desktop was blocked because the environment required unavailable
sudo credentials. The supplied Docker Compose configuration was reviewed, and PostgreSQL
validation was performed using native PostgreSQL 16 instead.

## Build Validation

Command:

```bash
./mvnw clean verify
```

Result:

- BUILD SUCCESS
- 12 tests run
- 0 failures
- 0 errors
- Executable JAR generated successfully

## PostgreSQL and Flyway Validation

The backend started successfully against:

```text
jdbc:postgresql://localhost:5432/onesub
```

Observed:

- PostgreSQL 16.16 connection established
- 2 Flyway migrations successfully validated
- V1 initial schema applied
- V2 notification preferences applied
- Flyway history remained at version 2 on subsequent starts
- Hibernate validate phase passed
- Public schema contained 8 application tables plus Flyway history

## Authentication and Security

Verified:

- CSRF token retrieval
- New user registration
- BCrypt password-based login
- Server-managed JSESSIONID session
- Protected `/api/v1/users/me`
- Protected `/api/v1/**` endpoints
- Unauthenticated dashboard access returns HTTP 401
- Logout session invalidation behavior
- Principal-derived user identity rather than a frontend-supplied user ID

## Merchant API Validation

Verified through PostgreSQL:

- Global merchant catalogue listing
- Custom merchant creation
- Custom merchant retrieval
- Custom merchant isolation
- Used merchant deletion rejected with HTTP 400
- Cross-user custom merchant access returns HTTP 404

## Subscription API Validation

Verified:

- Subscription creation
- January 31 monthly renewal produced February 28/29 correctly
- Subscription retrieval
- Pause endpoint
- Resume endpoint
- Cancel endpoint
- Cross-user subscription retrieval returns HTTP 403

## Payment API Validation

Verified:

- Manual payment creation
- Manual payment update
- Payment deletion
- Cross-user payment/subscription listing returns HTTP 403
- Payments remain separate manual records; no gateway or automatic success behavior

## Dashboard and Reporting Validation

Verified:

- Active subscription count
- Monthly recurring expenditure grouped by currency
- Annual estimated expenditure grouped by currency
- Recent paid payment history
- Merchant breakdown
- Upcoming renewals
- Monthly report
- Yearly report
- Merchant-wise report

Live values confirmed that USD and INR totals are not combined.

## Notification Validation

Verified:

- Notification preference GET
- Notification preference PUT
- 7-day, 3-day, and 1-day reminder configuration
- Development logging email provider
- Scheduled reminder generation
- Notification claiming code uses a pessimistic database lock
- Retry status handling is implemented
- Database unique index was exercised with duplicate reminder inserts
- Duplicate count remained 1 after repeated insert attempts

## Ownership and Authorization

Cross-user HTTP validation used two separate authenticated users:

| Operation | First-user data | Second-user result |
|---|---|---|
| Subscription GET | User 1 subscription | 403 |
| Payment list | User 1 subscription | 403 |
| Custom merchant GET | User 1 custom merchant | 404 |

No frontend-supplied user ID is trusted; ownership checks use the authenticated principal.

## Fixes Applied During Validation

1. Replaced JPQL string enum comparisons with enum constants.
2. Added timezone validation during registration and profile updates.
3. Prevented pausing/resuming cancelled or expired subscriptions.
4. Prevented deletion of merchants still referenced by subscriptions.
5. Simplified notification preferences to a direct `user_id` primary-key mapping.
6. Verified and retained the reminder unique database constraint.

## Known Environment and Follow-Up Items

- Docker Compose could not be executed in this shell due to unavailable Docker/sudo credentials.
- Production PostgreSQL integration should be repeated in Docker or Azure in CI/CD.
- A real email provider is intentionally not configured for local development.
- Distribution-safe notification claiming should receive a multi-instance integration test in the
  final infrastructure environment.
