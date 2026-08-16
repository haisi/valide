package li.selman.valide.validator;

/**
 * One broken rule, reported by a {@link ValueValidator}.
 *
 * <p>The code is what callers branch on and what stays stable across wording changes; the message is what a
 * human reads. When the rule is reported through {@link ValidValue}, the message becomes the constraint
 * violation's message template.
 *
 * @param code stable identifier of the broken rule, for example {@code jrn.blank}
 * @param message human-readable description of what is wrong
 */
public record ValidationResult(String code, String message) {}
