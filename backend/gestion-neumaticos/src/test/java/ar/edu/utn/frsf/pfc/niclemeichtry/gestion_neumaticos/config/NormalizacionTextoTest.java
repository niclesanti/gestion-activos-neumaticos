package ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class NormalizacionTextoTest {

    @Test
    void recortarQuitaLosEspaciosDeLosExtremos() {
        assertThat(NormalizacionTexto.recortar("  Calle  Falsa ")).isEqualTo("Calle  Falsa");
        assertThat(NormalizacionTexto.recortar(null)).isNull();
    }

    @Test
    void opcionalRecortaYTratarVacioComoNulo() {
        assertThat(NormalizacionTexto.opcional(" 3424 555123 ")).isEqualTo("3424 555123");
        assertThat(NormalizacionTexto.opcional("   ")).isNull();
        assertThat(NormalizacionTexto.opcional("")).isNull();
        assertThat(NormalizacionTexto.opcional(null)).isNull();
    }

    @Test
    void emailRecortaYPasaAMinusculas() {
        assertThat(NormalizacionTexto.email("  ABC@Mail.COM ")).isEqualTo("abc@mail.com");
        assertThat(NormalizacionTexto.email("  ")).isNull();
        assertThat(NormalizacionTexto.email(null)).isNull();
    }

    @Test
    void mayusculasRecortaYPasaAMayusculas() {
        assertThat(NormalizacionTexto.mayusculas(" s3000abc ")).isEqualTo("S3000ABC");
        assertThat(NormalizacionTexto.mayusculas("   ")).isNull();
        assertThat(NormalizacionTexto.mayusculas(null)).isNull();
    }

}
