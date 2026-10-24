package ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.common.api;

import static ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.TestDataFactory.PROVINCIA_ID;
import static ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.TestDataFactory.domicilioRequest;
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
 * Reglas del alta de domicilio (las mismas que replica el schema zod del frontend).
 */
class DomicilioRequestDTOTest {

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

    private static Set<String> propiedadesInvalidas(DomicilioRequestDTO request) {
        Set<ConstraintViolation<DomicilioRequestDTO>> violaciones = validator.validate(request);
        return violaciones.stream().map(violacion -> violacion.getPropertyPath().toString())
                .collect(Collectors.toSet());
    }

    private static DomicilioRequestDTO conCodigoPostal(String codigoPostal) {
        return domicilioRequest("San Martín", "2550", null, null, PROVINCIA_ID, "Santa Fe", codigoPostal);
    }

    private static DomicilioRequestDTO enRafaela(String calle, String altura, String piso, String departamento) {
        return domicilioRequest(calle, altura, piso, departamento, PROVINCIA_ID, "Rafaela", null);
    }

    static Stream<Arguments> validos() {
        return Stream.of(
                Arguments.of("completo", domicilioRequest()),
                Arguments.of("sin opcionales", enRafaela("Belgrano", "100", null, null)),
                Arguments.of("opcionales vacíos", enRafaela("Belgrano", "S/N", "", " ")),
                Arguments.of("código postal de 4 dígitos con espacios", conCodigoPostal(" 3000 ")),
                Arguments.of("código postal vacío", conCodigoPostal("")),
                Arguments.of("CPA en minúsculas", conCodigoPostal("s3000abc")),
                Arguments.of("textos en el máximo", domicilioRequest("a".repeat(100), "1".repeat(10),
                        "p".repeat(10), "d".repeat(10), PROVINCIA_ID, "l".repeat(100), null)));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("validos")
    void altaValida(String caso, DomicilioRequestDTO request) {
        assertThat(propiedadesInvalidas(request)).isEmpty();
    }

    static Stream<Arguments> invalidos() {
        return Stream.of(
                Arguments.of("calle en blanco", enRafaela(" ", "100", null, null), "calle"),
                Arguments.of("calle de 101 caracteres", enRafaela("a".repeat(101), "100", null, null), "calle"),
                Arguments.of("altura en blanco", enRafaela("Belgrano", "", null, null), "altura"),
                Arguments.of("altura de 11 caracteres", enRafaela("Belgrano", "1".repeat(11), null, null), "altura"),
                Arguments.of("piso de 11 caracteres", enRafaela("Belgrano", "100", "p".repeat(11), null), "piso"),
                Arguments.of("departamento de 11 caracteres", enRafaela("Belgrano", "100", null, "d".repeat(11)),
                        "departamento"),
                Arguments.of("localidad en blanco", domicilioRequest("Belgrano", "100", null, null, PROVINCIA_ID,
                        "  ", null), "localidad"),
                Arguments.of("localidad de 101 caracteres", domicilioRequest("Belgrano", "100", null, null,
                        PROVINCIA_ID, "l".repeat(101), null), "localidad"),
                Arguments.of("código postal de 3 dígitos", conCodigoPostal("300"), "codigoPostal"),
                Arguments.of("código postal de 5 dígitos", conCodigoPostal("30000"), "codigoPostal"),
                Arguments.of("CPA incompleto", conCodigoPostal("S3000AB"), "codigoPostal"));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("invalidos")
    void altaInvalida(String caso, DomicilioRequestDTO request, String propiedad) {
        assertThat(propiedadesInvalidas(request)).contains(propiedad);
    }

    @Test
    @DisplayName("sin datos, solo se informan los obligatorios")
    void vacio() {
        assertThat(propiedadesInvalidas(new DomicilioRequestDTO(null, null, null, null, null, null, null)))
                .containsExactlyInAnyOrder("calle", "altura", "provinciaId", "localidad");
    }

}
