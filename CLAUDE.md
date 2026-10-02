# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

Web application for managing the lifecycle of tire assets for a logistics company (Ingeniería en Sistemas de Información final thesis project, UTN FRSF). Spanish is the working language for domain terms, UI copy, commit messages, and docs. UI copy uses rioplatense voseo ("Ingresá tus credenciales"), matching the target users.

## Repository layout

This is two independent projects living in one repo root, not a single build:

- `backend/gestion-neumaticos/` — Spring Boot 4.1.1 (Java 21) API, built with Maven. Has its own `Dockerfile`.
- `frontend/gestion-neumaticos/` — a standalone Vite + React 19 + TypeScript app, tracked by the root repo (it is no longer a nested git repo). Not a monorepo/workspace setup either: it has its own `package.json`, `Dockerfile` and `nginx.conf`, and npm commands run from inside that directory.
- `docs/Entidades del dominio/` — PlantUML (`.puml`) entity diagrams, one per intended Modulith module (`dominio-neumaticos-svc.puml`, `dominio-conductores-svc.puml`, `dominio-usuarios-svc.puml`, `dominio-proveedores-svc.puml`, `dominio-servicios-reparacion-svc.puml`, `dominio-trazabilidad-svc.puml`, `dominio-unidades-transporte-svc.puml`, `dominio-centros-almacenamiento-svc.puml`). These are the design source of truth for entities/fields before code exists for a module — check the matching diagram before modeling a module's JPA entities.
- `infra/` — currently empty, reserved for future use.
- `.agents/skills/` — vendored agent skills (`shadcn`, `tailwind-v4-shadcn`, `java-springboot`, `vercel-react-best-practices`, `find-skills`) pinned by `skills-lock.json`. The root `package.json` exists only to hold the `shadcn` CLI devDependency — it is not a workspace root.

Root `compose.yaml` wires up `backend` (built from `backend/gestion-neumaticos/`), `frontend` (built from `frontend/gestion-neumaticos/`, nginx on container port 80 published as 3000), `postgres` (18-alpine, port 5432, healthcheck-gated), and `pgadmin` (port 5050). The root `.env` (gitignored) supplies `DB_NAME`, `DB_USER`, `DB_PASSWORD`, `PGADMIN_EMAIL`, `PGADMIN_PASSWORD`, `SPRING_PROFILES_ACTIVE`, `FRONTEND_URL` (for CORS), and `JWT_SECRET` to these services. `README.Docker.md` is the unmodified Spring Boot Docker Compose scaffolding doc. The backend also has its own standalone `backend/gestion-neumaticos/compose.yaml` (a bare `postgres` service only) for running the API outside the root compose stack during local dev.

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
- Cross-cutting infrastructure already implemented, outside the domain modules: `config/` (`CorsConfig`, `MapstructConfig`, `OpenApiConfig` — the latter declares the `bearerAuth` scheme used by Swagger's "Authorize"), `exception/` (a global `@ControllerAdvice`-style `ControllerAdvisor` plus `ExceptionInfo` and a set of custom exceptions — `CredencialesInvalidasException`, `EntidadDuplicadaException`, `ForbiddenException`, `OperacionNoPermitidaException`, `PermisosDenegadosException`, `UnauthorizedException`, `UsuarioNoEncontradoException` — new domain-specific exceptions should follow this same pattern and naming in Spanish; the generic 500 handler never echoes the exception message to the client), and `security/` (JWT auth, below).
- `security/` (a Modulith module; its public API is `TokenService` + `TokenEmitido` in the base package): `SecurityConfig` is stateless, CSRF off, CORS from `CorsConfig`; permits `/actuator/health`, `/actuator/info`, swagger/api-docs and `POST /api/auth/login`, everything else `authenticated()`. Auth is Spring Security's OAuth2 resource server with HS256 JWTs (Nimbus, `jwt/JwtConfig`), secret from `app.security.jwt.secret` (`JWT_SECRET`, startup fails if < 32 bytes), lifetime `app.security.jwt.expiracion` (default `PT8H`). Claims: `sub` = user `publicId` (the internal `id` never leaves `usuarios`), `jti`, `iss`, `nivelAcceso` (used as-is as the authority, already `ROLE_`-prefixed). Logout revokes the `jti` into `seguridad.tokens_revocados` (checked by `TokenNoRevocadoValidator` on every request, purged daily). `PasswordConfig` exposes a `DelegatingPasswordEncoder` defaulting to Argon2id (`{argon2}` prefix; needs `bcprov-jdk18on`). 401/403 from the filter chain are written as `ExceptionInfo` JSON by `web/JsonSecurityErrorHandler`. Endpoint-level authorization by role is not designed yet.
- `CorsConfig` allows only the single origin from `app.cors.frontend-url` (`FRONTEND_URL` env var, default `http://localhost:3000`), all standard HTTP methods, all headers, and credentials.
- Profiles: `application.properties` holds only profile-independent config; the datasource lives in `application-dev.properties` / `application-prod.properties` (selected via `SPRING_PROFILES_ACTIVE`). Since no datasource is defined in `application.properties`, run with `dev` (or `prod`) active rather than with no profile. Both profiles set `spring.jpa.hibernate.ddl-auto=validate` and enable Flyway, so Hibernate will not create tables: every new entity needs a matching Flyway migration or startup fails validation. `dev` additionally turns on SQL logging and DEBUG logs for the app package.
- Persistence: Spring Data JPA + PostgreSQL, H2 (`h2console`) available at runtime for local/dev use. Flyway (`flyway-database-postgresql`) manages schema migrations from `src/main/resources/db/migration`: `V1__crear_tabla_usuarios.sql` (schema `usuarios`) and `V2__crear_tabla_tokens_revocados.sql` (schema `seguridad`) — one Postgres schema per module. Keep migration SQL portable to H2 too (no `TIMESTAMPTZ`, no `OWNED BY`), since `GestionNeumaticosApplicationTests` runs them against the embedded H2. The `dev` profile also loads `db/seed/dev/R__usuarios_dev.sql` (repeatable, so later `V3+` never run out of order): test users `administrador` / `Admin.1234`, `editor` / `Editor.1234`, `lector` / `Lector.1234`. It never loads in `prod`.
- `spring.modulith.events.jpa.schema-initialization.enabled=true` is set in `application.properties` so Modulith's event-publication registry table self-creates instead of having a Flyway migration — revisit once module events are actually used.
- `spring-boot-docker-compose` is on the classpath as an optional runtime dependency, so running the app locally can auto-start `compose.yaml` if Docker is available.
- API docs via springdoc-openapi at `/swagger-ui.html` and `/api-docs` (paths configured explicitly in `application.properties`, not the defaults).
- Actuator exposes only `health` and `info` over the web.
- Lombok is available for boilerplate reduction.
- Test dependencies include `spring-modulith-starter-test` and `spring-restdocs-mockmvc`/`asciidoctor-maven-plugin` (restdocs generation wired into the Maven build) — new module tests should use Modulith's test support for module-boundary verification, and API tests intended to produce documentation should use Spring REST Docs conventions already set up in the POM.
- MapStruct (`mapstruct` + `mapstruct-processor`, with `lombok-mapstruct-binding` so it coexists with Lombok's annotation processor) is configured via `MapstructConfig` in `config/` for entity↔DTO mapping — no mappers exist yet, but new ones should be MapStruct `@Mapper` interfaces rather than hand-written conversion code.
- `usuarios` is the first real module: entity `Usuario` (`SEQUENCE` id with `allocationSize = 50` matching the sequence's `INCREMENT BY 50`; `publicId` as a Hibernate `@UuidGenerator(VERSION_7)`; `email`/`nombreUsuario` stored lowercase), `NivelAcceso` (`ROLE_ADMINISTRADOR`, `ROLE_EDITOR`, `ROLE_LECTOR`), and `AuthController` at `/api/auth` (`POST /login`, `POST /logout`, `GET /me`). Login always answers the same generic 401 and compares against a dummy hash when the user doesn't exist (constant-time, no account enumeration). Tests: Mockito unit tests for the service, `@WebMvcTest` with the real `SecurityConfig` for the controller (REST Docs snippet `auth-login`), and `ModularityTests` (`ApplicationModules.verify()`).
- The other domain modules have no controllers or JPA entities yet. Follow the `docs/Entidades del dominio/*.puml` diagrams as the entity spec, and use the Modulith module-per-package convention, when implementing the first real feature in any given module.

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
- Vite 8 + React 19 + TypeScript, Tailwind CSS 4 (via `@tailwindcss/vite`), shadcn/ui components (`components.json`: `style: "base-luma"`, `baseColor: "neutral"`, icon library `lucide`). Entry `src/main.tsx` → `AppProviders` (`src/app/providers.tsx`, composes `ThemeProvider` → `LanguageProvider`) → `AppRouter` (`src/app/router.tsx`). Path alias `@/*` → `./src/*` (declared in both `vite.config.ts` and `tsconfig.app.json`).
- `@base-ui/react` (Base UI) underlies the shadcn `base-luma` style — these components are **not** Radix-based. Composition uses Base UI's `render` prop (e.g. `<DropdownMenuTrigger render={<Button … />}>`), not Radix's `asChild`. Don't copy Radix-era shadcn snippets verbatim.
- To add a shadcn/ui component, run `npx shadcn@latest add <component>` from `frontend/gestion-neumaticos/`.
- Chosen libraries, already installed and to be preferred over alternatives: `react-router-dom` v7 (routing), `@tanstack/react-query` (+ devtools) for server state, `axios` for HTTP, `react-hook-form` + `zod` + `@hookform/resolvers` for forms/validation, `zustand` for client state, `@tanstack/react-table` for tables, `date-fns` for dates, `lucide-react` for icons. Toasts are **not** a third-party library: they use the shadcn Base UI `toast` (`src/components/ui/toast.tsx`), always through the `notify` helper in `src/lib/toast.ts` (`sonner` was removed — do not reintroduce it).
- `src/lib/utils.ts` re-exports `cn` from the `cn` package — it is not the usual local `clsx` + `tailwind-merge` helper. **Always import `cn` from `@/lib/utils`**, never from `"cn"` directly (it is the `utils` alias in `components.json`, so the shadcn CLI writes it that way too).

Code organization:
- **All files under `src/` are kebab-case** (shadcn's convention), with no exceptions — features included. Exported component names stay PascalCase (`login-card.tsx` exports `LoginCard`). The Linux build is case-sensitive while Windows is not, so a casing slip only fails in Docker/CI.
- **Named exports only.** No `export default` anywhere except `src/lib/i18n/index.ts` (the i18next instance). Use `export function X()` inline rather than a trailing `export { X }` block (`src/components/ui/*` keeps the trailing style because the CLI generates it).
- `src/app/` is the composition root: `providers.tsx` (every global provider — `QueryClientProvider` goes here; `<Toaster />` is already mounted inside `LanguageProvider`, outside the router, so toasts also work on `/login`) and `router.tsx` (all routes). `src/main.tsx` stays three lines; there is no `src/App.tsx`.
- `src/features/<feature>/{api,components,hooks,pages,schemas,store}/` holds feature code — create only the subfolders a feature actually uses: `auth` (real: login form, session store, route guards — see "Authentication" below) plus route-scaffold placeholders `home`, `transport-units`, `tires`, `repairs`, `storage`, `settings`, `audit` (each just a `pages/<name>-page.tsx` stub, no real functionality yet — build these out against the matching backend module once it exists).
- **Dependency rule, enforced by ESLint** (`boundaries/dependencies`, see below): a feature never imports another feature; `src/components/`, `src/hooks/` and `src/lib/` never import a feature. The one documented exception is the shell (`src/components/layout/`), which may consume `features/auth` (session + logout). Anything two features both need moves to `src/components/` or `src/lib/`.
- `src/hooks/` is for generic, domain-free hooks only (`use-mobile.ts`). A hook tied to a domain belongs in `features/<feature>/hooks/`.
- `src/components/common/` holds app-level shared components that are not shadcn primitives: `page-header.tsx` (title + subtitle of every section) and `scrollable-tabs-list.tsx` (tab list with horizontal scroll, for long Spanish labels). `settings-page` and `audit-page` are both built on those two — reuse them instead of re-copying the markup.
- `src/components/brand/` holds the app identity (`AppLogo`, `TireIcon`). The app name is not a constant: it comes from i18n (`app.name`, `common` namespace).
- The whole app shell lives in `src/components/layout/`: `app-layout.tsx` (wraps all authenticated routes) plus `app-sidebar`, `app-header`, `nav-breadcrumb`, `nav-user`, `mobile-bottom-nav`. There is no `src/layouts/`.
- `src/lib/routes.ts` (`ROUTES`) is the single source of truth for URL paths — a path is never written as a literal twice; both the router and the navigation read from it. `src/lib/navigation.ts` is the single source of truth for nav structure (groups with a stable `id`, `NavItem`s with i18n `labelKey`s and `to: AppRoute`) — sidebar, breadcrumb and mobile bottom nav all derive from it, and `MOBILE_NAV_ITEMS` filters by group `id`, never by position. Adding a section = one entry in `ROUTES`, one in `NAV_GROUPS`, one `<Route>`.
- Routes live in `src/app/router.tsx`: `/login` sits under `GuestOnly`; everything else (`/home`, `/transport-units`, `/tires`, `/repairs`, `/storage`, `/settings`, `/audit`) sits under `RequireAuth` → `AppLayout` → `RequireRouteAccess`. `/` and unmatched paths redirect to `/home` (which sends to `/login` without a session; a `TODO` notes that `*` should render a `NotFoundPage` instead of silently hiding broken links).

Authentication (frontend):
- The login returns a JWT that `features/auth/store/session-store.ts` (zustand + `persist` in **`sessionStorage`**) keeps; `src/lib/api/client.ts` adds it as `Authorization: Bearer`. `src/lib` can't import features, so `configureApiClient({ getToken, onUnauthorized })` is wired from `features/auth/api/setup-auth-client.ts`, called once in `src/app/providers.tsx`. Any 401 (except the login itself) clears the session and the guards send the user to `/login`.
- `RequireAuth` redirects to `/login` with `state.from`; after login `GuestOnly` (not `LoginCard`, which is already unmounted by then) sends to `from` or `/home`. `useSessionSync` validates a restored session against `GET /api/auth/me`, with the user's `publicId` in the query key so no other user's cached response can be reused.
- `syncSessionOnPageRestore` handles the bfcache: on `pageshow` with `persisted` it clears the react-query cache and re-reads `sessionStorage`. Without it, "back" after a logout revives the old session from memory.
- UI authorization: `src/lib/access.ts` (`AccessLevel`, `ROUTE_ACCESS`, `canAccessRoute`) is the single source of truth — sidebar/mobile nav filter via `filterNavGroups`/`filterNavItems` (`src/lib/navigation.ts`) and `RequireRouteAccess` blocks by URL. Today `/settings` and `/audit` are restricted to `ROLE_ADMINISTRADOR`; `ROLE_EDITOR` and `ROLE_LECTOR` see everything else. Security design rationale and the pending list live in `docs/seguridad.md` — keep it updated when touching auth.
- Logout (`nav-user.tsx`) asks for confirmation (`AlertDialog`), revokes the token in the backend and clears the local session even if that request fails.

Theming:
- `src/components/theme-provider.tsx` is a hand-written provider (not `next-themes`): it toggles the `light`/`dark` class on `<html>`, persists to `localStorage` under the `theme` key, follows the system `prefers-color-scheme` when set to `"system"`, syncs across tabs via the `storage` event, suppresses CSS transitions during a switch, and binds a bare **`d` keypress** as a light/dark toggle (ignored while typing in an editable element). `useTheme()` throws outside the provider. `ThemeToggle` (`src/components/theme-toggle.tsx`) is the dropdown UI for it.
- `src/index.css` is the single Tailwind v4 CSS-first entry: `@import "tailwindcss"`, `tw-animate-css`, `shadcn/tailwind.css`, `@fontsource-variable/inter`, then `@custom-variant dark (&:is(.dark *))`, an `@theme inline` token block mapping `--color-*`/`--radius-*`/`--font-*` to CSS variables, and `:root` / `.dark` oklch palettes. Add or change design tokens here — there is no Tailwind config file.
- Prettier (`.prettierrc`): no semicolons, double quotes, 2-space tabs, 80 print width, `trailingComma: es5`, LF; `prettier-plugin-tailwindcss` sorts classes using `src/index.css` as the stylesheet and also sorts inside `cn()` and `cva()` calls.
- ESLint flat config (`eslint.config.js`): `js.configs.recommended` + `typescript-eslint` recommended + `react-hooks` + `react-refresh` (Vite-aware), plus two architectural plugins:
  - `eslint-plugin-import-x` → `import-x/order`: react → externals → `@/` alias (alphabetical) → relatives, with a blank line between blocks. Autofixable, so never hand-sort imports — run `npm run lint -- --fix`. Off for `src/components/ui/**` (CLI-generated).
  - `eslint-plugin-boundaries` → `boundaries/dependencies`: enforces the layer rule above over the elements `app` | `feature` | `shell` | `shared`, the frontend counterpart of the backend's Spring Modulith boundaries. Both plugins resolve `@/` via `eslint-import-resolver-typescript`, but boundaries reads the legacy `import/resolver` setting while import-x reads `import-x/resolver-next` — having both in the config is deliberate, not a leftover.
- Data layer (implemented by `auth`; follow it in every module): axios instance + interceptors in `src/lib/api/client.ts`, reading `VITE_API_URL` through a validated `src/lib/env.ts` (`.env.development` points to `http://localhost:8080`; Docker receives it as a build arg `VITE_API_URL` because Vite bakes it in at build time); the `QueryClient` in `src/lib/api/query-client.ts` (no retries on 4xx), mounted from `src/app/providers.tsx`; and per feature `api/` (request functions), `schemas/` (zod), `hooks/` (react-query). Routes are not lazy yet and the bundle is a single ~600 kB chunk — switch `router.tsx` to `React.lazy` once pages carry real tables and forms.
- TypeScript: project-references setup (`tsconfig.json` → `tsconfig.app.json` / `tsconfig.node.json`).

Internationalization:
- `i18next` + `react-i18next` + `i18next-browser-languagedetector`, es/en, initialized in `src/lib/i18n/index.ts` (imported once, for side effects, from `main.tsx`) and configured in `src/lib/i18n/config.ts` (supported languages, fallback language, namespaces, the `LANGUAGE_STORAGE_KEY` localStorage key). Resources are namespaced JSON bundled at build time (`src/lib/i18n/locales/{es,en}/{common,auth}.json`, no lazy backend) — add a new feature's strings as its own namespace file in both locales rather than growing `common.json`.
- `LanguageProvider` (`src/components/language-provider.tsx`) mirrors `ThemeProvider`'s pattern: i18next is the source of truth for the language itself, and the provider only syncs side effects outside React — setting `<html lang>` and reacting to the `storage` event so a language change in one tab applies in others. `LanguageToggle` (`src/components/language-toggle.tsx`) is the dropdown UI for it, alongside `ThemeToggle`.
- Detection order is `localStorage` → `navigator` → `htmlTag`, cached to `localStorage`. `src/lib/i18n/i18next.d.ts` augments the `react-i18next` module types for the configured resources/namespaces.

Brand assets (generated — do not hand-edit):
- `npm run icons` runs `scripts/generate-icons.mjs`, which traces `assets/brand/source-tire.jpg` (sharp + potrace + png-to-ico) and writes `src/components/brand/tire-icon-path.ts` plus the whole `public/` icon set (`favicon.svg`, `favicon.ico`, `logo-light.svg`, `logo-dark.svg`, `icon-{192,512}[-dark].png`, `icon-maskable-512.png`, `apple-touch-icon.png`). Change the source image and rerun the script instead of editing any of those outputs.
- `index.html` (`lang="es"`) wires those icons, the PWA `manifest.webmanifest`, and light/dark `theme-color` meta tags.

Deployment: the frontend `Dockerfile` is a two-stage node:22-alpine build → nginx:1.27-alpine; `nginx.conf` does SPA history fallback (`try_files $uri $uri/ /index.html`).

## Current state

Branch `feature/login` implements HU-01 (log in) and HU-02 (log out) end to end: JWT login/logout/me in the backend (`usuarios` + `security`) and, in the frontend, the real login form, protected routes, and hiding/blocking `/settings` for anyone who isn't an administrator. The section pages (home, transport-units, tires, repairs, storage, settings, audit) are still placeholders. Pending: endpoint authorization by role in the backend, rate limiting/lockout on login, and user management (create/edit). The other domain modules (`neumaticos`, `conductores`, `proveedores`, `reparacion`, `trazabilidad`, `unidades_transporte`, `almacenamiento`) have no code yet, matching their still-placeholder frontend pages.

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
