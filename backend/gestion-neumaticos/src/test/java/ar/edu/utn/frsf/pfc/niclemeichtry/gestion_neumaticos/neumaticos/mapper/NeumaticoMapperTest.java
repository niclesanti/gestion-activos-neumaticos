package ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.neumaticos.mapper;

import static ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.TestDataFactory.marca;
import static ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.TestDataFactory.neumatico;
import static ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.TestDataFactory.neumaticoNuevoRequest;
import static ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.TestDataFactory.neumaticoResponse;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.neumaticos.domain.dto.MarcaNeumaticoResponseDTO;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.neumaticos.domain.dto.NeumaticoRequestDTO;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.neumaticos.domain.dto.NeumaticoResponseDTO;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.neumaticos.domain.entity.MarcaNeumatico;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.neumaticos.domain.entity.Neumatico;

/** Prueba la implementación que genera MapStruct (sin contexto de Spring: no tiene dependencias). */
class NeumaticoMapperTest {

    private final NeumaticoMapper mapper = new NeumaticoMapperImpl();

    @Test
    void toEntityCopiaElAltaSinIdsNiEstadoOperativo() {
        NeumaticoRequestDTO request = neumaticoNuevoRequest();

        Neumatico neumatico = mapper.toEntity(request);

        assertThat(neumatico.getNumeroSerie()).isEqualTo(request.numeroSerie());
        assertThat(neumatico.getEstadoActual()).isEqualTo(request.estadoActual());
        assertThat(neumatico.getKilometrajeTotal()).isEqualTo(request.kilometrajeTotal());
        assertThat(neumatico.getKilometrajeUltimaReparacion()).isEqualTo(request.kilometrajeUltimaReparacion());
        assertThat(neumatico.getTipoOrigen()).isEqualTo(request.tipoOrigen());
        assertThat(neumatico.getTipoActual()).isEqualTo(request.tipoActual());
        assertThat(neumatico.getCantidadRecapados()).isEqualTo(request.cantidadRecapados());
        assertThat(neumatico.getComentario()).isEqualTo(request.comentario());
        assertThat(neumatico.getId()).isNull();
        assertThat(neumatico.getPublicId()).isNull();
        assertThat(neumatico.getEnEspera()).isNull();
        assertThat(neumatico.getSituacionOperativaVigente()).isNull();
        assertThat(neumatico.getMarca()).isNull();
    }

    @Test
    void toResponseDTOIncluyeElNombreDeLaMarca() {
        Neumatico neumatico = neumatico(neumaticoNuevoRequest());

        assertThat(mapper.toResponseDTO(neumatico)).isEqualTo(neumaticoResponse(neumatico));
    }

    @Test
    void toMarcaDTO() {
        assertThat(mapper.toMarcaDTO(marca())).isEqualTo(new MarcaNeumaticoResponseDTO(1L, "Fate"));
    }

    @Test
    void unOrigenNuloDaNulo() {
        assertThat(mapper.toEntity(null)).isNull();
        assertThat(mapper.toResponseDTO(null)).isNull();
        assertThat(mapper.toMarcaDTO(null)).isNull();
    }

    @Test
    void losCamposNulosQuedanNulos() {
        assertThat(mapper.toEntity(new NeumaticoRequestDTO(null, null, null, null, null, null, null, null, null, null)))
                .isEqualTo(new Neumatico());
        assertThat(mapper.toResponseDTO(new Neumatico()))
                .isEqualTo(new NeumaticoResponseDTO(null, null, null, null, null, null, null, null, null, null, null,
                        null, null, null, null, null, null, null, null));
        assertThat(mapper.toResponseDTO(Neumatico.builder().marca(new MarcaNeumatico()).build()).marcaNombre())
                .isNull();
        assertThat(mapper.toMarcaDTO(new MarcaNeumatico())).isEqualTo(new MarcaNeumaticoResponseDTO(null, null));
    }

}
