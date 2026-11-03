package ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.almacenamiento.controller;

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

import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.almacenamiento.domain.dto.CentroAlmacenamientoRequestDTO;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.almacenamiento.domain.dto.CentroAlmacenamientoResponseDTO;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.almacenamiento.service.CentroAlmacenamientoService;
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
@RequestMapping("/api/v1/centros-almacenamiento")
@RequiredArgsConstructor
@Tag(name = "Centros de almacenamiento", description = "Operaciones relacionadas con los centros de almacenamiento (depósitos) de neumáticos")
public class CentroAlmacenamientoController {

    private final CentroAlmacenamientoService centroAlmacenamientoService;

    @PostMapping
    @PreAuthorize("hasRole('EDITOR')")
    @Operation(
            summary = "Registrar un centro de almacenamiento",
            description = "Da de alta un centro de almacenamiento con nombre y domicilio (obligatorio), sin "
                    + "neumáticos en stock. Si la localidad del domicilio no existe en la provincia, se crea. "
                    + "Solo editores y administradores.",
            security = @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH))
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Centro registrado; el header `Location` apunta "
                    + "al recurso creado"),
            @ApiResponse(responseCode = "400", description = "Campos obligatorios faltantes (incluido el "
                    + "domicilio) o con formato inválido",
                    content = @Content(schema = @Schema(implementation = ExceptionInfo.class))),
            @ApiResponse(responseCode = "401", description = "Token ausente, inválido, expirado o revocado",
                    content = @Content(schema = @Schema(implementation = ExceptionInfo.class))),
            @ApiResponse(responseCode = "403", description = "El usuario no es editor ni administrador",
                    content = @Content(schema = @Schema(implementation = ExceptionInfo.class))),
            @ApiResponse(responseCode = "404", description = "La provincia del domicilio no existe",
                    content = @Content(schema = @Schema(implementation = ExceptionInfo.class)))
    })
    public ResponseEntity<CentroAlmacenamientoResponseDTO> registrarCentroAlmacenamiento(
            @Valid @RequestBody CentroAlmacenamientoRequestDTO request,
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt) {
        CentroAlmacenamientoResponseDTO creado = centroAlmacenamientoService.registrarCentroAlmacenamiento(request,
                jwt.getSubject());
        URI ubicacion = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{publicId}")
                .buildAndExpand(creado.publicId())
                .toUri();
        return ResponseEntity.created(ubicacion).body(creado);
    }

}
