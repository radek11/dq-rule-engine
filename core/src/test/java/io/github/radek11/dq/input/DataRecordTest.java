package io.github.radek11.dq.input;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class DataRecordTest {

    @Test
    void theMapIsCopiedSoLaterChangesDoNotReachTheRecord() {
        Map<String, Object> fields = new HashMap<>(Map.of("id", "r1"));
        DataRecord record = DataRecord.of(fields);

        fields.put("id", "changed");
        fields.put("vatId", "DE111111111");

        assertThat(record.get("id")).isEqualTo("r1");
        assertThat(record.contains("vatId")).isFalse();
    }

    @Test
    void aFieldHoldingNullIsPresentWhileAnAbsentFieldIsNot() {
        Map<String, Object> fields = new HashMap<>();
        fields.put("vatId", null);
        DataRecord record = DataRecord.of(fields);

        assertThat(record.contains("vatId")).isTrue();
        assertThat(record.get("vatId")).isNull();
        assertThat(record.contains("iban")).isFalse();
    }

    @Test
    void toStringNamesTheFieldsButNotTheirValues() {
        DataRecord record = DataRecord.of(Map.of("legalName", "ACME GmbH"));

        assertThat(record.toString()).contains("legalName").doesNotContain("ACME");
    }
}
