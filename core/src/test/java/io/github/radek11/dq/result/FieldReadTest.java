package io.github.radek11.dq.result;

import org.junit.jupiter.api.Test;

import static io.github.radek11.dq.result.FieldRead.Presence.MISSING;
import static io.github.radek11.dq.result.FieldRead.Presence.NULL;
import static io.github.radek11.dq.result.FieldRead.Presence.PRESENT;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNoException;

class FieldReadTest {

    @Test
    void valueIsSetExactlyWhenTheFieldIsPresent() {
        assertThatNoException().isThrownBy(() -> new FieldRead("iban", PRESENT, ""));
        assertThatNoException().isThrownBy(() -> new FieldRead("iban", MISSING, null));
        assertThatNoException().isThrownBy(() -> new FieldRead("iban", NULL, null));
    }

    @Test
    void presentWithoutValueIsRejected() {
        assertThatIllegalArgumentException().isThrownBy(() -> new FieldRead("iban", PRESENT, null));
    }

    @Test
    void absentWithValueIsRejected() {
        assertThatIllegalArgumentException().isThrownBy(() -> new FieldRead("iban", MISSING, "DE1"));
        assertThatIllegalArgumentException().isThrownBy(() -> new FieldRead("iban", NULL, "DE1"));
    }
}
