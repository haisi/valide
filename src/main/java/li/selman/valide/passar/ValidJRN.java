package li.selman.valide.passar;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import li.selman.valide.validator.ValidValue;

@Target({FIELD, PARAMETER, RECORD_COMPONENT})
@Retention(RUNTIME)
@Constraint(validatedBy = {})
@ValidValue(JRN.Validator.class)
public @interface ValidJRN {

    String message() default "invalid JRN";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
