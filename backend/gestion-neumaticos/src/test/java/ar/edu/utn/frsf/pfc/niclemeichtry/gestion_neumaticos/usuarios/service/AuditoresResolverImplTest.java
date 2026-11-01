package ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.usuarios.service;

import static ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.TestDataFactory.administrador;
import static ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.TestDataFactory.editor;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.common.auditoria.AuditorDTO;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.usuarios.domain.entity.Usuario;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.usuarios.mapper.UsuarioMapper;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.usuarios.mapper.UsuarioMapperImpl;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.usuarios.repository.UsuarioRepository;

@ExtendWith(MockitoExtension.class)
class AuditoresResolverImplTest {

    @Mock private UsuarioRepository usuarioRepository;
    private final UsuarioMapper usuarioMapper = new UsuarioMapperImpl();

    @Test
    @DisplayName("resuelve los usuarios pedidos en una sola consulta, indexados por publicId")
    void resuelveEnUnaConsulta() {
        Usuario admin = administrador();
        admin.setPublicId(UUID.randomUUID());
        Usuario editor = editor();
        editor.setPublicId(UUID.randomUUID());
        Set<UUID> ids = Set.of(admin.getPublicId(), editor.getPublicId(), UUID.randomUUID());
        when(usuarioRepository.findAllByPublicIdIn(ids)).thenReturn(List.of(admin, editor));

        var resultado = new AuditoresResolverImpl(usuarioRepository, usuarioMapper).resolver(ids);

        assertThat(resultado).containsOnlyKeys(admin.getPublicId(), editor.getPublicId());
        AuditorDTO dto = resultado.get(admin.getPublicId());
        assertThat(dto.nombreUsuario()).isEqualTo(admin.getNombreUsuario());
        assertThat(dto.nombreApellido()).isEqualTo(admin.getNombreApellido());
    }

    @Test
    @DisplayName("sin ids no consulta la base")
    void sinIds() {
        assertThat(new AuditoresResolverImpl(usuarioRepository, usuarioMapper).resolver(Set.of())).isEmpty();

        verifyNoInteractions(usuarioRepository);
    }
}
