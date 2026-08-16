package li.selman.valide.passar;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import li.selman.valide.validator.ValidValue;

/**
 * Asserts that a raw {@link String} is a well-formed {@link JRN}, applying the same rules the {@code JRN}
 * constructor enforces.
 *
 * <p>Put it on the raw property of an inbound request, so a malformed number is rejected at the boundary with
 * one violation per broken rule, before anything tries to turn it into a {@code JRN}:
 *
 * <pre>{@code
 * record CreateJourneyRequest(@ValidJRN String jrn) {}
 * }</pre>
 */
@Target({FIELD, PARAMETER, RECORD_COMPONENT})
@Retention(RUNTIME)
@Constraint(validatedBy = {})
@ValidValue(JRN.Validator.class)
public @interface ValidJRN {

    String message() default "invalid JRN";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
