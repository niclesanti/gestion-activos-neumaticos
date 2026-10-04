package ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.usuarios.domain.entity;

import static ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.TestDataFactory.HASH_CLAVE;
import static ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.TestDataFactory.administrador;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class UsuarioTest {

    @Test
    void normalizaNombreDeUsuarioYEmailAMinusculasSinEspacios() {
        Usuario usuario = administrador();
        usuario.setNombreUsuario("  JPerez ");
        usuario.setEmail(" JPerez@Empresa.COM.ar  ");

        usuario.normalizar();

        assertThat(usuario.getNombreUsuario()).isEqualTo("jperez");
        assertThat(usuario.getEmail()).isEqualTo("jperez@empresa.com.ar");
    }

    @Test
    void noNormalizaElNombreYApellido() {
        Usuario usuario = administrador();
        usuario.setNombreApellido("Ana Administradora");

        usuario.normalizar();

        assertThat(usuario.getNombreApellido()).isEqualTo("Ana Administradora");
    }

    @Test
    void toleraCamposNulos() {
        Usuario usuario = new Usuario();

        usuario.normalizar();

        assertThat(usuario.getNombreUsuario()).isNull();
        assertThat(usuario.getEmail()).isNull();
    }

    @Test
    void elToStringNoExponeElHashDeLaClave() {
        assertThat(administrador().toString()).doesNotContain(HASH_CLAVE).doesNotContain("clave");
    }

}
