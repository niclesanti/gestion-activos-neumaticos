package ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.proveedores.controller;

import static ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.TestDataFactory.PROVEEDOR_PUBLIC_ID;
import static ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.TestDataFactory.proveedorResumen;
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

import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.config.CorsConfig;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.proveedores.ProveedorConsulta;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.security.NivelAccesoVigente;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.security.SecurityConfig;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.security.web.JsonSecurityErrorHandler;

@WebMvcTest(ProveedorController.class)
@Import({ SecurityConfig.class, JsonSecurityErrorHandler.class, CorsConfig.class })
class ProveedorControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockitoBean private ProveedorConsulta proveedorConsulta;
    @MockitoBean private JwtDecoder jwtDecoder;
    @MockitoBean private NivelAccesoVigente nivelAccesoVigente;

    @Test
    @DisplayName("lista publicId y nombre, sin exponer el id interno")
    void lista() throws Exception {
        when(proveedorConsulta.listar()).thenReturn(List.of(proveedorResumen()));

        mockMvc.perform(get("/api/proveedores").with(jwt().authorities(new SimpleGrantedAuthority("ROLE_LECTOR"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].publicId").value(PROVEEDOR_PUBLIC_ID.toString()))
                .andExpect(jsonPath("$[0].nombre").value("Neumáticos del Litoral S.A."))
                .andExpect(jsonPath("$[0].id").doesNotExist());
    }

    @Test
    @DisplayName("401 sin token")
    void sinToken() throws Exception {
        mockMvc.perform(get("/api/proveedores")).andExpect(status().isUnauthorized());
    }

}
