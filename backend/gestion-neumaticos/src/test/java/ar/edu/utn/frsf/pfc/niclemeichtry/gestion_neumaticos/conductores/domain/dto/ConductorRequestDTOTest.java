package ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.conductores.domain.dto;

import static ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.TestDataFactory.conductorRequest;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

/**
 * Reglas del alta (las mismas que replica el schema zod del frontend).
 */
class ConductorRequestDTOTest {

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

    private static Set<String> propiedadesInvalidas(ConductorRequestDTO request) {
        Set<ConstraintViolation<ConductorRequestDTO>> violaciones = validator.validate(request);
        return violaciones.stream().map(violacion -> violacion.getPropertyPath().toString())
                .collect(Collectors.toSet());
    }

    static Stream<Arguments> validos() {
        return Stream.of(
                Arguments.of("con DNI de 8 dígitos", conductorRequest()),
                Arguments.of("con DNI de 7 dígitos", conductorRequest("1234567", "Juan", "Pérez")),
                Arguments.of("sin DNI", conductorRequest(null, "Juan", "Pérez")),
                Arguments.of("DNI vacío (equivale a no tenerlo)", conductorRequest("", "Juan", "Pérez")),
                Arguments.of("DNI en blanco", conductorRequest("   ", "Juan", "Pérez")),
                Arguments.of("DNI con espacios en los extremos", conductorRequest(" 30123456 ", "Juan", "Pérez")),
                Arguments.of("nombre y apellido de 100 caracteres",
                        conductorRequest(null, "a".repeat(100), "b".repeat(100))));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("validos")
    void altaValida(String caso, ConductorRequestDTO request) {
        assertThat(propiedadesInvalidas(request)).isEmpty();
    }

    static Stream<Arguments> invalidos() {
        return Stream.of(
                Arguments.of("DNI de 6 dígitos", conductorRequest("123456", "Juan", "Pérez"), "dni"),
                Arguments.of("DNI de 9 dígitos", conductorRequest("123456789", "Juan", "Pérez"), "dni"),
                Arguments.of("DNI con puntos", conductorRequest("30.123.456", "Juan", "Pérez"), "dni"),
                Arguments.of("DNI con letras", conductorRequest("30123A56", "Juan", "Pérez"), "dni"),
                Arguments.of("DNI con espacios internos", conductorRequest("30 123 456", "Juan", "Pérez"), "dni"),
                Arguments.of("sin nombre", conductorRequest(null, null, "Pérez"), "nombre"),
                Arguments.of("nombre en blanco", conductorRequest(null, "   ", "Pérez"), "nombre"),
                Arguments.of("nombre de 101 caracteres", conductorRequest(null, "a".repeat(101), "Pérez"), "nombre"),
                Arguments.of("sin apellido", conductorRequest(null, "Juan", null), "apellido"),
                Arguments.of("apellido en blanco", conductorRequest(null, "Juan", ""), "apellido"),
                Arguments.of("apellido de 101 caracteres", conductorRequest(null, "Juan", "b".repeat(101)),
                        "apellido"));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("invalidos")
    void altaInvalida(String caso, ConductorRequestDTO request, String propiedad) {
        assertThat(propiedadesInvalidas(request)).contains(propiedad);
    }

    @Test
    @DisplayName("sin datos, solo se informan los obligatorios")
    void vacio() {
        assertThat(propiedadesInvalidas(new ConductorRequestDTO(null, null, null)))
                .containsExactlyInAnyOrder("nombre", "apellido");
    }

}
