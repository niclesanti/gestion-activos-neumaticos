package ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.conductores.controller;

import static ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.TestDataFactory.conductor;
import static ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.TestDataFactory.conductorRequest;
import static ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.TestDataFactory.conductorResponse;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
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
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
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

import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.conductores.domain.dto.ConductorRequestDTO;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.conductores.domain.entity.Conductor;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.conductores.service.ConductorService;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.config.CorsConfig;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.exception.EntidadDuplicadaException;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.security.NivelAccesoVigente;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.security.SecurityConfig;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.security.web.JsonSecurityErrorHandler;
import tools.jackson.databind.json.JsonMapper;

@WebMvcTest(ConductorController.class)
@Import({ SecurityConfig.class, JsonSecurityErrorHandler.class, CorsConfig.class })
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
                            "http://localhost/api/v1/conductores/" + conductor.getPublicId()))
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

}
