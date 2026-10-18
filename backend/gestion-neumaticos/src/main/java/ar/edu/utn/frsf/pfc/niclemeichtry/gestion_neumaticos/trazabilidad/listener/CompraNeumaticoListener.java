package ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.trazabilidad.listener;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.neumaticos.event.NeumaticoComprado;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.security.ComoSistema;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.trazabilidad.domain.entity.EventoCompra;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.trazabilidad.domain.entity.TipoEventoNeumatico;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.trazabilidad.repository.EventoNeumaticoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Registra en el historial la compra de un neumático nuevo.
 *
 * <p>Asíncrono y después del commit del alta, como un
 * {@code @ApplicationModuleListener}, pero sin su {@code @Transactional}: la
 * transacción la abre {@code save()} dentro de {@link ComoSistema}, así la
 * conexión queda como {@code gn_app} (el único rol que inserta en trazabilidad,
 * V11) y no con el rol del usuario que hizo el alta, cuyo contexto llega
 * propagado al hilo. Si falla, Modulith deja la publicación incompleta en
 * {@code event_publication} para reintentarla.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class CompraNeumaticoListener {

    private final EventoNeumaticoRepository eventoNeumaticoRepository;

    @Async
    @TransactionalEventListener
    public void on(NeumaticoComprado evento) {
        EventoCompra compra = EventoCompra.builder()
                .tipo(TipoEventoNeumatico.COMPRA)
                .fecha(evento.fecha())
                .usuario(evento.usuario())
                .comentario(evento.comentario())
                .neumaticoId(evento.neumaticoId())
                .numeroSerieNeumatico(evento.numeroSerie())
                .proveedorId(evento.proveedorId())
                .nombreProveedor(evento.nombreProveedor())
                .build();
        ComoSistema.ejecutar(() -> eventoNeumaticoRepository.save(compra));
        log.debug("Evento de compra registrado para el neumático {}", evento.neumaticoId());
    }

}
