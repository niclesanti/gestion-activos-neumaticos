package ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.reparacion.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.common.api.DomicilioResponseDTO;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.config.MapstructConfig;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.reparacion.domain.dto.ServicioReparacionRequestDTO;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.reparacion.domain.dto.ServicioReparacionResponseDTO;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.reparacion.domain.entity.ServicioReparacion;

@Mapper(config = MapstructConfig.class)
public interface ServicioReparacionMapper {

    /** Copia los datos del alta; los ids los genera la base/Hibernate y el domicilio lo registra common. */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "publicId", ignore = true)
    @Mapping(target = "domicilioId", ignore = true)
    @Mapping(target = "cantidadNeumaticosEnReparacion", ignore = true)
    ServicioReparacion toEntity(ServicioReparacionRequestDTO servicioReparacionRequestDTO);

    /** El domicilio llega aparte, desde la fachada de common. */
    @Mapping(target = "publicId", source = "servicio.publicId")
    @Mapping(target = "nombre", source = "servicio.nombre")
    @Mapping(target = "email", source = "servicio.email")
    @Mapping(target = "telefono", source = "servicio.telefono")
    @Mapping(target = "cantidadNeumaticosEnReparacion", source = "servicio.cantidadNeumaticosEnReparacion")
    @Mapping(target = "domicilio", source = "domicilio")
    ServicioReparacionResponseDTO toResponseDTO(ServicioReparacion servicio, DomicilioResponseDTO domicilio);

}
