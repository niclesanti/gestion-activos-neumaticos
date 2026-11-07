# Convenciones de código

> Homogeneidad extrema. La IA predice mejor cuando el repositorio se parece
> a sí mismo en todas partes. Las convenciones completas están en `CLAUDE.md`;
> este archivo resume las que más se violan y fija las del flujo SDD.

## Idioma

- Español para términos de dominio, textos de UI, commits y docs.
- Textos de UI en voseo rioplatense ("Ingresá tus credenciales").
- Commits: `tipo: oración corta.` (`feat:`, `fix:`, `chore:`, `docs:`, `refactor:`, `test:`). Sugerir al usuario nombre para el commit, pero NUNCA hacerlo automáticamente.

## Backend (Java 21, Spring Boot)

- **Paquete raíz:** `ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos` (con guion bajo).
- **Nombres de clases:** `<Entidad>Controller`, `<Entidad>Service` + `<Entidad>ServiceImpl`,
  `<Entidad>Repository`, `<Entidad>RequestDTO` / `<Entidad>ResponseDTO`,
  `<Entidad>Mapper` (MapStruct), `<Entidad>Api` + `<Entidad>ApiImpl` (si el módulo expone API).
- **Excepciones:** en español, sufijo `Exception`, siguiendo las existentes
  (`EntidadDuplicadaException`, `OperacionNoPermitidaException`...).
- **Lombok** para boilerplate; entidades con colecciones bidireccionales usan
  `@Getter`/`@Setter` + `@ToString(exclude…)`, no `@Data`.
- **Entidades auditables** extienden `AuditableEntity`; baja lógica con `darDeBaja`.
- **Servicios:** `@Slf4j` y logging según `CLAUDE.md`; `{}` como placeholders.
- **Controladores:** `@RequestMapping("/api/v1/<recurso>")` literal, `@PreAuthorize`
  en cada método, 201 + `Location` en creaciones.
- **Migraciones:** `src/main/resources/db/migration/V<n>__descripcion_en_snake_case.sql`,
  numeración correlativa (la siguiente a la última existente), una por tabla/cambio.
- **Constantes:** locales por defecto (regla en `CLAUDE.md`).

### Tests del backend

- JUnit 5 + Mockito para servicios y mappers; `@WebMvcTest` con la `SecurityConfig`
  real (más `@MockitoBean NivelAccesoVigente`) y REST Docs para controladores;
  Testcontainers (`TestcontainersConfiguration`) para repositorios, RLS y migraciones.
- Fixtures con los métodos estáticos de `TestDataFactory`; `@BeforeEach` solo para
  cableado del ciclo de vida.
- Nombres descriptivos en español o inglés consistente con la clase existente:
  `crear_devuelve201ConLocation`, `crear_conNombreDuplicado_lanza409`.
- Cobertura mínima LINE y BRANCH 85 % (JaCoCo); probar también mappers con fuente
  nula y campos nulos.

## Frontend (React 19, TypeScript)

- **Archivos en kebab-case** siempre; los componentes exportados van en PascalCase.
- **Solo exports nombrados** (`export function X()`), salvo `src/lib/i18n/index.ts`.
- **Imports:** orden react → externos → alias `@/` → relativos; se arreglan con
  `npm run lint -- --fix`, no a mano. `cn` siempre desde `@/lib/utils`.
- **Estructura de feature:** `src/features/<feature>/{api,components,hooks,pages,schemas,store}/`,
  solo las carpetas que se usan. Una feature no importa otra feature.
- **Datos:** funciones de request en `api/`, esquemas zod en `schemas/` (reflejan las
  reglas del DTO del backend), hooks de react-query en `hooks/`.
- **Rutas y navegación:** `ROUTES` (`lib/routes.ts`) y `NAV_GROUPS` (`lib/navigation.ts`);
  nunca paths literales duplicados.
- **Permisos de UI:** `lib/access.ts` (`ROUTE_ACCESS`, `ACTION_ACCESS`, `canPerform`).
- **i18n:** un namespace JSON por feature en `es` y `en`; nada de strings hardcodeados.
- **Formato:** Prettier (sin punto y coma, comillas dobles, 80 columnas, `trailingComma: es5`).
- **Toasts** solo con `notify` de `src/lib/toast.ts`. Componentes shadcn con Base UI
  (`render` prop, no `asChild`).

## Comentarios

Por defecto **no** se escriben. Solo cuando explican un *por qué* no obvio
(workaround documentado, invariante sutil). Los nombres deben hacer el resto.

## Specs y progreso

- `specs/<name>/` con `name` igual al de `feature_list.json` (snake_case).
- Ids estables `CA<n>` (historia de usuario: criterios de aceptación) y `T<n>` (tasks).
- Informes de agentes en `progress/impl_<name>.md`, `progress/review_<name>.md`,
  `progress/spec_<name>.md` (solo si bloquea).
