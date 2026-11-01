package ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.common.auditoria;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

/**
 * Convierte una página de entidades auditadas en fichas de auditoría. Resuelve los
 * usuarios de toda la página con una sola llamada al {@link AuditoresResolver}
 * (sin N+1).
 */
@Component
@RequiredArgsConstructor
public class EnsambladorAuditoria {

    private final AuditoresResolver auditoresResolver;

    public <T extends AuditableEntity> Page<RegistroAuditoriaDTO> ensamblar(Page<T> pagina,
            Function<T, UUID> publicId, Function<T, String> descripcion) {
        Set<UUID> ids = new LinkedHashSet<>();
        for (T entidad : pagina.getContent()) {
            agregar(ids, entidad.getCreatedBy());
            agregar(ids, entidad.getUpdatedBy());
            agregar(ids, entidad.getDeletedBy());
        }
        Map<UUID, AuditorDTO> auditores = ids.isEmpty() ? Map.of() : new HashMap<>(auditoresResolver.resolver(ids));

        return pagina.map(entidad -> new RegistroAuditoriaDTO(
                publicId.apply(entidad),
                descripcion.apply(entidad),
                entidad.getCreatedAt(),
                auditor(auditores, entidad.getCreatedBy()),
                entidad.getUpdatedAt(),
                auditor(auditores, entidad.getUpdatedBy()),
                entidad.getDeletedAt(),
                auditor(auditores, entidad.getDeletedBy()),
                entidad.isDadoDeBaja() ? EstadoRegistro.BAJA : EstadoRegistro.ACTIVO));
    }

    private static void agregar(Set<UUID> ids, UUID id) {
        if (id != null) {
            ids.add(id);
        }
    }

    /** {@code null} si la operación no tiene usuario; solo el id si ya no se puede resolver. */
    private static AuditorDTO auditor(Map<UUID, AuditorDTO> auditores, UUID id) {
        if (id == null) {
            return null;
        }
        return auditores.getOrDefault(id, AuditorDTO.desconocido(id));
    }
}
