package ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.neumaticos.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.config.MapstructConfig;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.config.NormalizacionTexto;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.neumaticos.api.UbicacionOperativaDTO;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.neumaticos.domain.dto.MarcaNeumaticoResponseDTO;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.neumaticos.domain.dto.NeumaticoDetalleDTO;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.neumaticos.domain.dto.NeumaticoListadoDTO;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.neumaticos.domain.dto.NeumaticoRequestDTO;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.neumaticos.domain.dto.NeumaticoResponseDTO;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.neumaticos.domain.entity.MarcaNeumatico;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.neumaticos.domain.entity.Neumatico;

@Mapper(config = MapstructConfig.class, uses = NormalizacionTexto.class)
public interface NeumaticoMapper {

    /**
     * Copia los datos del alta; el número de serie se normaliza (recortado y en
     * mayúsculas, vacío = sin serie). El estado operativo inicial y la marca los
     * asigna el servicio; los ids los genera la base/Hibernate.
     */
    @Mapping(target = "numeroSerie", qualifiedByName = "mayusculas")
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "publicId", ignore = true)
    @Mapping(target = "enEspera", ignore = true)
    @Mapping(target = "situacionOperativaVigente", ignore = true)
    @Mapping(target = "slotMontadoId", ignore = true)
    @Mapping(target = "centroAlmacenamientoId", ignore = true)
    @Mapping(target = "servicioReparacionId", ignore = true)
    @Mapping(target = "marca", ignore = true)
    Neumatico toEntity(NeumaticoRequestDTO neumaticoRequestDTO);

    /**
     * Los datos de ubicación (unidad de transporte, centro de almacenamiento,
     * servicio de reparación) quedan en null hasta que existan esos módulos.
     */
    @Mapping(target = "marcaNombre", source = "marca.nombre")
    NeumaticoResponseDTO toResponseDTO(Neumatico neumatico);

    /** Fila del listado: el nombre del centro lo resuelve el servicio por lote (null si no está en stock). */
    @Mapping(target = "marca", source = "neumatico.marca.nombre")
    @Mapping(target = "centroAlmacenamiento", source = "centroAlmacenamiento")
    NeumaticoListadoDTO toListadoDTO(Neumatico neumatico, String centroAlmacenamiento);

    /** Ficha: la ubicación viene resuelta por el servicio según la situación del neumático. */
    @Mapping(target = "marca", source = "neumatico.marca.nombre")
    @Mapping(target = "ubicacion", source = "ubicacion")
    NeumaticoDetalleDTO toDetalleDTO(Neumatico neumatico, UbicacionOperativaDTO ubicacion);

    MarcaNeumaticoResponseDTO toMarcaDTO(MarcaNeumatico marcaNeumatico);

    // No implementar aún. Se deja como referencia para futuras implementaciones.
    //void updateEntityFromDTO(NeumaticoUpdateDTO neumaticoUpdateDTO, @MappingTarget Neumatico neumatico);
}
