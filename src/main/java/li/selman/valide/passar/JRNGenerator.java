package li.selman.valide.passar;

import java.security.SecureRandom;
import java.time.YearMonth;
import java.util.random.RandomGenerator;

/**
 * Creates new {@link JRN}s for a given month.
 *
 * <p>The five-character code is drawn from a {@link SecureRandom} over the same alphabet the JRN pattern
 * accepts, so a number is not guessable from its neighbours. Uniqueness is a property of the issuing system,
 * not of this class: with 57<sup>5</sup> codes per month a collision is unlikely but not impossible, so a
 * caller that needs a guarantee has to enforce it where the numbers are stored.
 */
public class JRNGenerator {

    // Same alphabet as the code part of JRN: no O, 0, L, I, l.
    private static final char[] CODE_ALPHABET =
            "123456789ABCDEFGHJKMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz".toCharArray();
    private static final int CODE_LENGTH = 5;
    private static final RandomGenerator RANDOM = new SecureRandom();

    private JRNGenerator() {}

    /**
     * Creates a JRN carrying the given month.
     *
     * @param yearMonth the month to encode
     * @return a new JRN with a random code
     * @throws IllegalArgumentException if the year is outside 2000-2099, which a two-digit year cannot encode
     */
    static JRN at(YearMonth yearMonth) {
        int year = yearMonth.getYear();

        if (year < 2000 || year > 2099) {
            throw new IllegalArgumentException("JRN only encodes years 2000-2099, but was: " + year);
        }

        var value = new StringBuilder(9).append(String.format("%02d%02d", year % 100, yearMonth.getMonthValue()));

        for (int i = 0; i < CODE_LENGTH; i++) {
            value.append(CODE_ALPHABET[RANDOM.nextInt(CODE_ALPHABET.length)]);
        }

        return new JRN(value.toString());
    }
}
