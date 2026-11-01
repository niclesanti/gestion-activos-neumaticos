package ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.common.auditoria;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.exception.ParametroInvalidoException;

class OrdenAuditoriaTest {

    @Test
    @DisplayName("acepta ordenar por los campos de auditoría y sin orden")
    void aceptaCamposPermitidos() {
        Pageable porFechas = PageRequest.of(0, 20,
                Sort.by("createdAt").and(Sort.by("updatedAt").descending()).and(Sort.by("deletedAt")));

        assertThat(OrdenAuditoria.validar(porFechas)).isSameAs(porFechas);
        assertThat(OrdenAuditoria.validar(Pageable.unpaged())).isNotNull();
    }

    @Test
    @DisplayName("rechaza cualquier otro campo, aunque venga junto a uno permitido")
    void rechazaOtrosCampos() {
        Pageable clave = PageRequest.of(0, 20, Sort.by("updatedAt").and(Sort.by("clave")));

        assertThatThrownBy(() -> OrdenAuditoria.validar(clave))
                .isInstanceOf(ParametroInvalidoException.class)
                .hasMessage("Solo se puede ordenar por createdAt, deletedAt, updatedAt");
    }
}
