package ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.reparacion.service;

import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.neumaticos.api.ProveedorUbicacionNeumatico;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.neumaticos.api.TipoUbicacion;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.neumaticos.api.UbicacionOperativaDTO;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.reparacion.repository.ServicioReparacionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** Resuelve la ubicación de un neumático en reparación: el nombre del servicio de reparación. */
@Service
@Slf4j
@RequiredArgsConstructor
public class UbicacionServicioProveedor implements ProveedorUbicacionNeumatico {

    private final ServicioReparacionRepository servicioRepository;

    @Override
    public TipoUbicacion tipo() {
        return TipoUbicacion.SERVICIO_REPARACION;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UbicacionOperativaDTO> buscar(Long servicioId) {
        log.debug("Resolviendo la ubicación del servicio de reparación {}", servicioId);
        // El servicio dado de baja igual se nombra: indica dónde está el neumático.
        return servicioRepository.findById(servicioId)
                .map(servicio -> UbicacionOperativaDTO.deServicio(servicio.getNombre()));
    }

}
