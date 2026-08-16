package li.selman.valide.passar;

import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import li.selman.valide.validator.ValidationResult;
import li.selman.valide.validator.ValueObjectValidationException;
import li.selman.valide.validator.ValueValidator;
import org.jspecify.annotations.Nullable;

/**
 * A Journey Reference Number: identifies a single shipment, unique per country and year.
 *
 * <p>The format is {@code YYMMxxxxx}: a two-digit year, a two-digit month ({@code 01}-{@code 12}) and a
 * five-character code. The two-digit year only spans 2000-2099, which is the range {@link #yearMonth()}
 * decodes back into.
 *
 * <p>The code alphabet deliberately leaves out the characters most easily confused when a JRN is read aloud or
 * typed by hand: {@code 0} (zero), {@code O} (uppercase o), {@code I} (uppercase i), {@code L} (uppercase l)
 * and {@code l} (lowercase L). Lowercase {@code i} and {@code o} remain allowed.
 *
 * <p>The value is case-sensitive, and the compact constructor rejects anything that does not match with a
 * {@link ValueObjectValidationException}, so every instance is well-formed. Use
 * {@link #fromCandidate(String)} to get {@code null} instead of an exception, or {@link #validate(String)} to
 * inspect the individual {@link ValidationResult}s without constructing anything.
 *
 * @param value the raw JRN, for example {@code 2507duTaA}
 */
public record JRN(String value) {

    private static final Pattern PATTERN =
            Pattern.compile("^(?<year>[0-9]{2})" + "(?<month>0[1-9]|1[0-2])" + "(?<code>[A-HJ-KM-NP-Za-km-z1-9]{5})$");
    private static final Validator VALIDATOR = new Validator();

    public JRN {
        var violations = VALIDATOR.validate(value);

        if (!violations.isEmpty()) {
            throw new ValueObjectValidationException(violations);
        }
    }

    public static JRN of(String value) {
        return new JRN(value);
    }

    public static @Nullable JRN fromCandidate(String value) {
        var violations = VALIDATOR.validate(value);
        return !violations.isEmpty() ? null : new JRN(value);
    }

    public YearMonth yearMonth() {
        Matcher matcher = PATTERN.matcher(value);

        if (!matcher.matches()) {
            throw new IllegalArgumentException("Invalid value: " + value);
        }

        int year = 2000 + Integer.parseInt(matcher.group("year"));
        int month = Integer.parseInt(matcher.group("month"));

        return YearMonth.of(year, month);
    }

    public static List<ValidationResult> validate(String value) {
        return VALIDATOR.validate(value);
    }

    public static final class Validator implements ValueValidator<String> {

        @Override
        public List<ValidationResult> validate(@Nullable String value) {
            var violations = new ArrayList<ValidationResult>();

            if (value == null) {
                violations.add(new ValidationResult("jrn.null", "JRN must not be null"));

                return List.copyOf(violations);
            }

            if (value.isBlank()) {
                violations.add(new ValidationResult("jrn.blank", "JRN must not be blank"));
            }

            if (!PATTERN.matcher(value).matches()) {
                violations.add(new ValidationResult("jrn.format", "JRN must be a valid"));
            }
            return List.copyOf(violations);
        }
    }
}
