package ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.unidades_transporte.domain.dto;

import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.unidades_transporte.domain.entity.Lado;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.unidades_transporte.domain.entity.PosicionRueda;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.unidades_transporte.domain.entity.TipoEje;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.unidades_transporte.domain.entity.TipoSlot;

/** Posición de la unidad. Los datos de eje o de auxilio son null según el tipo de slot. */
public record SlotUnidadTransporteDTO(
    String codigo,
    TipoSlot tipoSlot,
    Integer nroEje,
    TipoEje tipoEje,
    Lado lado,
    PosicionRueda posicionRueda,
    Integer nroAuxilio,
    Boolean ocupado
) {

}
