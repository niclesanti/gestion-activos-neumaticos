package ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.proveedores.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.config.OpenApiConfig;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.exception.ExceptionInfo;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.proveedores.ProveedorConsulta;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.proveedores.domain.dto.ProveedorResponseDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/proveedores")
@RequiredArgsConstructor
@Tag(name = "Proveedores", description = "Consulta de proveedores de neumáticos (provisoria: datos de prueba)")
public class ProveedorController {

    private final ProveedorConsulta proveedorConsulta;

    @GetMapping
    @PreAuthorize("hasRole('LECTOR')")
    @Operation(
            summary = "Listar proveedores",
            description = "Devuelve los proveedores ordenados por nombre. Por ahora es un listado fijo de prueba "
                    + "hasta que se implemente el módulo de proveedores.",
            security = @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Listado de proveedores"),
            @ApiResponse(responseCode = "401", description = "Token ausente, inválido, expirado o revocado",
                    content = @Content(schema = @Schema(implementation = ExceptionInfo.class)))
    })
    public ResponseEntity<List<ProveedorResponseDTO>> listar() {
        return ResponseEntity.ok(proveedorConsulta.listar().stream()
                .map(proveedor -> new ProveedorResponseDTO(proveedor.publicId(), proveedor.nombre()))
                .toList());
    }

}
