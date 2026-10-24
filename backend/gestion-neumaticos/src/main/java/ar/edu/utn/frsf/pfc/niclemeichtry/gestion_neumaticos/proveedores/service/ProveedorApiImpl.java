package ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.proveedores.service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.proveedores.api.ProveedorApi;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.proveedores.api.ProveedorResumenDTO;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.proveedores.mapper.ProveedorMapper;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.proveedores.repository.ProveedorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** Implementación de la API pública del módulo para el resto de los módulos. */
@Service
@Slf4j
@RequiredArgsConstructor
public class ProveedorApiImpl implements ProveedorApi {

    private final ProveedorRepository proveedorRepository;
    private final ProveedorMapper proveedorMapper;

    @Override
    @Transactional(readOnly = true)
    public Optional<ProveedorResumenDTO> buscarPorPublicId(UUID publicId) {
        log.debug("Buscando proveedor {}", publicId);
        return proveedorRepository.findByPublicId(publicId).map(proveedorMapper::toResumen);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProveedorResumenDTO> listar() {
        List<ProveedorResumenDTO> proveedores = proveedorRepository.findAllByOrderByNombreAsc().stream()
                .map(proveedorMapper::toResumen)
                .toList();
        log.debug("Proveedores consultados: {}", proveedores.size());
        return proveedores;
    }

}
