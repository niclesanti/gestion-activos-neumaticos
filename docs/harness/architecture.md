# Arquitectura — Qué significa "hacer un buen trabajo"

> Este documento define el estándar de calidad. Los agentes revisores
> evalúan código contra este archivo. Si no está aquí ni en `CLAUDE.md`,
> no es una regla del proyecto. **La descripción detallada de la arquitectura vive en
> `CLAUDE.md`** (no se duplica acá): léela antes de diseñar o implementar.

## Estructura

Dos proyectos independientes en un mismo repo (no es un monorepo/workspace):

- `backend/gestion-neumaticos/` — Spring Boot 4.1 (Java 21), Maven. Monolito
  modular con **Spring Modulith**: cada paquete de primer nivel bajo
  `ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos` es un módulo.
- `frontend/gestion-neumaticos/` — Vite + React 19 + TypeScript + Tailwind 4 +
  shadcn/ui (Base UI), organizado por features.

## Principios

1. **Fronteras de módulo (backend).** Un módulo no importa clases internas de
   otro. Lo que cruza módulos va en `<modulo>/api/` (`@NamedInterface("api")`):
   la interfaz `<Entidad>Api` y los DTOs que la cruzan; la implementación vive
   en `service/`. `ModularityTests` (`ApplicationModules.verify()`) debe pasar.
2. **Dependencias del frontend (ESLint `boundaries`).** Una feature nunca
   importa otra feature; `components/`, `hooks/` y `lib/` nunca importan una
   feature (excepción documentada: el shell `components/layout/` puede usar
   `features/auth`). Lo que comparten dos features sube a `components/` o `lib/`.
3. **Seguridad en profundidad, denegar por defecto.** Cada endpoint de negocio
   lleva `@PreAuthorize` (jerarquía ADMINISTRADOR > EDITOR > LECTOR); cada tabla
   nueva trae en su migración `ENABLE ROW LEVEL SECURITY`, política restrictiva
   de autenticación, privilegios mínimos por rol y, si es raíz de agregado,
   columnas de auditoría y políticas de firma. Nunca `DELETE` físico en tablas de negocio.
4. **REST estricto.** Prefijo `/api/v1/`, recursos sustantivos plurales en
   kebab-case, instancias por `publicId` (nunca el id interno), `POST` → 201 +
   `Location`, errores siempre como `ExceptionInfo`. Cambiar una ruta implica
   actualizar en el mismo cambio: función de API del frontend, `@WebMvcTest`,
   tabla `@PreAuthorize` de `docs/Seguridad/seguridad.md` 2.10.
5. **Esquema gestionado solo por Flyway.** `ddl-auto=validate`: toda entidad
   nueva necesita su migración `V<n>__*.sql` (un esquema Postgres por módulo).
6. **Mapeo con MapStruct y normalización en el mapper.** Conversión entidad↔DTO
   solo con `@Mapper`; la normalización de texto va en `toEntity` vía
   `NormalizacionTexto`.
7. **Errores explícitos.** Excepciones propias en español (`...Exception`) manejadas
   por `ControllerAdvisor`; las excepciones de plataforma nunca devuelven su mensaje al cliente.
8. **Logging en servicios** con SLF4J (`debug` entrada, `info` cambios de estado,
   `warn` rechazos de negocio, `error` fallos inesperados). Sin datos sensibles.
9. **Constantes locales por defecto.** Un `static final` de clase solo si se usa
   2+ veces, debe ser constante de compilación, es un objeto costoso inmutable o
   un literal largo (ver regla completa en `CLAUDE.md`).
10. **UI en rioplatense, i18n es/en.** Strings de cada feature en su namespace
    JSON en ambos idiomas; rutas desde `ROUTES`; navegación desde `NAV_GROUPS`;
    acceso desde `src/lib/access.ts`.
11. **Dependencias nuevas, solo con justificación** en `design.md`
    (y `npm audit --omit=dev --audit-level=high` debe seguir limpio).

## Flujo de datos

```
navegador ─→ frontend (React, features/*/api → axios + Bearer JWT)
              │
              └─→ backend Controller (@PreAuthorize) ─→ Service ─→ Repository (JPA)
                                                          │            │
                                                          │            └─→ PostgreSQL (rol gn_<nivel> + RLS)
                                                          └─→ eventos Modulith ─→ otros módulos (p. ej. trazabilidad)
```

## Qué NO hacer

- No editar migraciones Flyway ya aplicadas: se crea una nueva.
- No usar H2 ni mocks de la base en tests de integración (Testcontainers + PostgreSQL real).
- No exponer el `id` interno ni stack traces al cliente.
- No usar `export default`, rutas literales repetidas ni strings de UI hardcodeados en el frontend.
- No agregar `localStorage`/`sessionStorage` para datos sensibles (el token vive en el store de sesión).
- No bajar el umbral de cobertura de JaCoCo (85 %) ni desactivar reglas de ESLint para "pasar".
