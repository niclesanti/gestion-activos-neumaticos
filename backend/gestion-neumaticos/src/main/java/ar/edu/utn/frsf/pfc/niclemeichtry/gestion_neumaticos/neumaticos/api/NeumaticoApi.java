package ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.neumaticos.api;

import java.util.Collection;
import java.util.Map;

public interface NeumaticoApi {

    /**
     * Busca neumáticos por lote de ids internos (la referencia que guarda el slot) en una sola consulta.
     * Los ids inexistentes no figuran en el resultado.
     *
     * @param ids ids internos de neumático.
     * @return mapa id interno a datos del neumático.
     */
    Map<Long, NeumaticoMontadoDTO> buscarMontadosPorIds(Collection<Long> ids);

}
