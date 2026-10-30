package ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.unidades_transporte.service;

import static ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.TestDataFactory.CONFIGURACION_ID;
import static ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.TestDataFactory.PATENTE;
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
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.exception.EntidadDuplicadaException;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.exception.EntidadNoEncontradaException;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.unidades_transporte.domain.dto.ConfiguracionUnidadDTO;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.unidades_transporte.domain.dto.UnidadTransporteResponseDTO;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.unidades_transporte.domain.entity.ConfiguracionUnidad;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.unidades_transporte.domain.entity.UnidadTransporte;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.unidades_transporte.mapper.UnidadTransporteMapper;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.unidades_transporte.mapper.UnidadTransporteMapperImpl;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.unidades_transporte.repository.ConfiguracionUnidadRepository;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.unidades_transporte.repository.UnidadTransporteRepository;

@ExtendWith(MockitoExtension.class)
class UnidadTransporteServiceImplTest {

    private static final String USUARIO = UUID.randomUUID().toString();

    @Mock private UnidadTransporteRepository unidadTransporteRepository;
    @Mock private ConfiguracionUnidadRepository configuracionUnidadRepository;
    /** El servicio depende solo de la interfaz: la fábrica se reemplaza por un mock. */
    @Mock private UnidadTransporteFactory unidadTransporteFactory;

    /** Mapper real (generado por MapStruct): el servicio depende de cómo copia los datos. */
    private final UnidadTransporteMapper unidadTransporteMapper = new UnidadTransporteMapperImpl();

    private UnidadTransporteServiceImpl servicio;

    @BeforeEach
    void crearServicio() {
        servicio = new UnidadTransporteServiceImpl(unidadTransporteRepository, configuracionUnidadRepository,
                unidadTransporteFactory, unidadTransporteMapper);
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

}
