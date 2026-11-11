package ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.unidades_transporte.controller;

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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.common.auditoria.FiltroAuditoria;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.common.auditoria.RegistroAuditoriaDTO;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.config.OpenApiConfig;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.config.PaginacionConfig;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.exception.ExceptionInfo;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.unidades_transporte.domain.dto.ConfiguracionUnidadDTO;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.unidades_transporte.domain.dto.UnidadTransporteListadoDTO;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.unidades_transporte.domain.dto.UnidadTransporteRequestDTO;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.unidades_transporte.domain.dto.UnidadTransporteResponseDTO;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.unidades_transporte.service.UnidadTransporteService;
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
@RequestMapping("/api/v1/unidades-transporte")
@RequiredArgsConstructor
@Tag(name = "Unidades de transporte", description = "Operaciones relacionadas con las unidades de transporte")
public class UnidadTransporteController {

    private final UnidadTransporteService unidadTransporteService;

    @PostMapping
    @PreAuthorize("hasRole('EDITOR')")
    @Operation(
            summary = "Registrar una unidad de transporte",
            description = "Da de alta una unidad (tractor o semirremolque) con su patente (única; formato ABC123 o "
                    + "AB123CD, se aceptan espacios, guiones y minúsculas) y una configuración precargada, y genera "
                    + "todos sus slots sin neumático asignado. Solo editores y administradores.",
            security = @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH))
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Unidad registrada con sus slots; el header "
                    + "`Location` apunta al recurso creado"),
            @ApiResponse(responseCode = "400", description = "Campos obligatorios faltantes o patente con formato "
                    + "inválido", content = @Content(schema = @Schema(implementation = ExceptionInfo.class))),
            @ApiResponse(responseCode = "401", description = "Token ausente, inválido, expirado o revocado",
                    content = @Content(schema = @Schema(implementation = ExceptionInfo.class))),
            @ApiResponse(responseCode = "403", description = "El usuario no es editor ni administrador",
                    content = @Content(schema = @Schema(implementation = ExceptionInfo.class))),
            @ApiResponse(responseCode = "404", description = "La configuración de unidad no existe",
                    content = @Content(schema = @Schema(implementation = ExceptionInfo.class))),
            @ApiResponse(responseCode = "409", description = "Ya existe una unidad con esa patente",
                    content = @Content(schema = @Schema(implementation = ExceptionInfo.class)))
    })
    public ResponseEntity<UnidadTransporteResponseDTO> registrarUnidadTransporte(
            @Valid @RequestBody UnidadTransporteRequestDTO unidadTransporteRequestDTO,
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt) {
        UnidadTransporteResponseDTO creada = unidadTransporteService.registrarUnidadTransporte(
                unidadTransporteRequestDTO, jwt.getSubject());
        URI ubicacion = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{publicId}")
                .buildAndExpand(creada.publicId())
                .toUri();
        return ResponseEntity.created(ubicacion).body(creada);
    }

    @GetMapping
    @PreAuthorize("hasRole('LECTOR')")
    @Operation(
            summary = "Buscar unidades de transporte por patente",
            description = "Lista, paginadas, las unidades activas cuya patente contiene el texto indicado (sin "
                    + "distinguir mayúsculas, espacios ni guiones). Sin `patente` devuelve todas las activas. "
                    + "Tamaño de página 10 por defecto y 20 como máximo; solo se ordena por `patente` "
                    + "(ascendente por defecto). Cualquier nivel de acceso.",
            security = @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Página de unidades (puede estar vacía)"),
            @ApiResponse(responseCode = "400", description = "Orden por un campo no permitido",
                    content = @Content(schema = @Schema(implementation = ExceptionInfo.class))),
            @ApiResponse(responseCode = "401", description = "Token ausente, inválido, expirado o revocado",
                    content = @Content(schema = @Schema(implementation = ExceptionInfo.class)))
    })
    public ResponseEntity<Page<UnidadTransporteListadoDTO>> buscar(
            @Parameter(description = "Patente completa o parcial") @RequestParam(required = false) String patente,
            @ParameterObject @PageableDefault(size = PaginacionConfig.TAMANIO_PAGINA_POR_DEFECTO, sort = "patente") Pageable pageable) {
        return ResponseEntity.ok(unidadTransporteService.buscar(patente, pageable));
    }

    @GetMapping("/configuraciones")
    @PreAuthorize("hasRole('LECTOR')")
    @Operation(
            summary = "Listar configuraciones de unidad",
            description = "Devuelve las configuraciones de ejes precargadas, con sus ejes en orden y la cantidad de "
                    + "slots que genera cada una.",
            security = @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Listado de configuraciones"),
            @ApiResponse(responseCode = "401", description = "Token ausente, inválido, expirado o revocado",
                    content = @Content(schema = @Schema(implementation = ExceptionInfo.class)))
    })
    public ResponseEntity<List<ConfiguracionUnidadDTO>> listarConfiguraciones() {
        return ResponseEntity.ok(unidadTransporteService.listarConfiguraciones());
    }

    @GetMapping("/auditoria")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @Operation(
            summary = "Consultar la auditoría de unidades de transporte",
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
            @ParameterObject @PageableDefault(size = PaginacionConfig.TAMANIO_PAGINA_POR_DEFECTO, sort = "updatedAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(unidadTransporteService.listarAuditoria(filtro, pageable));
    }

}
