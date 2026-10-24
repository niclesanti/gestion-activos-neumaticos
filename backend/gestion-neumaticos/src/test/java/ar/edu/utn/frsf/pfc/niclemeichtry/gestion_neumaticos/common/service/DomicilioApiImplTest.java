package ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.common.service;

import static ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.TestDataFactory.PROVINCIA_ID;
import static ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.TestDataFactory.domicilio;
import static ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.TestDataFactory.domicilioRequest;
import static ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.TestDataFactory.domicilioResponse;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.common.api.DomicilioRequestDTO;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.common.domain.entity.Domicilio;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.common.domain.entity.Localidad;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.common.mapper.DomicilioMapper;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.common.mapper.DomicilioMapperImpl;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.common.repository.DomicilioRepository;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.common.repository.LocalidadRepository;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.common.repository.ProvinciaRepository;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.exception.EntidadNoEncontradaException;

@ExtendWith(MockitoExtension.class)
class DomicilioApiImplTest {

    private static final String USUARIO = UUID.randomUUID().toString();
    private static final Long LOCALIDAD_ID = 10L;

    @Mock private DomicilioRepository domicilioRepository;
    @Mock private LocalidadRepository localidadRepository;
    @Mock private ProvinciaRepository provinciaRepository;

    /** Mapper real (generado por MapStruct): el servicio depende de cómo copia los datos. */
    private final DomicilioMapper domicilioMapper = new DomicilioMapperImpl();

    private DomicilioApiImpl servicio;

    @BeforeEach
    void crearServicio() {
        servicio = new DomicilioApiImpl(domicilioRepository, localidadRepository, provinciaRepository,
                domicilioMapper);
    }

    @Nested
    @DisplayName("registrarDomicilio")
    class RegistrarDomicilio {

        /** Simula la provincia existente, el upsert de la localidad y el INSERT del domicilio. */
        private void prepararAlta() {
            when(provinciaRepository.existsById(PROVINCIA_ID)).thenReturn(true);
            when(localidadRepository.buscarOCrear(anyLong(), anyString(), any())).thenReturn(LOCALIDAD_ID);
            when(localidadRepository.getReferenceById(LOCALIDAD_ID))
                    .thenReturn(Localidad.builder().id(LOCALIDAD_ID).build());
            when(domicilioRepository.saveAndFlush(any(Domicilio.class))).thenAnswer(invocacion -> {
                Domicilio domicilio = invocacion.getArgument(0);
                domicilio.setId(200L);
                return domicilio;
            });
        }

        private Domicilio domicilioGuardado() {
            ArgumentCaptor<Domicilio> guardado = ArgumentCaptor.forClass(Domicilio.class);
            verify(domicilioRepository).saveAndFlush(guardado.capture());
            return guardado.getValue();
        }

        @Test
        @DisplayName("registra el domicilio con la localidad resuelta y devuelve su id")
        void altaCorrecta() {
            prepararAlta();

            Long id = servicio.registrarDomicilio(domicilioRequest(), USUARIO);

            assertThat(id).isEqualTo(200L);
            Domicilio guardado = domicilioGuardado();
            assertThat(guardado.getCalle()).isEqualTo("San Martín");
            assertThat(guardado.getAltura()).isEqualTo("2550");
            assertThat(guardado.getPiso()).isEqualTo("3");
            assertThat(guardado.getDepartamento()).isEqualTo("B");
            assertThat(guardado.getLocalidad().getId()).isEqualTo(LOCALIDAD_ID);
            verify(localidadRepository).buscarOCrear(PROVINCIA_ID, "Santa Fe", "3000");
        }

        @Test
        @DisplayName("recorta los textos, colapsa los espacios de la localidad y pasa el CPA a mayúsculas")
        void normalizaDatos() {
            prepararAlta();

            servicio.registrarDomicilio(domicilioRequest("  San Martín ", " 2550 ", "  ", "", PROVINCIA_ID,
                    "  Santo   Tomé ", " s3016abc "), USUARIO);

            Domicilio guardado = domicilioGuardado();
            assertThat(guardado.getCalle()).isEqualTo("San Martín");
            assertThat(guardado.getAltura()).isEqualTo("2550");
            assertThat(guardado.getPiso()).isNull();
            assertThat(guardado.getDepartamento()).isNull();
            verify(localidadRepository).buscarOCrear(PROVINCIA_ID, "Santo Tomé", "S3016ABC");
        }

        @Test
        @DisplayName("sin código postal ni piso ni departamento, se guardan como nulos")
        void sinOpcionales() {
            prepararAlta();

            servicio.registrarDomicilio(domicilioRequest("Belgrano", "100", null, null, PROVINCIA_ID, "Rafaela",
                    null), USUARIO);

            assertThat(domicilioGuardado().getPiso()).isNull();
            verify(localidadRepository).buscarOCrear(eq(PROVINCIA_ID), anyString(), isNull());
        }

        @Test
        @DisplayName("una provincia inexistente se rechaza sin crear localidad ni domicilio")
        void provinciaInexistente() {
            when(provinciaRepository.existsById(99L)).thenReturn(false);
            DomicilioRequestDTO request = domicilioRequest("Belgrano", "100", null, null, 99L, "Rafaela", null);

            assertThatThrownBy(() -> servicio.registrarDomicilio(request, USUARIO))
                    .isInstanceOf(EntidadNoEncontradaException.class)
                    .hasMessage("La provincia indicada no existe");

            verify(localidadRepository, never()).buscarOCrear(any(), any(), any());
            verify(domicilioRepository, never()).saveAndFlush(any());
        }

    }

    @Nested
    @DisplayName("buscarDomicilio")
    class BuscarDomicilio {

        @Test
        @DisplayName("devuelve el domicilio con su localidad y provincia")
        void existente() {
            Domicilio domicilio = domicilio(domicilioRequest());
            when(domicilioRepository.findWithLocalidadById(200L)).thenReturn(Optional.of(domicilio));

            assertThat(servicio.buscarDomicilio(200L)).contains(domicilioResponse(domicilio));
        }

        @Test
        @DisplayName("vacío si no existe")
        void inexistente() {
            when(domicilioRepository.findWithLocalidadById(1L)).thenReturn(Optional.empty());

            assertThat(servicio.buscarDomicilio(1L)).isEmpty();
        }

    }

}
