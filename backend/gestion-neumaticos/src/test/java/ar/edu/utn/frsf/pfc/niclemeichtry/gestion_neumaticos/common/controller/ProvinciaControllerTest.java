package ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.common.controller;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.common.domain.dto.ProvinciaResponseDTO;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.common.service.ProvinciaService;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.config.CorsConfig;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.security.NivelAccesoVigente;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.security.SecurityConfig;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.security.web.JsonSecurityErrorHandler;

@WebMvcTest(ProvinciaController.class)
@Import({ SecurityConfig.class, JsonSecurityErrorHandler.class, CorsConfig.class })
class ProvinciaControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockitoBean private ProvinciaService provinciaService;
    @MockitoBean private JwtDecoder jwtDecoder;
    @MockitoBean private NivelAccesoVigente nivelAccesoVigente;

    @Test
    @DisplayName("el lector (y por jerarquía, cualquier rol) lista las provincias")
    void lista() throws Exception {
        when(provinciaService.listarProvincias()).thenReturn(List.of(new ProvinciaResponseDTO(21L, "Santa Fe")));

        mockMvc.perform(get("/api/provincias").with(jwt().authorities(new SimpleGrantedAuthority("ROLE_LECTOR"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(21))
                .andExpect(jsonPath("$[0].nombre").value("Santa Fe"));
    }

    @Test
    @DisplayName("401 sin token")
    void sinToken() throws Exception {
        mockMvc.perform(get("/api/provincias")).andExpect(status().isUnauthorized());

        verifyNoInteractions(provinciaService);
    }

}
