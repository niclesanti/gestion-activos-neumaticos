package ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.almacenamiento.controller;

import static ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.TestDataFactory.CENTRO_ALMACENAMIENTO_PUBLIC_ID;
import static ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.TestDataFactory.centroAlmacenamiento;
import static ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.TestDataFactory.centroAlmacenamientoRequest;
import static ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.TestDataFactory.centroAlmacenamientoResponse;
import static ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.TestDataFactory.domicilio;
import static ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.TestDataFactory.domicilioResponse;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.endsWith;
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

import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.almacenamiento.domain.dto.CentroAlmacenamientoRequestDTO;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.almacenamiento.domain.entity.CentroAlmacenamiento;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.almacenamiento.service.CentroAlmacenamientoService;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.config.CorsConfig;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.exception.EntidadNoEncontradaException;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.security.NivelAccesoVigente;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.security.SecurityConfig;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.security.web.JsonSecurityErrorHandler;
import tools.jackson.databind.json.JsonMapper;

@WebMvcTest(CentroAlmacenamientoController.class)
@Import({ SecurityConfig.class, JsonSecurityErrorHandler.class, CorsConfig.class })
@AutoConfigureRestDocs
class CentroAlmacenamientoControllerTest {

    private static final String CENTROS = "/api/v1/centros-almacenamiento";
    private static final UUID USUARIO = UUID.randomUUID();

    @Autowired private MockMvc mockMvc;
    @Autowired private JsonMapper jsonMapper;
    @MockitoBean private CentroAlmacenamientoService centroAlmacenamientoService;
    @MockitoBean private JwtDecoder jwtDecoder;
    @MockitoBean private NivelAccesoVigente nivelAccesoVigente;

    private String json(Object body) {
        return jsonMapper.writeValueAsString(body);
    }

    private static RequestPostProcessor usuario(String nivel) {
        return jwt().jwt(j -> j.subject(USUARIO.toString())).authorities(new SimpleGrantedAuthority(nivel));
    }

    @Test
    @DisplayName("201 con el centro y su domicilio, sin stock ni ids internos; el usuario es el sub del token")
    void registraConEditor() throws Exception {
        CentroAlmacenamientoRequestDTO request = centroAlmacenamientoRequest();
        CentroAlmacenamiento centro = centroAlmacenamiento(request);
        when(centroAlmacenamientoService.registrarCentroAlmacenamiento(request, USUARIO.toString()))
                .thenReturn(centroAlmacenamientoResponse(centro, domicilioResponse(domicilio(request.domicilio()))));

        mockMvc.perform(post(CENTROS).with(usuario("ROLE_EDITOR"))
                        .contentType(MediaType.APPLICATION_JSON).content(json(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.publicId").value(CENTRO_ALMACENAMIENTO_PUBLIC_ID.toString()))
                .andExpect(header().string(HttpHeaders.LOCATION,
                        endsWith("/api/v1/centros-almacenamiento/" + CENTRO_ALMACENAMIENTO_PUBLIC_ID)))
                .andExpect(jsonPath("$.nombre").value("Depósito Central"))
                .andExpect(jsonPath("$.cantidadNeumaticosEnStock").value(0))
                .andExpect(jsonPath("$.domicilio.localidad").value("Santa Fe"))
                .andExpect(jsonPath("$.domicilio.provincia").value("Santa Fe"))
                .andExpect(jsonPath("$.id").doesNotExist())
                .andExpect(jsonPath("$.domicilioId").doesNotExist())
                .andExpect(jsonPath("$.domicilio.id").doesNotExist())
                .andDo(document("centro-almacenamiento-registrar",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        responseHeaders(
                                headerWithName(HttpHeaders.LOCATION).description("URL del centro creado")),
                        requestFields(
                                fieldWithPath("nombre").description("Nombre (hasta 150 caracteres)"),
                                fieldWithPath("domicilio").description("Domicilio (obligatorio)"),
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
                                fieldWithPath("cantidadNeumaticosEnStock")
                                        .description("Neumáticos guardados hoy en el centro (0 al registrarlo)"),
                                fieldWithPath("domicilio").description("Domicilio"),
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
        CentroAlmacenamientoRequestDTO request = centroAlmacenamientoRequest();
        when(centroAlmacenamientoService.registrarCentroAlmacenamiento(eq(request), anyString()))
                .thenReturn(centroAlmacenamientoResponse(centroAlmacenamiento(request),
                        domicilioResponse(domicilio(request.domicilio()))));

        mockMvc.perform(post(CENTROS).with(usuario("ROLE_ADMINISTRADOR"))
                        .contentType(MediaType.APPLICATION_JSON).content(json(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.cantidadNeumaticosEnStock").value(0))
                .andExpect(jsonPath("$.id").doesNotExist());
    }

    @Test
    @DisplayName("una cantidad de stock enviada en el cuerpo se ignora: no forma parte del alta")
    void ignoraStockDelRequest() throws Exception {
        CentroAlmacenamientoRequestDTO request = centroAlmacenamientoRequest();
        when(centroAlmacenamientoService.registrarCentroAlmacenamiento(request, USUARIO.toString()))
                .thenReturn(centroAlmacenamientoResponse(centroAlmacenamiento(request),
                        domicilioResponse(domicilio(request.domicilio()))));
        String conStock = json(request).replaceFirst("\\{", "{\"cantidadNeumaticosEnStock\":50,");

        mockMvc.perform(post(CENTROS).with(usuario("ROLE_EDITOR"))
                        .contentType(MediaType.APPLICATION_JSON).content(conStock))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.cantidadNeumaticosEnStock").value(0));
    }

    @Test
    @DisplayName("403 para el lector, sin llegar al servicio")
    void lectorNoRegistra() throws Exception {
        mockMvc.perform(post(CENTROS).with(usuario("ROLE_LECTOR"))
                        .contentType(MediaType.APPLICATION_JSON).content(json(centroAlmacenamientoRequest())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));

        verifyNoInteractions(centroAlmacenamientoService);
    }

    @Test
    @DisplayName("401 sin token")
    void sinToken() throws Exception {
        mockMvc.perform(post(CENTROS).contentType(MediaType.APPLICATION_JSON)
                        .content(json(centroAlmacenamientoRequest())))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(centroAlmacenamientoService);
    }

    @Test
    @DisplayName("400 sin nombre ni domicilio")
    void sinNombreNiDomicilio() throws Exception {
        mockMvc.perform(post(CENTROS).with(usuario("ROLE_EDITOR"))
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(allOf(
                        containsString("nombre: es obligatorio"),
                        containsString("domicilio: es obligatorio"))));

        verifyNoInteractions(centroAlmacenamientoService);
    }

    @Test
    @DisplayName("400 sin domicilio")
    void sinDomicilio() throws Exception {
        mockMvc.perform(post(CENTROS).with(usuario("ROLE_EDITOR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(centroAlmacenamientoRequest("Depósito Norte", null))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("domicilio: es obligatorio"));

        verifyNoInteractions(centroAlmacenamientoService);
    }

    @Test
    @DisplayName("400 con un domicilio incompleto (sin localidad, entre otros), validado en cascada")
    void domicilioIncompleto() throws Exception {
        mockMvc.perform(post(CENTROS).with(usuario("ROLE_EDITOR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Depósito Norte\",\"domicilio\":{\"calle\":\"Belgrano\"}}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(allOf(
                        containsString("domicilio.altura: es obligatoria"),
                        containsString("domicilio.provinciaId: es obligatoria"),
                        containsString("domicilio.localidad: es obligatoria"))));

        verifyNoInteractions(centroAlmacenamientoService);
    }

    @Test
    @DisplayName("404 si la provincia del domicilio no existe")
    void provinciaInexistente() throws Exception {
        when(centroAlmacenamientoService.registrarCentroAlmacenamiento(any(), anyString()))
                .thenThrow(new EntidadNoEncontradaException("La provincia indicada no existe"));

        mockMvc.perform(post(CENTROS).with(usuario("ROLE_EDITOR"))
                        .contentType(MediaType.APPLICATION_JSON).content(json(centroAlmacenamientoRequest())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("La provincia indicada no existe"));
    }

}
