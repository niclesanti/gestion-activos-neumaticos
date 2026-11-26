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

## Segundo ciclo (2026-11-26)
**Veredicto:** APPROVED

Solo cambió el frontend: `tire-picker.tsx`, `mount-tire-drawer.tsx`, `drivers-api.ts`, `use-driver-search.ts`, `tires-api.ts`, `toast.tsx`, i18n es/en, y se borraron `combobox.tsx` e `input-group.tsx`. En el informe constan `verify.sh` completo `[OK]` (leader) y la prueba en navegador del segundo ciclo con `victoria.editor`. El defecto del texto de ayuda del paso 1 ya está corregido (`mount.steps.tire.hint` es/en).

### CA sin cobertura / tasks pendientes
(ninguno). No quedan tasks `[ ]`. CA1, CA3 y CA10 en UI se volvieron a verificar en navegador. Los CA de backend no cambiaron.

### Problemas encontrados
(ninguno bloqueante)

Lo que se comprobó contra `design.md` y las convenciones:
- Pestañas "En espera" (por defecto) / "En stock" con select de centro, sobre `RadioGroup` + `TablePagination`. Al volver al paso 1 se abre la pestaña del neumático elegido (`tire-picker.tsx:64`). La página vuelve a 0 al cambiar la búsqueda, la pestaña o el centro.
- `size` quedó opcional y axios omite el parámetro cuando es `undefined` (`tires-api.ts:67`).
- Conductor: búsqueda con debounce + lista paginada. "Quitar selección" funciona y "Omitir" solo aparece sin conductor elegido. Tiene estados de error con reintento, vacío y sin resultados (`mount-tire-drawer.tsx:565-730`). La `queryKey` incluye `term` y `page`.
- `ScrollArea` está entre la cabecera y el pie fijos (`mount-tire-drawer.tsx:250`).
- `ToastViewport` pasó a `z-100`, por encima del `z-50` de dialog y drawer (`toast.tsx:68`). Cerrar un toast no cierra la ficha (verificado en navegador).
- `combobox` e `input-group` no tienen referencias en `src/`. Las claves i18n viejas se eliminaron en es y en en, y no hay literales en la UI.
- No hay imports entre features: el picker sigue inyectándose por `renderTirePicker`. La constante `ALL_CENTERS` se usa varias veces, así que está justificada.

### Observaciones menores (no bloquean)
1. `specs/asignar_neumatico_a_posicion/design.md:17`: el paso 1 sigue diciendo "Buscá por número de serie o filtrá por origen". Hay que alinearlo con el texto nuevo de `mount.steps.tire.hint`.
2. `specs/asignar_neumatico_a_posicion/tasks.md:11` (T9) nombra `combobox`, que ya se borró. `historia-usuario.md:25` menciona "todos los disponibles", una opción que ya no existe con las pestañas. Conviene dejar una nota de que el segundo ciclo lo reemplazó.
3. `progress/impl_asignar_neumatico_a_posicion.md`: después del arreglo del texto solo quedaron registrados `typecheck` y `lint`. Falta la línea del `./verify.sh --frontend` `[OK]` posterior, que el leader dice haber corrido. Hay que agregarla al cerrar.
4. Tal como se declara en el informe, la barra del `ScrollArea` y la vista mobile no se vieron en capturas. Revisarlas a mano antes del merge.
5. La búsqueda de conductores distingue acentos ("gomez" no encuentra "Gómez"). Es del backend de `conductores` y queda fuera de esta HU: conviene anotarlo como pendiente (`unaccent`).
