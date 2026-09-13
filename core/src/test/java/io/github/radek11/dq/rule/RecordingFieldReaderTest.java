package io.github.radek11.dq.rule;

import io.github.radek11.dq.data.DataRecord;
import io.github.radek11.dq.data.FieldTypeException;
import io.github.radek11.dq.data.MissingFieldException;
import io.github.radek11.dq.result.FieldRead;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static io.github.radek11.dq.result.FieldRead.Presence.MISSING;
import static io.github.radek11.dq.result.FieldRead.Presence.NULL;
import static io.github.radek11.dq.result.FieldRead.Presence.PRESENT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RecordingFieldReaderTest {

    private static DataRecord recordWith(String name, Object value) {
        Map<String, Object> fields = new HashMap<>();
        fields.put(name, value);
        return DataRecord.of(fields);
    }

    @Test
    void textReturnsAPresentValueAndRecordsIt() {
        var reader = new RecordingFieldReader(recordWith("country", "DE"));

        assertThat(reader.text("country")).contains("DE");
        assertThat(reader.reads()).containsExactly(new FieldRead("country", PRESENT, "DE"));
    }

    @Test
    void textOfAMissingFieldIsEmptyAndRecordedAsMissing() {
        var reader = new RecordingFieldReader(recordWith("country", "DE"));

        assertThat(reader.text("vatId")).isEmpty();
        assertThat(reader.reads()).containsExactly(new FieldRead("vatId", MISSING, null));
    }

    @Test
    void textOfANullFieldIsEmptyAndRecordedAsNull() {
        var reader = new RecordingFieldReader(recordWith("vatId", null));

        assertThat(reader.text("vatId")).isEmpty();
        assertThat(reader.reads()).containsExactly(new FieldRead("vatId", NULL, null));
    }

    @Test
    void anEmptyStringIsPresentNotMissing() {
        var reader = new RecordingFieldReader(recordWith("iban", ""));

        assertThat(reader.text("iban")).contains("");
        assertThat(reader.reads()).containsExactly(new FieldRead("iban", PRESENT, ""));
    }

    @Test
    void requiredTextReturnsAPresentValue() {
        var reader = new RecordingFieldReader(recordWith("country", "DE"));

        assertThat(reader.requiredText("country")).isEqualTo("DE");
        assertThat(reader.reads()).containsExactly(new FieldRead("country", PRESENT, "DE"));
    }

    @Test
    void requiredTextOfAMissingFieldThrowsNamingTheField() {
        var reader = new RecordingFieldReader(recordWith("country", "DE"));

        assertThatThrownBy(() -> reader.requiredText("vatId"))
                .isInstanceOf(MissingFieldException.class)
                .extracting(e -> ((MissingFieldException) e).fieldName())
                .isEqualTo("vatId");
    }

    @Test
    void requiredTextOfANullFieldThrows() {
        var reader = new RecordingFieldReader(recordWith("vatId", null));

        assertThatThrownBy(() -> reader.requiredText("vatId")).isInstanceOf(MissingFieldException.class);
    }

    @Test
    void aNonTextValueThrowsWithoutRevealingTheValue() {
        var reader = new RecordingFieldReader(recordWith("vatId", 123456789));

        assertThatThrownBy(() -> reader.text("vatId"))
                .isInstanceOf(FieldTypeException.class)
                .hasMessageContaining("vatId")
                .hasMessageNotContaining("123456789");
        assertThatThrownBy(() -> reader.requiredText("vatId")).isInstanceOf(FieldTypeException.class);
    }

    @Test
    void readsKeepTheirOrderAndRepeats() {
        var reader = new RecordingFieldReader(DataRecord.of(Map.of("iban", "DE1", "country", "DE")));

        reader.text("iban");
        reader.text("country");
        reader.text("iban");

        assertThat(reader.reads()).extracting(FieldRead::name).containsExactly("iban", "country", "iban");
    }
}
