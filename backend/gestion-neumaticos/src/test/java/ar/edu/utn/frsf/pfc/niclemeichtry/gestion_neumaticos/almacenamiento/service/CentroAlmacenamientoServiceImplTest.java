package ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.almacenamiento.service;

import static ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.TestDataFactory.centroAlmacenamientoRequest;
import static ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.TestDataFactory.domicilio;
import static ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.TestDataFactory.domicilioRequest;
import static ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.TestDataFactory.domicilioResponse;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.almacenamiento.domain.dto.CentroAlmacenamientoRequestDTO;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.almacenamiento.domain.dto.CentroAlmacenamientoResponseDTO;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.almacenamiento.domain.entity.CentroAlmacenamiento;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.almacenamiento.mapper.CentroAlmacenamientoMapper;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.almacenamiento.mapper.CentroAlmacenamientoMapperImpl;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.almacenamiento.repository.CentroAlmacenamientoRepository;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.common.api.DomicilioApi;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.common.api.DomicilioRequestDTO;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.common.api.DomicilioResponseDTO;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.common.auditoria.EnsambladorAuditoria;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.common.auditoria.FiltroAuditoria;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.common.auditoria.RegistroAuditoriaDTO;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.exception.EntidadNoEncontradaException;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.exception.ParametroInvalidoException;

@ExtendWith(MockitoExtension.class)
class CentroAlmacenamientoServiceImplTest {

    private static final String USUARIO = UUID.randomUUID().toString();
    private static final String NOMBRE = "Depósito Central";
    private static final Long DOMICILIO_ID = 200L;

    @Mock private EnsambladorAuditoria ensamblador;
    @Mock private CentroAlmacenamientoRepository centroAlmacenamientoRepository;
    @Mock private DomicilioApi domicilioApi;

    /** Mapper real (generado por MapStruct): el servicio depende de cómo copia los datos. */
    private final CentroAlmacenamientoMapper centroAlmacenamientoMapper = spy(new CentroAlmacenamientoMapperImpl());

    private CentroAlmacenamientoServiceImpl servicio;

    @BeforeEach
    void crearServicio() {
        servicio = new CentroAlmacenamientoServiceImpl(ensamblador, centroAlmacenamientoRepository,
                centroAlmacenamientoMapper,
                domicilioApi);
    }

    /** Simula el INSERT: la base asigna id y publicId. */
    private void guardarAsignandoIds() {
        when(centroAlmacenamientoRepository.saveAndFlush(any(CentroAlmacenamiento.class))).thenAnswer(invocacion -> {
            CentroAlmacenamiento guardado = invocacion.getArgument(0);
            guardado.setId(1L);
            guardado.setPublicId(UUID.randomUUID());
            return guardado;
        });
    }

    private CentroAlmacenamiento centroGuardado() {
        ArgumentCaptor<CentroAlmacenamiento> guardado = ArgumentCaptor.forClass(CentroAlmacenamiento.class);
        verify(centroAlmacenamientoRepository).saveAndFlush(guardado.capture());
        return guardado.getValue();
    }

    @Test
    @DisplayName("registra el centro con su domicilio, sin neumáticos en stock y sin ids internos")
    void altaConDomicilio() {
        CentroAlmacenamientoRequestDTO request = centroAlmacenamientoRequest();
        DomicilioResponseDTO domicilio = domicilioResponse(domicilio(request.domicilio()));
        when(domicilioApi.registrarDomicilio(request.domicilio(), USUARIO)).thenReturn(DOMICILIO_ID);
        when(domicilioApi.buscarDomicilio(DOMICILIO_ID)).thenReturn(Optional.of(domicilio));
        guardarAsignandoIds();

        CentroAlmacenamientoResponseDTO respuesta = servicio.registrarCentroAlmacenamiento(request, USUARIO);

        CentroAlmacenamiento guardado = centroGuardado();
        assertThat(guardado.getNombre()).isEqualTo(NOMBRE);
        assertThat(guardado.getDomicilioId()).isEqualTo(DOMICILIO_ID);
        assertThat(guardado.getCantidadNeumaticosEnStock()).isZero();
        assertThat(respuesta).isEqualTo(new CentroAlmacenamientoResponseDTO(guardado.getPublicId(), NOMBRE, 0,
                domicilio));
    }

    @Test
    @DisplayName("el stock queda en 0 aunque el centro mapeado traiga otra cantidad")
    void stockSiempreEnCero() {
        CentroAlmacenamientoRequestDTO request = centroAlmacenamientoRequest();
        doReturn(CentroAlmacenamiento.builder().nombre(NOMBRE).cantidadNeumaticosEnStock(25).build())
                .when(centroAlmacenamientoMapper).toEntity(request);
        when(domicilioApi.registrarDomicilio(request.domicilio(), USUARIO)).thenReturn(DOMICILIO_ID);
        guardarAsignandoIds();

        CentroAlmacenamientoResponseDTO respuesta = servicio.registrarCentroAlmacenamiento(request, USUARIO);

        assertThat(centroGuardado().getCantidadNeumaticosEnStock()).isZero();
        assertThat(respuesta.cantidadNeumaticosEnStock()).isZero();
    }

    @Test
    @DisplayName("recorta los extremos del nombre")
    void normalizaNombre() {
        guardarAsignandoIds();

        servicio.registrarCentroAlmacenamiento(centroAlmacenamientoRequest("  Depósito Norte ", domicilioRequest()),
                USUARIO);

        assertThat(centroGuardado().getNombre()).isEqualTo("Depósito Norte");
    }

    @Test
    @DisplayName("si el domicilio desapareció al releerlo, responde sin domicilio")
    void domicilioNoReleido() {
        CentroAlmacenamientoRequestDTO request = centroAlmacenamientoRequest();
        when(domicilioApi.registrarDomicilio(request.domicilio(), USUARIO)).thenReturn(DOMICILIO_ID);
        when(domicilioApi.buscarDomicilio(DOMICILIO_ID)).thenReturn(Optional.empty());
        guardarAsignandoIds();

        assertThat(servicio.registrarCentroAlmacenamiento(request, USUARIO).domicilio()).isNull();
    }

    @Test
    @DisplayName("una provincia inexistente del domicilio corta el alta sin guardar el centro")
    void provinciaInexistente() {
        DomicilioRequestDTO domicilio = domicilioRequest();
        when(domicilioApi.registrarDomicilio(domicilio, USUARIO))
                .thenThrow(new EntidadNoEncontradaException("La provincia indicada no existe"));

        assertThatThrownBy(() -> servicio.registrarCentroAlmacenamiento(
                centroAlmacenamientoRequest("Depósito Norte", domicilio), USUARIO))
                .isInstanceOf(EntidadNoEncontradaException.class);

        verify(centroAlmacenamientoRepository, never()).saveAndFlush(any());
    }

    @Nested
    @DisplayName("listarAuditoria")
    class ListarAuditoria {

        @Test
        @DisplayName("aplica los filtros y ensambla la página de fichas")
        void ensamblaLaPagina() {
            Page<CentroAlmacenamiento> pagina = new PageImpl<>(List.of(new CentroAlmacenamiento()));
            Page<RegistroAuditoriaDTO> esperado = new PageImpl<>(List.of());
            when(centroAlmacenamientoRepository.findAll(
                    ArgumentMatchers.<Specification<CentroAlmacenamiento>>any(), any(Pageable.class)))
                    .thenReturn(pagina);
            when(ensamblador.ensamblar(eq(pagina), any(), any())).thenReturn(esperado);

            Page<RegistroAuditoriaDTO> resultado = servicio.listarAuditoria(
                    new FiltroAuditoria("abc", FiltroAuditoria.EstadoFiltro.ACTIVOS, LocalDate.of(2026, 1, 1),
                            LocalDate.of(2026, 12, 31)),
                    PageRequest.of(0, 20, Sort.by("updatedAt")));

            assertThat(resultado).isSameAs(esperado);
        }

        @Test
        @DisplayName("rechaza ordenar por un campo que no es de auditoría")
        void rechazaOrdenNoPermitido() {
            FiltroAuditoria sinFiltros = new FiltroAuditoria(null, null, null, null);
            PageRequest ordenInvalido = PageRequest.of(0, 20, Sort.by("clave"));

            assertThatThrownBy(() -> servicio.listarAuditoria(sinFiltros, ordenInvalido))
                    .isInstanceOf(ParametroInvalidoException.class);
            verifyNoInteractions(ensamblador);
        }
    }

}
