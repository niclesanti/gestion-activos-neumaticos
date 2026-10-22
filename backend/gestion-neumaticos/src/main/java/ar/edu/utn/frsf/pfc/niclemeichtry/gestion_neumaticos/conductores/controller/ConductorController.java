package ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.conductores.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.conductores.domain.dto.ConductorRequestDTO;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.conductores.domain.dto.ConductorResponseDTO;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.conductores.service.ConductorService;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.config.OpenApiConfig;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.exception.ExceptionInfo;
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
@RequestMapping("/api/conductores")
@RequiredArgsConstructor
@Tag(name = "Conductores", description = "Operaciones relacionadas con los conductores")
public class ConductorController {

    private final ConductorService conductorService;

    @PostMapping
    @PreAuthorize("hasRole('EDITOR')")
    @Operation(
            summary = "Registrar un conductor",
            description = "Da de alta un conductor con nombre, apellido y, opcionalmente, DNI (7 u 8 dígitos, sin "
                    + "puntos, único). Solo editores y administradores.",
            security = @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH))
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Conductor registrado"),
            @ApiResponse(responseCode = "400", description = "Campos obligatorios faltantes o con formato inválido",
                    content = @Content(schema = @Schema(implementation = ExceptionInfo.class))),
            @ApiResponse(responseCode = "401", description = "Token ausente, inválido, expirado o revocado",
                    content = @Content(schema = @Schema(implementation = ExceptionInfo.class))),
            @ApiResponse(responseCode = "403", description = "El usuario no es editor ni administrador",
                    content = @Content(schema = @Schema(implementation = ExceptionInfo.class))),
            @ApiResponse(responseCode = "409", description = "Ya existe un conductor con ese DNI",
                    content = @Content(schema = @Schema(implementation = ExceptionInfo.class)))
    })
    public ResponseEntity<ConductorResponseDTO> registrarConductor(
            @Valid @RequestBody ConductorRequestDTO conductorRequestDTO,
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(conductorService.registrarConductor(conductorRequestDTO, jwt.getSubject()));
    }

}
