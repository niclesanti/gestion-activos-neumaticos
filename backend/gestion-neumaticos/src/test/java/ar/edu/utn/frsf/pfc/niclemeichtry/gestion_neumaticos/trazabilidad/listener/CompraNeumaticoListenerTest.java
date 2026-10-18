package ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.trazabilidad.listener;

import static ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.TestDataFactory.autenticacion;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;

import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.neumaticos.event.NeumaticoComprado;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.trazabilidad.domain.entity.EventoCompra;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.trazabilidad.domain.entity.EventoNeumatico;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.trazabilidad.domain.entity.TipoEventoNeumatico;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.trazabilidad.repository.EventoNeumaticoRepository;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.usuarios.domain.entity.NivelAcceso;

@ExtendWith(MockitoExtension.class)
class CompraNeumaticoListenerTest {

    @Mock private EventoNeumaticoRepository repository;
    @InjectMocks private CompraNeumaticoListener listener;

    @AfterEach
    void limpiar() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void guardaLaCompraConElSnapshotDelEvento() {
        Instant fecha = Instant.parse("2026-10-17T12:00:00Z");
        // El contexto del request que publicó el evento llega propagado al hilo del listener.
        SecurityContextHolder.getContext().setAuthentication(autenticacion(UUID.randomUUID(), NivelAcceso.ROLE_EDITOR));
        when(repository.save(any())).thenAnswer(invocacion -> {
            assertThat(SecurityContextHolder.getContext().getAuthentication()).as("guarda como el sistema").isNull();
            return invocacion.getArgument(0);
        });

        listener.on(new NeumaticoComprado(100L, "ABC-123", 1L, "Neumáticos del Litoral S.A.", "usuario", fecha,
                "Compra inicial"));

        ArgumentCaptor<EventoNeumatico> guardado = ArgumentCaptor.forClass(EventoNeumatico.class);
        verify(repository).save(guardado.capture());
        assertThat(guardado.getValue()).isInstanceOfSatisfying(EventoCompra.class, compra -> {
            assertThat(compra.getId()).isNull();
            assertThat(compra.getTipo()).isEqualTo(TipoEventoNeumatico.COMPRA);
            assertThat(compra.getFecha()).isEqualTo(fecha);
            assertThat(compra.getUsuario()).isEqualTo("usuario");
            assertThat(compra.getComentario()).isEqualTo("Compra inicial");
            assertThat(compra.getNeumaticoId()).isEqualTo(100L);
            assertThat(compra.getNumeroSerieNeumatico()).isEqualTo("ABC-123");
            assertThat(compra.getProveedorId()).isEqualTo(1L);
            assertThat(compra.getNombreProveedor()).isEqualTo("Neumáticos del Litoral S.A.");
        });
    }

}
