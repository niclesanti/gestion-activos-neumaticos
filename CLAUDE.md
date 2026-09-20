# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

Web application for managing the lifecycle of tire assets for a logistics company (Ingeniería en Sistemas de Información final thesis project, UTN FRSF). Spanish is the working language for domain terms, commit messages, and docs.

## Repository layout

This is two independent projects living in one repo root, not a single build:

- `backend/gestion-neumaticos/` — Spring Boot 4.1.1 (Java 21) API, built with Maven.
- `frontend/` — a Turborepo/npm workspace monorepo (its own nested git repo — note the `.git` inside `frontend/`, currently untracked from the root repo's perspective). Contains `apps/web` (Vite + React 19) and `packages/ui` (`@workspace/ui`, a shared shadcn/ui component package).
- `docs/Entidades del dominio/` — PlantUML (`.puml`) entity diagrams, one per intended Modulith module (`dominio-neumaticos-svc.puml`, `dominio-conductores-svc.puml`, `dominio-usuarios-svc.puml`, `dominio-proveedores-svc.puml`, `dominio-servicios-reparacion-svc.puml`, `dominio-trazabilidad-svc.puml`, `dominio-unidades-transporte-svc.puml`, `dominio-centros-almacenamiento-svc.puml`). These are the design source of truth for entities/fields before code exists for a module — check the matching diagram before modeling a module's JPA entities.
- `infra/` — currently empty, reserved for future use.

Root `compose.yaml` and `README.Docker.md` are the unmodified Spring Boot Docker Compose scaffolding (no services enabled yet — Postgres/pgAdmin are commented out). The root `.env` (gitignored) defines `DB_NAME`, `DB_USER`, `DB_PASSWORD`, `PGADMIN_EMAIL`, `PGADMIN_PASSWORD`, `SPRING_PROFILES_ACTIVE`, `FRONTEND_URL` (for CORS), and `JWT_SECRET` — these aren't yet wired into `compose.yaml`.

Because `frontend/` is a separate git repository nested inside this one, commands run from the root (`git status`, `git add`, etc.) will not see changes inside `frontend/` — `cd` into it and use git there for frontend-only commits.

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
- No controllers or JPA entities exist yet for the domain modules — only the cross-cutting scaffolding above. Follow the `docs/Entidades del dominio/*.puml` diagrams as the entity spec, and use the Modulith module-per-package convention, when implementing the first real feature in any given module.

## Frontend (`frontend/`)

Package manager: npm workspaces (`packageManager: npm@12.0.0`, workspaces `apps/*` and `packages/*`), orchestrated by Turborepo. Run commands from `frontend/`.

```
npm install       # install all workspace deps
npm run dev       # turbo dev (all apps, persistent/uncached)
npm run build     # turbo build (respects workspace dependency graph)
npm run lint      # turbo lint
npm run format    # turbo format (prettier)
npm run typecheck # turbo typecheck
```

To run a command for a single workspace, use npm's `-w` flag or `turbo run <task> --filter=<name>`, e.g. `npm run dev -w web`.

Architecture/stack notes:
- `apps/web` — the actual application: Vite + React 19 + TypeScript, Tailwind CSS 4 (via `@tailwindcss/vite`), shadcn/ui components. Entry point `src/main.tsx` / `src/App.tsx`.
- `packages/ui` (`@workspace/ui`) — shared component package consumed by `apps/web` via workspace protocol (`"@workspace/ui": "*"`). Exports are subpath-based per `package.json#exports`: `@workspace/ui/globals.css`, `@workspace/ui/components/*`, `@workspace/ui/lib/*`, `@workspace/ui/hooks/*` — new shared UI code goes under `packages/ui/src/{components,lib,hooks}` and must be exported this way, not imported by relative path across packages.
- To add a new shadcn/ui component, run `shadcn` from `apps/web` targeting the ui package: `pnpm dlx shadcn@latest add <component> -c apps/web` (adjust to `npx`/`npm exec` if not using pnpm) — this places generated files in `packages/ui/src/components`.
- Prettier config (`frontend/.prettierrc`): no semicolons, double quotes, 2-space tabs, 80 print width, `prettier-plugin-tailwindcss` for class sorting against `packages/ui/src/styles/globals.css`.
- ESLint (flat config) in each package: `js.configs.recommended` + `typescript-eslint` recommended + `react-hooks` + `react-refresh` (Vite-aware).
- TypeScript: root `tsconfig.json` sets shared strict compiler options (`ES2022`, `bundler` resolution, `strict: true`); each package/app has its own `tsconfig*.json` extending/refining this.

## Codebase memory (MCP)

This repo is configured (`.mcp.json`) with the `codebase-memory-mcp` MCP server (https://github.com/DeusData/codebase-memory-mcp), a local code-intelligence graph indexer — no API keys, nothing leaves the machine. Before starting a non-trivial request (exploring unfamiliar code, tracing a call path, checking what references something, planning a change), prefer its tools over ad-hoc grepping/reading when they're available in the session:
- `index_repository` / `index_status` to (re-)index `backend/gestion-neumaticos/` or `frontend/` (index each separately — they are independent projects).
- `search_graph`, `search_code`, `get_code_snippet` to find symbols/code without reading whole files.
- `trace_path`, `detect_changes`, `get_architecture` to understand relationships and structural impact before editing.
- `query_graph` for anything more specific (Cypher-like queries).

If these tools aren't showing up in a session, the MCP server likely needs approval/reload — mention it rather than silently falling back to manual exploration for everything.

## Attribution

Commit messages in this repo so far follow a short `type: Sentence.` style (e.g. `feat: Configuración CORS`) with Spanish descriptions.
