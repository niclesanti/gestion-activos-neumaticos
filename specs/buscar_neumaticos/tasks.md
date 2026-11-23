# Tasks — HU-28 buscar_neumaticos

- [x] T1 — Especificaciones de filtro (activos, serie, marca, estado, situación operativa) y lista blanca de orden. Archivos: `neumaticos/domain/{EspecificacionesNeumatico,OrdenNeumaticos}.java`. Cubre: CA1, CA2, CA3, CA4, CA6, CA7, CA8.
- [x] T2 — DTO de listado, mapeo y consulta paginada con la marca en el fetch. Archivos: `neumaticos/domain/dto/NeumaticoListadoDTO.java`, `neumaticos/mapper/NeumaticoMapper.java`, `neumaticos/repository/NeumaticoRepository.java`. Cubre: CA1, CA5, CA8.
- [x] T3 — Servicio `buscar` (normaliza el término, 400 por orden inválido). Archivos: `neumaticos/service/NeumaticoService(Impl).java`. Cubre: CA1 a CA8.
- [x] T4 — Endpoint `GET /api/v1/neumaticos` (`LECTOR`, paginación por defecto y orden). Archivos: `neumaticos/controller/NeumaticoController.java`. Cubre: CA5, CA8, CA9.
- [x] T5 — Tests backend (fixtures en `TestDataFactory`, unitarios, `@WebMvcTest` + REST Docs, integración con inactivos insertados como owner y lectura con `gn_lector`). Archivos: `src/test/java/**`. Cubre: CA1 a CA9.
- [x] T6 — Request y hook del listado. Archivos: `features/tires/api/tires-api.ts`, `features/tires/hooks/{use-tires,use-register-tire}.ts`. Cubre: CA10.
- [x] T7 — Barra de filtros, tabla y página. Archivos: `features/tires/components/{tire-filters,tires-table}.tsx`, `features/tires/pages/tires-page.tsx`. Cubre: CA1 a CA5, CA10, CA11.
- [x] T8 — Textos es/en. Archivos: `lib/i18n/locales/{es,en}/tires.json`. Cubre: CA1 a CA5, CA10, CA11.
- [x] T9 — Fila del endpoint en 2.10 y estado actual. Archivos: `docs/Seguridad/seguridad.md`, `CLAUDE.md`. Cubre: CA9.
