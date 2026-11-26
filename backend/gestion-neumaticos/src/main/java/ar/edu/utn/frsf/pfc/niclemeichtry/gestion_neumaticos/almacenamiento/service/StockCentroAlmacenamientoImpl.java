package ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.almacenamiento.service;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.almacenamiento.domain.entity.CentroAlmacenamiento;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.almacenamiento.repository.CentroAlmacenamientoRepository;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.exception.EntidadNoEncontradaException;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.neumaticos.api.StockCentroAlmacenamiento;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** Stock de los centros de almacenamiento, para el módulo de neumáticos (puerto de su API). */
@Service
@Slf4j
@RequiredArgsConstructor
public class StockCentroAlmacenamientoImpl implements StockCentroAlmacenamiento {

    private final CentroAlmacenamientoRepository centroRepository;

    @Override
    @Transactional(readOnly = true)
    public Optional<Long> buscarIdPorPublicId(UUID publicId) {
        log.debug("Buscando el id del centro de almacenamiento {}", publicId);
        return centroRepository.findByPublicId(publicId).map(CentroAlmacenamiento::getId);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<Long, String> buscarNombresPorIds(Collection<Long> ids) {
        log.debug("Buscando nombres de centros de almacenamiento por lote: {} ids", ids.size());
        if (ids.isEmpty()) {
            return Map.of();
        }
        return centroRepository.findAllByIdIn(ids).stream()
                .collect(Collectors.toMap(CentroAlmacenamiento::getId, CentroAlmacenamiento::getNombre));
    }

    @Override
    @Transactional
    public void retirarNeumatico(Long centroId) {
        log.debug("Retirando un neumático del stock del centro {}", centroId);
        CentroAlmacenamiento centro = centroRepository.findParaActualizarById(centroId)
                .orElseThrow(() -> {
                    log.warn("Retiro de stock rechazado: el centro {} no existe", centroId);
                    return new EntidadNoEncontradaException("El centro de almacenamiento del neumático no existe");
                });
        centro.removerNeumaticoStock();
        log.info("Centro de almacenamiento {} con un neumático menos en stock (quedan {})", centro.getPublicId(),
                centro.getCantidadNeumaticosEnStock());
    }

}
