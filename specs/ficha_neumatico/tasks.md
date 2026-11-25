# Tasks — HU-14 ficha_neumatico

- [x] T1 — Puerto y contrato de ubicación en `neumaticos.api`. Archivos: `neumaticos/api/*`. Cubre: CA2, CA3, CA4.
- [x] T2 — DTO de detalle, consulta activa por `publicId` con marca y mapeo. Archivos: `neumaticos/domain/dto/NeumaticoDetalleDTO.java`, `neumaticos/repository/NeumaticoRepository.java`, `neumaticos/mapper/NeumaticoMapper.java`. Cubre: CA1, CA6, CA7.
- [x] T3 — Servicio `obtenerDetalle` (404, ubicación según situación vía proveedores). Archivos: `neumaticos/service/NeumaticoService(Impl).java`. Cubre: CA1, CA2, CA3, CA4, CA8.
- [x] T4 — Proveedores de ubicación en unidades, almacenamiento y reparación. Archivos: `unidades_transporte/{repository,service}/*`, `almacenamiento/service/UbicacionCentroProveedor.java`, `reparacion/service/UbicacionServicioProveedor.java`. Cubre: CA2.
- [x] T5 — Endpoint `GET /{publicId}` (`LECTOR`). Archivos: `neumaticos/controller/NeumaticoController.java`. Cubre: CA7, CA8.
- [x] T6 — Tests backend (fixtures en `TestDataFactory`, unitarios, `@WebMvcTest` + REST Docs, integración como lector con los tres casos de ubicación, `ModularityTests`). Archivos: `src/test/java/**`. Cubre: CA2, CA3, CA4, CA7, CA8.
- [x] T7 — Request y hook del detalle. Archivos: `features/tires/api/tires-api.ts`, `features/tires/hooks/use-tire.ts`. Cubre: CA1.
- [x] T8 — `TireDetailDialog` y apertura desde la fila/serie. Archivos: `features/tires/components/{tire-detail-dialog,tires-table}.tsx`. Cubre: CA1, CA2, CA3, CA4, CA5, CA6.
- [x] T9 — Abrir `/tires` al Lector y ocultar el alta. Archivos: `lib/access.ts`, `features/tires/pages/tires-page.tsx`. Cubre: CA9.
- [x] T10 — Textos es/en. Archivos: `lib/i18n/locales/{es,en}/tires.json`. Cubre: CA1, CA2, CA3, CA4, CA5, CA6.
- [x] T11 — Docs de seguridad, estado y `CLAUDE.md`. Archivos: `docs/Seguridad/seguridad.md`, `docs/estado-actual.md`, `CLAUDE.md`. Cubre: CA7, CA9.
