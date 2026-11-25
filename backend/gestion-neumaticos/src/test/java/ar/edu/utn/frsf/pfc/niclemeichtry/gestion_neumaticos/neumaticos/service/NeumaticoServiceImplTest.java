package ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.neumaticos.service;

import static ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.TestDataFactory.CENTRO_ID;
import static ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.TestDataFactory.MARCA_ID;
import static ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.TestDataFactory.PROVEEDOR_PUBLIC_ID;
import static ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.TestDataFactory.SERVICIO_ID;
import static ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.TestDataFactory.SLOT_ID;
import static ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.TestDataFactory.marca;
import static ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.TestDataFactory.neumatico;
import static ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.TestDataFactory.neumaticoEn;
import static ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.TestDataFactory.neumaticoListado;
import static ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.TestDataFactory.neumaticoNuevoRequest;
import static ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.TestDataFactory.neumaticoRequest;
import static ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.TestDataFactory.neumaticoUsadoRequest;
import static ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.TestDataFactory.proveedorResumen;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;

import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.exception.EntidadDuplicadaException;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.exception.EntidadNoEncontradaException;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.neumaticos.api.ProveedorUbicacionNeumatico;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.neumaticos.api.TipoUbicacion;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.neumaticos.api.UbicacionOperativaDTO;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.neumaticos.domain.dto.MarcaNeumaticoResponseDTO;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.neumaticos.domain.dto.NeumaticoDetalleDTO;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.neumaticos.domain.dto.NeumaticoListadoDTO;
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
class NeumaticoServiceImplTest {

    private static final String USUARIO = UUID.randomUUID().toString();

    @Mock private EnsambladorAuditoria ensamblador;
    @Mock private NeumaticoRepository neumaticoRepository;
    @Mock private MarcaNeumaticoRepository marcaNeumaticoRepository;
    @Mock private ProveedorApi proveedorApi;
    @Mock private ApplicationEventPublisher eventPublisher;
    @Mock private ProveedorUbicacionNeumatico proveedorUnidad;
    @Mock private ProveedorUbicacionNeumatico proveedorCentro;
    @Mock private ProveedorUbicacionNeumatico proveedorServicio;

    /** Mapper real (generado por MapStruct): el servicio depende de cómo copia los datos. */
    private final NeumaticoMapper neumaticoMapper = new NeumaticoMapperImpl();

    private NeumaticoServiceImpl servicio;

    @BeforeEach
    void crearServicio() {
        servicio = new NeumaticoServiceImpl(ensamblador, neumaticoRepository, neumaticoMapper, marcaNeumaticoRepository,
                proveedorApi, eventPublisher, List.of(proveedorUnidad, proveedorCentro, proveedorServicio));
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
    @DisplayName("buscar")
    class Buscar {

        @Test
        @DisplayName("mapea la página al listado y agrega el desempate por publicId al orden pedido")
        void mapeaYDesempata() {
            Neumatico neumatico = neumatico(neumaticoNuevoRequest());
            when(neumaticoRepository.findAll(ArgumentMatchers.<Specification<Neumatico>>any(), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of(neumatico), PageRequest.of(0, 10), 1));

            Page<NeumaticoListadoDTO> resultado = servicio.buscar(" abc ", MARCA_ID, EstadoNeumatico.NUEVO,
                    SituacionOperativa.EN_ESPERA, PageRequest.of(2, 5, Sort.by("numeroSerie").descending()));

            assertThat(resultado.getContent()).containsExactly(neumaticoListado(neumatico));
            assertThat(resultado.getTotalElements()).isEqualTo(1);
            ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
            verify(neumaticoRepository).findAll(ArgumentMatchers.<Specification<Neumatico>>any(), pageable.capture());
            assertThat(pageable.getValue().getPageNumber()).isEqualTo(2);
            assertThat(pageable.getValue().getPageSize()).isEqualTo(5);
            assertThat(pageable.getValue().getSort())
                    .isEqualTo(Sort.by(Sort.Order.desc("numeroSerie"), Sort.Order.asc("publicId")));
        }

        @Test
        @DisplayName("sin filtros ni orden consulta igual, ordenando solo por el desempate")
        void sinFiltros() {
            when(neumaticoRepository.findAll(ArgumentMatchers.<Specification<Neumatico>>any(), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of()));

            assertThat(servicio.buscar(null, null, null, null, PageRequest.of(0, 10)).getContent()).isEmpty();

            ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
            verify(neumaticoRepository).findAll(ArgumentMatchers.<Specification<Neumatico>>any(), pageable.capture());
            assertThat(pageable.getValue().getSort()).isEqualTo(Sort.by("publicId"));
        }

        @Test
        @DisplayName("rechaza ordenar por un campo no permitido, sin consultar")
        void rechazaOrdenNoPermitido() {
            PageRequest ordenInvalido = PageRequest.of(0, 10, Sort.by("id"));

            assertThatThrownBy(() -> servicio.buscar(null, null, null, null, ordenInvalido))
                    .isInstanceOf(ParametroInvalidoException.class);
            verifyNoInteractions(neumaticoRepository);
        }
    }

    @Nested
    @DisplayName("obtenerDetalle")
    class ObtenerDetalle {

        @BeforeEach
        void tiposDeProveedor() {
            lenient().when(proveedorUnidad.tipo()).thenReturn(TipoUbicacion.UNIDAD);
            lenient().when(proveedorCentro.tipo()).thenReturn(TipoUbicacion.CENTRO_ALMACENAMIENTO);
            lenient().when(proveedorServicio.tipo()).thenReturn(TipoUbicacion.SERVICIO_REPARACION);
        }

        private void sinConsultar(ProveedorUbicacionNeumatico... proveedores) {
            for (ProveedorUbicacionNeumatico proveedor : proveedores) {
                verify(proveedor, never()).buscar(any());
            }
        }

        private Neumatico encontrado(Neumatico neumatico) {
            when(neumaticoRepository.findByPublicIdAndDeletedAtIsNull(neumatico.getPublicId()))
                    .thenReturn(Optional.of(neumatico));
            return neumatico;
        }

        @Test
        @DisplayName("montado: pide la unidad y la posición al proveedor de unidades, solo a ese")
        void montado() {
            Neumatico neumatico = encontrado(neumaticoEn(SituacionOperativa.MONTADO));
            when(proveedorUnidad.buscar(SLOT_ID)).thenReturn(Optional.of(UbicacionOperativaDTO.deUnidad("AB123CD", "E1-IZQ")));

            NeumaticoDetalleDTO detalle = servicio.obtenerDetalle(neumatico.getPublicId());

            assertThat(detalle.ubicacion()).isEqualTo(UbicacionOperativaDTO.deUnidad("AB123CD", "E1-IZQ"));
            assertThat(detalle.marca()).isEqualTo("Fate");
            assertThat(detalle.situacionOperativaVigente()).isEqualTo(SituacionOperativa.MONTADO);
            sinConsultar(proveedorCentro, proveedorServicio);
        }

        @Test
        @DisplayName("en stock: pide el centro al proveedor de almacenamiento")
        void enStock() {
            Neumatico neumatico = encontrado(neumaticoEn(SituacionOperativa.EN_STOCK));
            when(proveedorCentro.buscar(CENTRO_ID)).thenReturn(Optional.of(UbicacionOperativaDTO.deCentro("Depósito Central")));

            assertThat(servicio.obtenerDetalle(neumatico.getPublicId()).ubicacion())
                    .isEqualTo(UbicacionOperativaDTO.deCentro("Depósito Central"));
            sinConsultar(proveedorUnidad, proveedorServicio);
        }

        @Test
        @DisplayName("en reparación: pide el servicio al proveedor de reparación")
        void enReparacion() {
            Neumatico neumatico = encontrado(neumaticoEn(SituacionOperativa.EN_REPARACION));
            when(proveedorServicio.buscar(SERVICIO_ID)).thenReturn(Optional.of(UbicacionOperativaDTO.deServicio("Gomería Sur")));

            assertThat(servicio.obtenerDetalle(neumatico.getPublicId()).ubicacion())
                    .isEqualTo(UbicacionOperativaDTO.deServicio("Gomería Sur"));
            sinConsultar(proveedorUnidad, proveedorCentro);
        }

        @ParameterizedTest
        @EnumSource(value = SituacionOperativa.class, names = { "EN_ESPERA", "DE_BAJA" })
        @DisplayName("en espera o de baja: ubicación vacía sin consultar a ningún módulo")
        void sinUbicacion(SituacionOperativa situacion) {
            Neumatico neumatico = encontrado(neumaticoEn(situacion));

            NeumaticoDetalleDTO detalle = servicio.obtenerDetalle(neumatico.getPublicId());

            assertThat(detalle.ubicacion()).isEqualTo(UbicacionOperativaDTO.vacia());
            assertThat(detalle.situacionOperativaVigente()).isEqualTo(situacion);
            sinConsultar(proveedorUnidad, proveedorCentro, proveedorServicio);
        }

        @Test
        @DisplayName("con estado DE_BAJA no muestra ubicación aunque la situación sea otra")
        void estadoDeBaja() {
            Neumatico neumatico = neumaticoEn(SituacionOperativa.MONTADO);
            neumatico.setEstadoActual(EstadoNeumatico.DE_BAJA);
            encontrado(neumatico);

            assertThat(servicio.obtenerDetalle(neumatico.getPublicId()).ubicacion())
                    .isEqualTo(UbicacionOperativaDTO.vacia());
            sinConsultar(proveedorUnidad, proveedorCentro, proveedorServicio);
        }

        @Test
        @DisplayName("si la referencia es nula queda vacía, sin consultar")
        void referenciaNula() {
            Neumatico neumatico = neumaticoEn(SituacionOperativa.MONTADO);
            neumatico.setSlotMontadoId(null);
            encontrado(neumatico);

            assertThat(servicio.obtenerDetalle(neumatico.getPublicId()).ubicacion())
                    .isEqualTo(UbicacionOperativaDTO.vacia());
            sinConsultar(proveedorUnidad, proveedorCentro, proveedorServicio);
        }

        @Test
        @DisplayName("si la referencia ya no existe queda vacía, sin error")
        void referenciaInexistente() {
            Neumatico neumatico = encontrado(neumaticoEn(SituacionOperativa.EN_STOCK));
            when(proveedorCentro.buscar(CENTRO_ID)).thenReturn(Optional.empty());

            assertThat(servicio.obtenerDetalle(neumatico.getPublicId()).ubicacion())
                    .isEqualTo(UbicacionOperativaDTO.vacia());
        }

        @Test
        @DisplayName("inexistente o con baja lógica: 404, sin consultar ubicaciones")
        void inexistente() {
            UUID publicId = UUID.randomUUID();
            when(neumaticoRepository.findByPublicIdAndDeletedAtIsNull(publicId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> servicio.obtenerDetalle(publicId))
                    .isInstanceOf(EntidadNoEncontradaException.class)
                    .hasMessage(NeumaticoServiceImpl.MENSAJE_NEUMATICO_INEXISTENTE);
            sinConsultar(proveedorUnidad, proveedorCentro, proveedorServicio);
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


    @Nested
    @DisplayName("listarAuditoria")
    class ListarAuditoria {

        @Test
        @DisplayName("aplica los filtros y ensambla la página de fichas")
        void ensamblaLaPagina() {
            Page<Neumatico> pagina = new PageImpl<>(List.of(new Neumatico()));
            Page<RegistroAuditoriaDTO> esperado = new PageImpl<>(List.of());
            when(neumaticoRepository.findAll(ArgumentMatchers.<Specification<Neumatico>>any(), any(Pageable.class))).thenReturn(pagina);
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
