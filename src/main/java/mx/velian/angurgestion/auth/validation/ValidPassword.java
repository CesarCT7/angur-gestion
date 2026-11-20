package mx.velian.angurgestion.auth.validation;

import jakarta.validation.*;

import java.lang.annotation.*;
import java.nio.charset.StandardCharsets;

@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = ValidPassword.Validator.class)
public @interface ValidPassword {
    String message() default
            "Usa al menos 12 caracteres y un máximo de 72 bytes; los acentos y símbolos pueden"
                + " ocupar más de un byte.";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class Validator implements ConstraintValidator<ValidPassword, String> {
        public boolean isValid(String value, ConstraintValidatorContext context) {
            return value == null
                    || (value.codePointCount(0, value.length()) >= 12
                            && value.getBytes(StandardCharsets.UTF_8).length <= 72);
        }
    }
}
