package li.selman.valide;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import jakarta.validation.ValidationException;
import java.lang.reflect.Field;
import org.junit.jupiter.api.Test;

class NullnessWalkerTest {

    @Test
    void readsAFieldThatHasBeenMadeAccessible() throws Exception {
        Field field = Secret.class.getDeclaredField("value");
        field.setAccessible(true);

        assertThat(NullnessWalker.readField(field, new Secret("classified"))).isEqualTo("classified");
    }

    @Test
    void wrapsAnInaccessibleFieldInAValidationException() throws Exception {
        Field field = Secret.class.getDeclaredField("value");

        assertThatThrownBy(() -> NullnessWalker.readField(field, new Secret("classified")))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("while validating @NullSafe")
                .hasCauseInstanceOf(IllegalAccessException.class);
    }

    record Secret(String value) {}
}
