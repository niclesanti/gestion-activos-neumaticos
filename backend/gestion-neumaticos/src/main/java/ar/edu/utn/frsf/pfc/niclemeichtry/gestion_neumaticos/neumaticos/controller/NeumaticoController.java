package ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.neumaticos.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.config.OpenApiConfig;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.exception.ExceptionInfo;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.neumaticos.domain.dto.MarcaNeumaticoResponseDTO;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.neumaticos.domain.dto.NeumaticoRequestDTO;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.neumaticos.domain.dto.NeumaticoResponseDTO;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.neumaticos.service.NeumaticoService;
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
@RequestMapping("/api/neumatico")
@RequiredArgsConstructor
@Tag(name = "Neumáticos", description = "Operaciones relacionadas con los neumáticos")
public class NeumaticoController {

    private final NeumaticoService neumaticoService;

    @PostMapping
    @PreAuthorize("hasRole('EDITOR')")
    @Operation(
            summary = "Registrar un neumático",
            description = "Da de alta un neumático (HU-11). Queda en espera de que se opere con él. El número de "
                    + "serie es opcional, único y no se puede modificar después. Si el estado es `NUEVO`, el "
                    + "kilometraje debe ser 0, no puede tener recapados y se registra de forma asíncrona el evento "
                    + "de compra en la trazabilidad (con el proveedor, si se indica). Solo editores y administradores.",
            security = @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH))
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Neumático registrado"),
            @ApiResponse(responseCode = "400", description = "Campos obligatorios faltantes, con formato inválido "
                    + "o que violan una regla del alta",
                    content = @Content(schema = @Schema(implementation = ExceptionInfo.class))),
            @ApiResponse(responseCode = "401", description = "Token ausente, inválido, expirado o revocado",
                    content = @Content(schema = @Schema(implementation = ExceptionInfo.class))),
            @ApiResponse(responseCode = "403", description = "El usuario no es editor ni administrador",
                    content = @Content(schema = @Schema(implementation = ExceptionInfo.class))),
            @ApiResponse(responseCode = "404", description = "La marca o el proveedor indicados no existen",
                    content = @Content(schema = @Schema(implementation = ExceptionInfo.class))),
            @ApiResponse(responseCode = "409", description = "Ya existe un neumático con ese número de serie",
                    content = @Content(schema = @Schema(implementation = ExceptionInfo.class)))
    })
    public ResponseEntity<NeumaticoResponseDTO> registrarNeumatico(
            @Valid @RequestBody NeumaticoRequestDTO neumaticoRequestDTO,
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(neumaticoService.registrarNeumatico(neumaticoRequestDTO, jwt.getSubject()));
    }

    @GetMapping("/marcas")
    @PreAuthorize("hasRole('LECTOR')")
    @Operation(
            summary = "Listar marcas de neumáticos",
            description = "Devuelve las marcas disponibles, ordenadas por nombre. Las marcas solo se cargan por "
                    + "migraciones: no hay alta ni modificación por la API.",
            security = @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Listado de marcas"),
            @ApiResponse(responseCode = "401", description = "Token ausente, inválido, expirado o revocado",
                    content = @Content(schema = @Schema(implementation = ExceptionInfo.class)))
    })
    public ResponseEntity<List<MarcaNeumaticoResponseDTO>> listarMarcas() {
        return ResponseEntity.ok(neumaticoService.listarMarcas());
    }

}
