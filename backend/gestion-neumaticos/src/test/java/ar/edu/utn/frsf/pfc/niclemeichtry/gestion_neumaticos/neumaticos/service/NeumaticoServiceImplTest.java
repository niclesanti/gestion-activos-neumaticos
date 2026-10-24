package ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.neumaticos.service;

import static ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.TestDataFactory.MARCA_ID;
import static ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.TestDataFactory.PROVEEDOR_PUBLIC_ID;
import static ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.TestDataFactory.marca;
import static ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.TestDataFactory.neumaticoNuevoRequest;
import static ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.TestDataFactory.neumaticoRequest;
import static ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.TestDataFactory.neumaticoUsadoRequest;
import static ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.TestDataFactory.proveedorResumen;
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
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;

import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.exception.EntidadDuplicadaException;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.exception.EntidadNoEncontradaException;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.neumaticos.domain.dto.MarcaNeumaticoResponseDTO;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.neumaticos.domain.dto.NeumaticoRequestDTO;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.neumaticos.domain.dto.NeumaticoResponseDTO;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.neumaticos.domain.entity.EstadoNeumatico;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.neumaticos.domain.entity.Neumatico;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.neumaticos.domain.entity.SituacionOperativa;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.neumaticos.event.NeumaticoComprado;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.neumaticos.mapper.NeumaticoMapper;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.neumaticos.mapper.NeumaticoMapperImpl;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.neumaticos.repository.MarcaNeumaticoRepository;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.neumaticos.repository.NeumaticoRepository;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.proveedores.api.ProveedorApi;

@ExtendWith(MockitoExtension.class)
class NeumaticoServiceImplTest {

    private static final String USUARIO = UUID.randomUUID().toString();

    @Mock private NeumaticoRepository neumaticoRepository;
    @Mock private MarcaNeumaticoRepository marcaNeumaticoRepository;
    @Mock private ProveedorApi proveedorApi;
    @Mock private ApplicationEventPublisher eventPublisher;

    /** Mapper real (generado por MapStruct): el servicio depende de cómo copia los datos. */
    private final NeumaticoMapper neumaticoMapper = new NeumaticoMapperImpl();

    private NeumaticoServiceImpl servicio;

    @BeforeEach
    void crearServicio() {
        servicio = new NeumaticoServiceImpl(neumaticoRepository, neumaticoMapper, marcaNeumaticoRepository,
                proveedorApi, eventPublisher);
    }

    /** Simula el INSERT: la base asigna id y publicId. */
    private void guardarAsignandoIds() {
        when(neumaticoRepository.saveAndFlush(any(Neumatico.class))).thenAnswer(invocacion -> {
            Neumatico neumatico = invocacion.getArgument(0);
            neumatico.setId(100L);
            neumatico.setPublicId(UUID.randomUUID());
            return neumatico;
        });
    }

    private Neumatico neumaticoGuardado() {
        ArgumentCaptor<Neumatico> guardado = ArgumentCaptor.forClass(Neumatico.class);
        verify(neumaticoRepository).saveAndFlush(guardado.capture());
        return guardado.getValue();
    }

    private NeumaticoComprado eventoPublicado() {
        ArgumentCaptor<NeumaticoComprado> evento = ArgumentCaptor.forClass(NeumaticoComprado.class);
        verify(eventPublisher).publishEvent(evento.capture());
        return evento.getValue();
    }

    @Nested
    @DisplayName("registrarNeumatico")
    class RegistrarNeumatico {

        @Test
        @DisplayName("un neumático nuevo queda en espera y publica la compra con el proveedor")
        void nuevoConProveedor() {
            NeumaticoRequestDTO request = neumaticoNuevoRequest();
            when(neumaticoRepository.existsByNumeroSerie("ABC-123")).thenReturn(false);
            when(marcaNeumaticoRepository.findById(MARCA_ID)).thenReturn(Optional.of(marca()));
            when(proveedorApi.buscarPorPublicId(PROVEEDOR_PUBLIC_ID)).thenReturn(Optional.of(proveedorResumen()));
            guardarAsignandoIds();

            NeumaticoResponseDTO respuesta = servicio.registrarNeumatico(request, USUARIO);

            Neumatico guardado = neumaticoGuardado();
            assertThat(guardado.getEnEspera()).isTrue();
            assertThat(guardado.getSituacionOperativaVigente()).isEqualTo(SituacionOperativa.EN_ESPERA);
            assertThat(guardado.getTipoActual()).isEqualTo(request.tipoActual());
            assertThat(guardado.getMarca().getNombre()).isEqualTo("Fate");

            assertThat(respuesta.publicId()).isEqualTo(guardado.getPublicId());
            assertThat(respuesta.marcaNombre()).isEqualTo("Fate");
            assertThat(respuesta.situacionOperativaVigente()).isEqualTo(SituacionOperativa.EN_ESPERA);

            NeumaticoComprado evento = eventoPublicado();
            assertThat(evento.neumaticoId()).isEqualTo(100L);
            assertThat(evento.numeroSerie()).isEqualTo("ABC-123");
            assertThat(evento.proveedorId()).isEqualTo(1L);
            assertThat(evento.nombreProveedor()).isEqualTo("Neumáticos del Litoral S.A.");
            assertThat(evento.usuario()).isEqualTo(USUARIO);
            assertThat(evento.comentario()).isEqualTo("Compra inicial");
            assertThat(evento.fecha()).isNotNull();
        }

        @Test
        @DisplayName("un neumático nuevo sin proveedor publica la compra sin proveedor")
        void nuevoSinProveedor() {
            NeumaticoRequestDTO base = neumaticoNuevoRequest();
            NeumaticoRequestDTO request = new NeumaticoRequestDTO(base.numeroSerie(), base.estadoActual(),
                    base.kilometrajeTotal(), base.kilometrajeUltimaReparacion(), base.tipoOrigen(), base.tipoActual(),
                    base.cantidadRecapados(), base.comentario(), base.marcaId(), null);
            when(marcaNeumaticoRepository.findById(MARCA_ID)).thenReturn(Optional.of(marca()));
            guardarAsignandoIds();

            servicio.registrarNeumatico(request, USUARIO);

            NeumaticoComprado evento = eventoPublicado();
            assertThat(evento.proveedorId()).isNull();
            assertThat(evento.nombreProveedor()).isNull();
            verifyNoInteractions(proveedorApi);
        }

        @Test
        @DisplayName("un neumático usado no publica la compra e ignora el proveedor")
        void usadoSinEvento() {
            NeumaticoRequestDTO base = neumaticoUsadoRequest();
            NeumaticoRequestDTO request = new NeumaticoRequestDTO(base.numeroSerie(), base.estadoActual(),
                    base.kilometrajeTotal(), base.kilometrajeUltimaReparacion(), base.tipoOrigen(), base.tipoActual(),
                    base.cantidadRecapados(), base.comentario(), base.marcaId(), PROVEEDOR_PUBLIC_ID);
            when(marcaNeumaticoRepository.findById(MARCA_ID)).thenReturn(Optional.of(marca()));
            guardarAsignandoIds();

            NeumaticoResponseDTO respuesta = servicio.registrarNeumatico(request, USUARIO);

            assertThat(respuesta.numeroSerie()).isNull();
            assertThat(respuesta.estadoActual()).isEqualTo(EstadoNeumatico.RECAPADO_USADO);
            verifyNoInteractions(proveedorApi, eventPublisher);
        }

        @Test
        @DisplayName("guarda el número de serie recortado y en mayúsculas")
        void normalizaNumeroSerie() {
            when(neumaticoRepository.existsByNumeroSerie("AB 12/X")).thenReturn(false);
            when(marcaNeumaticoRepository.findById(MARCA_ID)).thenReturn(Optional.of(marca()));
            when(proveedorApi.buscarPorPublicId(PROVEEDOR_PUBLIC_ID)).thenReturn(Optional.of(proveedorResumen()));
            guardarAsignandoIds();

            servicio.registrarNeumatico(neumaticoRequest("  ab 12/x ", EstadoNeumatico.NUEVO, 0f, 0f, 0), USUARIO);

            assertThat(neumaticoGuardado().getNumeroSerie()).isEqualTo("AB 12/X");
        }

        @Test
        @DisplayName("un número de serie en blanco equivale a no tenerlo y no se busca duplicado")
        void numeroSerieEnBlanco() {
            when(marcaNeumaticoRepository.findById(MARCA_ID)).thenReturn(Optional.of(marca()));
            when(proveedorApi.buscarPorPublicId(PROVEEDOR_PUBLIC_ID)).thenReturn(Optional.of(proveedorResumen()));
            guardarAsignandoIds();

            servicio.registrarNeumatico(neumaticoRequest("   ", EstadoNeumatico.NUEVO, 0f, 0f, 0), USUARIO);

            assertThat(neumaticoGuardado().getNumeroSerie()).isNull();
            verify(neumaticoRepository, never()).existsByNumeroSerie(anyString());
        }

        @Test
        @DisplayName("un número de serie ya registrado se rechaza sin guardar ni publicar")
        void numeroSerieDuplicado() {
            when(neumaticoRepository.existsByNumeroSerie("ABC-123")).thenReturn(true);

            assertThatThrownBy(() -> servicio.registrarNeumatico(neumaticoRequest("abc-123", EstadoNeumatico.NUEVO,
                    0f, 0f, 0), USUARIO))
                    .isInstanceOf(EntidadDuplicadaException.class)
                    .hasMessage("Ya existe un neumático con el número de serie ABC-123");

            verify(neumaticoRepository, never()).saveAndFlush(any());
            verifyNoInteractions(eventPublisher);
        }

        @Test
        @DisplayName("un duplicado concurrente que frena la restricción UNIQUE también responde como duplicado")
        void duplicadoConcurrente() {
            when(neumaticoRepository.existsByNumeroSerie("ABC-123")).thenReturn(false);
            when(marcaNeumaticoRepository.findById(MARCA_ID)).thenReturn(Optional.of(marca()));
            when(proveedorApi.buscarPorPublicId(PROVEEDOR_PUBLIC_ID)).thenReturn(Optional.of(proveedorResumen()));
            when(neumaticoRepository.saveAndFlush(any(Neumatico.class)))
                    .thenThrow(new DataIntegrityViolationException("uk_neumaticos_numero_serie"));

            assertThatThrownBy(() -> servicio.registrarNeumatico(neumaticoNuevoRequest(), USUARIO))
                    .isInstanceOf(EntidadDuplicadaException.class)
                    .hasCauseInstanceOf(DataIntegrityViolationException.class);

            verifyNoInteractions(eventPublisher);
        }

        @Test
        @DisplayName("una marca inexistente se rechaza")
        void marcaInexistente() {
            when(neumaticoRepository.existsByNumeroSerie("ABC-123")).thenReturn(false);
            when(marcaNeumaticoRepository.findById(MARCA_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> servicio.registrarNeumatico(neumaticoNuevoRequest(), USUARIO))
                    .isInstanceOf(EntidadNoEncontradaException.class)
                    .hasMessage(NeumaticoServiceImpl.MENSAJE_MARCA_INEXISTENTE);

            verify(neumaticoRepository, never()).saveAndFlush(any());
        }

        @Test
        @DisplayName("un proveedor inexistente se rechaza antes de guardar")
        void proveedorInexistente() {
            when(neumaticoRepository.existsByNumeroSerie("ABC-123")).thenReturn(false);
            when(marcaNeumaticoRepository.findById(MARCA_ID)).thenReturn(Optional.of(marca()));
            when(proveedorApi.buscarPorPublicId(PROVEEDOR_PUBLIC_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> servicio.registrarNeumatico(neumaticoNuevoRequest(), USUARIO))
                    .isInstanceOf(EntidadNoEncontradaException.class)
                    .hasMessage(NeumaticoServiceImpl.MENSAJE_PROVEEDOR_INEXISTENTE);

            verify(neumaticoRepository, never()).saveAndFlush(any());
            verifyNoInteractions(eventPublisher);
        }

    }

    @Nested
    @DisplayName("listarMarcas")
    class ListarMarcas {

        @Test
        @DisplayName("devuelve las marcas en el orden del repositorio")
        void devuelveLasMarcas() {
            when(marcaNeumaticoRepository.findAllByOrderByNombreAsc()).thenReturn(List.of(marca()));

            assertThat(servicio.listarMarcas()).containsExactly(new MarcaNeumaticoResponseDTO(MARCA_ID, "Fate"));
        }

    }

}
