package ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.proveedores.mapper;

import static ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.TestDataFactory.domicilio;
import static ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.TestDataFactory.domicilioRequest;
import static ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.TestDataFactory.domicilioResponse;
import static ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.TestDataFactory.proveedor;
import static ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.TestDataFactory.proveedorRequest;
import static ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.TestDataFactory.proveedorResponse;
import static ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.TestDataFactory.proveedorResumen;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.common.api.DomicilioResponseDTO;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.proveedores.api.ProveedorResumenDTO;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.proveedores.domain.dto.ProveedorListadoDTO;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.proveedores.domain.dto.ProveedorRequestDTO;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.proveedores.domain.dto.ProveedorResponseDTO;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.proveedores.domain.entity.ProveedorNeumaticos;

/** Prueba la implementación que genera MapStruct (sin contexto de Spring: no tiene dependencias). */
class ProveedorMapperTest {

    private final ProveedorMapper mapper = new ProveedorMapperImpl();

    @Test
    void toEntityCopiaElAltaSinIdsNiDomicilio() {
        ProveedorRequestDTO request = proveedorRequest();

        ProveedorNeumaticos proveedor = mapper.toEntity(request);

        assertThat(proveedor.getNombre()).isEqualTo(request.nombre());
        assertThat(proveedor.getEmail()).isEqualTo(request.email());
        assertThat(proveedor.getTelefono()).isEqualTo(request.telefono());
        assertThat(proveedor.getId()).isNull();
        assertThat(proveedor.getPublicId()).isNull();
        assertThat(proveedor.getDomicilioId()).isNull();
    }

    @Test
    void toResponseDTOCombinaProveedorYDomicilio() {
        ProveedorNeumaticos proveedor = proveedor(proveedorRequest());
        DomicilioResponseDTO domicilio = domicilioResponse(domicilio(domicilioRequest()));

        assertThat(mapper.toResponseDTO(proveedor, domicilio)).isEqualTo(proveedorResponse(proveedor, domicilio));
        assertThat(mapper.toResponseDTO(proveedor, null)).isEqualTo(proveedorResponse(proveedor, null));
        assertThat(mapper.toResponseDTO(null, domicilio))
                .isEqualTo(new ProveedorResponseDTO(null, null, null, null, domicilio));
    }

    @Test
    void toListadoYResumenCopianLosDatosMinimos() {
        ProveedorNeumaticos proveedor = proveedor(proveedorRequest());

        assertThat(mapper.toListadoDTO(proveedor))
                .isEqualTo(new ProveedorListadoDTO(proveedor.getPublicId(), proveedor.getNombre()));
        assertThat(mapper.toResumen(proveedor)).isEqualTo(proveedorResumen());
    }

    @Test
    void unOrigenNuloDaNulo() {
        assertThat(mapper.toEntity(null)).isNull();
        assertThat(mapper.toResponseDTO(null, null)).isNull();
        assertThat(mapper.toListadoDTO(null)).isNull();
        assertThat(mapper.toResumen(null)).isNull();
    }

    @Test
    void losCamposNulosQuedanNulos() {
        assertThat(mapper.toEntity(new ProveedorRequestDTO(null, null, null, null)))
                .isEqualTo(new ProveedorNeumaticos());
        assertThat(mapper.toResponseDTO(new ProveedorNeumaticos(), null))
                .isEqualTo(new ProveedorResponseDTO(null, null, null, null, null));
        assertThat(mapper.toListadoDTO(new ProveedorNeumaticos())).isEqualTo(new ProveedorListadoDTO(null, null));
        assertThat(mapper.toResumen(new ProveedorNeumaticos())).isEqualTo(new ProveedorResumenDTO(null, null, null));
    }

}
