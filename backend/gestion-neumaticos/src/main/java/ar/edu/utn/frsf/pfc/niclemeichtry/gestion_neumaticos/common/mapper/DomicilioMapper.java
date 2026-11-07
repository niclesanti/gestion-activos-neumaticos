package ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.common.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.common.api.DomicilioRequestDTO;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.common.api.DomicilioResponseDTO;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.common.domain.dto.ProvinciaResponseDTO;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.common.domain.entity.Domicilio;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.common.domain.entity.Provincia;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.config.MapstructConfig;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.config.NormalizacionTexto;

@Mapper(config = MapstructConfig.class, uses = NormalizacionTexto.class)
public interface DomicilioMapper {

    /** Copia los datos del alta, normalizados; el id lo genera Hibernate y la localidad la resuelve el servicio. */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "localidad", ignore = true)
    @Mapping(target = "calle", qualifiedByName = "recortar")
    @Mapping(target = "altura", qualifiedByName = "recortar")
    @Mapping(target = "piso", qualifiedByName = "opcional")
    @Mapping(target = "departamento", qualifiedByName = "opcional")
    Domicilio toEntity(DomicilioRequestDTO domicilioRequestDTO);

    @Mapping(target = "localidad", source = "localidad.nombre")
    @Mapping(target = "codigoPostal", source = "localidad.codigoPostal")
    @Mapping(target = "provinciaId", source = "localidad.provincia.id")
    @Mapping(target = "provincia", source = "localidad.provincia.nombre")
    DomicilioResponseDTO toResponseDTO(Domicilio domicilio);

    ProvinciaResponseDTO toProvinciaDTO(Provincia provincia);

}
