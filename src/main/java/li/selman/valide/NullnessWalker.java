package li.selman.valide;

import com.google.errorprone.annotations.Var;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.ConstraintValidatorContext.ConstraintViolationBuilder.NodeBuilderCustomizableContext;
import jakarta.validation.ValidationException;
import java.lang.reflect.AnnotatedArrayType;
import java.lang.reflect.AnnotatedParameterizedType;
import java.lang.reflect.AnnotatedType;
import java.lang.reflect.AnnotatedTypeVariable;
import java.lang.reflect.AnnotatedWildcardType;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.jspecify.annotations.Nullable;

/**
 * A single depth-first pass over one object graph, reporting every attribute that holds {@code null} where its
 * declaration does not allow it. One instance validates one root object; it is stateful and not reusable.
 */
final class NullnessWalker {

    /** Packages whose types are treated as opaque leaves rather than walked. */
    private static final List<String> LEAF_PACKAGE_PREFIXES =
            List.of("java.", "javax.", "jakarta.", "jdk.", "sun.", "com.sun.");

    private final ConstraintValidatorContext context;

    /** Objects already descended into, by identity, so shared and cyclic references are visited once. */
    private final Set<Object> visited = Collections.newSetFromMap(new IdentityHashMap<>());

    /** Property path segments from the root down to the value currently being checked. */
    private final Deque<String> path = new ArrayDeque<>();

    private boolean valid = true;

    NullnessWalker(ConstraintValidatorContext context) {
        this.context = context;
    }

    /** Walks {@code root}'s attributes and returns whether all of them satisfied their declared nullness. */
    boolean walk(Object root) {
        visited.add(root);
        walkAttributes(root);
        return valid;
    }

    /**
     * Reads {@code field} off {@code owner}.
     *
     * <p>Package-private rather than inlined into {@link #walkAttributes} so the failure branch stays
     * reachable from a test: once {@code setAccessible} has succeeded, {@code get} can no longer fail.
     */
    static @Nullable Object readField(Field field, Object owner) {
        try {
            return field.get(owner);
        } catch (IllegalAccessException e) {
            throw new ValidationException("Cannot read " + field + " while validating @NullSafe", e);
        }
    }

    /** Checks one value against the nullness its declaration states, then descends into it. */
    private void check(@Nullable Object value, @Nullable AnnotatedType declaredType) {
        if (value == null) {
            if (mustBeNonNull(declaredType)) {
                report();
            }
            return;
        }
        descend(value, declaredType);
    }

    /** Walks into a non-null value: through a container's elements, or over a bean's own attributes. */
    private void descend(Object value, @Nullable AnnotatedType declaredType) {
        if (!visited.add(value)) {
            return;
        }
        if (value instanceof Optional<?> optional) {
            // An absent Optional is not a null attribute; only a present value is worth descending into.
            optional.ifPresent(element -> descend(element, typeArgument(declaredType, 0, 1)));
        } else if (value.getClass().isArray()) {
            walkArray(value, declaredType);
        } else if (value instanceof Map<?, ?> map) {
            walkMap(map, declaredType);
        } else if (value instanceof Iterable<?> iterable) {
            walkIterable(iterable, declaredType);
        } else if (!isLeaf(value.getClass())) {
            walkAttributes(value);
        }
    }

    private void walkArray(Object array, @Nullable AnnotatedType declaredType) {
        // `@Nullable String[]` annotates the component; `String @Nullable []` annotates the array itself,
        // which check() has already handled by the time we get here.
        AnnotatedType componentType = declaredType instanceof AnnotatedArrayType arrayType
                ? arrayType.getAnnotatedGenericComponentType()
                : null;
        int length = Array.getLength(array);
        for (int index = 0; index < length; index++) {
            checkElement(Array.get(array, index), componentType, index);
        }
    }

    private void walkMap(Map<?, ?> map, @Nullable AnnotatedType declaredType) {
        AnnotatedType keyType = typeArgument(declaredType, 0, 2);
        AnnotatedType valueType = typeArgument(declaredType, 1, 2);
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            checkElement(entry.getKey(), keyType, entry.getKey());
            checkElement(entry.getValue(), valueType, entry.getKey());
        }
    }

    private void walkIterable(Iterable<?> iterable, @Nullable AnnotatedType declaredType) {
        AnnotatedType elementType = typeArgument(declaredType, 0, 1);
        @Var int index = 0;
        for (Object element : iterable) {
            checkElement(element, elementType, index++);
        }
    }

    private void walkAttributes(Object bean) {
        for (Field field : attributesOf(bean.getClass())) {
            field.setAccessible(true);
            path.addLast(field.getName());
            check(readField(field, bean), field.getAnnotatedType());
            path.removeLast();
        }
    }

    /**
     * Checks a container element, reporting it under the enclosing property's own path segment extended with
     * {@code position} - so nesting reads as {@code grid[0][1]} rather than needing a node per level.
     */
    private void checkElement(
            @Nullable Object element, @Nullable AnnotatedType declaredType, @Nullable Object position) {
        String segment = path.removeLast();
        path.addLast(segment + "[" + position + "]");
        check(element, declaredType);
        path.removeLast();
        path.addLast(segment);
    }

    private void report() {
        valid = false;
        Iterator<String> segments = path.iterator();
        @Var
        NodeBuilderCustomizableContext node = context.buildConstraintViolationWithTemplate(
                        context.getDefaultConstraintMessageTemplate())
                .addPropertyNode(segments.next());
        while (segments.hasNext()) {
            node = node.addPropertyNode(segments.next());
        }
        node.addConstraintViolation();
    }

    /** Every non-static, non-synthetic field declared by {@code type} or by one of its non-leaf supertypes. */
    private static List<Field> attributesOf(Class<?> type) {
        List<Field> attributes = new ArrayList<>();
        // Object is a leaf, so the walk always stops before getSuperclass() could return null.
        for (@Var Class<?> current = type; !isLeaf(current); current = current.getSuperclass()) {
            for (Field field : current.getDeclaredFields()) {
                if (!field.isSynthetic() && !Modifier.isStatic(field.getModifiers())) {
                    attributes.add(field);
                }
            }
        }
        return attributes;
    }

    /** Whether {@code type} is opaque to this walk: a JDK type, an enum constant, or a lambda. */
    private static boolean isLeaf(Class<?> type) {
        if (type.isHidden() || Enum.class.isAssignableFrom(type)) {
            return true;
        }
        String name = type.getName();
        return LEAF_PACKAGE_PREFIXES.stream().anyMatch(name::startsWith);
    }

    /**
     * The {@code index}-th annotated type argument of {@code declaredType}, or {@code null} when the
     * declaration carries no usable element type.
     *
     * <p>{@code arity} guards against reading arguments off a subtype that partially binds or re-orders its
     * supertype's type parameters, as {@code interface Index<V> extends Map<String, V>} does: unless the
     * declaration binds exactly as many arguments as the container has parameters, positions cannot be
     * trusted and element nullness is left unchecked rather than guessed at.
     */
    private static @Nullable AnnotatedType typeArgument(@Nullable AnnotatedType declaredType, int index, int arity) {
        if (declaredType instanceof AnnotatedParameterizedType parameterized) {
            AnnotatedType[] arguments = parameterized.getAnnotatedActualTypeArguments();
            if (arguments.length == arity) {
                return arguments[index];
            }
        }
        return null;
    }

    /** Whether a declaration states that its value may not be null. Unstated nullness is not enforced. */
    private static boolean mustBeNonNull(@Nullable AnnotatedType declaredType) {
        return declaredType != null
                && !(declaredType instanceof AnnotatedWildcardType)
                && !(declaredType instanceof AnnotatedTypeVariable)
                && !declaredType.isAnnotationPresent(Nullable.class);
    }
}
