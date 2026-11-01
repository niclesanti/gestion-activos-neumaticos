package ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.common.auditoria;

import static ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.TestDataFactory.AUDITOR_PUBLIC_ID;
import static ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.TestDataFactory.PROVEEDOR_PUBLIC_ID;
import static ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.TestDataFactory.paginaAuditoria;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessRequest;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessResponse;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.UUID;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.restdocs.test.autoconfigure.AutoConfigureRestDocs;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.conductores.controller.ConductorController;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.conductores.service.ConductorService;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.config.CorsConfig;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.config.PaginacionConfig;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.exception.ParametroInvalidoException;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.neumaticos.controller.NeumaticoController;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.neumaticos.service.NeumaticoService;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.proveedores.controller.ProveedorController;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.proveedores.service.ProveedorService;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.reparacion.controller.ServicioReparacionController;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.reparacion.service.ServicioReparacionService;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.security.NivelAccesoVigente;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.security.SecurityConfig;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.security.web.JsonSecurityErrorHandler;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.unidades_transporte.controller.UnidadTransporteController;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.unidades_transporte.service.UnidadTransporteService;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.usuarios.controller.UsuarioController;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.usuarios.service.UsuarioService;

/**
 * Los seis endpoints {@code GET /api/v1/<recurso>/auditoria} (HU-43) comparten
 * contrato: solo el administrador los consulta, son de solo lectura y paginan.
 */
@WebMvcTest({ ConductorController.class, NeumaticoController.class, ProveedorController.class,
        ServicioReparacionController.class, UnidadTransporteController.class, UsuarioController.class })
@Import({ SecurityConfig.class, JsonSecurityErrorHandler.class, CorsConfig.class, PaginacionConfig.class })
@AutoConfigureRestDocs
class AuditoriaControllersTest {

    private static final UUID USUARIO = UUID.randomUUID();

    @Autowired private MockMvc mockMvc;
    @MockitoBean private ConductorService conductorService;
    @MockitoBean private NeumaticoService neumaticoService;
    @MockitoBean private ProveedorService proveedorService;
    @MockitoBean private ServicioReparacionService servicioReparacionService;
    @MockitoBean private UnidadTransporteService unidadTransporteService;
    @MockitoBean private UsuarioService usuarioService;
    @MockitoBean private JwtDecoder jwtDecoder;
    @MockitoBean private NivelAccesoVigente nivelAccesoVigente;

    static Stream<Arguments> recursos() {
        return Stream.of(
                Arguments.of("conductores"),
                Arguments.of("neumaticos"),
                Arguments.of("proveedores"),
                Arguments.of("servicios-reparacion"),
                Arguments.of("unidades-transporte"),
                Arguments.of("usuarios"));
    }

    private static String ruta(String recurso) {
        return "/api/v1/" + recurso + "/auditoria";
    }

    private static RequestPostProcessor usuario(String nivel) {
        return jwt().jwt(j -> j.subject(USUARIO.toString())).authorities(new SimpleGrantedAuthority(nivel));
    }

    private void servicios_devuelven_una_pagina() {
        when(conductorService.listarAuditoria(any(), any())).thenReturn(paginaAuditoria());
        when(neumaticoService.listarAuditoria(any(), any())).thenReturn(paginaAuditoria());
        when(proveedorService.listarAuditoria(any(), any())).thenReturn(paginaAuditoria());
        when(servicioReparacionService.listarAuditoria(any(), any())).thenReturn(paginaAuditoria());
        when(unidadTransporteService.listarAuditoria(any(), any())).thenReturn(paginaAuditoria());
        when(usuarioService.listarAuditoria(any(), any())).thenReturn(paginaAuditoria());
    }

    private void verificarSinConsultas() {
        verifyNoInteractions(conductorService, neumaticoService, proveedorService, servicioReparacionService,
                unidadTransporteService, usuarioService);
    }

    @Nested
    @DisplayName("GET /api/v1/<recurso>/auditoria")
    class Consultar {

        @ParameterizedTest(name = "{0}")
        @MethodSource("ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.common.auditoria.AuditoriaControllersTest#recursos")
        @DisplayName("200 con la página de fichas para el administrador")
        void consultaConAdministrador(String recurso) throws Exception {
            servicios_devuelven_una_pagina();

            mockMvc.perform(get(ruta(recurso)).with(usuario("ROLE_ADMINISTRADOR")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].publicId").value(PROVEEDOR_PUBLIC_ID.toString()))
                    .andExpect(jsonPath("$.content[0].creadoPor.publicId").value(AUDITOR_PUBLIC_ID.toString()))
                    .andExpect(jsonPath("$.content[0].creadoPor.nombreUsuario").value("victoria.editor"))
                    .andExpect(jsonPath("$.content[0].dadoDeBajaPor").doesNotExist())
                    .andExpect(jsonPath("$.content[0].estado").value("ACTIVO"))
                    .andExpect(jsonPath("$.page.totalElements").value(1))
                    .andExpect(jsonPath("$.page.size").value(20))
                    .andDo(document(recurso + "-auditoria",
                            preprocessRequest(prettyPrint()),
                            preprocessResponse(prettyPrint())));
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.common.auditoria.AuditoriaControllersTest#recursos")
        @DisplayName("403 para el editor y el lector: no llega al servicio")
        void rechazaEditorYLector(String recurso) throws Exception {
            mockMvc.perform(get(ruta(recurso)).with(usuario("ROLE_EDITOR"))).andExpect(status().isForbidden());
            mockMvc.perform(get(ruta(recurso)).with(usuario("ROLE_LECTOR"))).andExpect(status().isForbidden());

            verificarSinConsultas();
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.common.auditoria.AuditoriaControllersTest#recursos")
        @DisplayName("401 sin token")
        void rechazaSinToken(String recurso) throws Exception {
            mockMvc.perform(get(ruta(recurso))).andExpect(status().isUnauthorized());

            verificarSinConsultas();
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.common.auditoria.AuditoriaControllersTest#recursos")
        @DisplayName("solo lectura: POST, PUT y DELETE sobre /auditoria no existen")
        void noPermiteEscrituras(String recurso) throws Exception {
            mockMvc.perform(put(ruta(recurso)).with(usuario("ROLE_ADMINISTRADOR")))
                    .andExpect(status().isMethodNotAllowed());
            mockMvc.perform(delete(ruta(recurso)).with(usuario("ROLE_ADMINISTRADOR")))
                    .andExpect(status().isMethodNotAllowed());

            verificarSinConsultas();
        }
    }

    @Nested
    @DisplayName("filtros, orden y paginación")
    class Parametros {

        @Test
        @DisplayName("convierte los query params en el filtro y el Pageable")
        void enlazaParametros() throws Exception {
            servicios_devuelven_una_pagina();

            mockMvc.perform(get(ruta("proveedores")).with(usuario("ROLE_ADMINISTRADOR"))
                            .param("q", "  litoral ")
                            .param("estado", "BAJAS")
                            .param("desde", "2026-01-01")
                            .param("hasta", "2026-03-31")
                            .param("page", "2")
                            .param("size", "50")
                            .param("sort", "createdAt,asc"))
                    .andExpect(status().isOk());

            ArgumentCaptor<FiltroAuditoria> filtro = ArgumentCaptor.forClass(FiltroAuditoria.class);
            ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
            verify(proveedorService).listarAuditoria(filtro.capture(), pageable.capture());
            assertThat(filtro.getValue()).isEqualTo(new FiltroAuditoria("litoral",
                    FiltroAuditoria.EstadoFiltro.BAJAS, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 3, 31)));
            assertThat(pageable.getValue().getPageNumber()).isEqualTo(2);
            assertThat(pageable.getValue().getPageSize()).isEqualTo(50);
            assertThat(pageable.getValue().getSort()).isEqualTo(Sort.by(Sort.Order.asc("createdAt")));
        }

        @Test
        @DisplayName("sin parámetros: todos los estados, 20 por página, última modificación primero")
        void usaLosValoresPorDefecto() throws Exception {
            servicios_devuelven_una_pagina();

            mockMvc.perform(get(ruta("conductores")).with(usuario("ROLE_ADMINISTRADOR")))
                    .andExpect(status().isOk());

            ArgumentCaptor<FiltroAuditoria> filtro = ArgumentCaptor.forClass(FiltroAuditoria.class);
            ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
            verify(conductorService).listarAuditoria(filtro.capture(), pageable.capture());
            assertThat(filtro.getValue()).isEqualTo(new FiltroAuditoria(null, null, null, null));
            assertThat(filtro.getValue().estado()).isEqualTo(FiltroAuditoria.EstadoFiltro.TODOS);
            assertThat(pageable.getValue().getPageSize()).isEqualTo(20);
            assertThat(pageable.getValue().getSort()).isEqualTo(Sort.by(Sort.Order.desc("updatedAt")));
        }

        @Test
        @DisplayName("el tamaño de página se acota a 100")
        void acotaElTamanioDePagina() throws Exception {
            servicios_devuelven_una_pagina();

            mockMvc.perform(get(ruta("usuarios")).with(usuario("ROLE_ADMINISTRADOR")).param("size", "5000"))
                    .andExpect(status().isOk());

            ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
            verify(usuarioService).listarAuditoria(any(), pageable.capture());
            assertThat(pageable.getValue().getPageSize()).isEqualTo(100);
        }

        @Test
        @DisplayName("400 con el mensaje propio si se ordena por un campo no permitido")
        void rechazaOrdenInvalido() throws Exception {
            when(neumaticoService.listarAuditoria(any(), any()))
                    .thenThrow(new ParametroInvalidoException("Solo se puede ordenar por createdAt, deletedAt, updatedAt"));

            mockMvc.perform(get(ruta("neumaticos")).with(usuario("ROLE_ADMINISTRADOR")).param("sort", "clave"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("Solo se puede ordenar por createdAt, deletedAt, updatedAt"));
        }

        @Test
        @DisplayName("400 si el estado o la fecha no tienen un valor válido")
        void rechazaParametrosMalFormados() throws Exception {
            mockMvc.perform(get(ruta("proveedores")).with(usuario("ROLE_ADMINISTRADOR")).param("estado", "OTRO"))
                    .andExpect(status().isBadRequest());
            mockMvc.perform(get(ruta("proveedores")).with(usuario("ROLE_ADMINISTRADOR")).param("desde", "ayer"))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(proveedorService);
        }
    }
}
