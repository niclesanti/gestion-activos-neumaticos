package ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.usuarios.domain.dto;

import static ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.TestDataFactory.CLAVE;
import static ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.TestDataFactory.TOKEN;
import static ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.TestDataFactory.loginRequest;
import static ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.TestDataFactory.loginResponse;
import static ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.TestDataFactory.usuarioSesionAdministrador;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/** Los DTOs del login pueden terminar en un log: nunca deben llevar la contraseña ni el token. */
class ToStringSinSecretosTest {

    @Test
    void loginRequestOcultaLaContrasena() {
        assertThat(loginRequest().toString())
                .contains("administrador")
                .contains("password=****")
                .doesNotContain(CLAVE);
    }

    @Test
    void loginResponseOcultaElToken() {
        assertThat(loginResponse(usuarioSesionAdministrador()).toString())
                .contains("tipo=Bearer")
                .doesNotContain(TOKEN);
    }

}
