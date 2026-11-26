package ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.neumaticos.api;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;

public interface NeumaticoApi {

    /**
     * Busca neumáticos por lote de ids internos (la referencia que guarda el slot) en una sola consulta.
     * Los ids inexistentes no figuran en el resultado.
     *
     * @param ids ids internos de neumático.
     * @return mapa id interno a datos del neumático.
     */
    Map<Long, NeumaticoMontadoDTO> buscarMontadosPorIds(Collection<Long> ids);

    /**
     * Monta el neumático en el slot (HU-31): queda {@code MONTADO}, fuera de espera y sin centro; si estaba en
     * stock, el centro lo descuenta en la misma transacción. Tipo de eje y kilometraje no cambian.
     *
     * @param publicId identificador público del neumático.
     * @param slotId   id interno del slot destino.
     * @return el neumático montado.
     * @throws ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.exception.EntidadNoEncontradaException
     *         si el neumático no existe.
     * @throws ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.exception.NeumaticoNoDisponibleException
     *         si no está en stock ni en espera, o está dado de baja (estado, situación o baja lógica).
     */
    NeumaticoMontajeDTO montar(UUID publicId, Long slotId);

    /**
     * Desmonta el neumático de su slot: queda {@code EN_ESPERA}, sin slot. Se escribe en la base en el
     * momento, así el slot queda libre para el que se monte a continuación.
     *
     * @param id id interno del neumático (el que guarda el slot).
     * @return el neumático desmontado.
     * @throws ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.exception.EntidadNoEncontradaException
     *         si el neumático no existe.
     */
    NeumaticoMontajeDTO desmontar(Long id);

}
