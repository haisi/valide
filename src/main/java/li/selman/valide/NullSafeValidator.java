package li.selman.valide;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.jspecify.annotations.Nullable;

/**
 * Validates {@link NullSafe} by walking the annotated object graph and reporting one violation per attribute
 * that holds {@code null} without its declared type permitting it.
 */
public final class NullSafeValidator implements ConstraintValidator<NullSafe, Object> {

    @Override
    public boolean isValid(@Nullable Object value, ConstraintValidatorContext context) {
        if (value == null) {
            // Whether null is acceptable here is @NotNull's business, not this constraint's.
            return true;
        }
        // Violations are reported per offending attribute, so the generic root-level one is not wanted.
        context.disableDefaultConstraintViolation();
        return new NullnessWalker(context).walk(value);
    }
}
