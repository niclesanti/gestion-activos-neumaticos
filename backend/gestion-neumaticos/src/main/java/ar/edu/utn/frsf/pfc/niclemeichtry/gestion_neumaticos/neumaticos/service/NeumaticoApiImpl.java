package ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.neumaticos.service;

import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.neumaticos.api.NeumaticoApi;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.neumaticos.api.NeumaticoMontadoDTO;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.neumaticos.domain.entity.Neumatico;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.neumaticos.repository.NeumaticoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class NeumaticoApiImpl implements NeumaticoApi {

    private final NeumaticoRepository neumaticoRepository;

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

    private static NeumaticoMontadoDTO aDto(Neumatico neumatico) {
        return new NeumaticoMontadoDTO(
                neumatico.getPublicId(),
                neumatico.getNumeroSerie(),
                neumatico.getMarca().getNombre(),
                neumatico.getEstadoActual().name(),
                neumatico.getKilometrajeTotal());
    }

}
