package ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.usuarios.config;

import static ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.TestDataFactory.HASH_CLAVE;
import static ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.TestDataFactory.administrador;
import static ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.TestDataFactory.lector;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.security.crypto.password.PasswordEncoder;

import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.usuarios.repository.UsuarioRepository;

@ExtendWith(MockitoExtension.class)
class SinCredencialesPorDefectoTest {

    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private SinCredencialesPorDefecto verificacion;

    @Test
    void arrancaSiNoHayUsuariosDelSeed() {
        when(usuarioRepository.buscarParaLogin(anyString())).thenReturn(Optional.empty());

        assertThatCode(() -> verificacion.run(new DefaultApplicationArguments())).doesNotThrowAnyException();
    }

    @Test
    void arrancaSiLosUsuariosDelSeedYaCambiaronSuClave() {
        when(usuarioRepository.buscarParaLogin(anyString())).thenReturn(Optional.empty());
        when(usuarioRepository.buscarParaLogin("administrador")).thenReturn(Optional.of(administrador()));
        when(passwordEncoder.matches("Admin.1234", HASH_CLAVE)).thenReturn(false);

        assertThatCode(() -> verificacion.run(new DefaultApplicationArguments())).doesNotThrowAnyException();
    }

    @Test
    void noArrancaSiQuedaUnUsuarioDelSeedConSuClavePorDefecto() {
        when(usuarioRepository.buscarParaLogin(anyString())).thenReturn(Optional.empty());
        when(usuarioRepository.buscarParaLogin("lector")).thenReturn(Optional.of(lector()));
        when(passwordEncoder.matches("Lector.1234", HASH_CLAVE)).thenReturn(true);

        assertThatThrownBy(() -> verificacion.run(new DefaultApplicationArguments()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("[lector]")
                .hasMessageNotContaining("Lector.1234");
    }

}
