package ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.unidades_transporte.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.config.MapstructConfig;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.unidades_transporte.domain.dto.ConfiguracionUnidadDTO;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.unidades_transporte.domain.dto.EjeConfiguracionDTO;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.unidades_transporte.domain.dto.SlotUnidadTransporteDTO;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.unidades_transporte.domain.dto.UnidadTransporteResponseDTO;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.unidades_transporte.domain.entity.ConfiguracionUnidad;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.unidades_transporte.domain.entity.EjeConfiguracion;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.unidades_transporte.domain.entity.SlotUnidadTransporte;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.unidades_transporte.domain.entity.UnidadTransporte;

@Mapper(config = MapstructConfig.class)
public interface UnidadTransporteMapper {

    /** La configuración va resumida; los slots, con su código derivado. */
    UnidadTransporteResponseDTO toResponseDTO(UnidadTransporte unidadTransporte);

    @Mapping(target = "ocupado", expression = "java(slot.estaOcupado())")
    SlotUnidadTransporteDTO toSlotDTO(SlotUnidadTransporte slot);

    @Mapping(target = "totalEjes", expression = "java(configuracion.obtenerTotalEjes())")
    @Mapping(target = "totalSlots", expression = "java(configuracion.obtenerTotalSlots())")
    ConfiguracionUnidadDTO toConfiguracionDTO(ConfiguracionUnidad configuracion);

    @Mapping(target = "cantidadNeumaticos", expression = "java(eje.cantidadNeumaticos())")
    EjeConfiguracionDTO toEjeDTO(EjeConfiguracion eje);

}
