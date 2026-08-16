package li.selman.valide.validator;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.ValidationException;

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
