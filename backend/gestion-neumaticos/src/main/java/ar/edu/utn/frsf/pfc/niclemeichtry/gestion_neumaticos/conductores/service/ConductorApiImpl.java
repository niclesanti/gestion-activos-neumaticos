package ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.conductores.service;

import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.conductores.api.ConductorApi;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.conductores.api.ConductorResumenDTO;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.conductores.mapper.ConductorMapper;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.conductores.repository.ConductorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** Implementación de la API pública del módulo para el resto de los módulos. */
@Service
@Slf4j
@RequiredArgsConstructor
public class ConductorApiImpl implements ConductorApi {

    private final ConductorRepository conductorRepository;
    private final ConductorMapper conductorMapper;

    @Override
    @Transactional(readOnly = true)
    public Optional<ConductorResumenDTO> buscarActivoPorPublicId(UUID publicId) {
        log.debug("Buscando conductor activo {}", publicId);
        return conductorRepository.findByPublicIdAndDeletedAtIsNull(publicId).map(conductorMapper::toResumen);
    }

}
