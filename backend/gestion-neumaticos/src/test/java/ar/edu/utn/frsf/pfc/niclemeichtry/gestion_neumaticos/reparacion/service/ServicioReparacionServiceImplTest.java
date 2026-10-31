package ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.reparacion.service;

import static ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.TestDataFactory.domicilio;
import static ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.TestDataFactory.domicilioRequest;
import static ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.TestDataFactory.domicilioResponse;
import static ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.TestDataFactory.servicioReparacionRequest;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.common.api.DomicilioApi;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.common.api.DomicilioRequestDTO;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.common.api.DomicilioResponseDTO;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.exception.EntidadDuplicadaException;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.exception.EntidadNoEncontradaException;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.reparacion.domain.dto.ServicioReparacionRequestDTO;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.reparacion.domain.dto.ServicioReparacionResponseDTO;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.reparacion.domain.entity.ServicioReparacion;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.reparacion.mapper.ServicioReparacionMapper;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.reparacion.mapper.ServicioReparacionMapperImpl;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.reparacion.repository.ServicioReparacionRepository;

@ExtendWith(MockitoExtension.class)
class ServicioReparacionServiceImplTest {

    private static final String USUARIO = UUID.randomUUID().toString();
    private static final String NOMBRE = "Gomería Del Centro";
    private static final Long DOMICILIO_ID = 200L;

    @Mock private ServicioReparacionRepository servicioReparacionRepository;
    @Mock private DomicilioApi domicilioApi;

    /** Mapper real (generado por MapStruct): el servicio depende de cómo copia los datos. */
    private final ServicioReparacionMapper servicioReparacionMapper = new ServicioReparacionMapperImpl();

    private ServicioReparacionServiceImpl servicio;

    @BeforeEach
    void crearServicio() {
        servicio = new ServicioReparacionServiceImpl(servicioReparacionRepository, servicioReparacionMapper,
                domicilioApi);
    }

    /** Simula el INSERT: la base asigna id y publicId. */
    private void guardarAsignandoIds() {
        when(servicioReparacionRepository.saveAndFlush(any(ServicioReparacion.class))).thenAnswer(invocacion -> {
            ServicioReparacion guardado = invocacion.getArgument(0);
            guardado.setId(1L);
            guardado.setPublicId(UUID.randomUUID());
            return guardado;
        });
    }

    private ServicioReparacion servicioGuardado() {
        ArgumentCaptor<ServicioReparacion> guardado = ArgumentCaptor.forClass(ServicioReparacion.class);
        verify(servicioReparacionRepository).saveAndFlush(guardado.capture());
        return guardado.getValue();
    }

    @Test
    @DisplayName("registra el servicio con su domicilio, sin neumáticos en reparación y sin ids internos")
    void altaConDomicilio() {
        ServicioReparacionRequestDTO request = servicioReparacionRequest();
        DomicilioResponseDTO domicilio = domicilioResponse(domicilio(request.domicilio()));
        when(servicioReparacionRepository.existsByNombreIgnoreCase(NOMBRE)).thenReturn(false);
        when(domicilioApi.registrarDomicilio(request.domicilio(), USUARIO)).thenReturn(DOMICILIO_ID);
        when(domicilioApi.buscarDomicilio(DOMICILIO_ID)).thenReturn(Optional.of(domicilio));
        guardarAsignandoIds();

        ServicioReparacionResponseDTO respuesta = servicio.registrarServicioReparacion(request, USUARIO);

        ServicioReparacion guardado = servicioGuardado();
        assertThat(guardado.getNombre()).isEqualTo(NOMBRE);
        assertThat(guardado.getEmail()).isEqualTo("contacto@gomeriacentro.com.ar");
        assertThat(guardado.getTelefono()).isEqualTo("+54 342 455-9876");
        assertThat(guardado.getDomicilioId()).isEqualTo(DOMICILIO_ID);
        assertThat(guardado.getCantidadNeumaticosEnReparacion()).isZero();
        assertThat(respuesta).isEqualTo(new ServicioReparacionResponseDTO(guardado.getPublicId(), NOMBRE,
                "contacto@gomeriacentro.com.ar", "+54 342 455-9876", 0, domicilio));
    }

    @Test
    @DisplayName("sin domicilio no llama a common y responde sin domicilio")
    void altaSinDomicilio() {
        guardarAsignandoIds();

        ServicioReparacionResponseDTO respuesta = servicio.registrarServicioReparacion(
                servicioReparacionRequest("Gomería Sur", null, null, null), USUARIO);

        ServicioReparacion guardado = servicioGuardado();
        assertThat(guardado.getDomicilioId()).isNull();
        assertThat(guardado.getEmail()).isNull();
        assertThat(guardado.getTelefono()).isNull();
        assertThat(respuesta.domicilio()).isNull();
        verifyNoInteractions(domicilioApi);
    }

    @Test
    @DisplayName("recorta los textos, pasa el email a minúsculas y trata el contacto en blanco como ausente")
    void normalizaDatos() {
        guardarAsignandoIds();

        servicio.registrarServicioReparacion(
                servicioReparacionRequest("  Gomería Sur ", "  Ventas@GomeriaSur.COM ", "   ", null), USUARIO);

        ServicioReparacion guardado = servicioGuardado();
        assertThat(guardado.getNombre()).isEqualTo("Gomería Sur");
        assertThat(guardado.getEmail()).isEqualTo("ventas@gomeriasur.com");
        assertThat(guardado.getTelefono()).isNull();
        verify(servicioReparacionRepository).existsByNombreIgnoreCase("Gomería Sur");
    }

    @Test
    @DisplayName("un email vacío equivale a no tenerlo")
    void emailVacio() {
        guardarAsignandoIds();

        servicio.registrarServicioReparacion(servicioReparacionRequest("Gomería Sur", "", " 0342 455-1234 ", null),
                USUARIO);

        ServicioReparacion guardado = servicioGuardado();
        assertThat(guardado.getEmail()).isNull();
        assertThat(guardado.getTelefono()).isEqualTo("0342 455-1234");
    }

    @Test
    @DisplayName("si el domicilio desapareció al releerlo, responde sin domicilio")
    void domicilioNoReleido() {
        ServicioReparacionRequestDTO request = servicioReparacionRequest();
        when(domicilioApi.registrarDomicilio(request.domicilio(), USUARIO)).thenReturn(DOMICILIO_ID);
        when(domicilioApi.buscarDomicilio(DOMICILIO_ID)).thenReturn(Optional.empty());
        guardarAsignandoIds();

        assertThat(servicio.registrarServicioReparacion(request, USUARIO).domicilio()).isNull();
    }

    @Test
    @DisplayName("un nombre ya registrado se rechaza sin crear domicilio ni guardar")
    void nombreDuplicado() {
        when(servicioReparacionRepository.existsByNombreIgnoreCase(NOMBRE)).thenReturn(true);

        assertThatThrownBy(() -> servicio.registrarServicioReparacion(servicioReparacionRequest(), USUARIO))
                .isInstanceOf(EntidadDuplicadaException.class)
                .hasMessage("Ya existe un servicio de reparación con el nombre Gomería Del Centro");

        verifyNoInteractions(domicilioApi);
        verify(servicioReparacionRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("una provincia inexistente del domicilio corta el alta sin guardar el servicio")
    void provinciaInexistente() {
        DomicilioRequestDTO domicilio = domicilioRequest();
        when(domicilioApi.registrarDomicilio(domicilio, USUARIO))
                .thenThrow(new EntidadNoEncontradaException("La provincia indicada no existe"));

        assertThatThrownBy(() -> servicio.registrarServicioReparacion(
                servicioReparacionRequest("Gomería Sur", null, null, domicilio), USUARIO))
                .isInstanceOf(EntidadNoEncontradaException.class);

        verify(servicioReparacionRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("un duplicado concurrente que frena el índice único también responde como duplicado")
    void duplicadoConcurrente() {
        when(servicioReparacionRepository.saveAndFlush(any(ServicioReparacion.class)))
                .thenThrow(new DataIntegrityViolationException("uk_servicios_reparacion_nombre"));

        assertThatThrownBy(() -> servicio.registrarServicioReparacion(
                servicioReparacionRequest("Gomería Sur", null, null, null), USUARIO))
                .isInstanceOf(EntidadDuplicadaException.class)
                .hasCauseInstanceOf(DataIntegrityViolationException.class);
    }

}
