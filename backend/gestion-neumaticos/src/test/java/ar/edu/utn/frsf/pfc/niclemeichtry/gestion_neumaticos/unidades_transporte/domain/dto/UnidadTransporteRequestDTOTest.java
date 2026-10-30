package ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.unidades_transporte.domain.dto;

import static ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.TestDataFactory.CONFIGURACION_ID;
import static ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.TestDataFactory.unidadTransporteRequest;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.unidades_transporte.domain.Patentes;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

/**
 * Reglas del alta (las mismas que replica el schema zod del frontend).
 */
class UnidadTransporteRequestDTOTest {

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

    private static Set<String> propiedadesInvalidas(UnidadTransporteRequestDTO request) {
        Set<ConstraintViolation<UnidadTransporteRequestDTO>> violaciones = validator.validate(request);
        return violaciones.stream().map(violacion -> violacion.getPropertyPath().toString())
                .collect(Collectors.toSet());
    }

    @ParameterizedTest
    @ValueSource(strings = { "AB123CD", "ABC123", "ab123cd", " ab 123 cd ", "AB-123-CD", "abc-123" })
    @DisplayName("acepta los formatos anterior y Mercosur, con espacios, guiones o minúsculas")
    void patentesValidas(String patente) {
        assertThat(propiedadesInvalidas(unidadTransporteRequest(patente, CONFIGURACION_ID))).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = { "A123BC", "ABCD123", "AB12CD", "123ABC", "AB123C", "AB_123CD", "ÁB123CD" })
    @DisplayName("rechaza una patente con otro formato")
    void patentesInvalidas(String patente) {
        assertThat(propiedadesInvalidas(unidadTransporteRequest(patente, CONFIGURACION_ID)))
                .containsExactly("patenteValida");
    }

    @Test
    @DisplayName("la patente y la configuración son obligatorias; en blanco no repite el error de formato")
    void obligatorios() {
        assertThat(propiedadesInvalidas(unidadTransporteRequest(null, null)))
                .containsExactlyInAnyOrder("patente", "configuracionId");
        assertThat(propiedadesInvalidas(unidadTransporteRequest("  ", CONFIGURACION_ID))).containsExactly("patente");
    }

    @Test
    @DisplayName("no admite más de 15 caracteres (antes de normalizar)")
    void largoMaximo() {
        assertThat(propiedadesInvalidas(unidadTransporteRequest("AB - 123 - CD   ", CONFIGURACION_ID)))
                .containsExactly("patente");
    }

    @Test
    @DisplayName("Patentes normaliza a mayúsculas sin espacios ni guiones; null queda null")
    void normalizacion() {
        assertThat(Patentes.normalizar(" ab-123 cd ")).isEqualTo("AB123CD");
        assertThat(Patentes.normalizar(null)).isNull();
        assertThat(Patentes.esValida(null)).isFalse();
    }

}
