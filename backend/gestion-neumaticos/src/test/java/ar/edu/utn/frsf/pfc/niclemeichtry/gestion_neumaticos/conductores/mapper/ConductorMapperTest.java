package ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.conductores.mapper;

import static ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.TestDataFactory.conductor;
import static ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.TestDataFactory.conductorListado;
import static ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.TestDataFactory.conductorRequest;
import static ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.TestDataFactory.conductorResponse;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.conductores.api.ConductorResumenDTO;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.conductores.domain.dto.ConductorListadoDTO;
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
    void toEntityNormalizaLosTextos() {
        Conductor conductor = mapper.toEntity(new ConductorRequestDTO(" 12345678 ", "  Juan ", " Pérez  "));

        assertThat(conductor.getDni()).isEqualTo("12345678");
        assertThat(conductor.getNombre()).isEqualTo("Juan");
        assertThat(conductor.getApellido()).isEqualTo("Pérez");
    }

    @Test
    void toEntityUnDniEnBlancoQuedaNulo() {
        assertThat(mapper.toEntity(new ConductorRequestDTO("   ", "Juan", "Pérez")).getDni()).isNull();
    }

    @Test
    void toListadoDTOYToResumenCopianLoQueNecesitaCadaUno() {
        Conductor conductor = conductor(conductorRequest());

        assertThat(mapper.toListadoDTO(conductor)).isEqualTo(conductorListado(conductor));
        assertThat(mapper.toResumen(conductor)).isEqualTo(new ConductorResumenDTO(conductor.getId(),
                conductor.getPublicId(), "Juan", "Pérez"));
        assertThat(mapper.toResumen(conductor).nombreCompleto()).isEqualTo("Juan Pérez");
    }

    @Test
    void unOrigenNuloDaNulo() {
        assertThat(mapper.toEntity(null)).isNull();
        assertThat(mapper.toResponseDTO(null)).isNull();
        assertThat(mapper.toListadoDTO(null)).isNull();
        assertThat(mapper.toResumen(null)).isNull();
    }

    @Test
    void losCamposNulosQuedanNulos() {
        assertThat(mapper.toEntity(new ConductorRequestDTO(null, null, null))).isEqualTo(new Conductor());
        assertThat(mapper.toResponseDTO(new Conductor())).isEqualTo(new ConductorResponseDTO(null, null, null, null));
        assertThat(mapper.toListadoDTO(new Conductor())).isEqualTo(new ConductorListadoDTO(null, null, null, null));
        assertThat(mapper.toResumen(new Conductor())).isEqualTo(new ConductorResumenDTO(null, null, null, null));
    }

}
