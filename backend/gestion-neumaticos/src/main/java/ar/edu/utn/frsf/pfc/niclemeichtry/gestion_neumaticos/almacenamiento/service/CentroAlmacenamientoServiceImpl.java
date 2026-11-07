package ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.almacenamiento.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.almacenamiento.domain.dto.CentroAlmacenamientoRequestDTO;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.almacenamiento.domain.dto.CentroAlmacenamientoResponseDTO;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.almacenamiento.domain.entity.CentroAlmacenamiento;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.almacenamiento.mapper.CentroAlmacenamientoMapper;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.almacenamiento.repository.CentroAlmacenamientoRepository;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.common.api.DomicilioApi;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.common.api.DomicilioResponseDTO;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.common.auditoria.EnsambladorAuditoria;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.common.auditoria.EspecificacionesAuditoria;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.common.auditoria.FiltroAuditoria;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.common.auditoria.OrdenAuditoria;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.common.auditoria.RegistroAuditoriaDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class CentroAlmacenamientoServiceImpl implements CentroAlmacenamientoService {

    private final EnsambladorAuditoria ensamblador;
    private final CentroAlmacenamientoRepository centroAlmacenamientoRepository;
    private final CentroAlmacenamientoMapper centroAlmacenamientoMapper;
    private final DomicilioApi domicilioApi;

    @Override
    @Transactional
    public CentroAlmacenamientoResponseDTO registrarCentroAlmacenamiento(CentroAlmacenamientoRequestDTO request,
            String usuario) {
        // El mapper ya recorta el nombre; el stock no viene en el request y la entidad arranca en 0.
        CentroAlmacenamiento centro = centroAlmacenamientoMapper.toEntity(request);
        log.debug("Registrando centro de almacenamiento: usuario={}, nombre={}", usuario, centro.getNombre());

        // Misma transacción: si el alta del centro falla, el domicilio se revierte con ella.
        centro.setDomicilioId(domicilioApi.registrarDomicilio(request.domicilio(), usuario));

        centro = centroAlmacenamientoRepository.saveAndFlush(centro);
        log.info("Centro de almacenamiento {} registrado por {}", centro.getPublicId(), usuario);

        DomicilioResponseDTO domicilio = domicilioApi.buscarDomicilio(centro.getDomicilioId()).orElse(null);
        return centroAlmacenamientoMapper.toResponseDTO(centro, domicilio);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<RegistroAuditoriaDTO> listarAuditoria(FiltroAuditoria filtro, Pageable pageable) {
        log.debug("Consultando auditoría de centros de almacenamiento: filtro={}, pageable={}", filtro, pageable);
        Specification<CentroAlmacenamiento> especificacion = EspecificacionesAuditoria.<CentroAlmacenamiento>de(filtro)
                .and(EspecificacionesAuditoria.contiene(filtro.q(), "nombre"));
        Page<CentroAlmacenamiento> pagina = centroAlmacenamientoRepository.findAll(especificacion, OrdenAuditoria.validar(pageable));
        return ensamblador.ensamblar(pagina, CentroAlmacenamiento::getPublicId, CentroAlmacenamiento::getNombre);
    }

}
