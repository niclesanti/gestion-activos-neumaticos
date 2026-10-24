package ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.proveedores.service;

import static ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.TestDataFactory.domicilio;
import static ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.TestDataFactory.domicilioRequest;
import static ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.TestDataFactory.domicilioResponse;
import static ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.TestDataFactory.proveedor;
import static ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.TestDataFactory.proveedorRequest;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
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
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.common.api.DomicilioApi;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.common.api.DomicilioRequestDTO;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.common.api.DomicilioResponseDTO;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.exception.EntidadDuplicadaException;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.exception.EntidadNoEncontradaException;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.proveedores.domain.dto.ProveedorListadoDTO;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.proveedores.domain.dto.ProveedorRequestDTO;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.proveedores.domain.dto.ProveedorResponseDTO;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.proveedores.domain.entity.ProveedorNeumaticos;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.proveedores.mapper.ProveedorMapper;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.proveedores.mapper.ProveedorMapperImpl;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.proveedores.repository.ProveedorRepository;

@ExtendWith(MockitoExtension.class)
class ProveedorServiceImplTest {

    private static final String USUARIO = UUID.randomUUID().toString();
    private static final String NOMBRE = "Neumáticos del Litoral S.A.";
    private static final Long DOMICILIO_ID = 200L;

    @Mock private ProveedorRepository proveedorRepository;
    @Mock private DomicilioApi domicilioApi;

    /** Mapper real (generado por MapStruct): el servicio depende de cómo copia los datos. */
    private final ProveedorMapper proveedorMapper = new ProveedorMapperImpl();

    private ProveedorServiceImpl servicio;

    @BeforeEach
    void crearServicio() {
        servicio = new ProveedorServiceImpl(proveedorRepository, proveedorMapper, domicilioApi);
    }

    @Nested
    @DisplayName("registrarProveedor")
    class RegistrarProveedor {

        /** Simula el INSERT: la base asigna id y publicId. */
        private void guardarAsignandoIds() {
            when(proveedorRepository.saveAndFlush(any(ProveedorNeumaticos.class))).thenAnswer(invocacion -> {
                ProveedorNeumaticos proveedor = invocacion.getArgument(0);
                proveedor.setId(1L);
                proveedor.setPublicId(UUID.randomUUID());
                return proveedor;
            });
        }

        private ProveedorNeumaticos proveedorGuardado() {
            ArgumentCaptor<ProveedorNeumaticos> guardado = ArgumentCaptor.forClass(ProveedorNeumaticos.class);
            verify(proveedorRepository).saveAndFlush(guardado.capture());
            return guardado.getValue();
        }

        @Test
        @DisplayName("registra el proveedor con su domicilio y devuelve ambos, sin ids internos")
        void altaConDomicilio() {
            ProveedorRequestDTO request = proveedorRequest();
            DomicilioResponseDTO domicilio = domicilioResponse(domicilio(request.domicilio()));
            when(proveedorRepository.existsByNombreIgnoreCase(NOMBRE)).thenReturn(false);
            when(domicilioApi.registrarDomicilio(request.domicilio(), USUARIO)).thenReturn(DOMICILIO_ID);
            when(domicilioApi.buscarDomicilio(DOMICILIO_ID)).thenReturn(Optional.of(domicilio));
            guardarAsignandoIds();

            ProveedorResponseDTO respuesta = servicio.registrarProveedor(request, USUARIO);

            ProveedorNeumaticos guardado = proveedorGuardado();
            assertThat(guardado.getNombre()).isEqualTo(NOMBRE);
            assertThat(guardado.getEmail()).isEqualTo("ventas@litoral.com.ar");
            assertThat(guardado.getTelefono()).isEqualTo("+54 342 455-1234");
            assertThat(guardado.getDomicilioId()).isEqualTo(DOMICILIO_ID);
            assertThat(respuesta).isEqualTo(new ProveedorResponseDTO(guardado.getPublicId(), NOMBRE,
                    "ventas@litoral.com.ar", "+54 342 455-1234", domicilio));
        }

        @Test
        @DisplayName("sin domicilio no llama a common y responde sin domicilio")
        void altaSinDomicilio() {
            guardarAsignandoIds();

            ProveedorResponseDTO respuesta = servicio.registrarProveedor(
                    proveedorRequest("Gomería Sur", null, null, null), USUARIO);

            ProveedorNeumaticos guardado = proveedorGuardado();
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

            servicio.registrarProveedor(proveedorRequest("  Gomería Sur ", "  Ventas@GomeriaSur.COM ", "   ", null),
                    USUARIO);

            ProveedorNeumaticos guardado = proveedorGuardado();
            assertThat(guardado.getNombre()).isEqualTo("Gomería Sur");
            assertThat(guardado.getEmail()).isEqualTo("ventas@gomeriasur.com");
            assertThat(guardado.getTelefono()).isNull();
            verify(proveedorRepository).existsByNombreIgnoreCase("Gomería Sur");
        }

        @Test
        @DisplayName("un email vacío equivale a no tenerlo")
        void emailVacio() {
            guardarAsignandoIds();

            servicio.registrarProveedor(proveedorRequest("Gomería Sur", "", " 0342 455-1234 ", null), USUARIO);

            ProveedorNeumaticos guardado = proveedorGuardado();
            assertThat(guardado.getEmail()).isNull();
            assertThat(guardado.getTelefono()).isEqualTo("0342 455-1234");
        }

        @Test
        @DisplayName("si el domicilio desapareció al releerlo, responde sin domicilio")
        void domicilioNoReleido() {
            ProveedorRequestDTO request = proveedorRequest();
            when(domicilioApi.registrarDomicilio(request.domicilio(), USUARIO)).thenReturn(DOMICILIO_ID);
            when(domicilioApi.buscarDomicilio(DOMICILIO_ID)).thenReturn(Optional.empty());
            guardarAsignandoIds();

            assertThat(servicio.registrarProveedor(request, USUARIO).domicilio()).isNull();
        }

        @Test
        @DisplayName("un nombre ya registrado se rechaza sin crear domicilio ni guardar")
        void nombreDuplicado() {
            when(proveedorRepository.existsByNombreIgnoreCase(NOMBRE)).thenReturn(true);

            assertThatThrownBy(() -> servicio.registrarProveedor(proveedorRequest(), USUARIO))
                    .isInstanceOf(EntidadDuplicadaException.class)
                    .hasMessage("Ya existe un proveedor con el nombre Neumáticos del Litoral S.A.");

            verifyNoInteractions(domicilioApi);
            verify(proveedorRepository, never()).saveAndFlush(any());
        }

        @Test
        @DisplayName("una provincia inexistente del domicilio corta el alta sin guardar el proveedor")
        void provinciaInexistente() {
            DomicilioRequestDTO domicilio = domicilioRequest();
            when(domicilioApi.registrarDomicilio(domicilio, USUARIO))
                    .thenThrow(new EntidadNoEncontradaException("La provincia indicada no existe"));

            assertThatThrownBy(() -> servicio.registrarProveedor(
                    proveedorRequest("Gomería Sur", null, null, domicilio), USUARIO))
                    .isInstanceOf(EntidadNoEncontradaException.class);

            verify(proveedorRepository, never()).saveAndFlush(any());
        }

        @Test
        @DisplayName("un duplicado concurrente que frena el índice único también responde como duplicado")
        void duplicadoConcurrente() {
            when(proveedorRepository.saveAndFlush(any(ProveedorNeumaticos.class)))
                    .thenThrow(new DataIntegrityViolationException("uk_proveedores_nombre"));

            assertThatThrownBy(() -> servicio.registrarProveedor(proveedorRequest("Gomería Sur", null, null, null),
                    USUARIO))
                    .isInstanceOf(EntidadDuplicadaException.class)
                    .hasCauseInstanceOf(DataIntegrityViolationException.class);
        }

    }

    @Test
    @DisplayName("listarProveedores devuelve publicId y nombre en el orden del repositorio")
    void listarProveedores() {
        ProveedorNeumaticos proveedor = proveedor(proveedorRequest());
        when(proveedorRepository.findAllByOrderByNombreAsc()).thenReturn(List.of(proveedor));

        assertThat(servicio.listarProveedores())
                .containsExactly(new ProveedorListadoDTO(proveedor.getPublicId(), NOMBRE));
    }

}
