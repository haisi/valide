package li.selman.valide.validator;

import java.util.List;
import org.jspecify.annotations.Nullable;

public interface ValueValidator<T> {
    List<ValidationResult> validate(@Nullable T value);
}
