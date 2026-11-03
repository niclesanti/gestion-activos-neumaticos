package ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.almacenamiento.mapper;

import static ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.TestDataFactory.centroAlmacenamiento;
import static ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.TestDataFactory.centroAlmacenamientoRequest;
import static ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.TestDataFactory.centroAlmacenamientoResponse;
import static ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.TestDataFactory.domicilio;
import static ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.TestDataFactory.domicilioRequest;
import static ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.TestDataFactory.domicilioResponse;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.almacenamiento.domain.dto.CentroAlmacenamientoRequestDTO;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.almacenamiento.domain.dto.CentroAlmacenamientoResponseDTO;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.almacenamiento.domain.entity.CentroAlmacenamiento;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.common.api.DomicilioResponseDTO;

/** Prueba la implementación que genera MapStruct (sin contexto de Spring: no tiene dependencias). */
class CentroAlmacenamientoMapperTest {

    private final CentroAlmacenamientoMapper mapper = new CentroAlmacenamientoMapperImpl();

    @Test
    void toEntityCopiaElAltaSinIdsNiDomicilioNiStock() {
        CentroAlmacenamientoRequestDTO request = centroAlmacenamientoRequest();

        CentroAlmacenamiento centro = mapper.toEntity(request);

        assertThat(centro.getNombre()).isEqualTo(request.nombre());
        assertThat(centro.getId()).isNull();
        assertThat(centro.getPublicId()).isNull();
        assertThat(centro.getDomicilioId()).isNull();
        assertThat(centro.getCantidadNeumaticosEnStock()).isZero();
    }

    @Test
    void toResponseDTOCombinaCentroYDomicilio() {
        CentroAlmacenamiento centro = centroAlmacenamiento(centroAlmacenamientoRequest());
        centro.agregarNeumaticoStock();
        DomicilioResponseDTO domicilio = domicilioResponse(domicilio(domicilioRequest()));

        assertThat(mapper.toResponseDTO(centro, domicilio))
                .isEqualTo(centroAlmacenamientoResponse(centro, domicilio));
        assertThat(mapper.toResponseDTO(centro, null)).isEqualTo(centroAlmacenamientoResponse(centro, null));
        assertThat(mapper.toResponseDTO(null, domicilio))
                .isEqualTo(new CentroAlmacenamientoResponseDTO(null, null, null, domicilio));
    }

    @Test
    void unOrigenNuloDaNulo() {
        assertThat(mapper.toEntity(null)).isNull();
        assertThat(mapper.toResponseDTO(null, null)).isNull();
    }

    @Test
    void losCamposNulosQuedanNulos() {
        assertThat(mapper.toEntity(new CentroAlmacenamientoRequestDTO(null, null)))
                .isEqualTo(new CentroAlmacenamiento());
        assertThat(mapper.toResponseDTO(new CentroAlmacenamiento(), null))
                .isEqualTo(new CentroAlmacenamientoResponseDTO(null, null, 0, null));
    }

}
