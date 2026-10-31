package ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.reparacion.controller;

import java.net.URI;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.config.OpenApiConfig;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.exception.ExceptionInfo;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.reparacion.domain.dto.ServicioReparacionRequestDTO;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.reparacion.domain.dto.ServicioReparacionResponseDTO;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.reparacion.service.ServicioReparacionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/servicios-reparacion")
@RequiredArgsConstructor
@Tag(name = "Servicios de reparación", description = "Operaciones relacionadas con los servicios de reparación y gomerías externas")
public class ServicioReparacionController {

    private final ServicioReparacionService servicioReparacionService;

    @PostMapping
    @PreAuthorize("hasRole('EDITOR')")
    @Operation(
            summary = "Registrar un servicio de reparación",
            description = "Da de alta un servicio de reparación o gomería con nombre (único, sin distinguir "
                    + "mayúsculas) y, opcionalmente, email, teléfono y domicilio. Si la localidad del domicilio no "
                    + "existe en la provincia, se crea. Solo editores y administradores.",
            security = @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH))
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Servicio registrado; el header `Location` apunta "
                    + "al recurso creado"),
            @ApiResponse(responseCode = "400", description = "Campos obligatorios faltantes o con formato inválido",
                    content = @Content(schema = @Schema(implementation = ExceptionInfo.class))),
            @ApiResponse(responseCode = "401", description = "Token ausente, inválido, expirado o revocado",
                    content = @Content(schema = @Schema(implementation = ExceptionInfo.class))),
            @ApiResponse(responseCode = "403", description = "El usuario no es editor ni administrador",
                    content = @Content(schema = @Schema(implementation = ExceptionInfo.class))),
            @ApiResponse(responseCode = "404", description = "La provincia del domicilio no existe",
                    content = @Content(schema = @Schema(implementation = ExceptionInfo.class))),
            @ApiResponse(responseCode = "409", description = "Ya existe un servicio de reparación con ese nombre",
                    content = @Content(schema = @Schema(implementation = ExceptionInfo.class)))
    })
    public ResponseEntity<ServicioReparacionResponseDTO> registrarServicioReparacion(
            @Valid @RequestBody ServicioReparacionRequestDTO request,
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt) {
        ServicioReparacionResponseDTO creado = servicioReparacionService.registrarServicioReparacion(request,
                jwt.getSubject());
        URI ubicacion = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{publicId}")
                .buildAndExpand(creado.publicId())
                .toUri();
        return ResponseEntity.created(ubicacion).body(creado);
    }

}
