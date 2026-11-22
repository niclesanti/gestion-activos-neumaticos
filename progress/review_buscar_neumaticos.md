# Review — HU-28 buscar_neumaticos

> Informe del agente `reviewer`, transcripto por el leader (el reviewer no tiene herramienta de escritura).

**Veredicto:** APPROVED

## CA sin cobertura / tasks pendientes

Ninguno. T1–T9 en `[x]`. CA1–CA9 cubiertos por `BusquedaNeumaticoIntegrationTests` (`parcialSinDistinguirMayusculas`, `comodinesSeBuscanLiterales`, `combinaFiltros`, `filtraPorSituacion`, `excluyeLosInactivos`, `pagina`, `cargaLaPaginaSinNMasUno`, `lectorLeeConSuRolDeBase`, …), `NeumaticoControllerTest.Buscar`, `NeumaticoServiceImplTest`, `OrdenNeumaticosTest` y `NeumaticoMapperTest`. CA10 y CA11 (UI) con verificación documentada en el stack real (`impl_buscar_neumaticos.md`).

## Problemas encontrados

Ninguno bloqueante.

Verificado: backend en `[OK]` (`mvn clean verify`) y lint, typecheck y build del frontend en `[OK]`; el fallo de `npm audit` (`picomatch`) es externo a la HU y está pendiente de decisión de la humana. Prueba en navegador presente. `@PreAuthorize('LECTOR')` y ruta `/api/v1/neumaticos`. Escape de comodines (`\`, `%`, `_`) con carácter de escape en el `LIKE`. Activos = sin `deletedAt` y sin `DE_BAJA` en estado ni situación. Orden con lista blanca (400 `ParametroInvalidoException`) y desempate por `publicId`. `@EntityGraph(marca)` en `findAll(spec, pageable)`, sin N+1. Logs del servicio correctos (debug de entrada, warn antes del throw, `readOnly`). Constantes locales (solo `Set.of` en `OrdenNeumaticos`). Sin migración y sin necesidad de RLS. i18n es/en simétrico, con la clave `empty` obsoleta eliminada y sin referencias colgantes. Archivos kebab-case, exports nombrados, sin `console.log` ni TODO, sin código muerto. Fila del `GET` agregada en `seguridad.md` 2.10. `ROUTE_ACCESS` intacto.

## Observaciones menores (no bloquean)

- `NeumaticoServiceImpl.buscar`: la normalización del término de búsqueda (`NormalizacionTexto.mayusculas`) se llama desde el servicio y no desde el mapper. Es un filtro de consulta y no un request de alta, y sigue el patrón de HU-27, así que es aceptable.
- El kilometraje de 4 cifras se muestra sin separador de miles (comportamiento de `Intl` en español). Ya documentado por el implementador.
- `docs/estado-actual.md` lo actualiza el leader al cerrar la HU.
