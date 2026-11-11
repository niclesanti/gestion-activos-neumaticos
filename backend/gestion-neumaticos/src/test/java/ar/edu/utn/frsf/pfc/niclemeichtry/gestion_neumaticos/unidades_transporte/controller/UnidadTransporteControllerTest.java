package ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.unidades_transporte.controller;

import static ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.TestDataFactory.PATENTE;
import static ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.TestDataFactory.UNIDAD_PUBLIC_ID;
import static ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.TestDataFactory.tractorComun;
import static ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.TestDataFactory.unidadTransporteRequest;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.endsWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.restdocs.headers.HeaderDocumentation.headerWithName;
import static org.springframework.restdocs.headers.HeaderDocumentation.responseHeaders;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessRequest;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessResponse;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.queryParameters;
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
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.restdocs.test.autoconfigure.AutoConfigureRestDocs;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.config.CorsConfig;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.config.PaginacionConfig;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.exception.ParametroInvalidoException;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.exception.EntidadDuplicadaException;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.exception.EntidadNoEncontradaException;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.security.NivelAccesoVigente;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.security.SecurityConfig;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.security.web.JsonSecurityErrorHandler;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.unidades_transporte.domain.dto.UnidadTransporteListadoDTO;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.unidades_transporte.domain.dto.UnidadTransporteRequestDTO;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.unidades_transporte.domain.dto.UnidadTransporteResponseDTO;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.unidades_transporte.domain.entity.UnidadTransporte;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.unidades_transporte.mapper.UnidadTransporteMapper;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.unidades_transporte.mapper.UnidadTransporteMapperImpl;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.unidades_transporte.service.UnidadTransporteFactoryImpl;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.unidades_transporte.service.UnidadTransporteService;
import tools.jackson.databind.json.JsonMapper;

@WebMvcTest(UnidadTransporteController.class)
@Import({ SecurityConfig.class, JsonSecurityErrorHandler.class, CorsConfig.class, PaginacionConfig.class })
@AutoConfigureRestDocs
class UnidadTransporteControllerTest {

    private static final String UNIDADES = "/api/v1/unidades-transporte";
    private static final String CONFIGURACIONES = UNIDADES + "/configuraciones";
    private static final UUID USUARIO = UUID.randomUUID();

    private final UnidadTransporteMapper mapper = new UnidadTransporteMapperImpl();

    @Autowired private MockMvc mockMvc;
    @Autowired private JsonMapper jsonMapper;
    @MockitoBean private UnidadTransporteService unidadTransporteService;
    @MockitoBean private JwtDecoder jwtDecoder;
    @MockitoBean private NivelAccesoVigente nivelAccesoVigente;

    private String json(Object body) {
        return jsonMapper.writeValueAsString(body);
    }

    private static RequestPostProcessor usuario(String nivel) {
        return jwt().jwt(j -> j.subject(USUARIO.toString())).authorities(new SimpleGrantedAuthority(nivel));
    }

    /** Respuesta del alta de un tractor común (7 slots). */
    private UnidadTransporteResponseDTO unidadRegistrada() {
        UnidadTransporte unidad = new UnidadTransporteFactoryImpl().crearUnidad(PATENTE, tractorComun());
        unidad.setPublicId(UNIDAD_PUBLIC_ID);
        return mapper.toResponseDTO(unidad);
    }

    @Nested
    @DisplayName("POST /api/v1/unidades-transporte")
    class Registrar {

        @Test
        @DisplayName("201 con la unidad y sus slots, sin ids internos; el usuario es el sub del token")
        void registraConEditor() throws Exception {
            UnidadTransporteRequestDTO request = unidadTransporteRequest();
            when(unidadTransporteService.registrarUnidadTransporte(request, USUARIO.toString()))
                    .thenReturn(unidadRegistrada());

            mockMvc.perform(post(UNIDADES).with(usuario("ROLE_EDITOR"))
                            .contentType(MediaType.APPLICATION_JSON).content(json(request)))
                    .andExpect(status().isCreated())
                    .andExpect(header().string(HttpHeaders.LOCATION,
                            endsWith("/api/v1/unidades-transporte/" + UNIDAD_PUBLIC_ID)))
                    .andExpect(jsonPath("$.publicId").value(UNIDAD_PUBLIC_ID.toString()))
                    .andExpect(jsonPath("$.patente").value(PATENTE))
                    .andExpect(jsonPath("$.configuracion.nombre").value("Tractor común"))
                    .andExpect(jsonPath("$.slots.length()").value(7))
                    .andExpect(jsonPath("$.slots[2].codigo").value("E2-IZQ-EXT"))
                    .andExpect(jsonPath("$.slots[6].codigo").value("AUX-1"))
                    .andExpect(jsonPath("$.id").doesNotExist())
                    .andExpect(jsonPath("$.slots[0].id").doesNotExist())
                    .andExpect(jsonPath("$.slots[0].neumaticoActualId").doesNotExist())
                    .andDo(document("unidad-transporte-registrar",
                            preprocessRequest(prettyPrint()),
                            preprocessResponse(prettyPrint()),
                            responseHeaders(
                                    headerWithName(HttpHeaders.LOCATION).description("URL de la unidad creada")),
                            requestFields(
                                    fieldWithPath("patente").description("Patente: formato ABC123 o AB123CD (se "
                                            + "aceptan espacios, guiones y minúsculas; única)"),
                                    fieldWithPath("configuracionId").description("Id de la configuración de ejes "
                                            + "(GET /api/v1/unidades-transporte/configuraciones)")),
                            responseFields(
                                    fieldWithPath("publicId").description("Identificador público"),
                                    fieldWithPath("patente").description("Patente normalizada"),
                                    fieldWithPath("configuracion.id").description("Id de la configuración"),
                                    fieldWithPath("configuracion.nombre").description("Nombre de la configuración"),
                                    fieldWithPath("configuracion.tipoUnidad").description("TRACTOR o SEMIRREMOLQUE"),
                                    fieldWithPath("slots[].codigo").description("Código de la posición (E1-IZQ, "
                                            + "E2-DER-INT, AUX-1…)"),
                                    fieldWithPath("slots[].tipoSlot").description("EJE o AUXILIO"),
                                    fieldWithPath("slots[].nroEje").description("Número de eje (null en auxilio)")
                                            .optional(),
                                    fieldWithPath("slots[].tipoEje").description("DIRECCION, TRACCION o ARRASTRE "
                                            + "(null en auxilio)").optional(),
                                    fieldWithPath("slots[].lado").description("IZQUIERDO o DERECHO (null en "
                                            + "auxilio)").optional(),
                                    fieldWithPath("slots[].posicionRueda").description("SIMPLE, EXTERIOR o "
                                            + "INTERIOR (null en auxilio)").optional(),
                                    fieldWithPath("slots[].nroAuxilio").description("Número de auxilio (null en "
                                            + "slot de eje)").optional(),
                                    fieldWithPath("slots[].ocupado").description("Si tiene un neumático montado "
                                            + "(siempre false en el alta)"))));
        }

        @Test
        @DisplayName("el administrador también puede registrar")
        void registraConAdministrador() throws Exception {
            when(unidadTransporteService.registrarUnidadTransporte(eq(unidadTransporteRequest()), anyString()))
                    .thenReturn(unidadRegistrada());

            mockMvc.perform(post(UNIDADES).with(usuario("ROLE_ADMINISTRADOR"))
                            .contentType(MediaType.APPLICATION_JSON).content(json(unidadTransporteRequest())))
                    .andExpect(status().isCreated());
        }

        @Test
        @DisplayName("403 para el lector, sin llegar al servicio")
        void lectorNoRegistra() throws Exception {
            mockMvc.perform(post(UNIDADES).with(usuario("ROLE_LECTOR"))
                            .contentType(MediaType.APPLICATION_JSON).content(json(unidadTransporteRequest())))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.status").value(403));

            verifyNoInteractions(unidadTransporteService);
        }

        @Test
        @DisplayName("401 sin token")
        void sinToken() throws Exception {
            mockMvc.perform(post(UNIDADES).contentType(MediaType.APPLICATION_JSON)
                            .content(json(unidadTransporteRequest())))
                    .andExpect(status().isUnauthorized());

            verifyNoInteractions(unidadTransporteService);
        }

        @Test
        @DisplayName("400 sin patente ni configuración")
        void sinDatos() throws Exception {
            mockMvc.perform(post(UNIDADES).with(usuario("ROLE_EDITOR"))
                            .contentType(MediaType.APPLICATION_JSON).content("{}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value(allOf(
                            containsString("patente: es obligatoria"),
                            containsString("configuracionId: es obligatoria"))));

            verifyNoInteractions(unidadTransporteService);
        }

        @Test
        @DisplayName("400 con una patente de otro formato")
        void patenteInvalida() throws Exception {
            mockMvc.perform(post(UNIDADES).with(usuario("ROLE_EDITOR"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(unidadTransporteRequest("A1B2C3", 1L))))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value(
                            containsString("la patente debe tener el formato ABC123 o AB123CD")));

            verifyNoInteractions(unidadTransporteService);
        }

        @Test
        @DisplayName("404 si la configuración no existe")
        void configuracionInexistente() throws Exception {
            when(unidadTransporteService.registrarUnidadTransporte(any(), anyString()))
                    .thenThrow(new EntidadNoEncontradaException("La configuración de unidad indicada no existe"));

            mockMvc.perform(post(UNIDADES).with(usuario("ROLE_EDITOR"))
                            .contentType(MediaType.APPLICATION_JSON).content(json(unidadTransporteRequest())))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value("La configuración de unidad indicada no existe"));
        }

        @Test
        @DisplayName("409 si la patente ya existe")
        void duplicada() throws Exception {
            when(unidadTransporteService.registrarUnidadTransporte(any(), anyString()))
                    .thenThrow(new EntidadDuplicadaException(
                            "Ya existe una unidad de transporte con la patente AB123CD"));

            mockMvc.perform(post(UNIDADES).with(usuario("ROLE_EDITOR"))
                            .contentType(MediaType.APPLICATION_JSON).content(json(unidadTransporteRequest())))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.message").value(
                            "Ya existe una unidad de transporte con la patente AB123CD"));
        }

    }

    @Nested
    @DisplayName("GET /api/v1/unidades-transporte")
    class Buscar {

        private Page<UnidadTransporteListadoDTO> paginaConUnaUnidad() {
            UnidadTransporte unidad = new UnidadTransporteFactoryImpl().crearUnidad(PATENTE, tractorComun());
            unidad.setPublicId(UNIDAD_PUBLIC_ID);
            return new PageImpl<>(List.of(mapper.toListadoDTO(unidad)), PageRequest.of(0, 10), 1);
        }

        @Test
        @DisplayName("200 con el listado resumido, sin slots ni ids internos")
        void buscaConLector() throws Exception {
            when(unidadTransporteService.buscar(eq("b12"), any(Pageable.class))).thenReturn(paginaConUnaUnidad());

            mockMvc.perform(get(UNIDADES).with(usuario("ROLE_LECTOR"))
                            .param("patente", "b12").param("page", "0").param("size", "5").param("sort", "patente,asc"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].publicId").value(UNIDAD_PUBLIC_ID.toString()))
                    .andExpect(jsonPath("$.content[0].patente").value(PATENTE))
                    .andExpect(jsonPath("$.content[0].configuracion.nombre").value("Tractor común"))
                    .andExpect(jsonPath("$.content[0].configuracion.tipoUnidad").value("TRACTOR"))
                    .andExpect(jsonPath("$.content[0].slots").doesNotExist())
                    .andExpect(jsonPath("$.content[0].configuracion.id").doesNotExist())
                    .andExpect(jsonPath("$.page.totalElements").value(1))
                    .andDo(document("unidad-transporte-buscar",
                            preprocessRequest(prettyPrint()),
                            preprocessResponse(prettyPrint()),
                            queryParameters(
                                    parameterWithName("patente").description("Patente completa o parcial (opcional)"),
                                    parameterWithName("page").description("Página, desde 0"),
                                    parameterWithName("size").description("Tamaño de página (10 por defecto, máximo 20)"),
                                    parameterWithName("sort").description("Solo `patente,asc|desc`")),
                            responseFields(
                                    fieldWithPath("content[].publicId").description("Identificador público"),
                                    fieldWithPath("content[].patente").description("Patente normalizada"),
                                    fieldWithPath("content[].configuracion.nombre")
                                            .description("Nombre de la configuración"),
                                    fieldWithPath("content[].configuracion.tipoUnidad")
                                            .description("TRACTOR o SEMIRREMOLQUE"),
                                    fieldWithPath("page.size").description("Tamaño de página"),
                                    fieldWithPath("page.number").description("Número de página, desde 0"),
                                    fieldWithPath("page.totalElements").description("Total de unidades"),
                                    fieldWithPath("page.totalPages").description("Total de páginas"))));

            ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
            verify(unidadTransporteService).buscar(eq("b12"), pageable.capture());
            assertThat(pageable.getValue().getPageNumber()).isZero();
            assertThat(pageable.getValue().getPageSize()).isEqualTo(5);
        }

        @ParameterizedTest
        @ValueSource(strings = { "ROLE_EDITOR", "ROLE_ADMINISTRADOR" })
        @DisplayName("200 también para editor y administrador")
        void buscaConEditorYAdministrador(String nivel) throws Exception {
            when(unidadTransporteService.buscar(any(), any(Pageable.class))).thenReturn(paginaConUnaUnidad());

            mockMvc.perform(get(UNIDADES).with(usuario(nivel))).andExpect(status().isOk());
        }

        @Test
        @DisplayName("sin parámetros: sin patente, 10 por página, ordenadas por patente ascendente")
        void sinPatente_usaOrdenPorPatenteYTamanioPorDefecto() throws Exception {
            when(unidadTransporteService.buscar(any(), any(Pageable.class))).thenReturn(paginaConUnaUnidad());

            mockMvc.perform(get(UNIDADES).with(usuario("ROLE_LECTOR"))).andExpect(status().isOk());

            ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
            verify(unidadTransporteService).buscar(isNull(), pageable.capture());
            assertThat(pageable.getValue().getPageNumber()).isZero();
            assertThat(pageable.getValue().getPageSize()).isEqualTo(PaginacionConfig.TAMANIO_PAGINA_POR_DEFECTO);
            assertThat(pageable.getValue().getSort()).isEqualTo(Sort.by(Sort.Order.asc("patente")));
        }

        @Test
        @DisplayName("200 con página vacía si no hay coincidencias")
        void sinResultados_devuelvePaginaVacia() throws Exception {
            when(unidadTransporteService.buscar(any(), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 10), 0));

            mockMvc.perform(get(UNIDADES).with(usuario("ROLE_LECTOR")).param("patente", "QQQ"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isEmpty())
                    .andExpect(jsonPath("$.page.totalElements").value(0));
        }

        @Test
        @DisplayName("un tamaño mayor al máximo se acota a 20")
        void tamanioMayorAlMaximo_seAcota() throws Exception {
            when(unidadTransporteService.buscar(any(), any(Pageable.class))).thenReturn(paginaConUnaUnidad());

            mockMvc.perform(get(UNIDADES).with(usuario("ROLE_LECTOR")).param("size", "500"))
                    .andExpect(status().isOk());

            ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
            verify(unidadTransporteService).buscar(any(), pageable.capture());
            assertThat(pageable.getValue().getPageSize()).isEqualTo(PaginacionConfig.TAMANIO_PAGINA_MAXIMO);
        }

        @Test
        @DisplayName("400 si se ordena por un campo no permitido")
        void ordenNoPermitido() throws Exception {
            when(unidadTransporteService.buscar(any(), any(Pageable.class)))
                    .thenThrow(new ParametroInvalidoException("Solo se puede ordenar por patente"));

            mockMvc.perform(get(UNIDADES).with(usuario("ROLE_LECTOR")).param("sort", "id,asc"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("Solo se puede ordenar por patente"));
        }

        @Test
        @DisplayName("401 sin token")
        void sinToken() throws Exception {
            mockMvc.perform(get(UNIDADES))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.status").value(401));

            verifyNoInteractions(unidadTransporteService);
        }
    }

    @Nested
    @DisplayName("GET /api/v1/unidades-transporte/configuraciones")
    class Configuraciones {

        @Test
        @DisplayName("lista las configuraciones con sus ejes y totales")
        void lista() throws Exception {
            when(unidadTransporteService.listarConfiguraciones())
                    .thenReturn(List.of(mapper.toConfiguracionDTO(tractorComun())));

            mockMvc.perform(get(CONFIGURACIONES).with(usuario("ROLE_LECTOR")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].nombre").value("Tractor común"))
                    .andExpect(jsonPath("$[0].totalSlots").value(7))
                    .andExpect(jsonPath("$[0].ejes[1].rodado").value("DUAL"))
                    .andDo(document("unidad-transporte-configuraciones",
                            preprocessResponse(prettyPrint()),
                            responseFields(
                                    fieldWithPath("[].id").description("Id de la configuración"),
                                    fieldWithPath("[].nombre").description("Nombre"),
                                    fieldWithPath("[].tipoUnidad").description("TRACTOR o SEMIRREMOLQUE"),
                                    fieldWithPath("[].cantSlotsAuxilio").description("Cantidad de auxilios"),
                                    fieldWithPath("[].totalEjes").description("Cantidad de ejes"),
                                    fieldWithPath("[].totalSlots").description("Slots que genera el alta"),
                                    fieldWithPath("[].ejes[].orden").description("Número de eje, de adelante "
                                            + "hacia atrás"),
                                    fieldWithPath("[].ejes[].tipoEje").description("DIRECCION, TRACCION o "
                                            + "ARRASTRE"),
                                    fieldWithPath("[].ejes[].rodado").description("SIMPLE o DUAL"),
                                    fieldWithPath("[].ejes[].cantidadNeumaticos").description("2 (simple) o 4 "
                                            + "(dual)"))));
        }

        @Test
        @DisplayName("401 sin token")
        void sinToken() throws Exception {
            mockMvc.perform(get(CONFIGURACIONES)).andExpect(status().isUnauthorized());

            verifyNoInteractions(unidadTransporteService);
        }

    }

}
