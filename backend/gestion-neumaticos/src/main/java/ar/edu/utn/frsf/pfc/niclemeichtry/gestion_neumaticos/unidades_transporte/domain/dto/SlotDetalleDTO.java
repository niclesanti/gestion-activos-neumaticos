package ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.unidades_transporte.domain.dto;

import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.neumaticos.api.NeumaticoMontadoDTO;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.unidades_transporte.domain.entity.Lado;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.unidades_transporte.domain.entity.PosicionRueda;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.unidades_transporte.domain.entity.TipoEje;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.unidades_transporte.domain.entity.TipoSlot;

/** Posición de la ficha de la unidad, con el neumático montado (null si está vacía). */
public record SlotDetalleDTO(
    String codigo,
    TipoSlot tipoSlot,
    Integer nroEje,
    TipoEje tipoEje,
    Lado lado,
    PosicionRueda posicionRueda,
    Integer nroAuxilio,
    Boolean ocupado,
    NeumaticoMontadoDTO neumatico
) {

}
