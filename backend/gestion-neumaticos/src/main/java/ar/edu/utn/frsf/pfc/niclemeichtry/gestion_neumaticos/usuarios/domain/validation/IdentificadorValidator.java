package ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.usuarios.domain.validation;

import java.util.regex.Pattern;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class IdentificadorValidator implements ConstraintValidator<IdentificadorValido, String> {

    /** Mismo patrón que el frontend (`login-schema.ts`). */
    static final Pattern NOMBRE_USUARIO = Pattern.compile("^[a-zA-Z0-9._-]{3,30}$");

    /**
     * Formato práctico de email (local@dominio.tld), sin espacios ni caracteres
     * de control. No pretende cubrir todo RFC 5322: sólo filtrar basura antes de
     * consultar la base.
     */
    static final Pattern EMAIL = Pattern.compile(
            "^[a-zA-Z0-9.!#$%&'*+/=?^_`{|}~-]+@[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?(?:\\.[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?)+$");

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }
        String identificador = value.strip();
        if (identificador.isEmpty()) {
            return true;
        }
        return identificador.contains("@")
                ? EMAIL.matcher(identificador).matches()
                : NOMBRE_USUARIO.matcher(identificador).matches();
    }

}
