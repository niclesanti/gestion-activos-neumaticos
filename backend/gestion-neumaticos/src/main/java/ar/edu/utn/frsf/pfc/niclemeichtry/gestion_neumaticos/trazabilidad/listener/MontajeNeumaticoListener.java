package ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.trazabilidad.listener;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.security.ComoSistema;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.trazabilidad.domain.entity.EventoAsignacion;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.trazabilidad.domain.entity.EventoDesasignacion;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.trazabilidad.repository.EventoNeumaticoRepository;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.unidades_transporte.event.NeumaticoAsignado;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.unidades_transporte.event.NeumaticoDesasignado;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Registra en el historial el montaje y el desmontaje de neumáticos en las unidades (HU-31).
 *
 * <p>Igual que {@link CompraNeumaticoListener}: asíncrono, después del commit y como el sistema
 * ({@code gn_app}, el único rol que inserta en trazabilidad). Si falla, Modulith deja la publicación
 * incompleta en {@code event_publication} para reintentarla.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class MontajeNeumaticoListener {

    private final EventoNeumaticoRepository eventoNeumaticoRepository;

    @Async
    @TransactionalEventListener
    public void on(NeumaticoAsignado evento) {
        EventoAsignacion asignacion = EventoAsignacion.builder()
                .fecha(evento.fecha())
                .usuario(evento.usuario())
                .neumaticoId(evento.neumaticoId())
                .numeroSerieNeumatico(evento.numeroSerie())
                .unidadId(evento.unidadId())
                .patenteUnidad(evento.patente())
                .slotId(evento.slotId())
                .posicionSlot(evento.posicion())
                .conductorId(evento.conductorId())
                .nombreConductor(evento.nombreConductor())
                .build();
        log.debug("Registrando evento de asignación del neumático {}", evento.neumaticoId());
        ComoSistema.ejecutar(() -> eventoNeumaticoRepository.save(asignacion));
        log.info("Evento de asignación registrado para el neumático {} ({} {})", evento.neumaticoId(),
                evento.patente(), evento.posicion());
    }

    @Async
    @TransactionalEventListener
    public void on(NeumaticoDesasignado evento) {
        EventoDesasignacion desasignacion = EventoDesasignacion.builder()
                .fecha(evento.fecha())
                .usuario(evento.usuario())
                .neumaticoId(evento.neumaticoId())
                .numeroSerieNeumatico(evento.numeroSerie())
                .unidadId(evento.unidadId())
                .patenteUnidad(evento.patente())
                .slotId(evento.slotId())
                .posicionSlot(evento.posicion())
                .build();
        log.debug("Registrando evento de desasignación del neumático {}", evento.neumaticoId());
        ComoSistema.ejecutar(() -> eventoNeumaticoRepository.save(desasignacion));
        log.info("Evento de desasignación registrado para el neumático {} ({} {})", evento.neumaticoId(),
                evento.patente(), evento.posicion());
    }

}
