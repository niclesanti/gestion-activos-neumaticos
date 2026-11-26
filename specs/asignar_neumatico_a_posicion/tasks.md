# Tasks — HU-31 asignar_neumatico_a_posicion

- [x] T1 — Migración `V20` (UPDATE de slot y unidad con RLS, índices únicos parciales, columnas y CHECK de asignación/desasignación). Archivos: `db/migration/V20__asignacion_neumaticos.sql`. Cubre: CA2, CA3, CA7.
- [x] T2 — `ConductorApi` (named interface) y listado paginado de conductores con búsqueda `q`. Archivos: `conductores/**`. Cubre: CA1, CA9.
- [x] T3 — Puerto `StockCentroAlmacenamiento`, su implementación y listado de centros. Archivos: `neumaticos/api/StockCentroAlmacenamiento.java`, `almacenamiento/**`. Cubre: CA1, CA6.
- [x] T4 — `montar`/`desmontar` en `NeumaticoApi` con reglas de disponibilidad. Archivos: `neumaticos/api/*`, `neumaticos/service/NeumaticoApiImpl.java`, `neumaticos/domain/entity/Neumatico.java`. Cubre: CA2, CA4, CA6, CA7, CA8.
- [x] T5 — Filtros `situacion` repetible y `centro`, y nombre del centro en el listado. Archivos: `neumaticos/{domain,service,controller}/**`. Cubre: CA1.
- [x] T6 — Eventos `NeumaticoAsignado`/`NeumaticoDesasignado`, servicio `asignarNeumatico` y endpoint `PUT`. Archivos: `unidades_transporte/**`, `exception/{PosicionOcupadaException,ControllerAdvisor}.java`. Cubre: CA2, CA3, CA5, CA7, CA8, CA9, CA10.
- [x] T7 — Subtipos y listener de trazabilidad. Archivos: `trazabilidad/**`. Cubre: CA2, CA7.
- [x] T8 — Tests backend (fixtures, unitarios, `@WebMvcTest` + REST Docs, integración con roles reales, `ModularityTests`). Archivos: `src/test/java/**`. Cubre: CA2 a CA10.
- [x] T9 — Instalar `drawer`, `radio-group`, `checkbox`, `alert` (`combobox` se quitó en el 2.º ciclo). Archivos: `components/ui/*`. Cubre: CA1.
- [x] T10 — Requests y hooks (búsqueda ampliada, centros, conductores, asignar) y acción `transportUnits.assignTire`. Archivos: `features/tires/{api,hooks}/*`, `features/transport-units/{api,hooks}/*`, `lib/access.ts`. Cubre: CA1, CA2, CA10.
- [x] T11 — `TirePicker` y contrato `lib/tire-picker.ts`, compuesto desde `router.tsx`. Archivos: `features/tires/components/{tire-picker,tire-filters}.tsx`, `lib/tire-picker.ts`, `app/router.tsx`, `features/transport-units/pages/transport-units-page.tsx`. Cubre: CA1.
- [x] T12 — Acción en el popover y `MountTireDrawer` de 3 pasos (neumático, conductor, confirmación inmutable con reemplazo), resaltado del slot y errores. Archivos: `features/transport-units/components/*`. Cubre: CA1, CA2, CA3, CA10.
- [x] T13 — Textos es/en. Archivos: `lib/i18n/locales/{es,en}/{transport-units,tires}.json`. Cubre: CA1, CA3.
- [x] T14 — Filas de las cuatro rutas en 2.10. Archivos: `docs/Seguridad/seguridad.md`. Cubre: CA5.
