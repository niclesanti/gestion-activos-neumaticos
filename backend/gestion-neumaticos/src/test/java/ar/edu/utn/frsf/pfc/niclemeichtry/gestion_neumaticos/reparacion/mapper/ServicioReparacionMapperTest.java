package ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.reparacion.mapper;

import static ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.TestDataFactory.domicilio;
import static ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.TestDataFactory.domicilioRequest;
import static ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.TestDataFactory.domicilioResponse;
import static ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.TestDataFactory.servicioReparacion;
import static ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.TestDataFactory.servicioReparacionRequest;
import static ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.TestDataFactory.servicioReparacionResponse;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.common.api.DomicilioResponseDTO;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.reparacion.domain.dto.ServicioReparacionRequestDTO;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.reparacion.domain.dto.ServicioReparacionResponseDTO;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.reparacion.domain.entity.ServicioReparacion;

/** Prueba la implementación que genera MapStruct (sin contexto de Spring: no tiene dependencias). */
class ServicioReparacionMapperTest {

    private final ServicioReparacionMapper mapper = new ServicioReparacionMapperImpl();

    @Test
    void toEntityCopiaElAltaSinIdsNiDomicilioNiCantidad() {
        ServicioReparacionRequestDTO request = servicioReparacionRequest();

        ServicioReparacion servicio = mapper.toEntity(request);

        assertThat(servicio.getNombre()).isEqualTo(request.nombre());
        assertThat(servicio.getEmail()).isEqualTo(request.email());
        assertThat(servicio.getTelefono()).isEqualTo(request.telefono());
        assertThat(servicio.getId()).isNull();
        assertThat(servicio.getPublicId()).isNull();
        assertThat(servicio.getDomicilioId()).isNull();
        assertThat(servicio.getCantidadNeumaticosEnReparacion()).isZero();
    }

    @Test
    void toResponseDTOCombinaServicioYDomicilio() {
        ServicioReparacion servicio = servicioReparacion(servicioReparacionRequest());
        servicio.enviarNeumaticoReparacion();
        DomicilioResponseDTO domicilio = domicilioResponse(domicilio(domicilioRequest()));

        assertThat(mapper.toResponseDTO(servicio, domicilio))
                .isEqualTo(servicioReparacionResponse(servicio, domicilio));
        assertThat(mapper.toResponseDTO(servicio, null)).isEqualTo(servicioReparacionResponse(servicio, null));
        assertThat(mapper.toResponseDTO(null, domicilio))
                .isEqualTo(new ServicioReparacionResponseDTO(null, null, null, null, null, domicilio));
    }

    @Test
    void unOrigenNuloDaNulo() {
        assertThat(mapper.toEntity(null)).isNull();
        assertThat(mapper.toResponseDTO(null, null)).isNull();
    }

    @Test
    void losCamposNulosQuedanNulos() {
        assertThat(mapper.toEntity(new ServicioReparacionRequestDTO(null, null, null, null)))
                .isEqualTo(new ServicioReparacion());
        assertThat(mapper.toResponseDTO(new ServicioReparacion(), null))
                .isEqualTo(new ServicioReparacionResponseDTO(null, null, null, null, 0, null));
    }

}
