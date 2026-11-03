package ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.common.auditoria;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;

/**
 * Query params de la consulta de auditoría. {@code estado} es {@code ACTIVOS},
 * {@code BAJAS} o {@code TODOS}; {@code desde}/{@code hasta} acotan la fecha de
 * última modificación (inclusive, en UTC).
 */
public record FiltroAuditoria(
        String q,
        EstadoFiltro estado,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {

    public enum EstadoFiltro {
        ACTIVOS,
        BAJAS,
        TODOS
    }

    public FiltroAuditoria {
        estado = estado == null ? EstadoFiltro.TODOS : estado;
        q = q == null || q.isBlank() ? null : q.strip();
    }
}
