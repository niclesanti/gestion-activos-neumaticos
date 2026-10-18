package ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.neumaticos.domain.dto;

import static ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.TestDataFactory.neumaticoNuevoRequest;
import static ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.TestDataFactory.neumaticoRequest;
import static ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.TestDataFactory.neumaticoUsadoRequest;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.neumaticos.domain.entity.EstadoNeumatico;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.neumaticos.domain.entity.TipoEjeUnidadTransporte;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

/**
 * Reglas del alta (las mismas que replica el schema zod del frontend).
 */
class NeumaticoRequestDTOTest {

    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void crearValidador() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void cerrarValidador() {
        factory.close();
    }

    private static Set<String> propiedadesInvalidas(NeumaticoRequestDTO request) {
        Set<ConstraintViolation<NeumaticoRequestDTO>> violaciones = validator.validate(request);
        return violaciones.stream().map(violacion -> violacion.getPropertyPath().toString())
                .collect(java.util.stream.Collectors.toSet());
    }

    private static NeumaticoRequestDTO conTipos(TipoEjeUnidadTransporte origen, TipoEjeUnidadTransporte actual) {
        NeumaticoRequestDTO b = neumaticoNuevoRequest();
        return new NeumaticoRequestDTO(b.numeroSerie(), b.estadoActual(), b.kilometrajeTotal(),
                b.kilometrajeUltimaReparacion(), origen, actual, b.cantidadRecapados(), b.comentario(), b.marcaId(),
                b.proveedorId());
    }

    private static NeumaticoRequestDTO conComentarioYMarca(String comentario, Long marcaId) {
        NeumaticoRequestDTO b = neumaticoNuevoRequest();
        return new NeumaticoRequestDTO(b.numeroSerie(), b.estadoActual(), b.kilometrajeTotal(),
                b.kilometrajeUltimaReparacion(), b.tipoOrigen(), b.tipoActual(), b.cantidadRecapados(), comentario,
                marcaId, b.proveedorId());
    }

    static Stream<Arguments> validos() {
        return Stream.of(
                Arguments.of("nuevo con serie y proveedor", neumaticoNuevoRequest()),
                Arguments.of("usado sin serie ni proveedor", neumaticoUsadoRequest()),
                Arguments.of("serie vacía (equivale a no tenerla)", neumaticoRequest("", EstadoNeumatico.NUEVO, 0f, 0f, 0)),
                Arguments.of("serie con espacios, puntos, guiones y barras",
                        neumaticoRequest("DOT 4B.12-34/56", EstadoNeumatico.NUEVO, 0f, 0f, 0)),
                Arguments.of("serie de 100 caracteres", neumaticoRequest("A".repeat(100), EstadoNeumatico.NUEVO, 0f, 0f, 0)),
                Arguments.of("hombros sin recapados", neumaticoRequest(null, EstadoNeumatico.HOMBROS, 1000f, 1000f, 0)),
                Arguments.of("recapado con un recapado", neumaticoRequest(null, EstadoNeumatico.RECAPADO, 10f, 0f, 1)),
                Arguments.of("recapado sin garantía al máximo",
                        neumaticoRequest(null, EstadoNeumatico.RECAPADO_SIN_GARANTIA, 9_999_999f, 9_999_999f, 20)),
                Arguments.of("comentario de 512 caracteres", conComentarioYMarca("x".repeat(512), 1L)));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("validos")
    void altaValida(String caso, NeumaticoRequestDTO request) {
        assertThat(propiedadesInvalidas(request)).isEmpty();
    }

    static Stream<Arguments> invalidos() {
        return Stream.of(
                Arguments.of("serie de 101 caracteres", neumaticoRequest("A".repeat(101), EstadoNeumatico.NUEVO, 0f, 0f, 0),
                        "numeroSerie"),
                Arguments.of("serie con caracteres no permitidos",
                        neumaticoRequest("ABC#1", EstadoNeumatico.NUEVO, 0f, 0f, 0), "numeroSerie"),
                Arguments.of("serie que empieza con guion", neumaticoRequest("-ABC", EstadoNeumatico.NUEVO, 0f, 0f, 0),
                        "numeroSerie"),
                Arguments.of("sin estado", neumaticoRequest(null, null, 0f, 0f, 0), "estadoActual"),
                Arguments.of("estado de baja", neumaticoRequest(null, EstadoNeumatico.DE_BAJA, 0f, 0f, 0),
                        "estadoActualRegistrable"),
                Arguments.of("sin kilometraje total", neumaticoRequest(null, EstadoNeumatico.HOMBROS, null, 0f, 0),
                        "kilometrajeTotal"),
                Arguments.of("kilometraje total negativo", neumaticoRequest(null, EstadoNeumatico.HOMBROS, -1f, 0f, 0),
                        "kilometrajeTotal"),
                Arguments.of("kilometraje total excesivo",
                        neumaticoRequest(null, EstadoNeumatico.HOMBROS, 10_000_001f, 0f, 0), "kilometrajeTotal"),
                Arguments.of("sin kilometraje desde la última reparación",
                        neumaticoRequest(null, EstadoNeumatico.HOMBROS, 0f, null, 0), "kilometrajeUltimaReparacion"),
                Arguments.of("kilometraje desde la última reparación negativo",
                        neumaticoRequest(null, EstadoNeumatico.HOMBROS, 0f, -5f, 0), "kilometrajeUltimaReparacion"),
                Arguments.of("kilometraje desde la última reparación mayor al total",
                        neumaticoRequest(null, EstadoNeumatico.HOMBROS, 100f, 101f, 0),
                        "kilometrajeUltimaReparacionValido"),
                Arguments.of("sin recapados", neumaticoRequest(null, EstadoNeumatico.HOMBROS, 0f, 0f, null),
                        "cantidadRecapados"),
                Arguments.of("recapados negativos", neumaticoRequest(null, EstadoNeumatico.HOMBROS, 0f, 0f, -1),
                        "cantidadRecapados"),
                Arguments.of("más de 20 recapados", neumaticoRequest(null, EstadoNeumatico.HOMBROS, 0f, 0f, 21),
                        "cantidadRecapados"),
                Arguments.of("nuevo con kilometraje total", neumaticoRequest(null, EstadoNeumatico.NUEVO, 10f, 0f, 0),
                        "nuevoSinUso"),
                Arguments.of("nuevo con kilometraje desde la última reparación",
                        neumaticoRequest(null, EstadoNeumatico.NUEVO, 10f, 10f, 0), "nuevoSinUso"),
                Arguments.of("nuevo con recapados", neumaticoRequest(null, EstadoNeumatico.NUEVO, 0f, 0f, 1),
                        "nuevoSinUso"),
                Arguments.of("recapado sin recapados", neumaticoRequest(null, EstadoNeumatico.RECAPADO, 0f, 0f, 0),
                        "recapadoConRecapados"),
                Arguments.of("recapado usado sin recapados",
                        neumaticoRequest(null, EstadoNeumatico.RECAPADO_USADO, 0f, 0f, 0), "recapadoConRecapados"),
                Arguments.of("sin tipo de origen", conTipos(null, TipoEjeUnidadTransporte.TRACCION), "tipoOrigen"),
                Arguments.of("sin tipo actual", conTipos(TipoEjeUnidadTransporte.TRACCION, null), "tipoActual"),
                Arguments.of("comentario de 513 caracteres", conComentarioYMarca("x".repeat(513), 1L), "comentario"),
                Arguments.of("sin marca", conComentarioYMarca(null, null), "marcaId"),
                Arguments.of("marca no positiva", conComentarioYMarca(null, 0L), "marcaId"));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("invalidos")
    void altaInvalida(String caso, NeumaticoRequestDTO request, String propiedad) {
        assertThat(propiedadesInvalidas(request)).contains(propiedad);
    }

    @Test
    @DisplayName("sin datos, las reglas entre campos no se suman a los errores de obligatorios")
    void vacio() {
        NeumaticoRequestDTO vacio = new NeumaticoRequestDTO(null, null, null, null, null, null, null, null, null, null);

        assertThat(propiedadesInvalidas(vacio)).containsExactlyInAnyOrder("estadoActual", "kilometrajeTotal",
                "kilometrajeUltimaReparacion", "tipoOrigen", "tipoActual", "cantidadRecapados", "marcaId");
    }

    @Test
    @DisplayName("un neumático nuevo sin datos de uso no viola las reglas entre campos")
    void nuevoConNulos() {
        NeumaticoRequestDTO request = neumaticoRequest(null, EstadoNeumatico.NUEVO, null, null, null);

        assertThat(request.isNuevoSinUso()).isTrue();
        assertThat(request.isKilometrajeUltimaReparacionValido()).isTrue();
    }

}
