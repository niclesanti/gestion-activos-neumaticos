# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

Web application for managing the lifecycle of tire assets for a logistics company (Ingeniería en Sistemas de Información final thesis project, UTN FRSF). Spanish is the working language for domain terms, commit messages, and docs.

## Repository layout

This is two independent projects living in one repo root, not a single build:

- `backend/gestion-neumaticos/` — Spring Boot 4.1.1 (Java 21) API, built with Maven.
- `frontend/` — a Turborepo/npm workspace monorepo (its own nested git repo — note the `.git` inside `frontend/`, currently untracked from the root repo's perspective). Contains `apps/web` (Vite + React 19) and `packages/ui` (`@workspace/ui`, a shared shadcn/ui component package).
- `docs/`, `infra/` — currently empty, reserved for future use.

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
- **Spring Modulith** is a dependency (`spring-modulith-starter-core/jpa/insight/runtime`) — the intended architecture is modular monolith with explicit module boundaries enforced by Modulith conventions (top-level packages = modules). Respect this when adding new feature packages.
- Persistence: Spring Data JPA + PostgreSQL in normal profiles, H2 available at runtime (likely for tests/local). Flyway manages schema migrations from `src/main/resources/db/migration` (currently empty — no migrations committed yet).
- `spring-boot-docker-compose` is on the classpath as an optional runtime dependency, so running the app locally can auto-start `compose.yaml` if Docker is available.
- API docs via springdoc-openapi (`/swagger-ui` etc. once controllers exist).
- Security: `spring-boot-starter-security` is present but not yet configured — no `SecurityConfig` exists yet.
- Lombok is available for boilerplate reduction.
- The codebase is currently just the generated Spring Initializr skeleton (one `@SpringBootApplication` class, no controllers/entities/services yet) — there is no established layering convention to follow yet; follow Modulith module-per-package conventions when adding the first real feature.

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
- To add a new shadcn/ui component, run `shadcn` from `apps/web` targeting the ui package (per `frontend/README.md`): `pnpm dlx shadcn@latest add <component> -c apps/web` (adjust to `npx`/`npm exec` if not using pnpm) — this places generated files in `packages/ui/src/components`.
- Prettier config (`frontend/.prettierrc`): no semicolons, double quotes, 2-space tabs, 80 print width, `prettier-plugin-tailwindcss` for class sorting against `packages/ui/src/styles/globals.css`.
- ESLint (flat config) in each package: `js.configs.recommended` + `typescript-eslint` recommended + `react-hooks` + `react-refresh` (Vite-aware).
- TypeScript: root `tsconfig.json` sets shared strict compiler options (`ES2022`, `bundler` resolution, `strict: true`); each package/app has its own `tsconfig*.json` extending/refining this.

## Attribution

Commit messages in this repo so far follow a short `type: Sentence.` style (e.g. `feat: Creación backend.`) with Spanish descriptions.
