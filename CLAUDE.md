# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

Web application for managing the lifecycle of tire assets for a logistics company (Ingeniería en Sistemas de Información final thesis project, UTN FRSF). Spanish is the working language for domain terms, commit messages, and docs.

## Repository layout

This is two independent projects living in one repo root, not a single build:

- `backend/gestion-neumaticos/` — Spring Boot 4.1.1 (Java 21) API, built with Maven. Has its own `Dockerfile`.
- `frontend/gestion-neumaticos/` — a standalone Vite + React 19 + TypeScript app (its own nested git repo — note the `.git` inside `frontend/gestion-neumaticos/`, currently untracked from the root repo's perspective). Not a monorepo/workspace setup.
- `docs/Entidades del dominio/` — PlantUML (`.puml`) entity diagrams, one per intended Modulith module (`dominio-neumaticos-svc.puml`, `dominio-conductores-svc.puml`, `dominio-usuarios-svc.puml`, `dominio-proveedores-svc.puml`, `dominio-servicios-reparacion-svc.puml`, `dominio-trazabilidad-svc.puml`, `dominio-unidades-transporte-svc.puml`, `dominio-centros-almacenamiento-svc.puml`). These are the design source of truth for entities/fields before code exists for a module — check the matching diagram before modeling a module's JPA entities.
- `infra/` — currently empty, reserved for future use.

Root `compose.yaml` wires up `backend` (built from `backend/gestion-neumaticos/`), `frontend` (built from `frontend/`, served on port 3000), `postgres` (18-alpine, port 5432, healthcheck-gated), and `pgadmin` (port 5050). The root `.env` (gitignored) supplies `DB_NAME`, `DB_USER`, `DB_PASSWORD`, `PGADMIN_EMAIL`, `PGADMIN_PASSWORD`, `SPRING_PROFILES_ACTIVE`, `FRONTEND_URL` (for CORS), and `JWT_SECRET` to these services. `README.Docker.md` is the unmodified Spring Boot Docker Compose scaffolding doc. The backend also has its own standalone `backend/gestion-neumaticos/compose.yaml` (a bare `postgres` service only) for running the API outside the root compose stack during local dev.

Because `frontend/gestion-neumaticos/` is a separate git repository nested inside this one, commands run from the root (`git status`, `git add`, etc.) will not see changes inside it — `cd` into it and use git there for frontend-only commits.

## Backend (`backend/gestion-neumaticos/`)

Run all Maven commands from `backend/gestion-neumaticos/` using the wrapper (`./mvnw` or `mvnw.cmd` on Windows).

```
./mvnw spring-boot:run        # run the app
./mvnw test                   # run all tests
./mvnw test -Dtest=ClassName  # run a single test class
./mvnw clean package          # build a jar
```

Architecture/stack notes:
- Package root: `ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos` (note: underscore, not hyphen — Maven's default package name from `gestion-neumaticos` is invalid Java, so it was changed; see `HELP.md`).
- **Spring Modulith** is a dependency (`spring-modulith-starter-core/jpa/insight`, `spring-modulith-runtime`) — the intended architecture is a modular monolith with explicit module boundaries enforced by Modulith conventions (top-level packages under the root = modules). Planned domain modules, each with an empty package directory reserved and a matching design diagram in `docs/Entidades del dominio/`: `neumaticos`, `conductores`, `usuarios`, `proveedores`, `reparacion`, `trazabilidad`, `unidades_transporte`, `almacenamiento`. `common` is a planned shared/cross-cutting module. Respect this module-per-package boundary when adding code — don't reach into another module's package directly; go through its public API once one exists.
- Cross-cutting infrastructure already implemented, outside the domain modules: `config/` (`SecurityConfig`, `CorsConfig`), `exception/` (a global `@ControllerAdvice`-style `ControllerAdvisor` plus a set of custom exceptions — `EntidadDuplicadaException`, `ForbiddenException`, `OperacionNoPermitidaException`, `PermisosDenegadosException`, `UnauthorizedException`, `UsuarioNoEncontradoException` — new domain-specific exceptions should follow this same pattern and naming in Spanish), and `security/` (currently empty, reserved for JWT auth wiring).
- `SecurityConfig` currently permits `/actuator/health`, `/actuator/info`, `/swagger-ui/**`, `/swagger-ui.html`, `/api-docs/**` and requires authentication on everything else, with CSRF disabled (stateless API) and CORS delegated to `CorsConfig`. No authentication mechanism (JWT filter, `UserDetailsService`, etc.) is wired up yet, so any new controller endpoint will be rejected with 401 until that's added — build out `security/` first if you need working auth.
- `CorsConfig` allows only the single origin from `app.cors.frontend-url` (`FRONTEND_URL` env var, default `http://localhost:3000`), all standard HTTP methods, all headers, and credentials.
- Persistence: Spring Data JPA + PostgreSQL in normal profiles, H2 (`h2console`) available at runtime for local/dev use. Flyway (`flyway-database-postgresql`) manages schema migrations from `src/main/resources/db/migration` (currently empty — no migrations committed yet, so Hibernate has nothing to validate against for real entities yet).
- `spring.modulith.events.jpa.schema-initialization.enabled=true` is set in `application.properties` specifically so Modulith's event-publication registry table can self-create in the absence of Flyway migrations — remove/revisit this once real migrations exist and Hibernate ddl-auto is validate-only in practice.
- `spring-boot-docker-compose` is on the classpath as an optional runtime dependency, so running the app locally can auto-start `compose.yaml` if Docker is available.
- API docs via springdoc-openapi at `/swagger-ui.html` and `/api-docs` (paths configured explicitly in `application.properties`, not the defaults).
- Actuator exposes only `health` and `info` over the web.
- Lombok is available for boilerplate reduction.
- Test dependencies include `spring-modulith-starter-test` and `spring-restdocs-mockmvc`/`asciidoctor-maven-plugin` (restdocs generation wired into the Maven build) — new module tests should use Modulith's test support for module-boundary verification, and API tests intended to produce documentation should use Spring REST Docs conventions already set up in the POM.
- MapStruct (`mapstruct` + `mapstruct-processor`, with `lombok-mapstruct-binding` so it coexists with Lombok's annotation processor) is configured via `MapstructConfig` in `config/` for entity↔DTO mapping — no mappers exist yet, but new ones should be MapStruct `@Mapper` interfaces rather than hand-written conversion code.
- No controllers or JPA entities exist yet for the domain modules — only the cross-cutting scaffolding above. Follow the `docs/Entidades del dominio/*.puml` diagrams as the entity spec, and use the Modulith module-per-package convention, when implementing the first real feature in any given module.

## Frontend (`frontend/gestion-neumaticos/`)

Package manager: npm. A plain Vite app, not a workspace/monorepo. Run commands from `frontend/gestion-neumaticos/`.

```
npm install       # install deps
npm run dev       # vite dev server
npm run build     # tsc -b && vite build
npm run lint      # eslint .
npm run format    # prettier --write "**/*.{ts,tsx}"
npm run typecheck # tsc --noEmit
npm run preview   # vite preview
```

Architecture/stack notes:
- Vite + React 19 + TypeScript, Tailwind CSS 4 (via `@tailwindcss/vite`), shadcn/ui components (`components.json`: `style: "base-luma"`, `baseColor: "neutral"`, icon library `lucide`). Entry point `src/main.tsx` / `src/App.tsx`; shadcn-generated components land in `src/components/ui/`, shared code under `src/lib/`, `src/hooks/` (aliased as `@/components`, `@/lib`, `@/ui`, `@/hooks` per `components.json`).
- To add a new shadcn/ui component, run `npx shadcn@latest add <component>` from `frontend/gestion-neumaticos/`.
- `@base-ui/react` (Base UI) is a dependency underlying the shadcn `base-luma` style/components.
- Prettier config (`.prettierrc`): no semicolons, double quotes, 2-space tabs, 80 print width, `prettier-plugin-tailwindcss` for class sorting.
- ESLint flat config (`eslint.config.js`): `js.configs.recommended` + `typescript-eslint` recommended + `react-hooks` + `react-refresh` (Vite-aware).
- TypeScript: project-references setup (`tsconfig.json` → `tsconfig.app.json` / `tsconfig.node.json`), path alias `@/*` → `./src/*`.

## Codebase memory (MCP)

This repo is configured (`.mcp.json`) with the `codebase-memory-mcp` MCP server (https://github.com/DeusData/codebase-memory-mcp), a local code-intelligence graph indexer — no API keys, nothing leaves the machine. Before starting a non-trivial request (exploring unfamiliar code, tracing a call path, checking what references something, planning a change), prefer its tools over ad-hoc grepping/reading when they're available in the session:
- `index_repository` / `index_status` to (re-)index `backend/gestion-neumaticos/` or `frontend/gestion-neumaticos/` (index each separately — they are independent projects).
- `search_graph`, `search_code`, `get_code_snippet` to find symbols/code without reading whole files.
- `trace_path`, `detect_changes`, `get_architecture` to understand relationships and structural impact before editing.
- `query_graph` for anything more specific (Cypher-like queries).

If these tools aren't showing up in a session, the MCP server likely needs approval/reload — mention it rather than silently falling back to manual exploration for everything.

## Attribution

Commit messages in this repo so far follow a short `type: Sentence.` style (e.g. `feat: Configuración CORS`) with Spanish descriptions.
