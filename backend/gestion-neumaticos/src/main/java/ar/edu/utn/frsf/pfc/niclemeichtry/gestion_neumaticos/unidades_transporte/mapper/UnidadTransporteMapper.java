package ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.unidades_transporte.mapper;

import java.util.Map;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.config.MapstructConfig;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.unidades_transporte.domain.dto.ConfiguracionUnidadDTO;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.unidades_transporte.domain.dto.ConfiguracionUnidadListadoDTO;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.unidades_transporte.domain.dto.EjeConfiguracionDTO;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.neumaticos.api.NeumaticoMontadoDTO;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.unidades_transporte.domain.dto.SlotDetalleDTO;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.unidades_transporte.domain.dto.SlotUnidadTransporteDTO;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.unidades_transporte.domain.dto.UnidadTransporteDetalleDTO;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.unidades_transporte.domain.dto.UnidadTransporteListadoDTO;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.unidades_transporte.domain.dto.UnidadTransporteResponseDTO;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.unidades_transporte.domain.entity.ConfiguracionUnidad;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.unidades_transporte.domain.entity.EjeConfiguracion;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.unidades_transporte.domain.entity.SlotUnidadTransporte;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.unidades_transporte.domain.entity.UnidadTransporte;

@Mapper(config = MapstructConfig.class)
public interface UnidadTransporteMapper {

    /** La configuración va resumida; los slots, con su código derivado. */
    UnidadTransporteResponseDTO toResponseDTO(UnidadTransporte unidadTransporte);

    /** Fila del listado: solo patente y configuración resumida, sin slots. */
    UnidadTransporteListadoDTO toListadoDTO(UnidadTransporte unidadTransporte);

    /** Ficha: los neumáticos vienen resueltos por id interno del slot (los vacíos no figuran). */
    @Mapping(target = "slots", expression = "java(unidad.getSlots().stream().map(slot -> toSlotDetalleDTO(slot, slot.getNeumaticoActualId() == null ? null : neumaticos.get(slot.getNeumaticoActualId()))).toList())")
    UnidadTransporteDetalleDTO toDetalleDTO(UnidadTransporte unidad, Map<Long, NeumaticoMontadoDTO> neumaticos);

    @Mapping(target = "ocupado", expression = "java(slot.estaOcupado())")
    SlotDetalleDTO toSlotDetalleDTO(SlotUnidadTransporte slot, NeumaticoMontadoDTO neumatico);

    ConfiguracionUnidadListadoDTO toConfiguracionListadoDTO(ConfiguracionUnidad configuracion);

    @Mapping(target = "ocupado", expression = "java(slot.estaOcupado())")
    SlotUnidadTransporteDTO toSlotDTO(SlotUnidadTransporte slot);

    @Mapping(target = "totalEjes", expression = "java(configuracion.obtenerTotalEjes())")
    @Mapping(target = "totalSlots", expression = "java(configuracion.obtenerTotalSlots())")
    ConfiguracionUnidadDTO toConfiguracionDTO(ConfiguracionUnidad configuracion);

    @Mapping(target = "cantidadNeumaticos", expression = "java(eje.cantidadNeumaticos())")
    EjeConfiguracionDTO toEjeDTO(EjeConfiguracion eje);

}
