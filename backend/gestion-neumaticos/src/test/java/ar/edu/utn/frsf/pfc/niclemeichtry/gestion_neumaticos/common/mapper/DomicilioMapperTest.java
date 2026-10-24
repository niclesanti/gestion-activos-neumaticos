package ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.common.mapper;

import static ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.TestDataFactory.domicilio;
import static ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.TestDataFactory.domicilioRequest;
import static ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.TestDataFactory.domicilioResponse;
import static ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.TestDataFactory.provincia;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.common.api.DomicilioRequestDTO;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.common.api.DomicilioResponseDTO;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.common.domain.dto.ProvinciaResponseDTO;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.common.domain.entity.Domicilio;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.common.domain.entity.Localidad;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.common.domain.entity.Provincia;

/** Prueba la implementación que genera MapStruct (sin contexto de Spring: no tiene dependencias). */
class DomicilioMapperTest {

    private final DomicilioMapper mapper = new DomicilioMapperImpl();

    @Test
    void toEntityCopiaElAltaSinIdNiLocalidad() {
        DomicilioRequestDTO request = domicilioRequest();

        Domicilio domicilio = mapper.toEntity(request);

        assertThat(domicilio.getCalle()).isEqualTo(request.calle());
        assertThat(domicilio.getAltura()).isEqualTo(request.altura());
        assertThat(domicilio.getPiso()).isEqualTo(request.piso());
        assertThat(domicilio.getDepartamento()).isEqualTo(request.departamento());
        assertThat(domicilio.getId()).isNull();
        assertThat(domicilio.getLocalidad()).isNull();
    }

    @Test
    void toResponseDTOAplanaLocalidadYProvincia() {
        Domicilio domicilio = domicilio(domicilioRequest());

        assertThat(mapper.toResponseDTO(domicilio)).isEqualTo(domicilioResponse(domicilio));
    }

    @Test
    void toProvinciaDTOCopiaIdYNombre() {
        assertThat(mapper.toProvinciaDTO(provincia())).isEqualTo(new ProvinciaResponseDTO(21L, "Santa Fe"));
    }

    @Test
    void unOrigenNuloDaNulo() {
        assertThat(mapper.toEntity(null)).isNull();
        assertThat(mapper.toResponseDTO(null)).isNull();
        assertThat(mapper.toProvinciaDTO(null)).isNull();
    }

    @Test
    void losCamposNulosQuedanNulos() {
        assertThat(mapper.toEntity(new DomicilioRequestDTO(null, null, null, null, null, null, null)))
                .isEqualTo(new Domicilio());
        DomicilioResponseDTO vacio = new DomicilioResponseDTO(null, null, null, null, null, null, null, null);
        assertThat(mapper.toResponseDTO(new Domicilio())).isEqualTo(vacio);
        assertThat(mapper.toResponseDTO(Domicilio.builder().localidad(new Localidad()).build())).isEqualTo(vacio);
        assertThat(mapper.toProvinciaDTO(new Provincia())).isEqualTo(new ProvinciaResponseDTO(null, null));
    }

}
