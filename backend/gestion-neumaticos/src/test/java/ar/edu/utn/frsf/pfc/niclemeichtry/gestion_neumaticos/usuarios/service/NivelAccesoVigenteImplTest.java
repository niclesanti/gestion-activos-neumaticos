package ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.usuarios.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.usuarios.repository.UsuarioRepository;

@ExtendWith(MockitoExtension.class)
class NivelAccesoVigenteImplTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @InjectMocks
    private NivelAccesoVigenteImpl nivelAccesoVigente;

    @Test
    void devuelveElNivelQueTieneHoyElUsuarioEnLaBase() {
        UUID publicId = UUID.randomUUID();
        when(usuarioRepository.nivelAccesoVigente(publicId)).thenReturn(Optional.of("ROLE_EDITOR"));

        assertThat(nivelAccesoVigente.buscar(publicId)).contains("ROLE_EDITOR");
    }

    @Test
    void vacioSiElUsuarioYaNoExiste() {
        UUID publicId = UUID.randomUUID();
        when(usuarioRepository.nivelAccesoVigente(publicId)).thenReturn(Optional.empty());

        assertThat(nivelAccesoVigente.buscar(publicId)).isEmpty();
    }

}
