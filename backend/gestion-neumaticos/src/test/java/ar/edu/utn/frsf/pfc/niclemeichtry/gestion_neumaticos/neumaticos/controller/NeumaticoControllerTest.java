package ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.neumaticos.controller;

import static ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.TestDataFactory.neumatico;
import static ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.TestDataFactory.neumaticoDetalle;
import static ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.TestDataFactory.neumaticoEn;
import static ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.TestDataFactory.neumaticoNuevoRequest;
import static ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.TestDataFactory.neumaticoRequest;
import static ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.TestDataFactory.neumaticoResponse;
import static ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.TestDataFactory.neumaticoListado;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.endsWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.restdocs.request.RequestDocumentation.queryParameters;
import static org.mockito.Mockito.when;
import static org.springframework.restdocs.headers.HeaderDocumentation.headerWithName;
import static org.springframework.restdocs.headers.HeaderDocumentation.responseHeaders;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessRequest;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessResponse;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.restdocs.test.autoconfigure.AutoConfigureRestDocs;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.config.CorsConfig;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.config.PaginacionConfig;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.exception.EntidadDuplicadaException;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.exception.EntidadNoEncontradaException;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.exception.ParametroInvalidoException;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.neumaticos.api.UbicacionOperativaDTO;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.neumaticos.domain.dto.MarcaNeumaticoResponseDTO;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.neumaticos.domain.dto.NeumaticoDetalleDTO;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.neumaticos.domain.dto.NeumaticoListadoDTO;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.neumaticos.domain.dto.NeumaticoRequestDTO;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.neumaticos.domain.entity.EstadoNeumatico;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.neumaticos.domain.entity.Neumatico;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.neumaticos.domain.entity.SituacionOperativa;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.neumaticos.service.NeumaticoService;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.security.NivelAccesoVigente;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.security.SecurityConfig;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.security.web.JsonSecurityErrorHandler;
import tools.jackson.databind.json.JsonMapper;

@WebMvcTest(NeumaticoController.class)
@Import({ SecurityConfig.class, JsonSecurityErrorHandler.class, CorsConfig.class, PaginacionConfig.class })
@AutoConfigureRestDocs
class NeumaticoControllerTest {

    private static final String NEUMATICOS = "/api/v1/neumaticos";
    private static final String MARCAS = "/api/v1/neumaticos/marcas";
    private static final UUID USUARIO = UUID.randomUUID();

    @Autowired private MockMvc mockMvc;
    @Autowired private JsonMapper jsonMapper;
    @MockitoBean private NeumaticoService neumaticoService;
    @MockitoBean private JwtDecoder jwtDecoder;
    @MockitoBean private NivelAccesoVigente nivelAccesoVigente;

    private String json(Object body) {
        return jsonMapper.writeValueAsString(body);
    }

    private static RequestPostProcessor usuario(String nivel) {
        return jwt().jwt(j -> j.subject(USUARIO.toString())).authorities(new SimpleGrantedAuthority(nivel));
    }

    @Nested
    @DisplayName("POST /api/v1/neumaticos")
    class Registrar {

        @Test
        @DisplayName("201 con el neumático registrado; el usuario es el sub del token")
        void registraConEditor() throws Exception {
            NeumaticoRequestDTO request = neumaticoNuevoRequest();
            Neumatico neumatico = neumatico(request);
            when(neumaticoService.registrarNeumatico(request, USUARIO.toString()))
                    .thenReturn(neumaticoResponse(neumatico));

            mockMvc.perform(post(NEUMATICOS).with(usuario("ROLE_EDITOR"))
                            .contentType(MediaType.APPLICATION_JSON).content(json(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.publicId").value(neumatico.getPublicId().toString()))
                    .andExpect(header().string(HttpHeaders.LOCATION,
                            endsWith("/api/v1/neumaticos/" + neumatico.getPublicId())))
                    .andExpect(jsonPath("$.numeroSerie").value("ABC-123"))
                    .andExpect(jsonPath("$.situacionOperativaVigente").value("EN_ESPERA"))
                    .andExpect(jsonPath("$.enEspera").value(true))
                    .andExpect(jsonPath("$.marcaNombre").value("Fate"))
                    .andExpect(jsonPath("$.id").doesNotExist())
                    .andDo(document("neumatico-registrar",
                            preprocessRequest(prettyPrint()),
                            preprocessResponse(prettyPrint()),
                            responseHeaders(
                                    headerWithName(HttpHeaders.LOCATION).description("URL del neumático creado")),
                            requestFields(
                                    fieldWithPath("numeroSerie").description("Número de serie (opcional, único, "
                                            + "no modificable)").optional(),
                                    fieldWithPath("estadoActual").description("Estado inicial (no DE_BAJA)"),
                                    fieldWithPath("kilometrajeTotal").description("Kilometraje actual (0 si es nuevo)"),
                                    fieldWithPath("kilometrajeUltimaReparacion")
                                            .description("Kilometraje desde la última reparación (≤ total)"),
                                    fieldWithPath("tipoOrigen").description("Tipo de eje de origen"),
                                    fieldWithPath("tipoActual").description("Tipo de eje actual"),
                                    fieldWithPath("cantidadRecapados").description("Cantidad de recapados (0 a 20)"),
                                    fieldWithPath("comentario").description("Comentario (opcional)").optional(),
                                    fieldWithPath("marcaId").description("Id de la marca"),
                                    fieldWithPath("proveedorId").description("Proveedor de la compra (opcional, "
                                            + "solo si es nuevo)").optional()),
                            responseFields(
                                    fieldWithPath("publicId").description("Identificador público"),
                                    fieldWithPath("numeroSerie").description("Número de serie normalizado").optional(),
                                    fieldWithPath("estadoActual").description("Estado"),
                                    fieldWithPath("enEspera").description("Siempre true al registrarse"),
                                    fieldWithPath("kilometrajeTotal").description("Kilometraje total"),
                                    fieldWithPath("kilometrajeUltimaReparacion")
                                            .description("Kilometraje desde la última reparación"),
                                    fieldWithPath("tipoOrigen").description("Tipo de eje de origen"),
                                    fieldWithPath("tipoActual").description("Tipo de eje actual"),
                                    fieldWithPath("cantidadRecapados").description("Cantidad de recapados"),
                                    fieldWithPath("situacionOperativaVigente").description("EN_ESPERA al registrarse"),
                                    fieldWithPath("comentario").description("Comentario").optional(),
                                    fieldWithPath("marcaNombre").description("Nombre de la marca"),
                                    fieldWithPath("patenteUnidadTransporte").description("Patente si está montado")
                                            .optional(),
                                    fieldWithPath("posicionEjeUnidadTransporte").description("Posición si está montado")
                                            .optional(),
                                    fieldWithPath("nroEjeUnidadTransporte").description("Eje si está montado")
                                            .optional(),
                                    fieldWithPath("tipoEjeUnidadTransporte").description("Tipo de eje si está montado")
                                            .optional(),
                                    fieldWithPath("nombreCentroAlmacenamiento").description("Centro si está en stock")
                                            .optional(),
                                    fieldWithPath("nombreServicioReparacion").description("Servicio si está en reparación")
                                            .optional(),
                                    fieldWithPath("ubicacionActual").description("Ubicación actual").optional())));
        }

        @Test
        @DisplayName("el administrador también puede registrar")
        void registraConAdministrador() throws Exception {
            NeumaticoRequestDTO request = neumaticoNuevoRequest();
            when(neumaticoService.registrarNeumatico(eq(request), anyString()))
                    .thenReturn(neumaticoResponse(neumatico(request)));

            mockMvc.perform(post(NEUMATICOS).with(usuario("ROLE_ADMINISTRADOR"))
                            .contentType(MediaType.APPLICATION_JSON).content(json(request)))
                    .andExpect(status().isCreated());
        }

        @Test
        @DisplayName("403 para el lector, sin llegar al servicio")
        void lectorNoRegistra() throws Exception {
            mockMvc.perform(post(NEUMATICOS).with(usuario("ROLE_LECTOR"))
                            .contentType(MediaType.APPLICATION_JSON).content(json(neumaticoNuevoRequest())))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.status").value(403));

            verifyNoInteractions(neumaticoService);
        }

        @Test
        @DisplayName("401 sin token")
        void sinToken() throws Exception {
            mockMvc.perform(post(NEUMATICOS).contentType(MediaType.APPLICATION_JSON).content(json(neumaticoNuevoRequest())))
                    .andExpect(status().isUnauthorized());

            verifyNoInteractions(neumaticoService);
        }

        @Test
        @DisplayName("400 con el campo y el motivo si viola una regla del alta")
        void reglaDeNegocio() throws Exception {
            NeumaticoRequestDTO request = neumaticoRequest(null, EstadoNeumatico.NUEVO, 1000f, 0f, 0);

            mockMvc.perform(post(NEUMATICOS).with(usuario("ROLE_EDITOR"))
                            .contentType(MediaType.APPLICATION_JSON).content(json(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value(containsString("nuevoSinUso:")));

            verifyNoInteractions(neumaticoService);
        }

        @ParameterizedTest(name = "estadoActual = {0}")
        @ValueSource(strings = { "\"INEXISTENTE\"", "123" })
        @DisplayName("400 si un enum trae un valor desconocido")
        void enumDesconocido(String valor) throws Exception {
            String cuerpo = json(neumaticoNuevoRequest()).replace("\"NUEVO\"", valor);

            mockMvc.perform(post(NEUMATICOS).with(usuario("ROLE_EDITOR"))
                            .contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("409 si el número de serie ya existe")
        void duplicado() throws Exception {
            when(neumaticoService.registrarNeumatico(any(), anyString()))
                    .thenThrow(new EntidadDuplicadaException("Ya existe un neumático con el número de serie ABC-123"));

            mockMvc.perform(post(NEUMATICOS).with(usuario("ROLE_EDITOR"))
                            .contentType(MediaType.APPLICATION_JSON).content(json(neumaticoNuevoRequest())))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.message").value("Ya existe un neumático con el número de serie ABC-123"));
        }

        @Test
        @DisplayName("404 si la marca no existe")
        void marcaInexistente() throws Exception {
            when(neumaticoService.registrarNeumatico(any(), anyString()))
                    .thenThrow(new EntidadNoEncontradaException("La marca de neumático indicada no existe"));

            mockMvc.perform(post(NEUMATICOS).with(usuario("ROLE_EDITOR"))
                            .contentType(MediaType.APPLICATION_JSON).content(json(neumaticoNuevoRequest())))
                    .andExpect(status().isNotFound());
        }

    }

    @Nested
    @DisplayName("GET /api/v1/neumaticos")
    class Buscar {

        private final NeumaticoListadoDTO fila = neumaticoListado(neumatico(neumaticoNuevoRequest()));

        private Page<NeumaticoListadoDTO> paginaConUnNeumatico() {
            return new PageImpl<>(List.of(fila), PageRequest.of(0, 10), 1);
        }

        private Pageable pageableUsado() {
            ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
            verify(neumaticoService).buscar(any(), any(), any(), any(), pageable.capture());
            return pageable.getValue();
        }

        @Test
        @DisplayName("200 con el listado resumido y los filtros recibidos, sin ids internos")
        void buscaConLector() throws Exception {
            when(neumaticoService.buscar(eq("abc"), eq(1L), eq(EstadoNeumatico.NUEVO),
                    eq(SituacionOperativa.EN_ESPERA), any(Pageable.class))).thenReturn(paginaConUnNeumatico());

            mockMvc.perform(get(NEUMATICOS).with(usuario("ROLE_LECTOR"))
                            .param("numeroSerie", "abc").param("marcaId", "1").param("estado", "NUEVO")
                            .param("situacion", "EN_ESPERA").param("page", "0").param("size", "5")
                            .param("sort", "numeroSerie,asc"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].publicId").value(fila.publicId().toString()))
                    .andExpect(jsonPath("$.content[0].numeroSerie").value("ABC-123"))
                    .andExpect(jsonPath("$.content[0].marca").value("Fate"))
                    .andExpect(jsonPath("$.content[0].estadoActual").value("NUEVO"))
                    .andExpect(jsonPath("$.content[0].situacionOperativaVigente").value("EN_ESPERA"))
                    .andExpect(jsonPath("$.content[0].kilometrajeTotal").value(0))
                    .andExpect(jsonPath("$.content[0].id").doesNotExist())
                    .andExpect(jsonPath("$.page.totalElements").value(1))
                    .andDo(document("neumaticos-busqueda",
                            preprocessRequest(prettyPrint()),
                            preprocessResponse(prettyPrint()),
                            queryParameters(
                                    parameterWithName("numeroSerie").description("Número de serie completo o parcial (opcional)"),
                                    parameterWithName("marcaId").description("Id de la marca, de `/marcas` (opcional)"),
                                    parameterWithName("estado").description("Estado técnico (opcional)"),
                                    parameterWithName("situacion").description("Situación operativa vigente (opcional)"),
                                    parameterWithName("page").description("Página, desde 0"),
                                    parameterWithName("size").description("Tamaño de página (10 por defecto, máximo 20)"),
                                    parameterWithName("sort").description("Solo `numeroSerie,asc|desc`")),
                            responseFields(
                                    fieldWithPath("content[].publicId").description("Identificador público"),
                                    fieldWithPath("content[].numeroSerie").description("Número de serie").optional(),
                                    fieldWithPath("content[].marca").description("Nombre de la marca"),
                                    fieldWithPath("content[].estadoActual").description("Estado técnico"),
                                    fieldWithPath("content[].situacionOperativaVigente").description("Situación operativa"),
                                    fieldWithPath("content[].kilometrajeTotal").description("Kilometraje total").optional(),
                                    fieldWithPath("page.size").description("Tamaño de página"),
                                    fieldWithPath("page.number").description("Número de página, desde 0"),
                                    fieldWithPath("page.totalElements").description("Total de neumáticos"),
                                    fieldWithPath("page.totalPages").description("Total de páginas"))));

            assertThat(pageableUsado().getPageSize()).isEqualTo(5);
        }

        @ParameterizedTest
        @ValueSource(strings = { "ROLE_EDITOR", "ROLE_ADMINISTRADOR" })
        @DisplayName("200 también para editor y administrador")
        void buscaConEditorYAdministrador(String nivel) throws Exception {
            when(neumaticoService.buscar(any(), any(), any(), any(), any(Pageable.class)))
                    .thenReturn(paginaConUnNeumatico());

            mockMvc.perform(get(NEUMATICOS).with(usuario(nivel))).andExpect(status().isOk());
        }

        @Test
        @DisplayName("sin parámetros: sin filtros, 10 por página, ordenados por número de serie ascendente")
        void sinParametros_usaOrdenYTamanioPorDefecto() throws Exception {
            when(neumaticoService.buscar(any(), any(), any(), any(), any(Pageable.class)))
                    .thenReturn(paginaConUnNeumatico());

            mockMvc.perform(get(NEUMATICOS).with(usuario("ROLE_LECTOR"))).andExpect(status().isOk());

            verify(neumaticoService).buscar(isNull(), isNull(), isNull(), isNull(), any(Pageable.class));
            Pageable pageable = pageableUsado();
            assertThat(pageable.getPageNumber()).isZero();
            assertThat(pageable.getPageSize()).isEqualTo(PaginacionConfig.TAMANIO_PAGINA_POR_DEFECTO);
            assertThat(pageable.getSort()).isEqualTo(Sort.by(Sort.Order.asc("numeroSerie")));
        }

        @Test
        @DisplayName("200 con página vacía si no hay coincidencias")
        void sinResultados_devuelvePaginaVacia() throws Exception {
            when(neumaticoService.buscar(any(), any(), any(), any(), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 10), 0));

            mockMvc.perform(get(NEUMATICOS).with(usuario("ROLE_LECTOR")).param("numeroSerie", "QQQ"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isEmpty())
                    .andExpect(jsonPath("$.page.totalElements").value(0));
        }

        @Test
        @DisplayName("un tamaño mayor al máximo se acota a 20")
        void tamanioMayorAlMaximo_seAcota() throws Exception {
            when(neumaticoService.buscar(any(), any(), any(), any(), any(Pageable.class)))
                    .thenReturn(paginaConUnNeumatico());

            mockMvc.perform(get(NEUMATICOS).with(usuario("ROLE_LECTOR")).param("size", "500"))
                    .andExpect(status().isOk());

            assertThat(pageableUsado().getPageSize()).isEqualTo(PaginacionConfig.TAMANIO_PAGINA_MAXIMO);
        }

        @Test
        @DisplayName("400 si se ordena por un campo no permitido")
        void ordenNoPermitido() throws Exception {
            when(neumaticoService.buscar(any(), any(), any(), any(), any(Pageable.class)))
                    .thenThrow(new ParametroInvalidoException("Solo se puede ordenar por número de serie"));

            mockMvc.perform(get(NEUMATICOS).with(usuario("ROLE_LECTOR")).param("sort", "id,asc"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("Solo se puede ordenar por número de serie"));
        }

        @ParameterizedTest
        @CsvSource({ "estado,INEXISTENTE", "situacion,INEXISTENTE", "marcaId,abc" })
        @DisplayName("400 si un filtro trae un valor de tipo inválido, sin llegar al servicio")
        void filtroConTipoInvalido(String parametro, String valor) throws Exception {
            mockMvc.perform(get(NEUMATICOS).with(usuario("ROLE_LECTOR")).param(parametro, valor))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400));

            verifyNoInteractions(neumaticoService);
        }

        @Test
        @DisplayName("401 sin token")
        void sinToken() throws Exception {
            mockMvc.perform(get(NEUMATICOS))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.status").value(401));

            verifyNoInteractions(neumaticoService);
        }
    }

    @Nested
    @DisplayName("GET /api/v1/neumaticos/marcas")
    class Marcas {

        @Test
        @DisplayName("el lector puede listar las marcas")
        void lectorLista() throws Exception {
            when(neumaticoService.listarMarcas()).thenReturn(List.of(
                    new MarcaNeumaticoResponseDTO(1L, "Bridgestone"), new MarcaNeumaticoResponseDTO(51L, "Fate")));

            mockMvc.perform(get(MARCAS).with(usuario("ROLE_LECTOR")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].nombre").value("Bridgestone"))
                    .andExpect(jsonPath("$[1].id").value(51));
        }

        @Test
        @DisplayName("401 sin token")
        void sinToken() throws Exception {
            mockMvc.perform(get(MARCAS)).andExpect(status().isUnauthorized());
        }

    }

    @Nested
    @DisplayName("GET /api/v1/neumaticos/{publicId}")
    class Detalle {

        private static final String DETALLE = NEUMATICOS + "/{publicId}";

        @Test
        @DisplayName("200 con la ficha y la ubicación, sin ids internos")
        void detalleConLector() throws Exception {
            Neumatico neumatico = neumaticoEn(SituacionOperativa.MONTADO);
            NeumaticoDetalleDTO detalle = new NeumaticoDetalleDTO(neumatico.getPublicId(), neumatico.getNumeroSerie(),
                    "Fate", neumatico.getEstadoActual(), SituacionOperativa.MONTADO, 1500f, 200f,
                    neumatico.getTipoOrigen(), neumatico.getTipoActual(), 1, "Compra inicial",
                    UbicacionOperativaDTO.deUnidad("AB123CD", "E1-IZQ"));
            when(neumaticoService.obtenerDetalle(neumatico.getPublicId())).thenReturn(detalle);

            mockMvc.perform(get(DETALLE, neumatico.getPublicId()).with(usuario("ROLE_LECTOR")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.publicId").value(neumatico.getPublicId().toString()))
                    .andExpect(jsonPath("$.numeroSerie").value("ABC-123"))
                    .andExpect(jsonPath("$.marca").value("Fate"))
                    .andExpect(jsonPath("$.situacionOperativaVigente").value("MONTADO"))
                    .andExpect(jsonPath("$.ubicacion.unidad").value("AB123CD"))
                    .andExpect(jsonPath("$.ubicacion.posicion").value("E1-IZQ"))
                    .andExpect(jsonPath("$.id").doesNotExist())
                    .andExpect(jsonPath("$.slotMontadoId").doesNotExist())
                    .andExpect(jsonPath("$.centroAlmacenamientoId").doesNotExist())
                    .andExpect(jsonPath("$.servicioReparacionId").doesNotExist())
                    .andDo(document("neumatico-detalle",
                            preprocessRequest(prettyPrint()),
                            preprocessResponse(prettyPrint()),
                            pathParameters(parameterWithName("publicId").description("Identificador público del neumático")),
                            responseFields(
                                    fieldWithPath("publicId").description("Identificador público"),
                                    fieldWithPath("numeroSerie").description("Número de serie (puede ser null)").optional(),
                                    fieldWithPath("marca").description("Nombre de la marca"),
                                    fieldWithPath("estadoActual").description("Estado técnico"),
                                    fieldWithPath("situacionOperativaVigente").description("Situación operativa vigente"),
                                    fieldWithPath("kilometrajeTotal").description("Kilometraje total (puede ser null)").optional(),
                                    fieldWithPath("kilometrajeUltimaReparacion")
                                            .description("Kilometraje desde la última reparación (puede ser null)").optional(),
                                    fieldWithPath("tipoOrigen").description("Tipo de eje de origen"),
                                    fieldWithPath("tipoActual").description("Tipo de eje actual"),
                                    fieldWithPath("cantidadRecapados").description("Cantidad de recapados (puede ser null)").optional(),
                                    fieldWithPath("comentario").description("Comentario (puede ser null)").optional(),
                                    fieldWithPath("ubicacion").description("Ubicación operativa vigente"),
                                    fieldWithPath("ubicacion.unidad").description("Patente de la unidad (si está montado)").optional(),
                                    fieldWithPath("ubicacion.posicion").description("Código de la posición (si está montado)").optional(),
                                    fieldWithPath("ubicacion.centroAlmacenamiento")
                                            .description("Nombre del centro (si está en stock)").optional(),
                                    fieldWithPath("ubicacion.servicioReparacion")
                                            .description("Nombre del servicio (si está en reparación)").optional())));
        }

        @ParameterizedTest
        @ValueSource(strings = { "ROLE_EDITOR", "ROLE_ADMINISTRADOR" })
        @DisplayName("200 también para editor y administrador")
        void detalleConEditorYAdministrador(String nivel) throws Exception {
            Neumatico neumatico = neumaticoEn(SituacionOperativa.EN_ESPERA);
            when(neumaticoService.obtenerDetalle(neumatico.getPublicId())).thenReturn(neumaticoDetalle(neumatico));

            mockMvc.perform(get(DETALLE, neumatico.getPublicId()).with(usuario(nivel))).andExpect(status().isOk());
        }

        @Test
        @DisplayName("404 si el neumático no existe o tiene baja lógica")
        void inexistente() throws Exception {
            UUID publicId = UUID.randomUUID();
            when(neumaticoService.obtenerDetalle(publicId))
                    .thenThrow(new EntidadNoEncontradaException("El neumático indicado no existe"));

            mockMvc.perform(get(DETALLE, publicId).with(usuario("ROLE_LECTOR")))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value("El neumático indicado no existe"));
        }

        @Test
        @DisplayName("400 si el identificador no es un UUID, sin llegar al servicio")
        void identificadorInvalido() throws Exception {
            mockMvc.perform(get(DETALLE, "no-es-uuid").with(usuario("ROLE_LECTOR")))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(neumaticoService);
        }

        @Test
        @DisplayName("401 sin token")
        void sinToken() throws Exception {
            mockMvc.perform(get(DETALLE, UUID.randomUUID())).andExpect(status().isUnauthorized());

            verifyNoInteractions(neumaticoService);
        }

    }

}
