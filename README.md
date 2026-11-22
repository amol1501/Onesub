# OneSub

OneSub is a manual multi-merchant subscription tracking application. It does not charge users, access merchant accounts, store card details, or automatically cancel merchant subscriptions.

## Repository Structure

- `onesub-backend/` - Java 21 Spring Boot REST API
- `onesub-frontend/` - React TypeScript Vite application
- `onesub-infrastructure/` - Azure and Nginx deployment configuration
- `docs/` - Architecture, database, API, security, and deployment documentation

## Local Development

1. Start PostgreSQL:
   ```bash
   docker compose up -d postgres
   ```
2. Start the backend:
   ```bash
   cd onesub-backend
   ./mvnw spring-boot:run
   ```
3. Start the frontend:
   ```bash
   cd onesub-frontend
   npm install
   npm run dev
   ```

## Required Environment Variables

See `.env.example`. Production credentials must be supplied through Azure Key Vault-backed environment variables.
