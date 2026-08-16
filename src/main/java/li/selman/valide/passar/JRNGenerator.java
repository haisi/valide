package li.selman.valide.passar;

import java.security.SecureRandom;
import java.time.YearMonth;
import java.util.random.RandomGenerator;

public class JRNGenerator {

    // Same alphabet as the code part of JRN: no O, 0, L, I, l.
    private static final char[] CODE_ALPHABET =
            "123456789ABCDEFGHJKMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz".toCharArray();
    private static final int CODE_LENGTH = 5;
    private static final RandomGenerator RANDOM = new SecureRandom();

    private JRNGenerator() {}

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
