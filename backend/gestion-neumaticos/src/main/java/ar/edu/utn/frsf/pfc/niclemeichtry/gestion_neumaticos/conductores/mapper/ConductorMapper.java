package ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.conductores.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.conductores.domain.dto.ConductorRequestDTO;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.conductores.domain.dto.ConductorResponseDTO;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.conductores.domain.entity.Conductor;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.config.MapstructConfig;

@Mapper(config = MapstructConfig.class)
public interface ConductorMapper {

    /** Copia los datos del alta; los ids los genera la base/Hibernate. */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "publicId", ignore = true)
    Conductor toEntity(ConductorRequestDTO conductorRequestDTO);

    ConductorResponseDTO toResponseDTO(Conductor conductor);

}
