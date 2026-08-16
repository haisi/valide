package li.selman.valide.validator;

import java.util.List;

public final class ValueObjectValidationException extends IllegalArgumentException {

    private final List<ValidationResult> violations;

    public ValueObjectValidationException(List<ValidationResult> violations) {
        super(violations.toString());
        this.violations = List.copyOf(violations);
    }

    public List<ValidationResult> violations() {
        return violations;
    }
}
