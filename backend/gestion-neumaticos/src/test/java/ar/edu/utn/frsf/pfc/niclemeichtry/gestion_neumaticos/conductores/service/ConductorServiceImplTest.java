package ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.conductores.service;

import static ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.TestDataFactory.DNI;
import static ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.TestDataFactory.conductorRequest;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.conductores.domain.dto.ConductorResponseDTO;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.conductores.domain.entity.Conductor;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.conductores.mapper.ConductorMapper;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.conductores.mapper.ConductorMapperImpl;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.conductores.repository.ConductorRepository;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.exception.EntidadDuplicadaException;

@ExtendWith(MockitoExtension.class)
class ConductorServiceImplTest {

    private static final String USUARIO = UUID.randomUUID().toString();

    @Mock private ConductorRepository conductorRepository;

    /** Mapper real (generado por MapStruct): el servicio depende de cómo copia los datos. */
    private final ConductorMapper conductorMapper = new ConductorMapperImpl();

    private ConductorServiceImpl servicio;

    @BeforeEach
    void crearServicio() {
        servicio = new ConductorServiceImpl(conductorRepository, conductorMapper);
    }

    /** Simula el INSERT: la base asigna id y publicId. */
    private void guardarAsignandoIds() {
        when(conductorRepository.saveAndFlush(any(Conductor.class))).thenAnswer(invocacion -> {
            Conductor conductor = invocacion.getArgument(0);
            conductor.setId(100L);
            conductor.setPublicId(UUID.randomUUID());
            return conductor;
        });
    }

    private Conductor conductorGuardado() {
        ArgumentCaptor<Conductor> guardado = ArgumentCaptor.forClass(Conductor.class);
        verify(conductorRepository).saveAndFlush(guardado.capture());
        return guardado.getValue();
    }

    @Nested
    @DisplayName("registrarConductor")
    class RegistrarConductor {

        @Test
        @DisplayName("registra el conductor con su DNI y devuelve su identificador público")
        void altaCorrecta() {
            when(conductorRepository.existsByDni(DNI)).thenReturn(false);
            guardarAsignandoIds();

            ConductorResponseDTO respuesta = servicio.registrarConductor(conductorRequest(), USUARIO);

            Conductor guardado = conductorGuardado();
            assertThat(guardado.getDni()).isEqualTo(DNI);
            assertThat(guardado.getNombre()).isEqualTo("Juan");
            assertThat(guardado.getApellido()).isEqualTo("Pérez");

            assertThat(respuesta).isEqualTo(new ConductorResponseDTO(guardado.getPublicId(), DNI, "Juan", "Pérez"));
        }

        @Test
        @DisplayName("sin DNI se registra sin buscar duplicado")
        void sinDni() {
            guardarAsignandoIds();

            ConductorResponseDTO respuesta = servicio.registrarConductor(conductorRequest(null, "Juan", "Pérez"),
                    USUARIO);

            assertThat(conductorGuardado().getDni()).isNull();
            assertThat(respuesta.dni()).isNull();
            verify(conductorRepository, never()).existsByDni(anyString());
        }

        @Test
        @DisplayName("un DNI en blanco equivale a no tenerlo y no se busca duplicado")
        void dniEnBlanco() {
            guardarAsignandoIds();

            servicio.registrarConductor(conductorRequest("   ", "Juan", "Pérez"), USUARIO);

            assertThat(conductorGuardado().getDni()).isNull();
            verify(conductorRepository, never()).existsByDni(anyString());
        }

        @Test
        @DisplayName("guarda DNI, nombre y apellido sin espacios en los extremos")
        void normalizaDatos() {
            when(conductorRepository.existsByDni(DNI)).thenReturn(false);
            guardarAsignandoIds();

            servicio.registrarConductor(conductorRequest("  " + DNI + " ", "  Juan Carlos ", " Pérez  "), USUARIO);

            Conductor guardado = conductorGuardado();
            assertThat(guardado.getDni()).isEqualTo(DNI);
            assertThat(guardado.getNombre()).isEqualTo("Juan Carlos");
            assertThat(guardado.getApellido()).isEqualTo("Pérez");
        }

        @Test
        @DisplayName("un DNI ya registrado se rechaza sin guardar")
        void dniDuplicado() {
            when(conductorRepository.existsByDni(DNI)).thenReturn(true);

            assertThatThrownBy(() -> servicio.registrarConductor(conductorRequest(), USUARIO))
                    .isInstanceOf(EntidadDuplicadaException.class)
                    .hasMessage("Ya existe un conductor con el DNI 30123456");

            verify(conductorRepository, never()).saveAndFlush(any());
        }

        @Test
        @DisplayName("un duplicado concurrente que frena la restricción UNIQUE también responde como duplicado")
        void duplicadoConcurrente() {
            when(conductorRepository.existsByDni(DNI)).thenReturn(false);
            when(conductorRepository.saveAndFlush(any(Conductor.class)))
                    .thenThrow(new DataIntegrityViolationException("uk_conductores_dni"));

            assertThatThrownBy(() -> servicio.registrarConductor(conductorRequest(), USUARIO))
                    .isInstanceOf(EntidadDuplicadaException.class)
                    .hasCauseInstanceOf(DataIntegrityViolationException.class);
        }

    }

}
