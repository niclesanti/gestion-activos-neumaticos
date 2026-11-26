# Review — HU-31 asignar_neumatico_a_posicion
**Veredicto:** APPROVED

## CA sin cobertura / tasks pendientes
(ninguno) — T1..T14 en `[x]`; CA1..CA10 con test backend concreto (`AsignacionNeumaticoIntegrationTests`, `UnidadTransporteServiceImplTest.AsignarNeumatico`, `NeumaticoApiImplTest`, `UnidadTransporteControllerTest.AsignarNeumatico`, `AutorizacionBaseDatosTests`) y la parte de UI (CA1, CA3, CA10) verificada en navegador sobre el stack real. `verify.sh: [OK]` registrado.

## Problemas encontrados
(ninguno bloqueante)

## Observaciones menores (no bloquean)
1. `unidades_transporte/service/UnidadTransporteServiceImpl.java:163` — `Instant.now()` en vez del bean `Clock` que ya existe (`AuditoriaJpaConfig`); inyectarlo haría la fecha del evento determinista en los tests. Mismo patrón previo en `NeumaticoServiceImpl.java:259`.
2. `almacenamiento/service/StockCentroAlmacenamientoImpl.java` — si un neumático `EN_STOCK` apunta a un centro con stock 0, `removerNeumaticoStock` lanza `OperacionNoPermitidaException` (422) dentro del montaje. Es una inconsistencia de datos fuera de los CA, pero conviene dejarla anotada para la HU de envío a stock.
3. `common/auditoria/AuditableEntity.java:79` — `registrarModificacion` solo marca la raíz como sucia; el valor real lo pisa Auditing en el flush. Correcto, pero el nombre del parámetro `fecha` sugiere que se persiste ese valor.
4. Prueba en navegador: el anillo con pulso del slot destino y la vista mobile del drawer no se verificaron visualmente (el informe lo declara). Revisarlos a mano antes del merge.
5. El endpoint `GET /api/v1/centros-almacenamiento` se consume desde `features/tires/api/storage-centers-api.ts` y no desde `features/storage-centers`. Respeta `boundaries`, pero si otro feature lo necesita habrá que moverlo a `src/lib`.
