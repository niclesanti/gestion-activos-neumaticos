package ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.common.auditoria;

import java.time.Instant;
import java.util.UUID;

/** Ficha de auditoría de un registro: quién y cuándo lo creó, modificó y dio de baja. */
public record RegistroAuditoriaDTO(
        UUID publicId,
        String descripcion,
        Instant creadoEn,
        AuditorDTO creadoPor,
        Instant modificadoEn,
        AuditorDTO modificadoPor,
        Instant dadoDeBajaEn,
        AuditorDTO dadoDeBajaPor,
        EstadoRegistro estado) {
}
