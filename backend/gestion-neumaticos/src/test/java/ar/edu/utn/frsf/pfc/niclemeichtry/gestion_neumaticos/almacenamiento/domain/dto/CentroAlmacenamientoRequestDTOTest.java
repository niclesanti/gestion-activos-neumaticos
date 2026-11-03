package ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.almacenamiento.domain.dto;

import static ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.TestDataFactory.PROVINCIA_ID;
import static ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.TestDataFactory.centroAlmacenamientoRequest;
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
 * Reglas del alta (las mismas que replica el schema zod del frontend).
 */
class CentroAlmacenamientoRequestDTOTest {

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

    private static Set<String> propiedadesInvalidas(CentroAlmacenamientoRequestDTO request) {
        Set<ConstraintViolation<CentroAlmacenamientoRequestDTO>> violaciones = validator.validate(request);
        return violaciones.stream().map(violacion -> violacion.getPropertyPath().toString())
                .collect(Collectors.toSet());
    }

    static Stream<Arguments> validos() {
        return Stream.of(
                Arguments.of("completo", centroAlmacenamientoRequest()),
                Arguments.of("domicilio sin los datos opcionales", centroAlmacenamientoRequest("Depósito Norte",
                        domicilioRequest("Belgrano", "100", null, null, PROVINCIA_ID, "Rafaela", null))),
                Arguments.of("nombre de 150 caracteres",
                        centroAlmacenamientoRequest("a".repeat(150), domicilioRequest())));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("validos")
    void altaValida(String caso, CentroAlmacenamientoRequestDTO request) {
        assertThat(propiedadesInvalidas(request)).isEmpty();
    }

    static Stream<Arguments> invalidos() {
        return Stream.of(
                Arguments.of("sin nombre", centroAlmacenamientoRequest(null, domicilioRequest()), "nombre"),
                Arguments.of("nombre en blanco", centroAlmacenamientoRequest("  ", domicilioRequest()), "nombre"),
                Arguments.of("nombre de 151 caracteres",
                        centroAlmacenamientoRequest("a".repeat(151), domicilioRequest()), "nombre"),
                Arguments.of("sin domicilio", centroAlmacenamientoRequest("Depósito Norte", null), "domicilio"),
                Arguments.of("domicilio sin calle", centroAlmacenamientoRequest("Depósito Norte",
                        domicilioRequest(null, "100", null, null, PROVINCIA_ID, "Rafaela", null)), "domicilio.calle"),
                Arguments.of("domicilio sin altura", centroAlmacenamientoRequest("Depósito Norte",
                        domicilioRequest("Belgrano", " ", null, null, PROVINCIA_ID, "Rafaela", null)),
                        "domicilio.altura"),
                Arguments.of("domicilio sin provincia", centroAlmacenamientoRequest("Depósito Norte",
                        domicilioRequest("Belgrano", "100", null, null, null, "Rafaela", null)),
                        "domicilio.provinciaId"),
                Arguments.of("domicilio sin localidad", centroAlmacenamientoRequest("Depósito Norte",
                        domicilioRequest("Belgrano", "100", null, null, PROVINCIA_ID, null, null)),
                        "domicilio.localidad"));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("invalidos")
    void altaInvalida(String caso, CentroAlmacenamientoRequestDTO request, String propiedad) {
        assertThat(propiedadesInvalidas(request)).contains(propiedad);
    }

    @Test
    @DisplayName("sin datos, se informan el nombre y el domicilio")
    void vacio() {
        assertThat(propiedadesInvalidas(new CentroAlmacenamientoRequestDTO(null, null)))
                .containsExactlyInAnyOrder("nombre", "domicilio");
    }

}
