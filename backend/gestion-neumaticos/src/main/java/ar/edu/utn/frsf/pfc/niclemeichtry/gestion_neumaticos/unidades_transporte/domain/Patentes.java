package ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.unidades_transporte.domain;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Normalización y formato de las patentes argentinas. La misma regla está en el
 * CHECK {@code ck_unidades_transporte_patente_formato} (V16) y en
 * {@code features/transport-units/schemas/transport-unit-schema.ts}: si cambia
 * una, cambian todas.
 */
public final class Patentes {

    /** Formato anterior (ABC123) o Mercosur (AB123CD), ya normalizada. */
    public static final String PATRON = "^([A-Z]{3}[0-9]{3}|[A-Z]{2}[0-9]{3}[A-Z]{2})$";

    private static final Pattern FORMATO = Pattern.compile(PATRON);
    private static final Pattern SEPARADORES = Pattern.compile("[\\s-]");

    private Patentes() {
    }

    /** Mayúsculas, sin espacios ni guiones: "ab 123-cd" → "AB123CD". Null queda null. */
    public static String normalizar(String patente) {
        if (patente == null) {
            return null;
        }
        return SEPARADORES.matcher(patente).replaceAll("").toUpperCase(Locale.ROOT);
    }

    /** Si la patente, una vez normalizada, tiene alguno de los dos formatos. */
    public static boolean esValida(String patente) {
        String normalizada = normalizar(patente);
        return normalizada != null && FORMATO.matcher(normalizada).matches();
    }

}
