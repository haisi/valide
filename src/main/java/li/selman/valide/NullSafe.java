package li.selman.valide;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Asserts that no attribute reachable from the annotated type holds {@code null}, unless its declared type
 * is annotated with JSpecify's {@link org.jspecify.annotations.Nullable @Nullable}.
 *
 * <p>Because {@code @Nullable} is a {@code TYPE_USE} annotation, it says precisely <em>which</em> part of a
 * declaration may be null, and this constraint honours that distinction:
 *
 * <pre>{@code
 * @NullSafe
 * record Order(
 *         String id,                              // must not be null
 *         @Nullable String note,                  // may be null
 *         List<String> tags,                      // list and every element must not be null
 *         List<@Nullable String> optionalTags,    // list must not be null, elements may be
 *         @Nullable List<String> extras,          // list may be null, its elements may not
 *         String[] codes,                         // array and every element must not be null
 *         String @Nullable [] altCodes,           // array may be null, its elements may not
 *         Map<String, @Nullable Address> shipTo,  // keys must not be null, values may be
 *         Customer customer) {}                   // must not be null, and is validated recursively
 * }</pre>
 *
 * <p>Validation is recursive: every non-null attribute value is walked in turn, descending through arrays,
 * {@link Iterable}s, {@link java.util.Map}s and {@link java.util.Optional}s into the objects they hold.
 * Types owned by the JDK ({@code java.*}, {@code javax.*}, {@code jakarta.*}, {@code jdk.*}, {@code sun.*},
 * {@code com.sun.*}), enum constants and lambdas are treated as opaque leaves and are not descended into.
 * Shared and cyclic references are visited once, so a parent/child back-reference does not loop.
 *
 * <p>Each offending attribute produces its own {@link jakarta.validation.ConstraintViolation}, whose property
 * path locates it from the annotated root, for example {@code customer.address.street} or
 * {@code lines[2].sku}. Container positions are rendered into the path segment they belong to rather than as
 * separate iterable nodes, so {@code getPropertyPath().toString()} reads as expected while
 * {@link jakarta.validation.Path.Node#getKind()} reports {@code PROPERTY} throughout.
 *
 * <p>Nullness is only asserted where the declaration states it. It is left unchecked for type variables
 * ({@code T value}), wildcards ({@code List<?> items}), and containers whose element type cannot be read off
 * the declaration - a raw {@code List}, or a subtype that re-binds its supertype's type parameters such as
 * {@code interface Index<V> extends Map<String, V>}. Generic type arguments are read from the declaration
 * itself and are not resolved through the supertype hierarchy, so {@code class Tags extends ArrayList<String>}
 * carries no element type information either.
 *
 * <p>Unlike {@link jakarta.validation.Valid @Valid}, nested types need not be annotated themselves - the walk
 * descends into them regardless.
 */
@Documented
@Constraint(validatedBy = NullSafeValidator.class)
@Target({TYPE, ANNOTATION_TYPE})
@Retention(RUNTIME)
public @interface NullSafe {

    String message() default "must not be null";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
