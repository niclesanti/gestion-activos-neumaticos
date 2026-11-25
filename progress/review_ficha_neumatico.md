# Review — HU-14 ficha_neumatico

> Informe del agente `reviewer`, transcripto por el leader (el reviewer no tiene herramienta de escritura).

**Veredicto:** APPROVED

## CA sin cobertura / tasks pendientes

Ninguno. T1 a T11 están en `[x]`. Los CA1 a CA9 tienen test backend o verificación documentada en el navegador. CA5 y CA9 son solo UI, y la prueba en el stack real las cubre, incluido el Lector sin el botón "Registrar neumático". No se volvió a correr `verify.sh`: el `[OK]` del leader consta en `impl_ficha_neumatico.md`.

## Problemas encontrados

Ninguno bloqueante.

Lo que se comprobó:

- **Arquitectura de módulos:**
  - El puerto `ProveedorUbicacionNeumatico` está en `neumaticos/api`. Las tres implementaciones están en `unidades_transporte/service`, `almacenamiento/service` y `reparacion/service`.
  - `NeumaticoServiceImpl` recibe `List<ProveedorUbicacionNeumatico>`. `neumaticos` no importa nada de los otros tres módulos, así que no hay ciclos.
  - Los DTO (`NeumaticoDetalleDTO`, `UbicacionOperativaDTO`) solo llevan patente, código de posición y nombres. No salen ids internos.
- **Corrección:**
  - La marca se carga con `@EntityGraph`, y el slot con su unidad en una sola consulta (`findConUnidadById`). Hay un test de no N+1 (`DetalleNeumaticoIntegrationTests.sinNMasUno`).
  - Los ids nulos o inexistentes devuelven ubicación vacía con `warn`, sin lanzar excepción.
  - Una baja lógica da 404. Un `estadoActual` o situación `DE_BAJA` devuelve la ficha sin consultar ubicación.
  - `GET /{publicId}` convive con `/marcas` y `/auditoria`: Spring prefiere la ruta literal, y `publicId` es `UUID`, así que un valor inválido da 400.
- **Convenciones:**
  - `@PreAuthorize("hasRole('LECTOR')")`.
  - Logs del servicio con el nivel correcto y placeholders.
  - La normalización queda en el mapper (`toDetalleDTO` solo ensambla).
  - `MENSAJE_NEUMATICO_INEXISTENTE` es package-private porque lo usa un test, lo que cumple la regla de constantes.
  - Frontend: nombres en kebab-case, exports nombrados, i18n es/en simétrico (mismas claves `detail.*` y `list.openDetail`), sin `console.log`, TODO ni código muerto.
- **Accesibilidad:**
  - El `Dialog` tiene título y descripción.
  - La fila tiene un botón con `aria-label`, así que se abre con teclado.
  - Los botones deshabilitados llevan `disabled` y `aria-disabled`, y el tooltip "Próximamente" cuelga de un `span` con `tabIndex=0`.
- **Lector:** `/tires` quedó fuera de `ROUTE_ACCESS`, y `tires.create` (admin y editor) oculta el alta con `canPerform`.

## Observaciones menores (no bloquean)

1. `frontend/gestion-neumaticos/src/features/tires/components/tire-detail-dialog.tsx`: `getHttpStatus(error) === 404` se evalúa tres veces. Se podría guardar en una variable local.
2. `TireLocation` en el frontend repite la condición `DE_BAJA` del backend (`estadoActual` o situación). Es redundante, pero inofensivo y coherente con el CA4.
3. El tooltip sobre el `span` enfocable no se verificó visualmente. El informe lo marca, y la prueba en navegador confirma el texto.
4. El nombre `TireLocation` en `tires-api.ts` y el componente `TireLocation` en el dialog comparten identificador. No chocan, porque el componente no importa el tipo, pero conviene renombrar uno.
