package ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.neumaticos.service;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.exception.EntidadDuplicadaException;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.exception.EntidadNoEncontradaException;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.neumaticos.domain.dto.MarcaNeumaticoResponseDTO;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.neumaticos.domain.dto.NeumaticoRequestDTO;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.neumaticos.domain.dto.NeumaticoResponseDTO;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.neumaticos.domain.entity.EstadoNeumatico;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.neumaticos.domain.entity.MarcaNeumatico;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.neumaticos.domain.entity.Neumatico;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.neumaticos.domain.entity.SituacionOperativa;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.neumaticos.event.NeumaticoComprado;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.neumaticos.mapper.NeumaticoMapper;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.neumaticos.repository.MarcaNeumaticoRepository;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.neumaticos.repository.NeumaticoRepository;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.proveedores.ProveedorConsulta;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.proveedores.ProveedorResumen;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class NeumaticoServiceImpl implements NeumaticoService {

    static final String MENSAJE_SERIE_DUPLICADA = "Ya existe un neumático con el número de serie %s";
    static final String MENSAJE_MARCA_INEXISTENTE = "La marca de neumático indicada no existe";
    static final String MENSAJE_PROVEEDOR_INEXISTENTE = "El proveedor indicado no existe";

    private final NeumaticoRepository neumaticoRepository;
    private final NeumaticoMapper neumaticoMapper;
    private final MarcaNeumaticoRepository marcaNeumaticoRepository;
    private final ProveedorConsulta proveedorConsulta;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional
    public NeumaticoResponseDTO registrarNeumatico(NeumaticoRequestDTO neumaticoRequestDTO, String usuario) {
        String numeroSerie = normalizarNumeroSerie(neumaticoRequestDTO.numeroSerie());
        log.debug("Registrando neumático: usuario={}, estado={}, numeroSerie={}, marcaId={}, proveedorId={}", usuario,
                neumaticoRequestDTO.estadoActual(), numeroSerie, neumaticoRequestDTO.marcaId(),
                neumaticoRequestDTO.proveedorId());
        if (numeroSerie != null && neumaticoRepository.existsByNumeroSerie(numeroSerie)) {
            log.warn("Alta rechazada: número de serie {} duplicado (usuario {})", numeroSerie, usuario);
            throw new EntidadDuplicadaException(MENSAJE_SERIE_DUPLICADA.formatted(numeroSerie));
        }
        MarcaNeumatico marca = marcaNeumaticoRepository.findById(neumaticoRequestDTO.marcaId())
                .orElseThrow(() -> {
                    log.warn("Alta rechazada: la marca {} no existe (usuario {})", neumaticoRequestDTO.marcaId(),
                            usuario);
                    return new EntidadNoEncontradaException(MENSAJE_MARCA_INEXISTENTE);
                });
        // Se resuelve antes de guardar: un proveedor inexistente no deja un alta a medias.
        ProveedorResumen proveedor = neumaticoRequestDTO.estadoActual() == EstadoNeumatico.NUEVO
                ? buscarProveedor(neumaticoRequestDTO.proveedorId())
                : null;

        Neumatico neumatico = neumaticoMapper.toEntity(neumaticoRequestDTO);
        neumatico.setNumeroSerie(numeroSerie);
        neumatico.setMarca(marca);
        neumatico.setEnEspera(true);
        neumatico.setSituacionOperativaVigente(SituacionOperativa.EN_ESPERA);

        try {
            neumatico = neumaticoRepository.saveAndFlush(neumatico);
        } catch (DataIntegrityViolationException ex) {
            // Otra alta concurrente con el mismo número de serie ganó la carrera
            // entre la verificación y el INSERT: la restricción UNIQUE la frena.
            log.warn("Alta rechazada por la base: número de serie {} duplicado (usuario {})", numeroSerie, usuario);
            throw new EntidadDuplicadaException(MENSAJE_SERIE_DUPLICADA.formatted(numeroSerie), ex);
        }
        log.info("Neumático {} registrado por {} (estado {})", neumatico.getPublicId(), usuario,
                neumatico.getEstadoActual());

        if (neumatico.getEstadoActual() == EstadoNeumatico.NUEVO) {
            publicarCompra(neumatico, proveedor, usuario);
        }
        return neumaticoMapper.toResponseDTO(neumatico);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MarcaNeumaticoResponseDTO> listarMarcas() {
        List<MarcaNeumaticoResponseDTO> marcas = marcaNeumaticoRepository.findAllByOrderByNombreAsc().stream()
                .map(neumaticoMapper::toMarcaDTO)
                .toList();
        log.debug("Marcas de neumáticos listadas: {}", marcas.size());
        return marcas;
    }

    /*
    * -----------------
    * MÉTODOS PRIVADOS
    * -----------------
    * Implementación de métodos privados para la lógica interna del servicio,
    * código auxiliar, validaciones, código repetitivo.
    */

    /** Recorta y pasa a mayúsculas; un número de serie vacío equivale a no tenerlo. */
    private static String normalizarNumeroSerie(String numeroSerie) {
        if (numeroSerie == null || numeroSerie.isBlank()) {
            return null;
        }
        return numeroSerie.trim().toUpperCase(Locale.ROOT);
    }

    /** El proveedor es opcional: null si no se indicó, error si se indicó uno inexistente. */
    private ProveedorResumen buscarProveedor(UUID proveedorId) {
        if (proveedorId == null) {
            return null;
        }
        return proveedorConsulta.buscarPorPublicId(proveedorId)
                .orElseThrow(() -> {
                    log.warn("Alta rechazada: el proveedor {} no existe", proveedorId);
                    return new EntidadNoEncontradaException(MENSAJE_PROVEEDOR_INEXISTENTE);
                });
    }

    /**
     * Evento de compra para trazabilidad. Modulith lo guarda en
     * {@code event_publication} dentro de esta transacción y lo entrega después
     * del commit: si el alta se revierte, el evento no existe.
     */
    private void publicarCompra(Neumatico neumatico, ProveedorResumen proveedor, String usuario) {
        eventPublisher.publishEvent(new NeumaticoComprado(
                neumatico.getId(),
                neumatico.getNumeroSerie(),
                proveedor == null ? null : proveedor.id(),
                proveedor == null ? null : proveedor.nombre(),
                usuario,
                Instant.now(),
                neumatico.getComentario()));
        log.info("Evento de compra publicado para el neumático {} (proveedor {})", neumatico.getPublicId(),
                proveedor == null ? null : proveedor.publicId());
    }

}
