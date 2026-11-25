package ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.almacenamiento.service;

import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.almacenamiento.repository.CentroAlmacenamientoRepository;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.neumaticos.api.ProveedorUbicacionNeumatico;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.neumaticos.api.TipoUbicacion;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.neumaticos.api.UbicacionOperativaDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** Resuelve la ubicación de un neumático en stock: el nombre del centro de almacenamiento. */
@Service
@Slf4j
@RequiredArgsConstructor
public class UbicacionCentroProveedor implements ProveedorUbicacionNeumatico {

    private final CentroAlmacenamientoRepository centroRepository;

    @Override
    public TipoUbicacion tipo() {
        return TipoUbicacion.CENTRO_ALMACENAMIENTO;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UbicacionOperativaDTO> buscar(Long centroId) {
        log.debug("Resolviendo la ubicación del centro de almacenamiento {}", centroId);
        // El centro dado de baja igual se nombra: indica dónde está el neumático.
        return centroRepository.findById(centroId)
                .map(centro -> UbicacionOperativaDTO.deCentro(centro.getNombre()));
    }

}
