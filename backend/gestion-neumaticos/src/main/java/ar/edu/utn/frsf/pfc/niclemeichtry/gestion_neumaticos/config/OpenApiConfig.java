package ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.config;

import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;

/**
 * Metadatos de la API y esquema Bearer JWT: habilita el botón "Authorize" de
 * Swagger UI para probar los endpoints protegidos con el token del login.
 */
@Configuration
@OpenAPIDefinition(info = @Info(
        title = "Gestión de Neumáticos API",
        version = "v1",
        description = "API para la gestión del ciclo de vida de neumáticos"))
@SecurityScheme(
        name = OpenApiConfig.BEARER_AUTH,
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT")
public class OpenApiConfig {

    public static final String BEARER_AUTH = "bearerAuth";

}
