package li.selman.valide.validator;

import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * The rules of one value object, in a form that can be run without constructing it.
 *
 * <p>An implementation reports <em>every</em> rule the value breaks rather than stopping at the first, so a
 * caller can show all of them at once. It is the single place those rules live: the value object calls it from
 * its own constructor, and {@link ValidValue} runs the very same implementation as a Bean Validation
 * constraint, so both paths can never drift apart.
 *
 * <p>Implementations must be stateless and have a public no-arg constructor, because
 * {@link ValueObjectConstraintValidator} instantiates them reflectively.
 *
 * @param <T> the raw representation the value object wraps, typically {@code String}
 */
public interface ValueValidator<T> {

    /**
     * Checks a raw value against every rule of the value object.
     *
     * @param value the raw value to check, which may be {@code null} - an implementation decides whether that
     *     is a violation or not
     * @return one result per broken rule, in the order they should be reported; empty if the value is valid
     */
    List<ValidationResult> validate(@Nullable T value);
}
