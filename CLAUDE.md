# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Overview

A self-service printer system (自助打印系统) with a Spring Boot backend and Nuxt 4 frontend. The `else/` directory contains an older Python/FastAPI prototype that should be ignored.

## Commands

### Backend (Spring Boot)

```bash
./gradlew bootRun              # Run development server on port 8080
./gradlew build                # Build the project
./gradlew test                 # Run tests
```

### Frontend (Nuxt 4)

```bash
cd web && bun run dev          # Run development server on port 3000
cd web && bun run build        # Build for production
cd web && bun run lint         # Run ESLint
cd web && bun run typecheck    # Run TypeScript check
```

## Architecture

### Backend Structure

- **Controller layer**: `src/main/java/.../controller/` - REST API endpoints
- **Service layer**: `src/main/java/.../service/` - Business logic
- **Mapper layer**: `src/main/java/.../mapper/` - MyBatis-Plus database access
- **Entity**: `src/main/java/.../entity/` - Database entities with Lombok
- **DTO**: `src/main/java/.../dto/` - Request/Response objects
- **Config**: `src/main/java/.../config/` - Spring config (Sa-Token, Security, CORS)

### Authentication

- Uses Sa-Token for session management (not JWT)
- Token stored in frontend localStorage, sent as `Authorization: Bearer <token>`
- Public endpoints: `/api/user/register`, `/api/user/login`, `/api/payment/notify`, `/api-docs/**`
- All other `/api/**` endpoints require authentication

### Frontend Structure

- `web/app/pages/` - Nuxt pages (file-based routing)
- `web/app/middleware/` - Route guards (auth, guest, admin)
- `web/api/` - API client functions wrapping `useApiRequest`
- `web/stores/` - Pinia stores (auth, user)
- `web/types/` - TypeScript type definitions

### API Response Format

All endpoints return `Result<T>` with structure:
```json
{ "code": 200, "message": "success", "data": {...} }
```

## Database

MySQL with tables: users, user_groups, files, orders, printers, payments, promotions, wallet_transactions, community_shares, likes. Uses MyBatis-Plus with auto-fill for `createdAt` fields and soft delete for users.

## External Integrations

- **CUPS**: Print server integration (configurable via `CUPS_HOST`, `CUPS_PORT`)
- **Qixiang Pay**: Payment gateway (configurable via `PAY_PID`, `PAY_KEY`)