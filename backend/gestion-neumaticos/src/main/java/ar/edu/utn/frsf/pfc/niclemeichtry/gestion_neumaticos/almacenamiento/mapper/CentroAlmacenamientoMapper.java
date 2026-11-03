package ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.almacenamiento.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.almacenamiento.domain.dto.CentroAlmacenamientoRequestDTO;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.almacenamiento.domain.dto.CentroAlmacenamientoResponseDTO;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.almacenamiento.domain.entity.CentroAlmacenamiento;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.common.api.DomicilioResponseDTO;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.config.MapstructConfig;

@Mapper(config = MapstructConfig.class)
public interface CentroAlmacenamientoMapper {

    /** Copia los datos del alta; los ids los genera la base/Hibernate y el domicilio lo registra common. */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "publicId", ignore = true)
    @Mapping(target = "domicilioId", ignore = true)
    @Mapping(target = "cantidadNeumaticosEnStock", ignore = true)
    CentroAlmacenamiento toEntity(CentroAlmacenamientoRequestDTO centroAlmacenamientoRequestDTO);

    /** El domicilio llega aparte, desde la fachada de common. */
    @Mapping(target = "publicId", source = "centro.publicId")
    @Mapping(target = "nombre", source = "centro.nombre")
    @Mapping(target = "cantidadNeumaticosEnStock", source = "centro.cantidadNeumaticosEnStock")
    @Mapping(target = "domicilio", source = "domicilio")
    CentroAlmacenamientoResponseDTO toResponseDTO(CentroAlmacenamiento centro, DomicilioResponseDTO domicilio);

}
