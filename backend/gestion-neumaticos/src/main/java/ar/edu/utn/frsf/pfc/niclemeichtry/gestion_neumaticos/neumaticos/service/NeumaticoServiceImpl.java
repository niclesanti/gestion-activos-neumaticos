package ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.neumaticos.service;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.common.auditoria.EnsambladorAuditoria;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.common.auditoria.EspecificacionesAuditoria;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.common.auditoria.FiltroAuditoria;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.common.auditoria.OrdenAuditoria;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.common.auditoria.RegistroAuditoriaDTO;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.config.NormalizacionTexto;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.exception.EntidadDuplicadaException;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.exception.EntidadNoEncontradaException;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.exception.ParametroInvalidoException;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.neumaticos.api.ProveedorUbicacionNeumatico;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.neumaticos.api.StockCentroAlmacenamiento;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.neumaticos.api.TipoUbicacion;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.neumaticos.api.UbicacionOperativaDTO;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.neumaticos.domain.EspecificacionesNeumatico;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.neumaticos.domain.OrdenNeumaticos;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.neumaticos.domain.dto.MarcaNeumaticoResponseDTO;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.neumaticos.domain.dto.NeumaticoDetalleDTO;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.neumaticos.domain.dto.NeumaticoListadoDTO;
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
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.proveedores.api.ProveedorApi;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.proveedores.api.ProveedorResumenDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class NeumaticoServiceImpl implements NeumaticoService {

    private static final String MENSAJE_SERIE_DUPLICADA = "Ya existe un neumático con el número de serie %s";
    static final String MENSAJE_MARCA_INEXISTENTE = "La marca de neumático indicada no existe";
    static final String MENSAJE_PROVEEDOR_INEXISTENTE = "El proveedor indicado no existe";
    static final String MENSAJE_NEUMATICO_INEXISTENTE = "El neumático indicado no existe";

    private final EnsambladorAuditoria ensamblador;
    private final NeumaticoRepository neumaticoRepository;
    private final NeumaticoMapper neumaticoMapper;
    private final MarcaNeumaticoRepository marcaNeumaticoRepository;
    private final ProveedorApi proveedorApi;
    private final ApplicationEventPublisher eventPublisher;
    private final StockCentroAlmacenamiento stockCentroAlmacenamiento;
    // Los módulos que conocen la ubicación (unidades, almacenamiento, reparación) implementan el puerto: sin ciclos.
    private final List<ProveedorUbicacionNeumatico> proveedoresUbicacion;

    @Override
    @Transactional
    public NeumaticoResponseDTO registrarNeumatico(NeumaticoRequestDTO neumaticoRequestDTO, String usuario) {
        // El mapper ya normaliza el número de serie (recortado y en mayúsculas; vacío = null).
        Neumatico neumatico = neumaticoMapper.toEntity(neumaticoRequestDTO);
        String numeroSerie = neumatico.getNumeroSerie();
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
        ProveedorResumenDTO proveedor = neumaticoRequestDTO.estadoActual() == EstadoNeumatico.NUEVO
                ? buscarProveedor(neumaticoRequestDTO.proveedorId())
                : null;

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
    public Page<NeumaticoListadoDTO> buscar(String numeroSerie, Long marcaId, EstadoNeumatico estado,
            Collection<SituacionOperativa> situaciones, UUID centro, Pageable pageable) {
        log.debug("Buscando neumáticos: numeroSerie={}, marcaId={}, estado={}, situaciones={}, centro={}, pageable={}",
                numeroSerie, marcaId, estado, situaciones, centro, pageable);
        if (!OrdenNeumaticos.esValido(pageable)) {
            log.warn("Búsqueda de neumáticos rechazada: orden no permitido {}", pageable.getSort());
            throw new ParametroInvalidoException("Solo se puede ordenar por número de serie");
        }
        Long centroId = null;
        if (centro != null) {
            Optional<Long> encontrado = stockCentroAlmacenamiento.buscarIdPorPublicId(centro);
            if (encontrado.isEmpty()) {
                log.debug("Búsqueda de neumáticos por un centro inexistente ({}): sin resultados", centro);
                return Page.empty(pageable);
            }
            centroId = encontrado.get();
        }
        Specification<Neumatico> especificacion = EspecificacionesNeumatico.activos()
                .and(EspecificacionesNeumatico.numeroSerieContiene(NormalizacionTexto.mayusculas(numeroSerie)))
                .and(EspecificacionesNeumatico.deMarca(marcaId))
                .and(EspecificacionesNeumatico.conEstado(estado))
                .and(EspecificacionesNeumatico.conSituaciones(situaciones))
                .and(EspecificacionesNeumatico.enCentro(centroId));
        // Desempate fijo por publicId (UUIDv7, orden de alta): páginas estables aun con muchos sin serie.
        Pageable ordenado = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(),
                pageable.getSort().and(Sort.by("publicId")));
        Page<Neumatico> neumaticos = neumaticoRepository.findAll(especificacion, ordenado);
        // Nombres de los centros de toda la página en una sola consulta: sin N+1.
        Map<Long, String> centros = stockCentroAlmacenamiento.buscarNombresPorIds(neumaticos.getContent().stream()
                .map(Neumatico::getCentroAlmacenamientoId)
                .filter(Objects::nonNull)
                .distinct()
                .toList());
        Page<NeumaticoListadoDTO> pagina = neumaticos.map(neumatico -> neumaticoMapper.toListadoDTO(neumatico,
                neumatico.getCentroAlmacenamientoId() == null ? null
                        : centros.get(neumatico.getCentroAlmacenamientoId())));
        log.debug("Neumáticos encontrados: {}", pagina.getTotalElements());
        return pagina;
    }

    @Override
    @Transactional(readOnly = true)
    public NeumaticoDetalleDTO obtenerDetalle(UUID publicId) {
        log.debug("Consultando la ficha del neumático {}", publicId);
        Neumatico neumatico = neumaticoRepository.findByPublicIdAndDeletedAtIsNull(publicId)
                .orElseThrow(() -> {
                    log.warn("Ficha rechazada: el neumático {} no existe o está dado de baja", publicId);
                    return new EntidadNoEncontradaException(MENSAJE_NEUMATICO_INEXISTENTE);
                });
        UbicacionOperativaDTO ubicacion = resolverUbicacion(neumatico);
        log.debug("Ficha del neumático {} armada (situación {})", publicId, neumatico.getSituacionOperativaVigente());
        return neumaticoMapper.toDetalleDTO(neumatico, ubicacion);
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

    /**
     * Ubicación según la situación vigente, con una sola consulta al módulo que la conoce. De baja o en espera
     * no se consulta a nadie; si el dato no se resuelve (id nulo o inexistente) queda vacía.
     */
    private UbicacionOperativaDTO resolverUbicacion(Neumatico neumatico) {
        if (neumatico.getEstadoActual() == EstadoNeumatico.DE_BAJA) {
            return UbicacionOperativaDTO.vacia();
        }
        TipoUbicacion tipo;
        Long idInterno;
        switch (neumatico.getSituacionOperativaVigente()) {
            case MONTADO -> {
                tipo = TipoUbicacion.UNIDAD;
                idInterno = neumatico.getSlotMontadoId();
            }
            case EN_STOCK -> {
                tipo = TipoUbicacion.CENTRO_ALMACENAMIENTO;
                idInterno = neumatico.getCentroAlmacenamientoId();
            }
            case EN_REPARACION -> {
                tipo = TipoUbicacion.SERVICIO_REPARACION;
                idInterno = neumatico.getServicioReparacionId();
            }
            default -> {
                return UbicacionOperativaDTO.vacia();
            }
        }
        if (idInterno == null) {
            log.warn("El neumático {} está {} pero no tiene referencia de ubicación", neumatico.getPublicId(),
                    neumatico.getSituacionOperativaVigente());
            return UbicacionOperativaDTO.vacia();
        }
        Optional<UbicacionOperativaDTO> ubicacion = proveedoresUbicacion.stream()
                .filter(proveedor -> proveedor.tipo() == tipo)
                .findFirst()
                .flatMap(proveedor -> proveedor.buscar(idInterno));
        if (ubicacion.isEmpty()) {
            log.warn("No se resolvió la ubicación {} del neumático {}", tipo, neumatico.getPublicId());
        }
        return ubicacion.orElseGet(UbicacionOperativaDTO::vacia);
    }

    /** El proveedor es opcional: null si no se indicó, error si se indicó uno inexistente. */
    private ProveedorResumenDTO buscarProveedor(UUID proveedorId) {
        if (proveedorId == null) {
            return null;
        }
        return proveedorApi.buscarPorPublicId(proveedorId)
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
    private void publicarCompra(Neumatico neumatico, ProveedorResumenDTO proveedor, String usuario) {
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

    /** Etiqueta del neumático en la auditoría: su número de serie o, si no tiene, la marca y el final del id. */
    static String descripcion(Neumatico neumatico) {
        if (neumatico.getNumeroSerie() != null) {
            return neumatico.getNumeroSerie();
        }
        String publicId = neumatico.getPublicId().toString();
        return neumatico.getMarca().getNombre() + " · " + publicId.substring(publicId.length() - 8);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<RegistroAuditoriaDTO> listarAuditoria(FiltroAuditoria filtro, Pageable pageable) {
        log.debug("Consultando auditoría de neumáticos: filtro={}, pageable={}", filtro, pageable);
        Specification<Neumatico> especificacion = EspecificacionesAuditoria.<Neumatico>de(filtro)
                .and(EspecificacionesAuditoria.contiene(filtro.q(), "numeroSerie"));
        Page<Neumatico> pagina = neumaticoRepository.findAll(especificacion, OrdenAuditoria.validar(pageable));
        return ensamblador.ensamblar(pagina, Neumatico::getPublicId, NeumaticoServiceImpl::descripcion);
    }

}
