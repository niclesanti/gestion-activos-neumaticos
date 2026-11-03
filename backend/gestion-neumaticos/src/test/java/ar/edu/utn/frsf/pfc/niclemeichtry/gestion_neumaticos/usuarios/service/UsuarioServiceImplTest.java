package ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.usuarios.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.common.auditoria.EnsambladorAuditoria;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.common.auditoria.FiltroAuditoria;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.common.auditoria.RegistroAuditoriaDTO;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.exception.ParametroInvalidoException;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.usuarios.domain.entity.Usuario;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.usuarios.repository.UsuarioRepository;

@ExtendWith(MockitoExtension.class)
class UsuarioServiceImplTest {

    @Mock private UsuarioRepository usuarioRepository;
    @Mock private EnsambladorAuditoria ensamblador;
    @InjectMocks private UsuarioServiceImpl servicio;

    @Test
    @DisplayName("aplica los filtros y ensambla la página de fichas")
    void ensamblaLaPagina() {
        Page<Usuario> pagina = new PageImpl<>(List.of(new Usuario()));
        Page<RegistroAuditoriaDTO> esperado = new PageImpl<>(List.of());
        when(usuarioRepository.findAll(ArgumentMatchers.<Specification<Usuario>>any(), any(Pageable.class)))
                .thenReturn(pagina);
        when(ensamblador.ensamblar(eq(pagina), any(), any())).thenReturn(esperado);

        Page<RegistroAuditoriaDTO> resultado = servicio.listarAuditoria(
                new FiltroAuditoria("victoria", FiltroAuditoria.EstadoFiltro.TODOS, null, null),
                PageRequest.of(0, 20, Sort.by("updatedAt")));

        assertThat(resultado).isSameAs(esperado);
    }

    @Test
    @DisplayName("rechaza ordenar por un campo que no es de auditoría")
    void rechazaOrdenNoPermitido() {
        FiltroAuditoria sinFiltros = new FiltroAuditoria(null, null, null, null);
        PageRequest ordenInvalido = PageRequest.of(0, 20, Sort.by("clave"));

        assertThatThrownBy(() -> servicio.listarAuditoria(sinFiltros, ordenInvalido))
                .isInstanceOf(ParametroInvalidoException.class);
        verifyNoInteractions(ensamblador);
    }
}
