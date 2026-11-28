# Tasks — HU-32 desasignar_neumatico_de_posicion

- [x] T1 — `PosicionLibreException` (409) y su manejo. Archivos: `exception/{PosicionLibreException,ControllerAdvisor}.java`. Cubre: CA3, CA7.
- [x] T2 — Liberar la posición en el slot y bloqueo de la unidad en el repositorio. Archivos: `unidades_transporte/domain/**`, `unidades_transporte/repository/**`. Cubre: CA2, CA5.
- [x] T3 — `desasignarNeumatico` y método de desmontaje compartido con el reemplazo (HU-31 intacta). Archivos: `unidades_transporte/service/**`. Cubre: CA2, CA5, CA6, CA7, CA8.
- [x] T4 — Endpoint `DELETE .../slots/{codigo}/neumatico` con `hasRole('EDITOR')`. Archivos: `unidades_transporte/controller/UnidadTransporteController.java`. Cubre: CA4, CA5, CA7, CA8, CA9.
- [x] T5 — Tests backend (fixtures, servicio, `@WebMvcTest` + REST Docs, integración con roles reales y desmontaje doble, listener, HU-31 sin regresión). Archivos: `src/test/java/**`. Cubre: CA2 a CA9.
- [x] T6 — Acción `transportUnits.unassignTire`, request y hook de desmontaje. Archivos: `lib/access.ts`, `features/transport-units/{api,hooks}/*`. Cubre: CA4, CA5, CA9.
- [x] T7 — Acción "Desmontar neumático" en el popover y `UnassignTireDialog` de confirmación, con errores y resaltado de la posición. Archivos: `features/transport-units/components/*`. Cubre: CA1, CA9, CA10, CA11.
- [x] T8 — Textos es/en. Archivos: `lib/i18n/locales/{es,en}/transport-units.json`. Cubre: CA10.
- [x] T9 — Fila del `DELETE` en 2.10. Archivos: `docs/Seguridad/seguridad.md`. Cubre: CA4.
