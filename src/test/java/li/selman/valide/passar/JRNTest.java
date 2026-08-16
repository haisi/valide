package li.selman.valide.passar;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.assertj.core.api.Assertions.tuple;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.time.YearMonth;
import java.util.stream.Stream;
import li.selman.valide.validator.ValidationResult;
import li.selman.valide.validator.ValueObjectValidationException;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Covers {@link JRN} from three angles: the value object itself, its {@link JRN.Validator} (whose
 * {@link ValidationResult} codes are what callers branch on), and the {@link ValidJRN} bean-validation
 * constraint driven through a real provider.
 *
 * <p>Nullness checks are suppressed for the whole class: several tests deliberately pass {@code null} where a
 * static null checker would reject it, because {@link JRN.Validator} accepts {@code @Nullable} input and
 * reports it as a violation at runtime.
 */
@SuppressWarnings({"NullAway", "NullArgumentForNonNullParameter"})
class JRNTest {

    private static final Validator BEAN_VALIDATOR =
            Validation.buildDefaultValidatorFactory().getValidator();

    /** A JRN that is valid, so tests about *other* aspects do not accidentally depend on a rejection. */
    private static final String VALID = "2507duTaA";

    @Nested
    class Accepted {

        static Stream<String> validValues() {
            return Stream.of(
                    VALID,
                    "0001abcde", // earliest encodable year, first month
                    "9912zyxwv", // latest encodable year, last month
                    "251299999", // an all-digit code
                    "2507aiodz"); // lowercase i and o are allowed - only uppercase I/O and lowercase l are not
        }

        @ParameterizedTest
        @MethodSource("validValues")
        void keeps_the_value_verbatim(String value) {
            assertThat(JRN.of(value).value()).isEqualTo(value);
        }

        @ParameterizedTest
        @MethodSource("validValues")
        void reports_no_violations(String value) {
            assertThat(JRN.validate(value)).isEmpty();
        }

        @Test
        void equal_values_are_equal() {
            assertThat(JRN.of(VALID)).isEqualTo(new JRN(VALID)).hasSameHashCodeAs(new JRN(VALID));
        }

        @Test
        void is_case_sensitive() {
            assertThat(new JRN("2507duTaA")).isNotEqualTo(new JRN("2507duTaa"));
        }
    }

    @Nested
    class Rejected {

        @ParameterizedTest
        @ValueSource(
                strings = {
                    "",
                    "   ",
                    "garbage",
                    "2507duTa", // one character too short
                    "2507duTaAB", // one character too long
                    "2500duTaA", // month 00
                    "2513duTaA", // month 13
                    "25a7duTaA", // non-numeric year
                    "2507duTa0", // zero is excluded from the code alphabet
                    "2507duTaO", // uppercase O is excluded
                    "2507duTaI", // uppercase I is excluded
                    "2507duTaL", // uppercase L is excluded
                    "2507duTal", // lowercase l is excluded
                    "2507duTa-", // punctuation is not part of the alphabet
                    " 2507duTaA", // leading whitespace is not trimmed away
                    "2507duTaA ", // trailing whitespace is not trimmed away
                    "2507duTaA\nX", // the pattern anchors the whole input, not a single line
                })
        void constructor_throws(String value) {
            assertThatThrownBy(() -> new JRN(value)).isInstanceOf(ValueObjectValidationException.class);
        }

        @Test
        void constructor_rejects_null() {
            assertThatThrownBy(() -> new JRN(null)).isInstanceOf(ValueObjectValidationException.class);
        }

        @Test
        void the_exception_carries_every_violation() {
            var thrown = catchThrowableOfType(ValueObjectValidationException.class, () -> new JRN(""));

            assertThat(thrown.violations())
                    .extracting(ValidationResult::code, ValidationResult::message)
                    .containsExactlyInAnyOrder(
                            tuple("jrn.blank", "JRN must not be blank"), tuple("jrn.format", "JRN must be a valid"));
        }
    }

    @Nested
    class Validate {

        @Test
        void reports_null_as_its_own_code_and_stops_there() {
            assertThat(JRN.validate(null))
                    .extracting(ValidationResult::code, ValidationResult::message)
                    .containsExactly(tuple("jrn.null", "JRN must not be null"));
        }

        @ParameterizedTest
        @ValueSource(strings = {"", "   "})
        void reports_blank_and_format_for_blank_input(String value) {
            assertThat(JRN.validate(value))
                    .extracting(ValidationResult::code)
                    .containsExactlyInAnyOrder("jrn.blank", "jrn.format");
        }

        @Test
        void reports_only_the_format_code_for_a_non_blank_mismatch() {
            assertThat(JRN.validate("garbage"))
                    .extracting(ValidationResult::code)
                    .containsExactly("jrn.format");
        }
    }

    @Nested
    class FromCandidate {

        @Test
        void returns_a_jrn_for_a_valid_value() {
            assertThat(JRN.fromCandidate(VALID)).isEqualTo(JRN.of(VALID));
        }

        @ParameterizedTest
        @ValueSource(strings = {"", "garbage", "2507duTal"})
        void returns_null_instead_of_throwing(String value) {
            assertThat(JRN.fromCandidate(value)).isNull();
        }

        @Test
        void returns_null_for_null() {
            assertThat(JRN.fromCandidate(null)).isNull();
        }
    }

    @Nested
    class YearMonthDecoding {

        @ParameterizedTest
        @CsvSource({"0001abcde, 2000-01", "2507duTaA, 2025-07", "9912zyxwv, 2099-12"})
        void reads_the_year_and_month_prefix(String value, String expected) {
            assertThat(JRN.of(value).yearMonth()).isEqualTo(YearMonth.parse(expected));
        }
    }

    @Nested
    class BeanValidation {

        record CreateJourneyRequest(@ValidJRN String jrn) {}

        @Test
        void accepts_a_valid_jrn() {
            assertThat(BEAN_VALIDATOR.validate(new CreateJourneyRequest(VALID))).isEmpty();
        }

        @Test
        void exposes_all_validation_results_as_bean_violations() {
            var violations = BEAN_VALIDATOR.validate(new CreateJourneyRequest(""));

            assertThat(violations)
                    .extracting(ConstraintViolation::getMessage)
                    .containsExactlyInAnyOrder("JRN must not be blank", "JRN must be a valid");
        }

        @Test
        void replaces_the_default_constraint_message() {
            var violations = BEAN_VALIDATOR.validate(new CreateJourneyRequest("garbage"));

            assertThat(violations).extracting(ConstraintViolation::getMessage).containsExactly("JRN must be a valid");
        }

        @Test
        void reports_a_null_value_rather_than_skipping_it() {
            var violations = BEAN_VALIDATOR.validate(new CreateJourneyRequest(null));

            assertThat(violations).extracting(ConstraintViolation::getMessage).containsExactly("JRN must not be null");
        }

        @Test
        void points_at_the_annotated_property() {
            var violations = BEAN_VALIDATOR.validate(new CreateJourneyRequest(""));

            assertThat(violations)
                    .extracting(violation -> violation.getPropertyPath().toString())
                    .containsOnly("jrn");
        }
    }
}
