package ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.unidades_transporte.domain.dto;

import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.unidades_transporte.domain.entity.TipoEje;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.unidades_transporte.domain.entity.TipoRodado;

public record EjeConfiguracionDTO(
    Integer orden,
    TipoEje tipoEje,
    TipoRodado rodado,
    Integer cantidadNeumaticos
) {

}
