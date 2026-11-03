package ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.common.auditoria;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.exception.OperacionNoPermitidaException;

@ExtendWith(MockitoExtension.class)
class EnsambladorAuditoriaTest {

    private static final UUID CREADOR = UUID.randomUUID();
    private static final UUID EDITOR = UUID.randomUUID();
    private static final UUID BORRADOR = UUID.randomUUID();
    private static final UUID DESCONOCIDO = UUID.randomUUID();
    private static final Instant ALTA = Instant.parse("2026-10-01T10:00:00Z");

    @Mock private AuditoresResolver resolver;
    @InjectMocks private EnsambladorAuditoria ensamblador;

    /** Entidad auditada mínima: los campos de AuditableEntity no tienen setters. */
    private static class Fila extends AuditableEntity {
        private final UUID publicId = UUID.randomUUID();
        private final String nombre;

        Fila(String nombre) {
            this.nombre = nombre;
        }
    }

    /** Fila como la dejaría el listener de auditoría: alta y una modificación 60 s después. */
    private static Fila fila(String nombre, UUID creador, UUID editor) {
        Fila fila = new Fila(nombre);
        ReflectionTestUtils.setField(fila, "createdAt", ALTA);
        ReflectionTestUtils.setField(fila, "createdBy", creador);
        ReflectionTestUtils.setField(fila, "updatedAt", ALTA.plusSeconds(60));
        ReflectionTestUtils.setField(fila, "updatedBy", editor);
        return fila;
    }

    private static AuditorDTO auditor(UUID id, String usuario) {
        return new AuditorDTO(id, usuario, usuario.toUpperCase());
    }

    @Test
    @DisplayName("resuelve a todos los usuarios de la página en una sola llamada y completa cada ficha")
    void ensamblaLaPagina() {
        Fila activa = fila("Activa", CREADOR, EDITOR);
        Fila dada = fila("Dada de baja", CREADOR, CREADOR);
        dada.darDeBaja(BORRADOR, ALTA.plusSeconds(120));
        Page<Fila> pagina = new PageImpl<>(List.of(activa, dada), PageRequest.of(0, 20), 2);
        when(resolver.resolver(anyCollection())).thenReturn(Map.of(
                CREADOR, auditor(CREADOR, "creador"),
                EDITOR, auditor(EDITOR, "editor"),
                BORRADOR, auditor(BORRADOR, "borrador")));

        Page<RegistroAuditoriaDTO> resultado = ensamblador.ensamblar(pagina, f -> f.publicId, f -> f.nombre);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Collection<UUID>> ids = ArgumentCaptor.forClass(Collection.class);
        verify(resolver).resolver(ids.capture());
        assertThat(ids.getValue()).containsExactlyInAnyOrder(CREADOR, EDITOR, BORRADOR);

        assertThat(resultado.getTotalElements()).isEqualTo(2);
        RegistroAuditoriaDTO ficha = resultado.getContent().get(0);
        assertThat(ficha.publicId()).isEqualTo(activa.publicId);
        assertThat(ficha.descripcion()).isEqualTo("Activa");
        assertThat(ficha.creadoEn()).isEqualTo(ALTA);
        assertThat(ficha.creadoPor().nombreUsuario()).isEqualTo("creador");
        assertThat(ficha.modificadoEn()).isEqualTo(ALTA.plusSeconds(60));
        assertThat(ficha.modificadoPor().nombreUsuario()).isEqualTo("editor");
        assertThat(ficha.dadoDeBajaEn()).isNull();
        assertThat(ficha.dadoDeBajaPor()).isNull();
        assertThat(ficha.estado()).isEqualTo(EstadoRegistro.ACTIVO);

        RegistroAuditoriaDTO baja = resultado.getContent().get(1);
        assertThat(baja.estado()).isEqualTo(EstadoRegistro.BAJA);
        assertThat(baja.dadoDeBajaEn()).isEqualTo(ALTA.plusSeconds(120));
        assertThat(baja.dadoDeBajaPor().nombreUsuario()).isEqualTo("borrador");
    }

    @Test
    @DisplayName("un usuario que ya no se puede resolver queda solo con su id; sin usuario, null")
    void usuariosNoResueltosYSinFirma() {
        Fila conDesconocido = fila("Desconocido", DESCONOCIDO, DESCONOCIDO);
        Fila sinFirma = fila("Anterior a la auditoría", null, null);
        when(resolver.resolver(anyCollection())).thenReturn(Map.of());

        Page<RegistroAuditoriaDTO> resultado = ensamblador.ensamblar(
                new PageImpl<>(List.of(conDesconocido, sinFirma)), f -> f.publicId, f -> f.nombre);

        assertThat(resultado.getContent().get(0).creadoPor()).isEqualTo(AuditorDTO.desconocido(DESCONOCIDO));
        assertThat(resultado.getContent().get(0).creadoPor().nombreUsuario()).isNull();
        assertThat(resultado.getContent().get(1).creadoPor()).isNull();
        assertThat(resultado.getContent().get(1).modificadoPor()).isNull();
    }

    @Test
    @DisplayName("una página vacía o sin firmas no consulta usuarios")
    void sinUsuariosNoConsulta() {
        ensamblador.ensamblar(new PageImpl<Fila>(List.of()), f -> f.publicId, f -> f.nombre);
        ensamblador.ensamblar(new PageImpl<>(List.of(fila("Sin firma", null, null))), f -> f.publicId, f -> f.nombre);

        verifyNoInteractions(resolver);
    }

    @Test
    @DisplayName("la baja es única: darDeBaja dos veces falla y conserva la primera")
    void bajaDuplicada() {
        Fila fila = fila("X", CREADOR, CREADOR);
        assertThat(fila.isDadoDeBaja()).isFalse();
        fila.darDeBaja(BORRADOR, ALTA);

        assertThatThrownBy(() -> fila.darDeBaja(EDITOR, ALTA.plusSeconds(5)))
                .isInstanceOf(OperacionNoPermitidaException.class);
        assertThat(fila.isDadoDeBaja()).isTrue();
        assertThat(fila.getDeletedBy()).isEqualTo(BORRADOR);
        assertThat(fila.getDeletedAt()).isEqualTo(ALTA);
    }
}
