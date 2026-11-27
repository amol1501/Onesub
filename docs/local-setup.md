# Local Setup

## Prerequisites

- Java 21
- Maven 3.9+
- Node.js 20+
- Docker with Docker Compose (only for the local PostgreSQL service)

## Database

```bash
docker compose up -d postgres
docker compose logs -f postgres
```

The local database is `onesub`, with credentials from `.env.example`.

## Backend

```bash
cd onesub-backend
./mvnw spring-boot:run
```

## Frontend

```bash
cd onesub-frontend
npm install
npm run dev
```
