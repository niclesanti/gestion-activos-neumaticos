package ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.neumaticos.api;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Puerto del stock de los centros de almacenamiento que consume {@code neumaticos} y que implementa
 * {@code almacenamiento}: así el montaje descuenta el stock en la misma transacción sin que neumáticos
 * dependa de almacenamiento (que ya depende de esta API: sería un ciclo).
 */
public interface StockCentroAlmacenamiento {

    /**
     * Id interno del centro con ese identificador público (para filtrar neumáticos por centro).
     *
     * @return el id, o vacío si el centro no existe.
     */
    Optional<Long> buscarIdPorPublicId(UUID publicId);

    /**
     * Nombres de los centros por lote de ids internos, en una sola consulta. Los ids inexistentes no figuran.
     */
    Map<Long, String> buscarNombresPorIds(Collection<Long> ids);

    /**
     * Descuenta un neumático del stock del centro (salió para montarse).
     *
     * @throws ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.exception.EntidadNoEncontradaException
     *         si el centro no existe.
     * @throws ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.exception.OperacionNoPermitidaException
     *         si el centro no tiene stock.
     */
    void retirarNeumatico(Long centroId);

}
