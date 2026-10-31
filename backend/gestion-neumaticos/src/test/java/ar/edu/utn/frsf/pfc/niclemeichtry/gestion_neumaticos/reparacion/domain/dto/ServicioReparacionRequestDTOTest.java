package ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.reparacion.domain.dto;

import static ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.TestDataFactory.PROVINCIA_ID;
import static ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.TestDataFactory.domicilioRequest;
import static ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.TestDataFactory.servicioReparacionRequest;
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
class ServicioReparacionRequestDTOTest {

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

    private static Set<String> propiedadesInvalidas(ServicioReparacionRequestDTO request) {
        Set<ConstraintViolation<ServicioReparacionRequestDTO>> violaciones = validator.validate(request);
        return violaciones.stream().map(violacion -> violacion.getPropertyPath().toString())
                .collect(Collectors.toSet());
    }

    private static ServicioReparacionRequestDTO contacto(String email, String telefono) {
        return servicioReparacionRequest("Gomería Sur", email, telefono, null);
    }

    static Stream<Arguments> validos() {
        return Stream.of(
                Arguments.of("completo", servicioReparacionRequest()),
                Arguments.of("solo el nombre", contacto(null, null)),
                Arguments.of("contacto vacío (equivale a no tenerlo)", contacto("", "  ")),
                Arguments.of("email con espacios en los extremos", contacto(" ventas@sur.com ", null)),
                Arguments.of("teléfono local con paréntesis", contacto(null, "(0342) 455-1234")),
                Arguments.of("teléfono de 6 dígitos", contacto(null, "455123")),
                Arguments.of("nombre de 150 caracteres",
                        servicioReparacionRequest("a".repeat(150), null, null, null)));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("validos")
    void altaValida(String caso, ServicioReparacionRequestDTO request) {
        assertThat(propiedadesInvalidas(request)).isEmpty();
    }

    static Stream<Arguments> invalidos() {
        return Stream.of(
                Arguments.of("sin nombre", servicioReparacionRequest(null, null, null, null), "nombre"),
                Arguments.of("nombre en blanco", servicioReparacionRequest("  ", null, null, null), "nombre"),
                Arguments.of("nombre de 151 caracteres",
                        servicioReparacionRequest("a".repeat(151), null, null, null), "nombre"),
                Arguments.of("email sin arroba", contacto("ventas.sur.com", null), "email"),
                Arguments.of("email sin dominio", contacto("ventas@sur", null), "email"),
                Arguments.of("email con espacios internos", contacto("ven tas@sur.com", null), "email"),
                Arguments.of("email de 255 caracteres", contacto("a".repeat(247) + "@sur.com", null), "email"),
                Arguments.of("teléfono con letras", contacto(null, "0342-VENTAS"), "telefono"),
                Arguments.of("teléfono de 5 dígitos", contacto(null, "45512"), "telefono"),
                Arguments.of("teléfono de 31 dígitos", contacto(null, "1".repeat(31)), "telefono"),
                Arguments.of("domicilio sin calle", servicioReparacionRequest("Gomería Sur", null, null,
                        domicilioRequest(null, "100", null, null, PROVINCIA_ID, "Rafaela", null)), "domicilio.calle"),
                Arguments.of("domicilio sin provincia", servicioReparacionRequest("Gomería Sur", null, null,
                        domicilioRequest("Belgrano", "100", null, null, null, "Rafaela", null)),
                        "domicilio.provinciaId"));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("invalidos")
    void altaInvalida(String caso, ServicioReparacionRequestDTO request, String propiedad) {
        assertThat(propiedadesInvalidas(request)).contains(propiedad);
    }

    @Test
    @DisplayName("sin datos, solo se informa el nombre")
    void vacio() {
        assertThat(propiedadesInvalidas(new ServicioReparacionRequestDTO(null, null, null, null)))
                .containsExactly("nombre");
    }

}
