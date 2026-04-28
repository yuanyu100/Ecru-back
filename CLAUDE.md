# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

**Ecru** is an AI-powered wardrobe and outfit recommendation system. The backend is a Spring Boot multi-module Maven project; the frontend consists of two Vue 3 apps (user-facing and admin dashboard).

## Build & Run

### Backend

```bash
cd backend
mvn clean package -DskipTests       # Build all modules
mvn spring-boot:run -pl ecru-web    # Run the server (port 8081, context /api/v1)
mvn test -pl ecru-web               # Run tests
mvn test -pl ecru-web -Dtest=FooTest  # Run a single test class
```

### Frontend

```bash
cd frontend/user-app && npm install && npm run dev   # User app
cd frontend/admin-app && npm install && npm run dev  # Admin app
```

Both Vite apps proxy `/api/v1` → `http://localhost:8081`.

### One-command local startup (PowerShell)

```powershell
./scripts/start-local-dev.ps1   # Starts MinIO + backend + both frontends
./scripts/status-local-dev.ps1  # Check service status
./scripts/check-dev-env.ps1     # Validate environment
```

## Architecture

### Module Structure

| Module | Role |
|---|---|
| `ecru-common` | Shared utilities, base entities, exception handling, AI mapper interfaces |
| `ecru-user` | User auth (JWT), profile management, admin user CRUD |
| `ecru-clothing` | Clothing item CRUD, image upload to MinIO, tag management |
| `ecru-outfit` | Outfit advice generation, wear log, AI agent orchestration |
| `ecru-web` | Single deployable entry point; imports all other modules, holds `application.yml`, Spring Security config, datasource config |

`ecru-web` is the only runnable module. All other modules are libraries it assembles.

### Request Flow

```
Client → ecru-web (Spring Security + JWT filter)
       → Controller (ecru-user / ecru-clothing / ecru-outfit)
       → Service → Mapper (MyBatis Plus) → MySQL
                 → AI Service (LangChain4j) → Qwen3-VL / SiliconFlow APIs
                 → MinIO (image storage)
                 → PostgreSQL (pgvector, RAG knowledge base)
```

### Key Frameworks

- **Spring Boot 3.2 / Java 17**
- **MyBatis Plus 3.5.6** — ORM; uses logical delete (`isDeleted`), underscore-to-camelCase mapping, auto-fill for `createTime`/`updateTime`
- **LangChain4j 0.25.0** — LLM integration, RAG pipeline, AI agent workflows
- **Spring Security + JWT** — Stateless auth; token secret and expiry in `application.yml`
- **MapStruct 1.5.5** — DTO ↔ entity conversion
- **Knife4j 4.4.0** — Swagger UI at `/api/v1/doc.html`

### Data Sources (configured in `ecru-web/src/main/resources/application.yml`)

- **MySQL** `localhost:3306/ecru` — primary relational data
- **PostgreSQL** `localhost:5432/ecru-pg` — pgvector store for RAG embeddings
- **MinIO** `localhost:9000` — object storage for clothing images
- **Redis** — present in config but currently disabled

### AI / RAG Pipeline

Outfit advice uses a multi-step agent in `ecru-outfit`:
1. User query + style profile → LangChain4j agent
2. Agent calls tools: wardrobe lookup, weather API (Amap), vector similarity search on knowledge base
3. Qwen3-VL handles image understanding; SiliconFlow provides embeddings (dimension 1024)
4. Agent timeout: 600 s, max 50 steps

### UserStyleProfile Location

`UserStyleProfile` entity lives in `ecru-user` (not `ecru-outfit`). The outfit module accesses it via the user module's mapper/service, not a local entity.

## Configuration Notes

Sensitive values (API keys, DB passwords) are expected as environment variables or local `application-local.yml` overrides — do not hardcode them.

The `DataSourceConfig` in `ecru-web` wires the two datasources (MySQL + PostgreSQL) with separate `SqlSessionFactory` instances; MyBatis mapper packages are split by datasource.
