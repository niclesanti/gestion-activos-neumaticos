package ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.config;

import java.util.Locale;

import org.mapstruct.Named;

/**
 * Normalización de texto del alta, para usar desde los mappers de MapStruct
 * ({@code uses = NormalizacionTexto.class} + {@code qualifiedByName}).
 *
 * <p>El mapper es el único punto donde el DTO del request se traduce a la
 * entidad, así que el formato del texto (recortes, mayúsculas) se resuelve acá
 * y el servicio no tiene que pisar lo ya mapeado.
 */
public final class NormalizacionTexto {

    private NormalizacionTexto() {
    }

    /** Recorta los extremos de un texto obligatorio. */
    @Named("recortar")
    public static String recortar(String texto) {
        return texto == null ? null : texto.trim();
    }

    /** Recorta los extremos; un texto opcional vacío equivale a no tenerlo. */
    @Named("opcional")
    public static String opcional(String texto) {
        if (texto == null || texto.isBlank()) {
            return null;
        }
        return texto.trim();
    }

    /** Recorta y pasa a minúsculas; vacío equivale a no tenerlo. */
    @Named("email")
    public static String email(String email) {
        String recortado = opcional(email);
        return recortado == null ? null : recortado.toLowerCase(Locale.ROOT);
    }

    /** Recorta y pasa a mayúsculas; vacío equivale a no tenerlo. */
    @Named("mayusculas")
    public static String mayusculas(String texto) {
        String recortado = opcional(texto);
        return recortado == null ? null : recortado.toUpperCase(Locale.ROOT);
    }

}
