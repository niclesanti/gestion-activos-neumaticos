package ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.proveedores.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.common.api.DomicilioResponseDTO;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.config.MapstructConfig;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.proveedores.api.ProveedorResumenDTO;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.proveedores.domain.dto.ProveedorListadoDTO;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.proveedores.domain.dto.ProveedorRequestDTO;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.proveedores.domain.dto.ProveedorResponseDTO;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.proveedores.domain.entity.ProveedorNeumaticos;

@Mapper(config = MapstructConfig.class)
public interface ProveedorMapper {

    /** Copia los datos del alta; los ids los genera la base/Hibernate y el domicilio lo registra common. */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "publicId", ignore = true)
    @Mapping(target = "domicilioId", ignore = true)
    ProveedorNeumaticos toEntity(ProveedorRequestDTO proveedorRequestDTO);

    /** El domicilio llega aparte, desde la fachada de common. */
    @Mapping(target = "publicId", source = "proveedor.publicId")
    @Mapping(target = "nombre", source = "proveedor.nombre")
    @Mapping(target = "email", source = "proveedor.email")
    @Mapping(target = "telefono", source = "proveedor.telefono")
    @Mapping(target = "domicilio", source = "domicilio")
    ProveedorResponseDTO toResponseDTO(ProveedorNeumaticos proveedor, DomicilioResponseDTO domicilio);

    ProveedorListadoDTO toListadoDTO(ProveedorNeumaticos proveedor);

    ProveedorResumenDTO toResumen(ProveedorNeumaticos proveedor);

}
