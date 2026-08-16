package li.selman.valide;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

/**
 * Drives {@link NullSafe} through a real Bean Validation provider, so the property paths and messages
 * asserted here are the ones a consumer of the library actually observes.
 *
 * <p>The nullness checks are suppressed for the whole class: the fixtures below deliberately hold {@code null}
 * in places a static null checker would reject, which is precisely what {@link NullSafeValidator} exists to
 * catch at runtime. {@code ClassCanBeStatic} likewise: {@code Enclosing.Inner} is inner on purpose, for its
 * synthetic outer-instance field.
 */
@SuppressWarnings({"NullAway", "NullArgumentForNonNullParameter", "ClassCanBeStatic"})
class NullSafeValidatorTest {

    private static final Validator VALIDATOR =
            Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void acceptsARecordWhoseOnlyNullsAreDeclaredNullable() {
        assertThat(pathsOf(new Person("Ada", null, new Address("Main St", null))))
                .isEmpty();
    }

    @Test
    void reportsEachNullAttributeSeparatelyAndRecursively() {
        assertThat(pathsOf(new Person(null, null, new Address(null, null)))).containsExactly("address.street", "name");
    }

    @Test
    void usesTheConstraintMessageForEveryViolation() {
        Set<ConstraintViolation<Person>> violations =
                VALIDATOR.validate(new Person(null, null, new Address(null, null)));

        assertThat(violations).extracting(ConstraintViolation::getMessage).containsOnly("must not be null");
    }

    @Test
    void treatsNullRootAsValidAndLeavesTheContextUntouched() {
        ConstraintValidatorContext context = (ConstraintValidatorContext) Proxy.newProxyInstance(
                ConstraintValidatorContext.class.getClassLoader(),
                new Class<?>[] {ConstraintValidatorContext.class},
                (proxy, method, args) -> {
                    throw new AssertionError("context must not be touched for a null root: " + method.getName());
                });

        assertThat(new NullSafeValidator().isValid(null, context)).isTrue();
    }

    @Test
    void distinguishesNullableArraysFromArraysOfNullableComponents() {
        assertThat(pathsOf(new ArrayHolder())).containsExactly("codes[1]", "grid[1][0]");
    }

    @Test
    void checksCollectionAndMapElementsAgainstTheirTypeArguments() {
        assertThat(pathsOf(new CollectionHolder())).containsExactly("labels[b]", "labels[null]", "names[1]", "tags[1]");
    }

    @Test
    void leavesNullnessUncheckedWhereTheDeclarationDoesNotStateIt() {
        assertThat(pathsOf(new UnknownHolder())).isEmpty();
    }

    @Test
    void doesNotDescendIntoEnumsLambdasOrJdkTypes() {
        assertThat(pathsOf(new LeafHolder())).isEmpty();
    }

    @Test
    void descendsThroughAPresentOptionalAndIgnoresAnAbsentOne() {
        assertThat(pathsOf(new OptionalHolder())).containsExactly("present.street");
    }

    @Test
    void visitsCyclicReferencesOnlyOnce() {
        Node first = new Node(null);
        Node second = new Node(null);
        first.next = second;
        second.next = first;

        assertThat(pathsOf(first)).containsExactly("label", "next.label");
    }

    @Test
    void includesInheritedAttributesButNotStaticOnes() {
        assertThat(pathsOf(new Child())).containsExactly("inherited", "own");
    }

    @Test
    void ignoresTheSyntheticOuterInstanceReferenceOfAnInnerClass() {
        assertThat(pathsOf(new SyntheticHolder())).isEmpty();
    }

    private static List<String> pathsOf(Object bean) {
        return VALIDATOR.validate(bean).stream()
                .map(violation -> violation.getPropertyPath().toString())
                .sorted()
                .toList();
    }

    @NullSafe
    record Person(String name, @Nullable String nickname, Address address) {}

    record Address(String street, @Nullable String zip) {}

    @NullSafe
    static final class ArrayHolder {
        String[] codes = {"a", null};

        @Nullable String[] looseCodes = {"a", null};

        String @Nullable [] absentCodes = null;

        String[][] grid = {{"a"}, {null}};

        int[] numbers = {1, 2};

        Object untypedArray = new String[] {null};
    }

    @NullSafe
    static final class CollectionHolder {
        List<String> tags = Arrays.asList("a", null);

        List<@Nullable String> looseTags = Arrays.asList("a", null);

        Set<String> names = new LinkedHashSet<>(Arrays.asList("x", null));

        Map<String, String> labels = labels();

        Map<String, @Nullable String> looseLabels = looseLabels();

        private static Map<String, String> labels() {
            Map<String, String> map = new LinkedHashMap<>();
            map.put("a", "1");
            map.put("b", null);
            map.put(null, "2");
            return map;
        }

        private static Map<String, String> looseLabels() {
            Map<String, String> map = new LinkedHashMap<>();
            map.put("a", null);
            return map;
        }
    }

    @NullSafe
    static final class UnknownHolder {
        List<?> wildcards = Arrays.asList("a", null);

        Box<String> box = new Box<>();

        Object untypedList = Arrays.asList("a", null);

        Index<String> index = index();

        private static Index<String> index() {
            Index<String> map = new SimpleIndex<>();
            map.put("a", null);
            return map;
        }
    }

    /** A type variable states nothing about nullness, so {@code value} is left unchecked. */
    static final class Box<T> {
        @Nullable T value;
    }

    /** Binds only one of {@link Map}'s two type parameters, so element positions cannot be trusted. */
    interface Index<V> extends Map<String, V> {}

    static final class SimpleIndex<V> extends LinkedHashMap<String, V> implements Index<V> {}

    @NullSafe
    static final class LeafHolder {
        Status status = Status.ACTIVE;

        Runnable action = () -> {};

        String text = "x";

        BigDecimal amount = BigDecimal.ONE;
    }

    enum Status {
        ACTIVE
    }

    @NullSafe
    static final class OptionalHolder {
        Optional<Address> present = Optional.of(new Address(null, null));

        Optional<Address> absent = Optional.empty();
    }

    @NullSafe
    static final class Node {
        @Nullable Node next;

        String label;

        Node(String label) {
            this.label = label;
        }
    }

    static class Base {
        String inherited;
    }

    @NullSafe
    static final class Child extends Base {
        static final String IGNORED = null;

        String own;
    }

    /**
     * {@code Inner} carries a synthetic reference to its {@code Enclosing} instance; walking it would surface
     * {@code Enclosing.secret} under {@code inner.this$0.secret}.
     */
    @NullSafe
    static final class SyntheticHolder {
        Enclosing.Inner inner = new Enclosing().inner();
    }

    static final class Enclosing {
        String secret = null;

        Inner inner() {
            return new Inner();
        }

        final class Inner {
            String innerValue = "ok";
        }
    }
}
