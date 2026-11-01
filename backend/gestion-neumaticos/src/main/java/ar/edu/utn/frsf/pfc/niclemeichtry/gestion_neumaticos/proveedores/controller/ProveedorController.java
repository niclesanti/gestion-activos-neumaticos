package ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.proveedores.controller;

import java.net.URI;
import java.util.List;

import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.common.auditoria.FiltroAuditoria;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.common.auditoria.RegistroAuditoriaDTO;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.config.OpenApiConfig;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.exception.ExceptionInfo;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.proveedores.domain.dto.ProveedorListadoDTO;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.proveedores.domain.dto.ProveedorRequestDTO;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.proveedores.domain.dto.ProveedorResponseDTO;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.proveedores.service.ProveedorService;
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
@RequestMapping("/api/v1/proveedores")
@RequiredArgsConstructor
@Tag(name = "Proveedores", description = "Operaciones relacionadas con los proveedores de neumáticos")
public class ProveedorController {

    private final ProveedorService proveedorService;

    @PostMapping
    @PreAuthorize("hasRole('EDITOR')")
    @Operation(
            summary = "Registrar un proveedor",
            description = "Da de alta un proveedor con nombre (único, sin distinguir mayúsculas) y, opcionalmente, "
                    + "email, teléfono y domicilio. Si la localidad del domicilio no existe en la provincia, se "
                    + "crea. Solo editores y administradores.",
            security = @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH))
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Proveedor registrado; el header `Location` apunta "
                    + "al recurso creado"),
            @ApiResponse(responseCode = "400", description = "Campos obligatorios faltantes o con formato inválido",
                    content = @Content(schema = @Schema(implementation = ExceptionInfo.class))),
            @ApiResponse(responseCode = "401", description = "Token ausente, inválido, expirado o revocado",
                    content = @Content(schema = @Schema(implementation = ExceptionInfo.class))),
            @ApiResponse(responseCode = "403", description = "El usuario no es editor ni administrador",
                    content = @Content(schema = @Schema(implementation = ExceptionInfo.class))),
            @ApiResponse(responseCode = "404", description = "La provincia del domicilio no existe",
                    content = @Content(schema = @Schema(implementation = ExceptionInfo.class))),
            @ApiResponse(responseCode = "409", description = "Ya existe un proveedor con ese nombre",
                    content = @Content(schema = @Schema(implementation = ExceptionInfo.class)))
    })
    public ResponseEntity<ProveedorResponseDTO> registrarProveedor(
            @Valid @RequestBody ProveedorRequestDTO proveedorRequestDTO,
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt) {
        ProveedorResponseDTO creado = proveedorService.registrarProveedor(proveedorRequestDTO, jwt.getSubject());
        URI ubicacion = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{publicId}")
                .buildAndExpand(creado.publicId())
                .toUri();
        return ResponseEntity.created(ubicacion).body(creado);
    }

    @GetMapping
    @PreAuthorize("hasRole('LECTOR')")
    @Operation(
            summary = "Listar proveedores",
            description = "Devuelve los proveedores ordenados por nombre (identificador público y nombre).",
            security = @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Listado de proveedores"),
            @ApiResponse(responseCode = "401", description = "Token ausente, inválido, expirado o revocado",
                    content = @Content(schema = @Schema(implementation = ExceptionInfo.class)))
    })
    public ResponseEntity<List<ProveedorListadoDTO>> listar() {
        return ResponseEntity.ok(proveedorService.listarProveedores());
    }

    @GetMapping("/auditoria")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @Operation(
            summary = "Consultar la auditoría de proveedores",
            description = "Lista, paginada, la ficha de auditoría de cada registro: quién y cuándo lo creó, lo "
                    + "modificó por última vez y lo dio de baja (HU-43). Solo lectura y solo para administradores. "
                    + "Se puede ordenar por `createdAt`, `updatedAt` o `deletedAt`.",
            security = @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Página de fichas de auditoría"),
            @ApiResponse(responseCode = "400", description = "Filtro u orden inválido",
                    content = @Content(schema = @Schema(implementation = ExceptionInfo.class))),
            @ApiResponse(responseCode = "401", description = "Token ausente, inválido, expirado o revocado",
                    content = @Content(schema = @Schema(implementation = ExceptionInfo.class))),
            @ApiResponse(responseCode = "403", description = "El usuario no es administrador",
                    content = @Content(schema = @Schema(implementation = ExceptionInfo.class)))
    })
    public ResponseEntity<Page<RegistroAuditoriaDTO>> listarAuditoria(
            @ParameterObject FiltroAuditoria filtro,
            @ParameterObject @PageableDefault(size = 20, sort = "updatedAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(proveedorService.listarAuditoria(filtro, pageable));
    }

}
