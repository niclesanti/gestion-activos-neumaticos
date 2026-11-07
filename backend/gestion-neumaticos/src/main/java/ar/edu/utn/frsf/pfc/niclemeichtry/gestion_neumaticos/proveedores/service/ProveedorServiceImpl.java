package ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.proveedores.service;

import java.util.List;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.common.api.DomicilioApi;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.common.api.DomicilioResponseDTO;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.common.auditoria.EnsambladorAuditoria;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.common.auditoria.EspecificacionesAuditoria;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.common.auditoria.FiltroAuditoria;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.common.auditoria.OrdenAuditoria;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.common.auditoria.RegistroAuditoriaDTO;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.exception.EntidadDuplicadaException;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.proveedores.domain.dto.ProveedorListadoDTO;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.proveedores.domain.dto.ProveedorRequestDTO;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.proveedores.domain.dto.ProveedorResponseDTO;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.proveedores.domain.entity.ProveedorNeumaticos;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.proveedores.mapper.ProveedorMapper;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.proveedores.repository.ProveedorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class ProveedorServiceImpl implements ProveedorService {

    private static final String MENSAJE_NOMBRE_DUPLICADO = "Ya existe un proveedor con el nombre %s";

    private final EnsambladorAuditoria ensamblador;
    private final ProveedorRepository proveedorRepository;
    private final ProveedorMapper proveedorMapper;
    private final DomicilioApi domicilioApi;

    @Override
    @Transactional
    public ProveedorResponseDTO registrarProveedor(ProveedorRequestDTO proveedorRequestDTO, String usuario) {
        // El mapper ya normaliza los textos (recortes, email en minúsculas, opcionales vacíos = null).
        ProveedorNeumaticos proveedor = proveedorMapper.toEntity(proveedorRequestDTO);
        String nombre = proveedor.getNombre();
        log.debug("Registrando proveedor: usuario={}, nombre={}, conDomicilio={}", usuario, nombre,
                proveedorRequestDTO.domicilio() != null);
        if (proveedorRepository.existsByNombreIgnoreCase(nombre)) {
            log.warn("Alta rechazada: proveedor {} duplicado (usuario {})", nombre, usuario);
            throw new EntidadDuplicadaException(MENSAJE_NOMBRE_DUPLICADO.formatted(nombre));
        }

        // Misma transacción: si el alta del proveedor falla, el domicilio se revierte con ella.
        if (proveedorRequestDTO.domicilio() != null) {
            proveedor.setDomicilioId(domicilioApi.registrarDomicilio(proveedorRequestDTO.domicilio(), usuario));
        }

        try {
            proveedor = proveedorRepository.saveAndFlush(proveedor);
        } catch (DataIntegrityViolationException ex) {
            // Otra alta concurrente con el mismo nombre ganó la carrera entre la
            // verificación y el INSERT: el índice único la frena.
            log.warn("Alta rechazada por la base: proveedor {} duplicado (usuario {})", nombre, usuario);
            throw new EntidadDuplicadaException(MENSAJE_NOMBRE_DUPLICADO.formatted(nombre), ex);
        }
        log.info("Proveedor {} registrado por {}", proveedor.getPublicId(), usuario);

        DomicilioResponseDTO domicilio = proveedor.getDomicilioId() == null
                ? null
                : domicilioApi.buscarDomicilio(proveedor.getDomicilioId()).orElse(null);
        return proveedorMapper.toResponseDTO(proveedor, domicilio);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProveedorListadoDTO> listarProveedores() {
        List<ProveedorListadoDTO> proveedores = proveedorRepository.findAllByOrderByNombreAsc().stream()
                .map(proveedorMapper::toListadoDTO)
                .toList();
        log.debug("Proveedores listados: {}", proveedores.size());
        return proveedores;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<RegistroAuditoriaDTO> listarAuditoria(FiltroAuditoria filtro, Pageable pageable) {
        log.debug("Consultando auditoría de proveedores: filtro={}, pageable={}", filtro, pageable);
        Specification<ProveedorNeumaticos> especificacion = EspecificacionesAuditoria.<ProveedorNeumaticos>de(filtro)
                .and(EspecificacionesAuditoria.contiene(filtro.q(), "nombre"));
        Page<ProveedorNeumaticos> pagina = proveedorRepository.findAll(especificacion, OrdenAuditoria.validar(pageable));
        return ensamblador.ensamblar(pagina, ProveedorNeumaticos::getPublicId, ProveedorNeumaticos::getNombre);
    }

}
