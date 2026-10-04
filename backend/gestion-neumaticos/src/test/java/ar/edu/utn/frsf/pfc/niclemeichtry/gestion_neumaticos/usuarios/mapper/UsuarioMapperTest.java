package ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.usuarios.mapper;

import static ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.TestDataFactory.administrador;
import static ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.TestDataFactory.usuarioSesion;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.usuarios.domain.dto.UsuarioSesionDTO;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.usuarios.domain.entity.Usuario;

/** Prueba la implementación que genera MapStruct (sin contexto de Spring: no tiene dependencias). */
class UsuarioMapperTest {

    private final UsuarioMapper mapper = new UsuarioMapperImpl();

    @Test
    void mapeaLosDatosDeSesionSinExponerIdNiClave() {
        Usuario usuario = administrador();

        assertThat(mapper.toSesionDTO(usuario)).isEqualTo(usuarioSesion(usuario));
    }

    @Test
    void unUsuarioNuloDaNulo() {
        assertThat(mapper.toSesionDTO(null)).isNull();
    }

    @Test
    void losCamposNulosQuedanNulos() {
        assertThat(mapper.toSesionDTO(new Usuario()))
                .isEqualTo(new UsuarioSesionDTO(null, null, null, null, null));
    }

}
