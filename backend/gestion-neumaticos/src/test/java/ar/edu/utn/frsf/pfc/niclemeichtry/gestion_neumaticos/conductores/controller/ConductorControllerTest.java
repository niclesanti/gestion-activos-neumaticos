package ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.conductores.controller;

import static ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.TestDataFactory.conductor;
import static ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.TestDataFactory.conductorListado;
import static ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.TestDataFactory.conductorRequest;
import static ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.TestDataFactory.conductorResponse;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.endsWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.queryParameters;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
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

import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.conductores.domain.dto.ConductorListadoDTO;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.conductores.domain.dto.ConductorRequestDTO;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.conductores.domain.entity.Conductor;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.conductores.service.ConductorService;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.config.CorsConfig;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.config.PaginacionConfig;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.exception.EntidadDuplicadaException;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.security.NivelAccesoVigente;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.security.SecurityConfig;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.security.web.JsonSecurityErrorHandler;
import tools.jackson.databind.json.JsonMapper;

@WebMvcTest(ConductorController.class)
@Import({ SecurityConfig.class, JsonSecurityErrorHandler.class, CorsConfig.class, PaginacionConfig.class })
@AutoConfigureRestDocs
class ConductorControllerTest {

    private static final String CONDUCTORES = "/api/v1/conductores";
    private static final UUID USUARIO = UUID.randomUUID();

    @Autowired private MockMvc mockMvc;
    @Autowired private JsonMapper jsonMapper;
    @MockitoBean private ConductorService conductorService;
    @MockitoBean private JwtDecoder jwtDecoder;
    @MockitoBean private NivelAccesoVigente nivelAccesoVigente;

    private String json(Object body) {
        return jsonMapper.writeValueAsString(body);
    }

    private static RequestPostProcessor usuario(String nivel) {
        return jwt().jwt(j -> j.subject(USUARIO.toString())).authorities(new SimpleGrantedAuthority(nivel));
    }

    @Nested
    @DisplayName("POST /api/v1/conductores")
    class Registrar {

        @Test
        @DisplayName("201 con el conductor registrado, sin id interno; el usuario es el sub del token")
        void registraConEditor() throws Exception {
            ConductorRequestDTO request = conductorRequest();
            Conductor conductor = conductor(request);
            when(conductorService.registrarConductor(request, USUARIO.toString()))
                    .thenReturn(conductorResponse(conductor));

            mockMvc.perform(post(CONDUCTORES).with(usuario("ROLE_EDITOR"))
                            .contentType(MediaType.APPLICATION_JSON).content(json(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.publicId").value(conductor.getPublicId().toString()))
                    .andExpect(header().string(HttpHeaders.LOCATION,
                            endsWith("/api/v1/conductores/" + conductor.getPublicId())))
                    .andExpect(jsonPath("$.dni").value("30123456"))
                    .andExpect(jsonPath("$.nombre").value("Juan"))
                    .andExpect(jsonPath("$.apellido").value("Pérez"))
                    .andExpect(jsonPath("$.id").doesNotExist())
                    .andDo(document("conductor-registrar",
                            preprocessRequest(prettyPrint()),
                            preprocessResponse(prettyPrint()),
                            responseHeaders(
                                    headerWithName(HttpHeaders.LOCATION).description("URL del conductor creado")),
                            requestFields(
                                    fieldWithPath("dni").description("DNI: 7 u 8 dígitos sin puntos (opcional, "
                                            + "único)").optional(),
                                    fieldWithPath("nombre").description("Nombre (hasta 100 caracteres)"),
                                    fieldWithPath("apellido").description("Apellido (hasta 100 caracteres)")),
                            responseFields(
                                    fieldWithPath("publicId").description("Identificador público"),
                                    fieldWithPath("dni").description("DNI normalizado").optional(),
                                    fieldWithPath("nombre").description("Nombre"),
                                    fieldWithPath("apellido").description("Apellido"))));
        }

        @Test
        @DisplayName("el administrador también puede registrar")
        void registraConAdministrador() throws Exception {
            ConductorRequestDTO request = conductorRequest();
            when(conductorService.registrarConductor(eq(request), anyString()))
                    .thenReturn(conductorResponse(conductor(request)));

            mockMvc.perform(post(CONDUCTORES).with(usuario("ROLE_ADMINISTRADOR"))
                            .contentType(MediaType.APPLICATION_JSON).content(json(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").doesNotExist());
        }

        @Test
        @DisplayName("403 para el lector, sin llegar al servicio")
        void lectorNoRegistra() throws Exception {
            mockMvc.perform(post(CONDUCTORES).with(usuario("ROLE_LECTOR"))
                            .contentType(MediaType.APPLICATION_JSON).content(json(conductorRequest())))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.status").value(403));

            verifyNoInteractions(conductorService);
        }

        @Test
        @DisplayName("401 sin token")
        void sinToken() throws Exception {
            mockMvc.perform(post(CONDUCTORES).contentType(MediaType.APPLICATION_JSON).content(json(conductorRequest())))
                    .andExpect(status().isUnauthorized());

            verifyNoInteractions(conductorService);
        }

        @Test
        @DisplayName("400 con cada campo obligatorio faltante")
        void camposFaltantes() throws Exception {
            mockMvc.perform(post(CONDUCTORES).with(usuario("ROLE_EDITOR"))
                            .contentType(MediaType.APPLICATION_JSON).content("{}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value(allOf(
                            containsString("nombre: es obligatorio"), containsString("apellido: es obligatorio"))));

            verifyNoInteractions(conductorService);
        }

        @Test
        @DisplayName("400 si el DNI no tiene 7 u 8 dígitos")
        void dniInvalido() throws Exception {
            mockMvc.perform(post(CONDUCTORES).with(usuario("ROLE_EDITOR"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(conductorRequest("30.123.456", "Juan", "Pérez"))))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value(containsString("dni:")));

            verifyNoInteractions(conductorService);
        }

        @Test
        @DisplayName("409 si el DNI ya existe")
        void duplicado() throws Exception {
            when(conductorService.registrarConductor(any(), anyString()))
                    .thenThrow(new EntidadDuplicadaException("Ya existe un conductor con el DNI 30123456"));

            mockMvc.perform(post(CONDUCTORES).with(usuario("ROLE_EDITOR"))
                            .contentType(MediaType.APPLICATION_JSON).content(json(conductorRequest())))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.message").value("Ya existe un conductor con el DNI 30123456"));
        }

    }

    @Nested
    @DisplayName("GET /api/v1/conductores")
    class Buscar {

        private final ConductorListadoDTO fila = conductorListado(conductor(conductorRequest()));

        @Test
        @DisplayName("200 con la página de conductores activos y el texto buscado")
        void buscaConLector() throws Exception {
            when(conductorService.buscar(eq("pér"), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of(fila), PageRequest.of(0, 5), 1));

            mockMvc.perform(get(CONDUCTORES).with(usuario("ROLE_LECTOR"))
                            .param("q", "pér").param("page", "0").param("size", "5"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].publicId").value(fila.publicId().toString()))
                    .andExpect(jsonPath("$.content[0].apellido").value("Pérez"))
                    .andExpect(jsonPath("$.content[0].id").doesNotExist())
                    .andExpect(jsonPath("$.page.totalElements").value(1))
                    .andDo(document("conductores-buscar",
                            preprocessRequest(prettyPrint()),
                            preprocessResponse(prettyPrint()),
                            queryParameters(
                                    parameterWithName("q").description("Apellido, nombre o DNI, completo o parcial "
                                            + "(opcional)"),
                                    parameterWithName("page").description("Página, desde 0"),
                                    parameterWithName("size").description("Tamaño de página (10 por defecto, máximo 20)")),
                            responseFields(
                                    fieldWithPath("content[].publicId").description("Identificador público"),
                                    fieldWithPath("content[].nombre").description("Nombre"),
                                    fieldWithPath("content[].apellido").description("Apellido"),
                                    fieldWithPath("content[].dni").description("DNI (puede ser null)").optional(),
                                    fieldWithPath("page.size").description("Tamaño de página"),
                                    fieldWithPath("page.number").description("Número de página, desde 0"),
                                    fieldWithPath("page.totalElements").description("Total de conductores"),
                                    fieldWithPath("page.totalPages").description("Total de páginas"))));

            ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
            verify(conductorService).buscar(eq("pér"), pageable.capture());
            assertThat(pageable.getValue().getPageSize()).isEqualTo(5);
        }

        @ParameterizedTest
        @ValueSource(strings = { "ROLE_EDITOR", "ROLE_ADMINISTRADOR" })
        @DisplayName("200 también para editor y administrador, 10 por página por defecto")
        void buscaConEditorYAdministrador(String nivel) throws Exception {
            when(conductorService.buscar(any(), any(Pageable.class))).thenReturn(new PageImpl<>(List.of(fila)));

            mockMvc.perform(get(CONDUCTORES).with(usuario(nivel))).andExpect(status().isOk());

            ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
            verify(conductorService).buscar(isNull(), pageable.capture());
            assertThat(pageable.getValue().getPageSize()).isEqualTo(PaginacionConfig.TAMANIO_PAGINA_POR_DEFECTO);
        }

        @Test
        @DisplayName("401 sin token")
        void sinToken() throws Exception {
            mockMvc.perform(get(CONDUCTORES)).andExpect(status().isUnauthorized());

            verifyNoInteractions(conductorService);
        }

    }

}
