package ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.neumaticos.service;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.exception.EntidadNoEncontradaException;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.exception.NeumaticoNoDisponibleException;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.neumaticos.api.NeumaticoApi;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.neumaticos.api.NeumaticoMontadoDTO;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.neumaticos.api.NeumaticoMontajeDTO;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.neumaticos.api.StockCentroAlmacenamiento;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.neumaticos.domain.entity.Neumatico;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.neumaticos.domain.entity.SituacionOperativa;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.neumaticos.repository.NeumaticoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class NeumaticoApiImpl implements NeumaticoApi {

    private final NeumaticoRepository neumaticoRepository;
    // Lo implementa almacenamiento: el descuento de stock queda en la misma transacción que el montaje.
    private final StockCentroAlmacenamiento stockCentroAlmacenamiento;

    @Override
    @Transactional(readOnly = true)
    public Map<Long, NeumaticoMontadoDTO> buscarMontadosPorIds(Collection<Long> ids) {
        log.debug("Buscando neumáticos montados por lote: {} ids", ids.size());
        if (ids.isEmpty()) {
            return Map.of();
        }
        return neumaticoRepository.findAllByIdIn(ids).stream()
                .collect(Collectors.toMap(Neumatico::getId, NeumaticoApiImpl::aDto));
    }

    @Override
    @Transactional
    public NeumaticoMontajeDTO montar(UUID publicId, Long slotId) {
        log.debug("Montando el neumático {} en el slot {}", publicId, slotId);
        Neumatico neumatico = neumaticoRepository.findByPublicId(publicId)
                .orElseThrow(() -> {
                    log.warn("Montaje rechazado: el neumático {} no existe", publicId);
                    return new EntidadNoEncontradaException(NeumaticoServiceImpl.MENSAJE_NEUMATICO_INEXISTENTE);
                });
        if (!neumatico.estaDisponible()) {
            log.warn("Montaje rechazado: el neumático {} no está disponible (situación {}, estado {}, baja {})",
                    publicId, neumatico.getSituacionOperativaVigente(), neumatico.getEstadoActual(),
                    neumatico.isDadoDeBaja());
            throw new NeumaticoNoDisponibleException(
                    "El neumático no está disponible para montar: tiene que estar en stock o en espera");
        }
        Long centroId = neumatico.getSituacionOperativaVigente() == SituacionOperativa.EN_STOCK
                ? neumatico.getCentroAlmacenamientoId()
                : null;
        neumatico.montar(slotId);
        if (centroId != null) {
            stockCentroAlmacenamiento.retirarNeumatico(centroId);
        }
        neumatico = neumaticoRepository.saveAndFlush(neumatico);
        log.info("Neumático {} montado en el slot {} (centro de origen {})", publicId, slotId, centroId);
        return aMontaje(neumatico);
    }

    @Override
    @Transactional
    public NeumaticoMontajeDTO desmontar(Long id) {
        log.debug("Desmontando el neumático {}", id);
        Neumatico neumatico = neumaticoRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Desmontaje rechazado: el neumático {} no existe", id);
                    return new EntidadNoEncontradaException(NeumaticoServiceImpl.MENSAJE_NEUMATICO_INEXISTENTE);
                });
        Long slotAnterior = neumatico.getSlotMontadoId();
        neumatico.desmontar();
        // Flush ya: libera el slot en la base antes de que se monte el que lo reemplaza (índice único).
        neumatico = neumaticoRepository.saveAndFlush(neumatico);
        log.info("Neumático {} desmontado del slot {}: queda en espera", neumatico.getPublicId(), slotAnterior);
        return aMontaje(neumatico);
    }

    private static NeumaticoMontadoDTO aDto(Neumatico neumatico) {
        return new NeumaticoMontadoDTO(
                neumatico.getPublicId(),
                neumatico.getNumeroSerie(),
                neumatico.getMarca().getNombre(),
                neumatico.getEstadoActual().name(),
                neumatico.getKilometrajeTotal());
    }

    private static NeumaticoMontajeDTO aMontaje(Neumatico neumatico) {
        return new NeumaticoMontajeDTO(neumatico.getId(), neumatico.getPublicId(), neumatico.getNumeroSerie());
    }

}
