package li.selman.valide.validator;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Runs a {@link ValueValidator} as a Bean Validation constraint, reporting one violation per broken rule
 * instead of a single generic one.
 *
 * <p>Put it on the raw property directly, or meta-annotate a constraint of your own so the rule reads in the
 * domain's language:
 *
 * <pre>{@code
 * @Target({FIELD, PARAMETER, RECORD_COMPONENT})
 * @Retention(RUNTIME)
 * @Constraint(validatedBy = {})
 * @ValidValue(JRN.Validator.class)
 * public @interface ValidJRN { ... }
 * }</pre>
 *
 * <p>Each {@link ValidationResult} the validator reports becomes its own violation, using the result's message
 * as the message template. {@link #message()} is therefore only a fallback for a validator that rejects a
 * value without reporting anything.
 */
@Target({FIELD, PARAMETER, RECORD_COMPONENT, ANNOTATION_TYPE})
@Retention(RUNTIME)
@Constraint(validatedBy = ValueObjectConstraintValidator.class)
public @interface ValidValue {

    /** {@return the validator to run, which must have a public no-arg constructor} */
    Class<? extends ValueValidator<?>> value();

    String message() default "invalid value";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
