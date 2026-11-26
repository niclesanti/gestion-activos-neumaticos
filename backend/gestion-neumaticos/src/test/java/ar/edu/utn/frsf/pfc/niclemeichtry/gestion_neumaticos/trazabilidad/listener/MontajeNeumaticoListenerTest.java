package ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.trazabilidad.listener;

import static ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.TestDataFactory.PATENTE;
import static ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.TestDataFactory.autenticacion;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;

import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.trazabilidad.domain.entity.EventoAsignacion;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.trazabilidad.domain.entity.EventoDesasignacion;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.trazabilidad.domain.entity.EventoNeumatico;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.trazabilidad.domain.entity.TipoEventoNeumatico;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.trazabilidad.repository.EventoNeumaticoRepository;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.unidades_transporte.event.NeumaticoAsignado;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.unidades_transporte.event.NeumaticoDesasignado;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.usuarios.domain.entity.NivelAcceso;

@ExtendWith(MockitoExtension.class)
class MontajeNeumaticoListenerTest {

    private final Instant fecha = Instant.parse("2026-11-20T12:00:00Z");

    @Mock private EventoNeumaticoRepository repository;
    @InjectMocks private MontajeNeumaticoListener listener;

    @BeforeEach
    void guardaComoElSistema() {
        // El contexto del request que publicó el evento llega propagado al hilo del listener.
        SecurityContextHolder.getContext().setAuthentication(autenticacion(UUID.randomUUID(), NivelAcceso.ROLE_EDITOR));
        when(repository.save(any())).thenAnswer(invocacion -> {
            assertThat(SecurityContextHolder.getContext().getAuthentication()).as("guarda como el sistema").isNull();
            return invocacion.getArgument(0);
        });
    }

    @AfterEach
    void limpiar() {
        SecurityContextHolder.clearContext();
    }

    private EventoNeumatico guardado() {
        ArgumentCaptor<EventoNeumatico> guardado = ArgumentCaptor.forClass(EventoNeumatico.class);
        verify(repository).save(guardado.capture());
        return guardado.getValue();
    }

    @Test
    @DisplayName("CA2: guarda la asignación con unidad, posición, conductor, fecha y usuario")
    void asignacion() {
        listener.on(new NeumaticoAsignado(20L, "XYZ-999", 50L, PATENTE, 500L, "E1-IZQ", 100L, "Juan Pérez",
                "usuario", fecha));

        assertThat(guardado()).isInstanceOfSatisfying(EventoAsignacion.class, asignacion -> {
            assertThat(asignacion.getTipo()).isEqualTo(TipoEventoNeumatico.ASIGNACION);
            assertThat(asignacion.getFecha()).isEqualTo(fecha);
            assertThat(asignacion.getUsuario()).isEqualTo("usuario");
            assertThat(asignacion.getNeumaticoId()).isEqualTo(20L);
            assertThat(asignacion.getNumeroSerieNeumatico()).isEqualTo("XYZ-999");
            assertThat(asignacion.getUnidadId()).isEqualTo(50L);
            assertThat(asignacion.getPatenteUnidad()).isEqualTo(PATENTE);
            assertThat(asignacion.getSlotId()).isEqualTo(500L);
            assertThat(asignacion.getPosicionSlot()).isEqualTo("E1-IZQ");
            assertThat(asignacion.getConductorId()).isEqualTo(100L);
            assertThat(asignacion.getNombreConductor()).isEqualTo("Juan Pérez");
        });
    }

    @Test
    @DisplayName("CA7: guarda la desasignación con unidad, posición, fecha y usuario")
    void desasignacion() {
        listener.on(new NeumaticoDesasignado(10L, null, 50L, PATENTE, 500L, "E1-IZQ", "usuario", fecha));

        assertThat(guardado()).isInstanceOfSatisfying(EventoDesasignacion.class, desasignacion -> {
            assertThat(desasignacion.getTipo()).isEqualTo(TipoEventoNeumatico.DESASIGNACION);
            assertThat(desasignacion.getFecha()).isEqualTo(fecha);
            assertThat(desasignacion.getUsuario()).isEqualTo("usuario");
            assertThat(desasignacion.getNeumaticoId()).isEqualTo(10L);
            assertThat(desasignacion.getNumeroSerieNeumatico()).isNull();
            assertThat(desasignacion.getUnidadId()).isEqualTo(50L);
            assertThat(desasignacion.getPatenteUnidad()).isEqualTo(PATENTE);
            assertThat(desasignacion.getSlotId()).isEqualTo(500L);
            assertThat(desasignacion.getPosicionSlot()).isEqualTo("E1-IZQ");
        });
    }

}
