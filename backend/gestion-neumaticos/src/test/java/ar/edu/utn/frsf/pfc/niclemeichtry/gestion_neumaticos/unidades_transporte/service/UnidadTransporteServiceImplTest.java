package ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.unidades_transporte.service;

import static ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.TestDataFactory.CONFIGURACION_ID;
import static ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.TestDataFactory.NEUMATICO_MONTADO_ID;
import static ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.TestDataFactory.PATENTE;
import static ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.TestDataFactory.UNIDAD_PUBLIC_ID;
import static ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.TestDataFactory.neumaticoMontado;
import static ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.TestDataFactory.unidadConNeumaticoMontado;
import static ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.TestDataFactory.semirremolque;
import static ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.TestDataFactory.tractorComun;
import static ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.TestDataFactory.unidadTransporteRequest;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.exception.EntidadDuplicadaException;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.exception.EntidadNoEncontradaException;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.neumaticos.api.NeumaticoApi;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.unidades_transporte.domain.dto.ConfiguracionUnidadDTO;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.unidades_transporte.domain.dto.UnidadTransporteDetalleDTO;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.unidades_transporte.domain.dto.UnidadTransporteListadoDTO;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.unidades_transporte.domain.dto.UnidadTransporteResponseDTO;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.unidades_transporte.domain.entity.ConfiguracionUnidad;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.unidades_transporte.domain.entity.UnidadTransporte;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.unidades_transporte.mapper.UnidadTransporteMapper;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.unidades_transporte.mapper.UnidadTransporteMapperImpl;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.unidades_transporte.repository.ConfiguracionUnidadRepository;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.unidades_transporte.repository.UnidadTransporteRepository;
import static org.mockito.ArgumentMatchers.eq;

import java.time.LocalDate;

import org.mockito.ArgumentMatchers;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.common.auditoria.EnsambladorAuditoria;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.common.auditoria.FiltroAuditoria;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.common.auditoria.RegistroAuditoriaDTO;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.exception.ParametroInvalidoException;

@ExtendWith(MockitoExtension.class)
class UnidadTransporteServiceImplTest {

    private static final String USUARIO = UUID.randomUUID().toString();

    @Mock private EnsambladorAuditoria ensamblador;
    @Mock private UnidadTransporteRepository unidadTransporteRepository;
    @Mock private ConfiguracionUnidadRepository configuracionUnidadRepository;
    /** El servicio depende solo de la interfaz: la fábrica se reemplaza por un mock. */
    @Mock private UnidadTransporteFactory unidadTransporteFactory;
    @Mock private NeumaticoApi neumaticoApi;

    /** Mapper real (generado por MapStruct): el servicio depende de cómo copia los datos. */
    private final UnidadTransporteMapper unidadTransporteMapper = new UnidadTransporteMapperImpl();

    private UnidadTransporteServiceImpl servicio;

    @BeforeEach
    void crearServicio() {
        servicio = new UnidadTransporteServiceImpl(ensamblador, unidadTransporteRepository, configuracionUnidadRepository,
                unidadTransporteFactory, unidadTransporteMapper, neumaticoApi);
    }

    @Nested
    @DisplayName("obtenerDetalle")
    class ObtenerDetalle {

        @Test
        @DisplayName("arma la ficha con el neumático montado en su slot y los demás vacíos, con una sola llamada")
        void armaLaFicha() {
            UnidadTransporte unidad = unidadConNeumaticoMontado();
            when(unidadTransporteRepository.findByPublicIdAndDeletedAtIsNull(UNIDAD_PUBLIC_ID))
                    .thenReturn(Optional.of(unidad));
            when(neumaticoApi.buscarMontadosPorIds(List.of(NEUMATICO_MONTADO_ID)))
                    .thenReturn(Map.of(NEUMATICO_MONTADO_ID, neumaticoMontado()));

            UnidadTransporteDetalleDTO detalle = servicio.obtenerDetalle(UNIDAD_PUBLIC_ID);

            assertThat(detalle.publicId()).isEqualTo(UNIDAD_PUBLIC_ID);
            assertThat(detalle.configuracion().ejes()).hasSize(2);
            assertThat(detalle.slots()).hasSize(7);
            assertThat(detalle.slots().get(0).ocupado()).isTrue();
            assertThat(detalle.slots().get(0).neumatico()).isEqualTo(neumaticoMontado());
            assertThat(detalle.slots().subList(1, 7)).allSatisfy(slot -> {
                assertThat(slot.ocupado()).isFalse();
                assertThat(slot.neumatico()).isNull();
            });
            verify(configuracionUnidadRepository).findConEjesById(1L);
        }

        @Test
        @DisplayName("sin neumáticos montados consulta el lote vacío")
        void sinNeumaticos() {
            UnidadTransporte unidad = unidadConNeumaticoMontado();
            unidad.getSlots().get(0).setNeumaticoActualId(null);
            when(unidadTransporteRepository.findByPublicIdAndDeletedAtIsNull(UNIDAD_PUBLIC_ID))
                    .thenReturn(Optional.of(unidad));
            when(neumaticoApi.buscarMontadosPorIds(List.of())).thenReturn(Map.of());

            assertThat(servicio.obtenerDetalle(UNIDAD_PUBLIC_ID).slots()).allMatch(slot -> slot.neumatico() == null);
        }

        @Test
        @DisplayName("404 si no existe o está dada de baja, sin consultar neumáticos")
        void inexistente() {
            when(unidadTransporteRepository.findByPublicIdAndDeletedAtIsNull(UNIDAD_PUBLIC_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> servicio.obtenerDetalle(UNIDAD_PUBLIC_ID))
                    .isInstanceOf(EntidadNoEncontradaException.class)
                    .hasMessage(UnidadTransporteServiceImpl.MENSAJE_UNIDAD_INEXISTENTE);
            verifyNoInteractions(neumaticoApi);
        }

    }

    @Nested
    @DisplayName("registrarUnidadTransporte")
    class Registrar {

        /** Unidad que arma la fábrica real; el mock la devuelve tal cual. */
        private UnidadTransporte fabricar(String patente, ConfiguracionUnidad configuracion) {
            UnidadTransporte unidad = new UnidadTransporteFactoryImpl().crearUnidad(patente, configuracion);
            when(unidadTransporteFactory.crearUnidad(patente, configuracion)).thenReturn(unidad);
            return unidad;
        }

        /** Simula el INSERT: Hibernate asigna el publicId. */
        private void guardarAsignandoIds() {
            when(unidadTransporteRepository.saveAndFlush(any(UnidadTransporte.class))).thenAnswer(invocacion -> {
                UnidadTransporte unidad = invocacion.getArgument(0);
                unidad.setId(1L);
                unidad.setPublicId(UUID.randomUUID());
                return unidad;
            });
        }

        @Test
        @DisplayName("normaliza la patente, arma la unidad con la fábrica y la guarda con todos sus slots")
        void registra() {
            ConfiguracionUnidad configuracion = semirremolque();
            when(configuracionUnidadRepository.findConEjesById(3L)).thenReturn(Optional.of(configuracion));
            UnidadTransporte unidad = fabricar(PATENTE, configuracion);
            guardarAsignandoIds();

            UnidadTransporteResponseDTO respuesta = servicio.registrarUnidadTransporte(
                    unidadTransporteRequest(" ab-123 cd ", 3L), USUARIO);

            verify(unidadTransporteRepository).existsByPatente(PATENTE);
            verify(unidadTransporteRepository).saveAndFlush(unidad);
            assertThat(respuesta.publicId()).isEqualTo(unidad.getPublicId());
            assertThat(respuesta.patente()).isEqualTo(PATENTE);
            assertThat(respuesta.configuracion().nombre()).isEqualTo("Semirremolque");
            assertThat(respuesta.slots()).hasSize(13).allSatisfy(slot -> assertThat(slot.ocupado()).isFalse());
        }

        @Test
        @DisplayName("409 si la patente ya existe, sin buscar la configuración ni guardar")
        void patenteDuplicada() {
            when(unidadTransporteRepository.existsByPatente(PATENTE)).thenReturn(true);

            assertThatThrownBy(() -> servicio.registrarUnidadTransporte(unidadTransporteRequest("ab123cd",
                    CONFIGURACION_ID), USUARIO))
                    .isInstanceOf(EntidadDuplicadaException.class)
                    .hasMessage(UnidadTransporteServiceImpl.MENSAJE_PATENTE_DUPLICADA.formatted(PATENTE));

            verifyNoInteractions(configuracionUnidadRepository, unidadTransporteFactory);
            verify(unidadTransporteRepository, never()).saveAndFlush(any());
        }

        @Test
        @DisplayName("409 si otra alta concurrente con la misma patente gana la carrera")
        void duplicadoConcurrente() {
            ConfiguracionUnidad configuracion = tractorComun();
            when(configuracionUnidadRepository.findConEjesById(CONFIGURACION_ID))
                    .thenReturn(Optional.of(configuracion));
            fabricar(PATENTE, configuracion);
            DataIntegrityViolationException causa = new DataIntegrityViolationException("uk_unidades_transporte_patente");
            when(unidadTransporteRepository.saveAndFlush(any(UnidadTransporte.class))).thenThrow(causa);

            assertThatThrownBy(() -> servicio.registrarUnidadTransporte(unidadTransporteRequest(), USUARIO))
                    .isInstanceOf(EntidadDuplicadaException.class)
                    .hasCause(causa);
        }

        @Test
        @DisplayName("404 si la configuración no existe, sin crear la unidad")
        void configuracionInexistente() {
            when(configuracionUnidadRepository.findConEjesById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> servicio.registrarUnidadTransporte(unidadTransporteRequest(PATENTE, 99L),
                    USUARIO))
                    .isInstanceOf(EntidadNoEncontradaException.class)
                    .hasMessage(UnidadTransporteServiceImpl.MENSAJE_CONFIGURACION_INEXISTENTE);

            verify(unidadTransporteFactory, never()).crearUnidad(anyString(), any());
            verify(unidadTransporteRepository, never()).saveAndFlush(any());
        }

    }

    @Test
    @DisplayName("listarConfiguraciones devuelve las configuraciones con sus ejes y totales")
    void listarConfiguraciones() {
        when(configuracionUnidadRepository.findAllByOrderByIdAsc()).thenReturn(List.of(tractorComun(),
                semirremolque()));

        List<ConfiguracionUnidadDTO> configuraciones = servicio.listarConfiguraciones();

        assertThat(configuraciones).extracting(ConfiguracionUnidadDTO::totalSlots).containsExactly(7, 13);
        assertThat(configuraciones.get(0).ejes()).hasSize(2);
    }


    @Nested
    @DisplayName("buscar")
    class Buscar {

        @Test
        @DisplayName("consulta con el pageable recibido")
        void buscaConElPageableRecibido() {
            Pageable pageable = PageRequest.of(0, 10, Sort.by("patente"));
            when(unidadTransporteRepository.findAll(ArgumentMatchers.<Specification<UnidadTransporte>>any(), eq(pageable)))
                    .thenReturn(new PageImpl<>(List.of()));

            servicio.buscar(" ab-1 ", pageable);

            verify(unidadTransporteRepository).findAll(ArgumentMatchers.<Specification<UnidadTransporte>>any(), eq(pageable));
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = { "  ", "-" })
        @DisplayName("un término en blanco no filtra por patente")
        void terminoEnBlanco_noFiltraPorPatente(String termino) {
            Pageable pageable = PageRequest.of(0, 10, Sort.by("patente"));
            when(unidadTransporteRepository.findAll(ArgumentMatchers.<Specification<UnidadTransporte>>any(), eq(pageable)))
                    .thenReturn(new PageImpl<>(List.of()));

            assertThat(servicio.buscar(termino, pageable).getContent()).isEmpty();

            verify(unidadTransporteRepository).findAll(ArgumentMatchers.<Specification<UnidadTransporte>>any(), eq(pageable));
        }

        @Test
        @DisplayName("rechaza ordenar por otro campo sin consultar")
        void ordenNoPermitido_lanzaParametroInvalido() {
            Pageable ordenInvalido = PageRequest.of(0, 10, Sort.by("id"));

            assertThatThrownBy(() -> servicio.buscar("AB", ordenInvalido))
                    .isInstanceOf(ParametroInvalidoException.class);
            verifyNoInteractions(unidadTransporteRepository);
        }

        @Test
        @DisplayName("mapea la página al DTO de listado conservando los metadatos")
        void mapeaLaPaginaConToListadoDTO() {
            UnidadTransporte unidad = new UnidadTransporteFactoryImpl().crearUnidad(PATENTE, tractorComun());
            Pageable pageable = PageRequest.of(1, 5, Sort.by("patente"));
            when(unidadTransporteRepository.findAll(ArgumentMatchers.<Specification<UnidadTransporte>>any(), eq(pageable)))
                    .thenReturn(new PageImpl<>(List.of(unidad), pageable, 11));

            Page<UnidadTransporteListadoDTO> resultado = servicio.buscar(null, pageable);

            assertThat(resultado.getContent()).containsExactly(unidadTransporteMapper.toListadoDTO(unidad));
            assertThat(resultado.getTotalElements()).isEqualTo(11);
            assertThat(resultado.getNumber()).isEqualTo(1);
            assertThat(resultado.getSize()).isEqualTo(5);
        }
    }

    @Nested
    @DisplayName("listarAuditoria")
    class ListarAuditoria {

        @Test
        @DisplayName("aplica los filtros y ensambla la página de fichas")
        void ensamblaLaPagina() {
            Page<UnidadTransporte> pagina = new PageImpl<>(List.of(new UnidadTransporte()));
            Page<RegistroAuditoriaDTO> esperado = new PageImpl<>(List.of());
            when(unidadTransporteRepository.findAll(ArgumentMatchers.<Specification<UnidadTransporte>>any(), any(Pageable.class))).thenReturn(pagina);
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
