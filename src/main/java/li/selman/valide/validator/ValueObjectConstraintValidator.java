package li.selman.valide.validator;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.ValidationException;

/**
 * Bridges {@link ValidValue} to the {@link ValueValidator} it names: instantiates that validator once when the
 * constraint is initialized, then turns every {@link ValidationResult} it reports into a constraint violation
 * of its own, in place of the constraint's default violation.
 *
 * <p>A validator that cannot be instantiated - because it has no accessible no-arg constructor, or because
 * that constructor threw - is a configuration error and fails fast with a {@link ValidationException} rather
 * than silently accepting every value.
 */
@SuppressWarnings("NullAway.Init")
public final class ValueObjectConstraintValidator implements ConstraintValidator<ValidValue, Object> {

    private ValueValidator<Object> validator;

    @Override
    @SuppressWarnings("unchecked")
    public void initialize(ValidValue annotation) {
        try {
            validator = (ValueValidator<Object>)
                    annotation.value().getDeclaredConstructor().newInstance();
        } catch (ReflectiveOperationException e) {
            throw new ValidationException(
                    "Cannot instantiate validator: " + annotation.value().getName(), e);
        }
    }

    @Override
    public boolean isValid(Object value, ConstraintValidatorContext context) {
        var violations = validator.validate(value);

        if (violations.isEmpty()) {
            return true;
        }

        context.disableDefaultConstraintViolation();

        for (var violation : violations) {
            context.buildConstraintViolationWithTemplate(violation.message()).addConstraintViolation();
        }

        return false;
    }
}
