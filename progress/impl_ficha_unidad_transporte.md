# Implementación HU-10 — ficha_unidad_transporte

## Resumen

Backend: `GET /api/v1/unidades-transporte/{publicId}` (`hasRole('LECTOR')`) devuelve `UnidadTransporteDetalleDTO` (configuración completa + `SlotDetalleDTO` con `neumatico` o null). Nuevo contrato `neumaticos.api` (`NeumaticoApi`, `NeumaticoMontadoDTO`, `@NamedInterface("api")`) con `NeumaticoApiImpl` y `NeumaticoRepository.findAllByIdIn` (marca en el mismo fetch). La unidad se carga con entity graph (`configuracion`, `slots`); los ejes se cargan con `findConEjesById` (dos `List` en un mismo fetch no se pueden traer juntas) y los neumáticos en una sola llamada por lote. 404 si no existe o está de baja; `ControllerAdvisor` traduce `MethodArgumentTypeMismatchException` a 400. Sin migración. Fila agregada en `docs/Seguridad/seguridad.md` 2.10.

Frontend (`transport-units`): `AxleDiagram` extraído de `AxleLayoutPreview` (ocupada/vacía, leyenda, render-prop por rueda); `SlotPopover` (Popover con `openOnHover`, la rueda es un botón con `aria-label`); `TransportUnitDetailDialog` (patente + badge de tipo, `Collapsible` con datos, diagrama, Skeleton/error con reintento/404); filas y patente de la tabla abren el dialog. Instalados `collapsible` y `popover` con el CLI (se corrigió el import de `cn` a `@/lib/utils` en `popover.tsx`). i18n es/en.

Verificación puntual: tests backend de la feature en verde (`UnidadTransporte*Test`, `NeumaticoApiImplTest`, `ControllerAdvisorTest`, `DetalleUnidadTransporteIntegrationTests`, `ModularityTests`); `npm run lint` y `npm run typecheck` limpios. No se corrió `./verify.sh` (lo corre el leader).

## Trazabilidad

- CA1 (esquema del chasis con ejes y auxilio): `UnidadTransporteServiceImplTest.ObtenerDetalle.armaLaFicha`, `UnidadTransporteMapperTest.toDetalleDTO_*`, `UnidadTransporteControllerTest.Detalle.detalleConLector` (+ REST Docs `unidad-transporte-detalle`), `DetalleUnidadTransporteIntegrationTests.todasVacias`; UI: verificación en navegador (abajo).
- CA2 (ocupada vs vacía): `ObtenerDetalle.armaLaFicha`, `toDetalleDTO_*`, `DetalleUnidadTransporteIntegrationTests.conNeumaticoMontado`; UI: relleno `primary` vs fondo + leyenda.
- CA3 (serie, marca, estado, kilometraje al seleccionar): `NeumaticoApiImplTest`, `DetalleUnidadTransporteIntegrationTests.conNeumaticoMontado` y `datosNoRegistrados` (nulos = "No registrado"); UI: popover.
- Baja = inexistente / 400 UUID / 401 / roles: `ObtenerDetalle.inexistente`, `DetalleUnidadTransporteIntegrationTests.bajaOInexistente`, `Detalle.inexistente|identificadorInvalido|sinToken|detalleConEditorYAdministrador`, `ControllerAdvisorTest.parametroConTipoInvalido`.
- Sin N+1: `DetalleUnidadTransporteIntegrationTests.sinNMasUno` (<= 6 sentencias).

## Pasos para el navegador

Hasta HU-31 nada monta neumáticos: para ver CA2/CA3 asignar por SQL (dev, como owner) un neumático a un slot. Ejemplo (reemplazar la patente; requiere al menos un neumático registrado, p. ej. desde "Registrar neumático"):

```sql
UPDATE unidades_transporte.slots_unidad_transporte
SET neumatico_actual_id = (SELECT id FROM neumaticos.neumaticos ORDER BY id LIMIT 1)
WHERE id = (SELECT s.id FROM unidades_transporte.slots_unidad_transporte s
            JOIN unidades_transporte.unidades_transporte u ON u.id = s.unidad_id
            WHERE u.patente = 'AB123CD' ORDER BY s.id LIMIT 1);
```

Usuarios de seed: `victoria.administrador` / `victoria.editor` / `victoria.lector` (contraseñas por rol del seed). Ruta: `/transport-units`.

- CA1: clic en una fila (o en la patente, con teclado: Tab + Enter) abre el dialog con la patente, badge de tipo, bloque "Datos de la unidad" (colapsable, abierto) y el esquema con ejes y auxilios.
- CA2: posiciones ocupadas rellenas (color primario), vacías con fondo; leyenda "Ocupada / Vacía"; "Posiciones ocupadas: n de total" en los datos.
- CA3: pasar el mouse, hacer clic o enfocar con Tab una rueda abre el popover: ocupada = serie, marca, estado, kilometraje (o "No registrado"); vacía = código, tipo de eje y "Sin neumático".
- Probar con los tres roles (lector incluido: debe poder abrir la ficha); unidad dada de baja por SQL mientras el dialog está abierto y reintentar: mensaje "La unidad ya no existe". Ningún rol debe quedar sin acceso a esta acción (es solo lectura).
- Regresión: en "Registrar unidad" la vista previa del esquema sigue igual (sin leyenda ni botones).

## Prueba en el stack real

`./verify.sh`: [OK]. Stack levantado con `docker compose up -d --build`; login local con `victoria.administrador` (seed dev).

Verificado por árbol de accesibilidad (las capturas de pantalla dieron timeout 2 veces en la extensión):
- CA1 OK: clic en la fila AB123CD abre el dialog con patente, badge "Tractor", bloque colapsable "Datos de la unidad" (Configuración, Tipo, Posiciones ocupadas "0 de 7"), esquema con Eje 1 (dirección, simple), Eje 2 (tracción, dual), Auxilios y leyenda Ocupada/Vacía.
- Popover de slot vacío: existe `E1-IZQ · Eje de Dirección · Sin neumático`, pero tras el clic quedó oculto (posible alternancia hover/clic): no confirmado.

Segunda pasada (capturas ya funcionando), con 2 slots de AB123CD asignados por SQL (revertido al terminar):
- CA2 OK: ocupadas rellenas (oscuro en tema claro, claro en tema oscuro), vacías con el color del fondo y borde; leyenda Ocupada/Vacía; "Posiciones ocupadas: 2 de 7".
- CA3 OK: hover sobre rueda ocupada abre popover con N.º de serie, Marca, Estado, Kilometraje; en rueda vacía: código, tipo de eje y "Sin neumático".
- Colapsable "Datos de la unidad" OK (abre/cierra).
- Tema claro y oscuro OK. Idioma es y en OK (dialog, popover, leyendas).
- Responsive OK: verificado manualmente por el humano (modo dispositivo de Chrome); `resize_window` no cambia el viewport.

Sin verificar en navegador: roles editor/lector, estado 404 (cubiertos por tests de backend).

## Bloqueos

Ninguno.
