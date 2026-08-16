package li.selman.valide.validator;

import java.util.List;

/**
 * Thrown by a value object's constructor when the raw value breaks at least one of its rules.
 *
 * <p>It is an {@link IllegalArgumentException}, because handing an invalid raw value to a value object is a
 * programming error: the value should have been checked before. For input that is untrusted by nature - user
 * input, an inbound request - check it first with the {@link ValueValidator}, or use the value object's
 * candidate factory if it offers one, instead of catching this.
 */
public final class ValueObjectValidationException extends IllegalArgumentException {

    private final List<ValidationResult> violations;

    /**
     * Creates an exception carrying every rule the value broke.
     *
     * @param violations the broken rules; their {@code toString()} becomes the exception message
     */
    public ValueObjectValidationException(List<ValidationResult> violations) {
        super(violations.toString());
        this.violations = List.copyOf(violations);
    }

    /** {@return every rule the value broke, in the order the validator reported them} */
    public List<ValidationResult> violations() {
        return violations;
    }
}
