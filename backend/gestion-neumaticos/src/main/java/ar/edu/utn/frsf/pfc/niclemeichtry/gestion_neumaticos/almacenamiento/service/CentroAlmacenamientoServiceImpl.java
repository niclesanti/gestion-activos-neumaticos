package ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.almacenamiento.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.almacenamiento.domain.dto.CentroAlmacenamientoRequestDTO;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.almacenamiento.domain.dto.CentroAlmacenamientoResponseDTO;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.almacenamiento.domain.entity.CentroAlmacenamiento;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.almacenamiento.mapper.CentroAlmacenamientoMapper;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.almacenamiento.repository.CentroAlmacenamientoRepository;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.common.api.DomicilioApi;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.common.api.DomicilioResponseDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class CentroAlmacenamientoServiceImpl implements CentroAlmacenamientoService {

    private final CentroAlmacenamientoRepository centroAlmacenamientoRepository;
    private final CentroAlmacenamientoMapper centroAlmacenamientoMapper;
    private final DomicilioApi domicilioApi;

    @Override
    @Transactional
    public CentroAlmacenamientoResponseDTO registrarCentroAlmacenamiento(CentroAlmacenamientoRequestDTO request,
            String usuario) {
        String nombre = request.nombre().trim();
        log.debug("Registrando centro de almacenamiento: usuario={}, nombre={}", usuario, nombre);

        CentroAlmacenamiento centro = centroAlmacenamientoMapper.toEntity(request);
        centro.setNombre(nombre);
        // Un centro nuevo arranca sin stock, sin importar lo que traiga el request.
        centro.setCantidadNeumaticosEnStock(0);
        // Misma transacción: si el alta del centro falla, el domicilio se revierte con ella.
        centro.setDomicilioId(domicilioApi.registrarDomicilio(request.domicilio(), usuario));

        centro = centroAlmacenamientoRepository.saveAndFlush(centro);
        log.info("Centro de almacenamiento {} registrado por {}", centro.getPublicId(), usuario);

        DomicilioResponseDTO domicilio = domicilioApi.buscarDomicilio(centro.getDomicilioId()).orElse(null);
        return centroAlmacenamientoMapper.toResponseDTO(centro, domicilio);
    }

}
