package ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.neumaticos.controller;

import static ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.TestDataFactory.neumatico;
import static ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.TestDataFactory.neumaticoNuevoRequest;
import static ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.TestDataFactory.neumaticoRequest;
import static ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.TestDataFactory.neumaticoResponse;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.restdocs.test.autoconfigure.AutoConfigureRestDocs;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.config.CorsConfig;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.exception.EntidadDuplicadaException;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.exception.EntidadNoEncontradaException;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.neumaticos.domain.dto.MarcaNeumaticoResponseDTO;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.neumaticos.domain.dto.NeumaticoRequestDTO;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.neumaticos.domain.entity.EstadoNeumatico;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.neumaticos.domain.entity.Neumatico;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.neumaticos.service.NeumaticoService;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.security.NivelAccesoVigente;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.security.SecurityConfig;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.security.web.JsonSecurityErrorHandler;
import tools.jackson.databind.json.JsonMapper;

@WebMvcTest(NeumaticoController.class)
@Import({ SecurityConfig.class, JsonSecurityErrorHandler.class, CorsConfig.class })
@AutoConfigureRestDocs
class NeumaticoControllerTest {

    private static final String NEUMATICO = "/api/neumatico";
    private static final String MARCAS = "/api/neumatico/marcas";
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
    @DisplayName("POST /api/neumatico")
    class Registrar {

        @Test
        @DisplayName("201 con el neumático registrado; el usuario es el sub del token")
        void registraConEditor() throws Exception {
            NeumaticoRequestDTO request = neumaticoNuevoRequest();
            Neumatico neumatico = neumatico(request);
            when(neumaticoService.registrarNeumatico(request, USUARIO.toString()))
                    .thenReturn(neumaticoResponse(neumatico));

            mockMvc.perform(post(NEUMATICO).with(usuario("ROLE_EDITOR"))
                            .contentType(MediaType.APPLICATION_JSON).content(json(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.publicId").value(neumatico.getPublicId().toString()))
                    .andExpect(jsonPath("$.numeroSerie").value("ABC-123"))
                    .andExpect(jsonPath("$.situacionOperativaVigente").value("EN_ESPERA"))
                    .andExpect(jsonPath("$.enEspera").value(true))
                    .andExpect(jsonPath("$.marcaNombre").value("Fate"))
                    .andExpect(jsonPath("$.id").doesNotExist())
                    .andDo(document("neumatico-registrar",
                            preprocessRequest(prettyPrint()),
                            preprocessResponse(prettyPrint()),
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

            mockMvc.perform(post(NEUMATICO).with(usuario("ROLE_ADMINISTRADOR"))
                            .contentType(MediaType.APPLICATION_JSON).content(json(request)))
                    .andExpect(status().isCreated());
        }

        @Test
        @DisplayName("403 para el lector, sin llegar al servicio")
        void lectorNoRegistra() throws Exception {
            mockMvc.perform(post(NEUMATICO).with(usuario("ROLE_LECTOR"))
                            .contentType(MediaType.APPLICATION_JSON).content(json(neumaticoNuevoRequest())))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.status").value(403));

            verifyNoInteractions(neumaticoService);
        }

        @Test
        @DisplayName("401 sin token")
        void sinToken() throws Exception {
            mockMvc.perform(post(NEUMATICO).contentType(MediaType.APPLICATION_JSON).content(json(neumaticoNuevoRequest())))
                    .andExpect(status().isUnauthorized());

            verifyNoInteractions(neumaticoService);
        }

        @Test
        @DisplayName("400 con el campo y el motivo si viola una regla del alta")
        void reglaDeNegocio() throws Exception {
            NeumaticoRequestDTO request = neumaticoRequest(null, EstadoNeumatico.NUEVO, 1000f, 0f, 0);

            mockMvc.perform(post(NEUMATICO).with(usuario("ROLE_EDITOR"))
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

            mockMvc.perform(post(NEUMATICO).with(usuario("ROLE_EDITOR"))
                            .contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("409 si el número de serie ya existe")
        void duplicado() throws Exception {
            when(neumaticoService.registrarNeumatico(any(), anyString()))
                    .thenThrow(new EntidadDuplicadaException("Ya existe un neumático con el número de serie ABC-123"));

            mockMvc.perform(post(NEUMATICO).with(usuario("ROLE_EDITOR"))
                            .contentType(MediaType.APPLICATION_JSON).content(json(neumaticoNuevoRequest())))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.message").value("Ya existe un neumático con el número de serie ABC-123"));
        }

        @Test
        @DisplayName("404 si la marca no existe")
        void marcaInexistente() throws Exception {
            when(neumaticoService.registrarNeumatico(any(), anyString()))
                    .thenThrow(new EntidadNoEncontradaException("La marca de neumático indicada no existe"));

            mockMvc.perform(post(NEUMATICO).with(usuario("ROLE_EDITOR"))
                            .contentType(MediaType.APPLICATION_JSON).content(json(neumaticoNuevoRequest())))
                    .andExpect(status().isNotFound());
        }

    }

    @Nested
    @DisplayName("GET /api/neumatico/marcas")
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

}
