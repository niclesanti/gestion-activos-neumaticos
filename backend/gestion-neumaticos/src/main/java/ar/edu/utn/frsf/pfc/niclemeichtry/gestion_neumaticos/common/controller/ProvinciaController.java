package ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.common.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.common.domain.dto.ProvinciaResponseDTO;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.common.service.ProvinciaService;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.config.OpenApiConfig;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.exception.ExceptionInfo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/provincias")
@RequiredArgsConstructor
@Tag(name = "Provincias", description = "Catálogo de provincias para los domicilios")
public class ProvinciaController {

    private final ProvinciaService provinciaService;

    @GetMapping
    @PreAuthorize("hasRole('LECTOR')")
    @Operation(
            summary = "Listar provincias",
            description = "Devuelve las provincias ordenadas por nombre. Se cargan por migraciones: no hay alta ni "
                    + "modificación por la API.",
            security = @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Listado de provincias"),
            @ApiResponse(responseCode = "401", description = "Token ausente, inválido, expirado o revocado",
                    content = @Content(schema = @Schema(implementation = ExceptionInfo.class)))
    })
    public ResponseEntity<List<ProvinciaResponseDTO>> listarProvincias() {
        return ResponseEntity.ok(provinciaService.listarProvincias());
    }

}
