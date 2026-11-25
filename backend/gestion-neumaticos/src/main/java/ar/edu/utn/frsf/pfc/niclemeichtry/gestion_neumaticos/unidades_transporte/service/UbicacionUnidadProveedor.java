package ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.unidades_transporte.service;

import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.neumaticos.api.ProveedorUbicacionNeumatico;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.neumaticos.api.TipoUbicacion;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.neumaticos.api.UbicacionOperativaDTO;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.unidades_transporte.repository.SlotUnidadTransporteRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** Resuelve la ubicación de un neumático montado: patente de la unidad y código de la posición. */
@Service
@Slf4j
@RequiredArgsConstructor
public class UbicacionUnidadProveedor implements ProveedorUbicacionNeumatico {

    private final SlotUnidadTransporteRepository slotRepository;

    @Override
    public TipoUbicacion tipo() {
        return TipoUbicacion.UNIDAD;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UbicacionOperativaDTO> buscar(Long slotId) {
        log.debug("Resolviendo la ubicación del slot {}", slotId);
        // La unidad dada de baja igual se nombra: indica dónde está el neumático.
        return slotRepository.findConUnidadById(slotId)
                .map(slot -> UbicacionOperativaDTO.deUnidad(slot.getUnidad().getPatente(), slot.getCodigo()));
    }

}
