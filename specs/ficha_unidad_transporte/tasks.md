# Tasks — HU-10 ficha_unidad_transporte

- [ ] T1 — Exponer `NeumaticoApi` (named interface `api`) con su DTO, implementación y consulta por lote de ids con marca. Archivos: `neumaticos/api/*`, `neumaticos/service/NeumaticoApiImpl.java`, `neumaticos/repository/NeumaticoRepository.java`. Cubre: CA3.
- [ ] T2 — DTOs de detalle, consulta activa por `publicId` con entity graph y mapeo. Archivos: `unidades_transporte/domain/dto/{UnidadTransporteDetalleDTO,SlotDetalleDTO}.java`, `unidades_transporte/repository/UnidadTransporteRepository.java`, `unidades_transporte/mapper/UnidadTransporteMapper.java`. Cubre: CA1, CA2.
- [ ] T3 — Servicio `obtenerDetalle` (404 si no existe o está de baja; neumáticos en una sola llamada). Archivos: `unidades_transporte/service/UnidadTransporteService(Impl).java`. Cubre: CA1, CA2, CA3.
- [ ] T4 — Endpoint `GET /{publicId}` (`LECTOR`) y 400 por `publicId` no UUID. Archivos: `unidades_transporte/controller/UnidadTransporteController.java`, `exception/ControllerAdvisor.java`. Cubre: CA1.
- [ ] T5 — Tests backend (fixtures en `TestDataFactory`, unitarios, `@WebMvcTest` + REST Docs, integración como lector con slot ocupado). Archivos: `src/test/java/**`. Cubre: CA1, CA2, CA3.
- [ ] T6 — Instalar `collapsible` y `popover` con el CLI. Archivos: `components/ui/{collapsible,popover}.tsx`. Cubre: CA3.
- [ ] T7 — Request y hook del detalle. Archivos: `features/transport-units/api/transport-units-api.ts`, `features/transport-units/hooks/use-transport-unit.ts`. Cubre: CA1.
- [ ] T8 — Extraer `AxleDiagram` (vacío/ocupado + render-prop) y adaptar `AxleLayoutPreview`. Archivos: `features/transport-units/components/{axle-diagram,axle-layout-preview}.tsx`. Cubre: CA1, CA2.
- [ ] T9 — `SlotPopover` y `TransportUnitDetailDialog`; abrir desde la fila/patente de la tabla. Archivos: `features/transport-units/components/{slot-popover,transport-unit-detail-dialog,transport-units-table}.tsx`. Cubre: CA1, CA2, CA3.
- [ ] T10 — Textos es/en. Archivos: `lib/i18n/locales/{es,en}/transport-units.json`. Cubre: CA1, CA2, CA3.
- [ ] T11 — Fila del endpoint en la tabla 2.10. Archivos: `docs/Seguridad/seguridad.md`. Cubre: CA1.
