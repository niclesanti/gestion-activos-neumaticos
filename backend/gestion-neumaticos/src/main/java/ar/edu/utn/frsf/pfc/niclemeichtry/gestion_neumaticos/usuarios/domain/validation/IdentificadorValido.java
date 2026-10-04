package ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.usuarios.domain.validation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

/**
 * El identificador de login es un correo electrónico (si contiene {@code @}) o
 * un nombre de usuario. Un valor nulo o vacío se considera válido: la
 * obligatoriedad la cubre {@code @NotBlank}, así no se duplican los mensajes.
 */
@Documented
@Constraint(validatedBy = IdentificadorValidator.class)
@Target({ ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT })
@Retention(RetentionPolicy.RUNTIME)
public @interface IdentificadorValido {

    String message() default "Debe ser un correo electrónico o un nombre de usuario válido";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

}
