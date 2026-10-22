package ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.conductores.mapper;

import static ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.TestDataFactory.conductor;
import static ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.TestDataFactory.conductorRequest;
import static ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.TestDataFactory.conductorResponse;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.conductores.domain.dto.ConductorRequestDTO;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.conductores.domain.dto.ConductorResponseDTO;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.conductores.domain.entity.Conductor;

/** Prueba la implementación que genera MapStruct (sin contexto de Spring: no tiene dependencias). */
class ConductorMapperTest {

    private final ConductorMapper mapper = new ConductorMapperImpl();

    @Test
    void toEntityCopiaElAltaSinIds() {
        ConductorRequestDTO request = conductorRequest();

        Conductor conductor = mapper.toEntity(request);

        assertThat(conductor.getDni()).isEqualTo(request.dni());
        assertThat(conductor.getNombre()).isEqualTo(request.nombre());
        assertThat(conductor.getApellido()).isEqualTo(request.apellido());
        assertThat(conductor.getId()).isNull();
        assertThat(conductor.getPublicId()).isNull();
    }

    @Test
    void toResponseDTOCopiaLosDatosPublicos() {
        Conductor conductor = conductor(conductorRequest());

        assertThat(mapper.toResponseDTO(conductor)).isEqualTo(conductorResponse(conductor));
    }

    @Test
    void unOrigenNuloDaNulo() {
        assertThat(mapper.toEntity(null)).isNull();
        assertThat(mapper.toResponseDTO(null)).isNull();
    }

    @Test
    void losCamposNulosQuedanNulos() {
        assertThat(mapper.toEntity(new ConductorRequestDTO(null, null, null))).isEqualTo(new Conductor());
        assertThat(mapper.toResponseDTO(new Conductor())).isEqualTo(new ConductorResponseDTO(null, null, null, null));
    }

}
