package li.selman.valide.validator;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import jakarta.validation.Validation;
import jakarta.validation.ValidationException;
import jakarta.validation.Validator;
import java.util.List;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

/**
 * Covers the misconfiguration path of {@link ValueObjectConstraintValidator}. The happy paths are exercised
 * through the constraints that build on {@link ValidValue}.
 */
class ValueObjectConstraintValidatorTest {

    private static final Validator BEAN_VALIDATOR =
            Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void fails_loudly_when_the_validator_cannot_be_instantiated() {
        assertThatThrownBy(() -> BEAN_VALIDATOR.validate(new Bean("anything")))
                .isInstanceOf(ValidationException.class)
                .hasStackTraceContaining("Cannot instantiate validator")
                .hasStackTraceContaining(NeedsAnArgument.class.getName());
    }

    record Bean(@ValidValue(NeedsAnArgument.class) String value) {}

    /** Has no no-arg constructor, so {@link ValueObjectConstraintValidator} cannot instantiate it. */
    static final class NeedsAnArgument implements ValueValidator<String> {

        NeedsAnArgument(String unused) {}

        @Override
        public List<ValidationResult> validate(@Nullable String value) {
            return List.of();
        }
    }
}
