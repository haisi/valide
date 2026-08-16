/**
 * Value objects that carry their own rules, and expose them to Bean Validation.
 *
 * <p>A value object states its rules once, in a {@link li.selman.valide.validator.ValueValidator}, which
 * reports one {@link li.selman.valide.validator.ValidationResult} per broken rule instead of stopping at the
 * first. Two things then run those same rules:
 *
 * <ul>
 *   <li>the value object's own constructor, which rejects a bad raw value with a
 *       {@link li.selman.valide.validator.ValueObjectValidationException}, so an instance that exists is
 *       always well-formed;
 *   <li>the {@link li.selman.valide.validator.ValidValue} constraint, which reports each broken rule as its
 *       own {@link jakarta.validation.ConstraintViolation} on the bean that holds the raw value - typically an
 *       inbound request, before it has been turned into value objects.
 * </ul>
 *
 * <p>Because both paths share one implementation, a rule can never hold at the boundary but not in the domain.
 * {@link li.selman.valide.passar.JRN} is a worked example: its {@code JRN.Validator} is used by the record's
 * constructor and by the {@code @ValidJRN} constraint alike.
 */
@NullMarked
package li.selman.valide.validator;

import org.jspecify.annotations.NullMarked;
