package ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.usuarios.domain.validation;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class IdentificadorValidatorTest {

    private final IdentificadorValidator validator = new IdentificadorValidator();

    @ParameterizedTest
    @ValueSource(strings = {
            "administrador", "jperez", "j.perez", "j_perez-2", "abc", "ADMIN",
            "usuario@ejemplo.com", "Juan.Perez@empresa.com.ar", "a+b@sub.dominio.io",
            " administrador " })
    void aceptaUsuariosYEmailsValidos(String identificador) {
        assertThat(validator.isValid(identificador, null)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "ab", "a234567890123456789012345678901", "juan perez", "juan#perez", "usuario\u0000",
            "sin-dominio@", "@ejemplo.com", "usuario@ejemplo", "usu ario@ejemplo.com", "a@b@c.com" })
    void rechazaFormatosInvalidos(String identificador) {
        assertThat(validator.isValid(identificador, null)).isFalse();
    }

    /** La obligatoriedad la valida @NotBlank: este validador no duplica el error. */
    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = { "   " })
    void delegaVacioANotBlank(String identificador) {
        assertThat(validator.isValid(identificador, null)).isTrue();
    }

}
