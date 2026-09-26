# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

Web application for managing the lifecycle of tire assets for a logistics company (Ingeniería en Sistemas de Información final thesis project, UTN FRSF). Spanish is the working language for domain terms, UI copy, commit messages, and docs. UI copy uses rioplatense voseo ("Ingresá tus credenciales"), matching the target users.

## Repository layout

This is two independent projects living in one repo root, not a single build:

- `backend/gestion-neumaticos/` — Spring Boot 4.1.1 (Java 21) API, built with Maven. Has its own `Dockerfile`.
- `frontend/gestion-neumaticos/` — a standalone Vite + React 19 + TypeScript app (its own nested git repo — note the `.git` inside `frontend/gestion-neumaticos/`, currently untracked from the root repo's perspective). Not a monorepo/workspace setup. Has its own `Dockerfile` + `nginx.conf`.
- `docs/Entidades del dominio/` — PlantUML (`.puml`) entity diagrams, one per intended Modulith module (`dominio-neumaticos-svc.puml`, `dominio-conductores-svc.puml`, `dominio-usuarios-svc.puml`, `dominio-proveedores-svc.puml`, `dominio-servicios-reparacion-svc.puml`, `dominio-trazabilidad-svc.puml`, `dominio-unidades-transporte-svc.puml`, `dominio-centros-almacenamiento-svc.puml`). These are the design source of truth for entities/fields before code exists for a module — check the matching diagram before modeling a module's JPA entities.
- `infra/` — currently empty, reserved for future use.
- `.agents/skills/` — vendored agent skills (`shadcn`, `tailwind-v4-shadcn`, `java-springboot`, `vercel-react-best-practices`, `find-skills`) pinned by `skills-lock.json`. The root `package.json` exists only to hold the `shadcn` CLI devDependency — it is not a workspace root.

Root `compose.yaml` wires up `backend` (built from `backend/gestion-neumaticos/`), `frontend` (built from `frontend/gestion-neumaticos/`, nginx on container port 80 published as 3000), `postgres` (18-alpine, port 5432, healthcheck-gated), and `pgadmin` (port 5050). The root `.env` (gitignored) supplies `DB_NAME`, `DB_USER`, `DB_PASSWORD`, `PGADMIN_EMAIL`, `PGADMIN_PASSWORD`, `SPRING_PROFILES_ACTIVE`, `FRONTEND_URL` (for CORS), and `JWT_SECRET` to these services. `README.Docker.md` is the unmodified Spring Boot Docker Compose scaffolding doc. The backend also has its own standalone `backend/gestion-neumaticos/compose.yaml` (a bare `postgres` service only) for running the API outside the root compose stack during local dev.

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
- Cross-cutting infrastructure already implemented, outside the domain modules: `config/` (`SecurityConfig`, `CorsConfig`, `MapstructConfig`), `exception/` (a global `@ControllerAdvice`-style `ControllerAdvisor` plus `ExceptionInfo` and a set of custom exceptions — `EntidadDuplicadaException`, `ForbiddenException`, `OperacionNoPermitidaException`, `PermisosDenegadosException`, `UnauthorizedException`, `UsuarioNoEncontradoException` — new domain-specific exceptions should follow this same pattern and naming in Spanish), and `security/` (currently empty, reserved for JWT auth wiring).
- `SecurityConfig` currently permits `/actuator/health`, `/actuator/info`, `/swagger-ui/**`, `/swagger-ui.html`, `/api-docs/**` and requires authentication on everything else, with CSRF disabled (stateless API) and CORS delegated to `CorsConfig`. No authentication mechanism (JWT filter, `UserDetailsService`, etc.) is wired up yet, so any new controller endpoint will be rejected with 401 until that's added — build out `security/` first if you need working auth. `application.properties` already reserves `app.security.jwt-secret` (`JWT_SECRET`, empty default) for it.
- `CorsConfig` allows only the single origin from `app.cors.frontend-url` (`FRONTEND_URL` env var, default `http://localhost:3000`), all standard HTTP methods, all headers, and credentials.
- Profiles: `application.properties` holds only profile-independent config; the datasource lives in `application-dev.properties` / `application-prod.properties` (selected via `SPRING_PROFILES_ACTIVE`). Since no datasource is defined in `application.properties`, run with `dev` (or `prod`) active rather than with no profile. Both profiles set `spring.jpa.hibernate.ddl-auto=validate` and enable Flyway, so Hibernate will not create tables: every new entity needs a matching Flyway migration or startup fails validation. `dev` additionally turns on SQL logging and DEBUG logs for the app package.
- Persistence: Spring Data JPA + PostgreSQL, H2 (`h2console`) available at runtime for local/dev use. Flyway (`flyway-database-postgresql`) manages schema migrations from `src/main/resources/db/migration` (currently empty apart from `.gitkeep` — no migrations committed yet).
- `spring.modulith.events.jpa.schema-initialization.enabled=true` is set in `application.properties` specifically so Modulith's event-publication registry table can self-create in the absence of Flyway migrations — remove/revisit this once real migrations exist.
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
npm run icons     # regenerate the brand icon set (see "Brand assets")
npm run preview   # vite preview
```

Architecture/stack notes:
- Vite 8 + React 19 + TypeScript, Tailwind CSS 4 (via `@tailwindcss/vite`), shadcn/ui components (`components.json`: `style: "base-luma"`, `baseColor: "neutral"`, icon library `lucide`). Entry `src/main.tsx` → `ThemeProvider` → `src/App.tsx`. Path alias `@/*` → `./src/*` (declared in both `vite.config.ts` and `tsconfig.app.json`).
- `@base-ui/react` (Base UI) underlies the shadcn `base-luma` style — these components are **not** Radix-based. Composition uses Base UI's `render` prop (e.g. `<DropdownMenuTrigger render={<Button … />}>`), not Radix's `asChild`. Don't copy Radix-era shadcn snippets verbatim.
- To add a shadcn/ui component, run `npx shadcn@latest add <component>` from `frontend/gestion-neumaticos/`.
- Chosen libraries, already installed and to be preferred over alternatives: `react-router-dom` v7 (routing), `@tanstack/react-query` (+ devtools) for server state, `axios` for HTTP, `react-hook-form` + `zod` + `@hookform/resolvers` for forms/validation, `zustand` for client state, `@tanstack/react-table` for tables, `sonner` for toasts, `date-fns` for dates, `lucide-react` for icons.
- `src/lib/utils.ts` re-exports `cn` from the `cn` package — it is not the usual local `clsx` + `tailwind-merge` helper. Existing components import from either `@/lib/utils` or `"cn"` directly.

Code organization:
- `src/features/<feature>/{components,pages}/` holds feature code (currently `features/auth/pages/LoginPage.tsx` and `features/auth/components/LoginCard.tsx`). Feature files are **PascalCase**; shared files under `src/components/`, `src/lib/`, `src/hooks/` are **kebab-case** (shadcn's convention). Keep new code on the matching side of that split.
- `src/components/brand/` holds the app identity (`AppLogo`, `TireIcon`); `src/lib/constants.ts` holds `APP_NAME`.
- Routes live in `src/App.tsx`. Everything currently redirects to `/login`.

Theming:
- `src/components/theme-provider.tsx` is a hand-written provider (not `next-themes`): it toggles the `light`/`dark` class on `<html>`, persists to `localStorage` under the `theme` key, follows the system `prefers-color-scheme` when set to `"system"`, syncs across tabs via the `storage` event, suppresses CSS transitions during a switch, and binds a bare **`d` keypress** as a light/dark toggle (ignored while typing in an editable element). `useTheme()` throws outside the provider. `ThemeToggle` (`src/components/theme-toggle.tsx`) is the dropdown UI for it.
- `src/index.css` is the single Tailwind v4 CSS-first entry: `@import "tailwindcss"`, `tw-animate-css`, `shadcn/tailwind.css`, `@fontsource-variable/inter`, then `@custom-variant dark (&:is(.dark *))`, an `@theme inline` token block mapping `--color-*`/`--radius-*`/`--font-*` to CSS variables, and `:root` / `.dark` oklch palettes. Add or change design tokens here — there is no Tailwind config file.
- Prettier (`.prettierrc`): no semicolons, double quotes, 2-space tabs, 80 print width, `trailingComma: es5`, LF; `prettier-plugin-tailwindcss` sorts classes using `src/index.css` as the stylesheet and also sorts inside `cn()` and `cva()` calls.
- ESLint flat config (`eslint.config.js`): `js.configs.recommended` + `typescript-eslint` recommended + `react-hooks` + `react-refresh` (Vite-aware).
- TypeScript: project-references setup (`tsconfig.json` → `tsconfig.app.json` / `tsconfig.node.json`).

Brand assets (generated — do not hand-edit):
- `npm run icons` runs `scripts/generate-icons.mjs`, which traces `assets/brand/source-tire.jpg` (sharp + potrace + png-to-ico) and writes `src/components/brand/tire-icon-path.ts` plus the whole `public/` icon set (`favicon.svg`, `favicon.ico`, `logo-light.svg`, `logo-dark.svg`, `icon-{192,512}[-dark].png`, `icon-maskable-512.png`, `apple-touch-icon.png`). Change the source image and rerun the script instead of editing any of those outputs.
- `index.html` (`lang="es"`) wires those icons, the PWA `manifest.webmanifest`, and light/dark `theme-color` meta tags.

Deployment: the frontend `Dockerfile` is a two-stage node:22-alpine build → nginx:1.27-alpine; `nginx.conf` does SPA history fallback (`try_files $uri $uri/ /index.html`).

## Current state

The active work (branch `feature/login`) is the login screen. `LoginCard` is UI-only: `handleSubmit` has a `TODO` where the backend auth call belongs, and the backend has no authentication wired up yet either — real login means building out the backend `security/` package and the `usuarios` module first.

## Codebase memory (MCP)

This repo is configured (`.mcp.json`) with the `codebase-memory-mcp` MCP server (https://github.com/DeusData/codebase-memory-mcp), a local code-intelligence graph indexer — no API keys, nothing leaves the machine. Before starting a non-trivial request (exploring unfamiliar code, tracing a call path, checking what references something, planning a change), prefer its tools over ad-hoc grepping/reading when they're available in the session:
- `index_repository` / `index_status` to (re-)index `backend/gestion-neumaticos/` or `frontend/gestion-neumaticos/` (index each separately — they are independent projects).
- `search_graph`, `search_code`, `get_code_snippet` to find symbols/code without reading whole files.
- `trace_path`, `detect_changes`, `get_architecture` to understand relationships and structural impact before editing.
- `query_graph` for anything more specific (Cypher-like queries).

A `shadcn` MCP server is also configured, for component/registry lookups.

If these tools aren't showing up in a session, the MCP server likely needs approval/reload — mention it rather than silently falling back to manual exploration for everything.

## Attribution

Commit messages in this repo so far follow a short `type: Sentence.` style (e.g. `feat: Configuración CORS`) with Spanish descriptions.
