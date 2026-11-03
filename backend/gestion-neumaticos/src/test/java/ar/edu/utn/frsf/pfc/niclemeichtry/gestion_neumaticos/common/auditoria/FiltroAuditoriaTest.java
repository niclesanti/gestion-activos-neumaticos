package ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.common.auditoria;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class FiltroAuditoriaTest {

    @Test
    @DisplayName("sin estado equivale a TODOS y el texto en blanco a sin búsqueda")
    void valoresPorDefecto() {
        FiltroAuditoria filtro = new FiltroAuditoria("   ", null, null, null);

        assertThat(filtro.estado()).isEqualTo(FiltroAuditoria.EstadoFiltro.TODOS);
        assertThat(filtro.q()).isNull();
        assertThat(new FiltroAuditoria(null, null, null, null).q()).isNull();
    }

    @Test
    @DisplayName("recorta los extremos del texto")
    void recortaElTexto() {
        assertThat(new FiltroAuditoria("  Litoral ", FiltroAuditoria.EstadoFiltro.BAJAS, null, null).q())
                .isEqualTo("Litoral");
    }
}
