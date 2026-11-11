# Convenciones y criterios de calidad

> Resumen de lo que más se viola. El detalle completo (arquitectura, seguridad, REST, constantes,
> logging, auditoría, frontend) está en `CLAUDE.md`, que ya está cargado: no se repite acá.
> Los revisores evalúan contra `CLAUDE.md` + este archivo.

## Idioma y commits

- Español para dominio, UI, commits y docs; UI en voseo rioplatense.
- Commits `tipo: oración corta.` (`feat:`, `fix:`, `chore:`, `docs:`, `refactor:`, `test:`). Se **sugiere** el mensaje al usuario; nunca se commitea automáticamente.

## Backend

- Paquete raíz `ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos` (con guion bajo).
- Nombres: `<Entidad>Controller`, `Service` + `ServiceImpl`, `Repository`, `RequestDTO` / `ResponseDTO`, `Mapper` (MapStruct), `Api` + `ApiImpl` (si el módulo expone API en `<modulo>/api/`).
- Excepciones propias en español con sufijo `Exception`, manejadas por `ControllerAdvisor`.
- Controladores: `@RequestMapping("/api/v1/<recurso>")` literal, `@PreAuthorize` en cada método, 201 + `Location` en creaciones.
- Cada tabla nueva: migración `V<n>__descripcion_en_snake_case.sql` (correlativa, nunca se edita una existente) con RLS, política restrictiva de autenticación, privilegios mínimos y, si es raíz de agregado, auditoría.
- Servicios con `@Slf4j` y el logging de `CLAUDE.md`; constantes locales por defecto.
- Un módulo no importa clases internas de otro (`ModularityTests` debe pasar).
- Tests: JUnit 5 + Mockito, `@WebMvcTest` (más `@MockitoBean NivelAccesoVigente`), Testcontainers; fixtures con `TestDataFactory`; mappers también con fuente y campos nulos.

## Frontend

- Archivos en kebab-case; solo exports nombrados (salvo `src/lib/i18n/index.ts`); `cn` desde `@/lib/utils`.
- Imports ordenados con `npm run lint -- --fix`, no a mano. Una feature nunca importa otra.
- Rutas desde `ROUTES`, navegación desde `NAV_GROUPS`, permisos desde `lib/access.ts`.
- Un namespace i18n por feature en `es` y `en`; sin strings hardcodeados. Toasts solo con `notify`.
- Cambiar una ruta del backend implica, en el mismo cambio: función de API del frontend, `@WebMvcTest`, tabla `@PreAuthorize` de `docs/Seguridad/seguridad.md` 2.10.

## Comentarios

Por defecto no se escriben; solo un *por qué* no obvio.

## Specs y progreso

- `specs/<name>/` con `name` igual al de `feature_list.json`. Ids `CA<n>` y `T<n>` estables.
- Informes: `progress/impl_<name>.md`, `progress/review_<name>.md` y `progress/spec_<name>.md` (solo si bloquea). `progress/history.md` es la bitácora append-only (memoria compartida entre sesiones; se consulta solo cuando hace falta).

## Checklist del reviewer (estado final correcto)

- [ ] Una sola feature `in_progress`; las `done` tienen sus tests y todas las tasks `[x]`.
- [ ] Cada `CA<n>` tiene un test concreto (o la verificación documentada para UI).
- [ ] Backend: código en el módulo correcto; cada endpoint con `@PreAuthorize`, ruta `/api/v1/...` y fila en la tabla 2.10; cada tabla con migración + RLS; sin dependencias nuevas sin justificar en `design.md`.
- [ ] Frontend: kebab-case, exports nombrados, i18n es/en, reglas de `boundaries`.
- [ ] Tests con `TestDataFactory` y PostgreSQL real; mappers/servicios/controladores nuevos con test.
- [ ] Sin `System.out.println`, `console.log` de debug ni TODOs sin contexto; sin temporales sin ignorar.
- [ ] `./verify.sh` verde (resultado anotado en `impl_<name>.md`) y, si cambió el frontend, prueba en navegador registrada.
