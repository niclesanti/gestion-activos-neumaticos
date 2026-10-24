package ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.proveedores.service;

import java.util.List;
import java.util.Locale;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.common.api.DomicilioApi;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.common.api.DomicilioResponseDTO;
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

    static final String MENSAJE_NOMBRE_DUPLICADO = "Ya existe un proveedor con el nombre %s";

    private final ProveedorRepository proveedorRepository;
    private final ProveedorMapper proveedorMapper;
    private final DomicilioApi domicilioApi;

    @Override
    @Transactional
    public ProveedorResponseDTO registrarProveedor(ProveedorRequestDTO proveedorRequestDTO, String usuario) {
        String nombre = proveedorRequestDTO.nombre().trim();
        log.debug("Registrando proveedor: usuario={}, nombre={}, conDomicilio={}", usuario, nombre,
                proveedorRequestDTO.domicilio() != null);
        if (proveedorRepository.existsByNombreIgnoreCase(nombre)) {
            log.warn("Alta rechazada: proveedor {} duplicado (usuario {})", nombre, usuario);
            throw new EntidadDuplicadaException(MENSAJE_NOMBRE_DUPLICADO.formatted(nombre));
        }

        ProveedorNeumaticos proveedor = proveedorMapper.toEntity(proveedorRequestDTO);
        proveedor.setNombre(nombre);
        proveedor.setEmail(normalizarEmail(proveedorRequestDTO.email()));
        proveedor.setTelefono(opcional(proveedorRequestDTO.telefono()));
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

    /*
    * -----------------
    * MÉTODOS PRIVADOS
    * -----------------
    * Implementación de métodos privados para la lógica interna del servicio,
    * código auxiliar, validaciones, código repetitivo.
    */

    /** Recorta los extremos; un texto opcional vacío equivale a no tenerlo. */
    private static String opcional(String texto) {
        if (texto == null || texto.isBlank()) {
            return null;
        }
        return texto.trim();
    }

    /** Recorta y pasa a minúsculas; vacío equivale a no tenerlo. */
    private static String normalizarEmail(String email) {
        String recortado = opcional(email);
        return recortado == null ? null : recortado.toLowerCase(Locale.ROOT);
    }

}
