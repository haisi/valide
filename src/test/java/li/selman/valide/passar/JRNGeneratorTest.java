package li.selman.valide.passar;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.YearMonth;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class JRNGeneratorTest {

    @ParameterizedTest
    @ValueSource(strings = {"2000-01", "2025-07", "2099-12"})
    void encodes_the_given_year_month(String yearMonth) {
        var expected = YearMonth.parse(yearMonth);

        var jrn = JRNGenerator.at(expected);

        assertThat(jrn.yearMonth()).isEqualTo(expected);
    }

    @Test
    void generates_values_accepted_by_the_validator() {
        var values = IntStream.range(0, 1_000)
                .mapToObj(i -> JRNGenerator.at(YearMonth.of(2025, 7)).value())
                .toList();

        assertThat(values).allSatisfy(value -> assertThat(JRN.validate(value)).isEmpty());
    }

    @Test
    void generates_distinct_codes() {
        var distinct = IntStream.range(0, 100)
                .mapToObj(i -> JRNGenerator.at(YearMonth.of(2025, 7)).value())
                .collect(Collectors.toSet());

        assertThat(distinct).hasSizeGreaterThan(1);
    }

    @Test
    void every_alphabet_character_is_valid_in_a_code() {
        var alphabet = "123456789ABCDEFGHJKMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz";

        assertThat(alphabet).doesNotContain("0", "O", "I", "L", "l");
        assertThat(alphabet.chars().distinct().count()).isEqualTo(alphabet.length());
        assertThat(alphabet.chars()).allSatisfy(c -> assertThat(JRN.validate(
                        "2507" + String.valueOf((char) c.intValue()).repeat(5)))
                .isEmpty());
    }

    @ParameterizedTest
    @ValueSource(strings = {"1999-12", "2100-01"})
    void rejects_years_outside_the_two_digit_range(String yearMonth) {
        var out_of_range = YearMonth.parse(yearMonth);

        assertThatThrownBy(() -> JRNGenerator.at(out_of_range)).isInstanceOf(IllegalArgumentException.class);
    }
}
