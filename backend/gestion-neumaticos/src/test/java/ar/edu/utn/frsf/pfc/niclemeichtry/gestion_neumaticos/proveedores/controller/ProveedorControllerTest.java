package ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.proveedores.controller;

import static ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.TestDataFactory.PROVEEDOR_PUBLIC_ID;
import static ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.TestDataFactory.domicilio;
import static ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.TestDataFactory.domicilioResponse;
import static ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.TestDataFactory.proveedor;
import static ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.TestDataFactory.proveedorRequest;
import static ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.TestDataFactory.proveedorResponse;
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
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.exception.EntidadDuplicadaException;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.exception.EntidadNoEncontradaException;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.proveedores.domain.dto.ProveedorListadoDTO;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.proveedores.domain.dto.ProveedorRequestDTO;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.proveedores.domain.entity.ProveedorNeumaticos;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.proveedores.service.ProveedorService;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.security.NivelAccesoVigente;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.security.SecurityConfig;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.security.web.JsonSecurityErrorHandler;
import tools.jackson.databind.json.JsonMapper;

@WebMvcTest(ProveedorController.class)
@Import({ SecurityConfig.class, JsonSecurityErrorHandler.class, CorsConfig.class })
@AutoConfigureRestDocs
class ProveedorControllerTest {

    private static final String PROVEEDORES = "/api/v1/proveedores";
    private static final UUID USUARIO = UUID.randomUUID();

    @Autowired private MockMvc mockMvc;
    @Autowired private JsonMapper jsonMapper;
    @MockitoBean private ProveedorService proveedorService;
    @MockitoBean private JwtDecoder jwtDecoder;
    @MockitoBean private NivelAccesoVigente nivelAccesoVigente;

    private String json(Object body) {
        return jsonMapper.writeValueAsString(body);
    }

    private static RequestPostProcessor usuario(String nivel) {
        return jwt().jwt(j -> j.subject(USUARIO.toString())).authorities(new SimpleGrantedAuthority(nivel));
    }

    @Nested
    @DisplayName("POST /api/v1/proveedores")
    class Registrar {

        @Test
        @DisplayName("201 con el proveedor y su domicilio, sin ids internos; el usuario es el sub del token")
        void registraConEditor() throws Exception {
            ProveedorRequestDTO request = proveedorRequest();
            ProveedorNeumaticos proveedor = proveedor(request);
            when(proveedorService.registrarProveedor(request, USUARIO.toString()))
                    .thenReturn(proveedorResponse(proveedor, domicilioResponse(domicilio(request.domicilio()))));

            mockMvc.perform(post(PROVEEDORES).with(usuario("ROLE_EDITOR"))
                            .contentType(MediaType.APPLICATION_JSON).content(json(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.publicId").value(PROVEEDOR_PUBLIC_ID.toString()))
                    .andExpect(header().string(HttpHeaders.LOCATION,
                            "http://localhost/api/v1/proveedores/" + PROVEEDOR_PUBLIC_ID))
                    .andExpect(jsonPath("$.nombre").value("Neumáticos del Litoral S.A."))
                    .andExpect(jsonPath("$.domicilio.localidad").value("Santa Fe"))
                    .andExpect(jsonPath("$.domicilio.provincia").value("Santa Fe"))
                    .andExpect(jsonPath("$.id").doesNotExist())
                    .andExpect(jsonPath("$.domicilioId").doesNotExist())
                    .andExpect(jsonPath("$.domicilio.id").doesNotExist())
                    .andDo(document("proveedor-registrar",
                            preprocessRequest(prettyPrint()),
                            preprocessResponse(prettyPrint()),
                            responseHeaders(
                                    headerWithName(HttpHeaders.LOCATION).description("URL del proveedor creado")),
                            requestFields(
                                    fieldWithPath("nombre").description("Nombre (hasta 150 caracteres, único sin "
                                            + "distinguir mayúsculas)"),
                                    fieldWithPath("email").description("Email de contacto (opcional)").optional(),
                                    fieldWithPath("telefono").description("Teléfono de contacto: dígitos, espacios, "
                                            + "+, -, ( y ) (opcional)").optional(),
                                    fieldWithPath("domicilio").description("Domicilio (opcional)").optional(),
                                    fieldWithPath("domicilio.calle").description("Calle (hasta 100 caracteres)"),
                                    fieldWithPath("domicilio.altura").description("Altura (hasta 10 caracteres)"),
                                    fieldWithPath("domicilio.piso").description("Piso (opcional)").optional(),
                                    fieldWithPath("domicilio.departamento").description("Departamento (opcional)")
                                            .optional(),
                                    fieldWithPath("domicilio.provinciaId").description("Id de la provincia "
                                            + "(GET /api/v1/provincias)"),
                                    fieldWithPath("domicilio.localidad").description("Localidad: si no existe en la "
                                            + "provincia, se crea"),
                                    fieldWithPath("domicilio.codigoPostal").description("Código postal: 4 dígitos o "
                                            + "CPA (opcional)").optional()),
                            responseFields(
                                    fieldWithPath("publicId").description("Identificador público"),
                                    fieldWithPath("nombre").description("Nombre"),
                                    fieldWithPath("email").description("Email normalizado").optional(),
                                    fieldWithPath("telefono").description("Teléfono").optional(),
                                    fieldWithPath("domicilio").description("Domicilio, o null").optional(),
                                    fieldWithPath("domicilio.calle").description("Calle"),
                                    fieldWithPath("domicilio.altura").description("Altura"),
                                    fieldWithPath("domicilio.piso").description("Piso").optional(),
                                    fieldWithPath("domicilio.departamento").description("Departamento").optional(),
                                    fieldWithPath("domicilio.localidad").description("Localidad"),
                                    fieldWithPath("domicilio.codigoPostal").description("Código postal").optional(),
                                    fieldWithPath("domicilio.provinciaId").description("Id de la provincia"),
                                    fieldWithPath("domicilio.provincia").description("Nombre de la provincia"))));
        }

        @Test
        @DisplayName("el administrador también puede registrar")
        void registraConAdministrador() throws Exception {
            ProveedorRequestDTO request = proveedorRequest("Gomería Sur", null, null, null);
            when(proveedorService.registrarProveedor(eq(request), anyString()))
                    .thenReturn(proveedorResponse(proveedor(request), null));

            mockMvc.perform(post(PROVEEDORES).with(usuario("ROLE_ADMINISTRADOR"))
                            .contentType(MediaType.APPLICATION_JSON).content(json(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.domicilio").doesNotExist());
        }

        @Test
        @DisplayName("403 para el lector, sin llegar al servicio")
        void lectorNoRegistra() throws Exception {
            mockMvc.perform(post(PROVEEDORES).with(usuario("ROLE_LECTOR"))
                            .contentType(MediaType.APPLICATION_JSON).content(json(proveedorRequest())))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.status").value(403));

            verifyNoInteractions(proveedorService);
        }

        @Test
        @DisplayName("401 sin token")
        void sinToken() throws Exception {
            mockMvc.perform(post(PROVEEDORES).contentType(MediaType.APPLICATION_JSON).content(json(proveedorRequest())))
                    .andExpect(status().isUnauthorized());

            verifyNoInteractions(proveedorService);
        }

        @Test
        @DisplayName("400 sin nombre")
        void sinNombre() throws Exception {
            mockMvc.perform(post(PROVEEDORES).with(usuario("ROLE_EDITOR"))
                            .contentType(MediaType.APPLICATION_JSON).content("{}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value(containsString("nombre: es obligatorio")));

            verifyNoInteractions(proveedorService);
        }

        @Test
        @DisplayName("400 con un domicilio incompleto, validado en cascada")
        void domicilioIncompleto() throws Exception {
            mockMvc.perform(post(PROVEEDORES).with(usuario("ROLE_EDITOR"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"nombre\":\"Gomería Sur\",\"domicilio\":{\"calle\":\"Belgrano\"}}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value(allOf(
                            containsString("domicilio.altura: es obligatoria"),
                            containsString("domicilio.provinciaId: es obligatoria"),
                            containsString("domicilio.localidad: es obligatoria"))));

            verifyNoInteractions(proveedorService);
        }

        @Test
        @DisplayName("404 si la provincia del domicilio no existe")
        void provinciaInexistente() throws Exception {
            when(proveedorService.registrarProveedor(any(), anyString()))
                    .thenThrow(new EntidadNoEncontradaException("La provincia indicada no existe"));

            mockMvc.perform(post(PROVEEDORES).with(usuario("ROLE_EDITOR"))
                            .contentType(MediaType.APPLICATION_JSON).content(json(proveedorRequest())))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value("La provincia indicada no existe"));
        }

        @Test
        @DisplayName("409 si el nombre ya existe")
        void duplicado() throws Exception {
            when(proveedorService.registrarProveedor(any(), anyString()))
                    .thenThrow(new EntidadDuplicadaException("Ya existe un proveedor con el nombre Gomería Sur"));

            mockMvc.perform(post(PROVEEDORES).with(usuario("ROLE_EDITOR"))
                            .contentType(MediaType.APPLICATION_JSON).content(json(proveedorRequest())))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.message").value("Ya existe un proveedor con el nombre Gomería Sur"));
        }

    }

    @Nested
    @DisplayName("GET /api/v1/proveedores")
    class Listar {

        @Test
        @DisplayName("lista publicId y nombre, sin exponer el id interno")
        void lista() throws Exception {
            when(proveedorService.listarProveedores())
                    .thenReturn(List.of(new ProveedorListadoDTO(PROVEEDOR_PUBLIC_ID, "Neumáticos del Litoral S.A.")));

            mockMvc.perform(get(PROVEEDORES).with(usuario("ROLE_LECTOR")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].publicId").value(PROVEEDOR_PUBLIC_ID.toString()))
                    .andExpect(jsonPath("$[0].nombre").value("Neumáticos del Litoral S.A."))
                    .andExpect(jsonPath("$[0].id").doesNotExist());
        }

        @Test
        @DisplayName("401 sin token")
        void sinToken() throws Exception {
            mockMvc.perform(get(PROVEEDORES)).andExpect(status().isUnauthorized());
        }

    }

}
