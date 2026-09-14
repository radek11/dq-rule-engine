package io.github.radek11.dq;

import io.github.radek11.dq.RecordIdentifier.Identification;
import io.github.radek11.dq.RecordIdentifier.Identified;
import io.github.radek11.dq.RecordIdentifier.Rejected;
import io.github.radek11.dq.input.DataRecord;
import io.github.radek11.dq.input.FieldTypeException;
import io.github.radek11.dq.input.MissingFieldException;
import io.github.radek11.dq.output.RecordFailure;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class RecordIdentifierTest {

    private final RecordIdentifier identifier = new RecordIdentifier("id", "country");

    @Test
    void aRecordWithTextIdAndCountryIsIdentified() {
        DataRecord record = DataRecord.of(Map.of("id", "r1", "country", "DE"));

        Identification identification = identifier.identify(record, 0);

        assertThat(identification).isEqualTo(new Identified(record, "r1", Optional.of("DE")));
    }

    @Test
    void aMissingOrNullCountryIsNoCountryNotAFailure() {
        DataRecord missing = DataRecord.of(Map.of("id", "r1"));
        DataRecord holdsNull = DataRecord.of(withNull("country", Map.of("id", "r1")));

        assertThat(identifier.identify(missing, 0)).isEqualTo(new Identified(missing, "r1", Optional.empty()));
        assertThat(identifier.identify(holdsNull, 0)).isEqualTo(new Identified(holdsNull, "r1", Optional.empty()));
    }

    @Test
    void theConfiguredFieldNamesAreRead() {
        DataRecord record = DataRecord.of(Map.of("partnerId", "p7", "land", "PL", "id", "ignored"));

        Identification identification = new RecordIdentifier("partnerId", "land").identify(record, 0);

        assertThat(identification).isEqualTo(new Identified(record, "p7", Optional.of("PL")));
    }

    @Test
    void aNullElementIsRejectedWithoutAnId() {
        RecordFailure failure = rejected(identifier.identify(null, 5));

        assertThat(failure.recordId()).isEmpty();
        assertThat(failure.recordIndex()).isEqualTo(5);
        assertThat(failure.cause()).isInstanceOf(NullPointerException.class);
    }

    @Test
    void aMissingOrNullIdIsRejectedAsAMissingField() {
        RecordFailure missing = rejected(identifier.identify(DataRecord.of(Map.of("country", "DE")), 2));
        RecordFailure holdsNull = rejected(identifier.identify(DataRecord.of(withNull("id", Map.of())), 3));

        assertThat(missing.recordId()).isEmpty();
        assertThat(missing.recordIndex()).isEqualTo(2);
        assertThat(missing.cause()).isInstanceOfSatisfying(MissingFieldException.class,
                cause -> assertThat(cause.fieldName()).isEqualTo("id"));
        assertThat(holdsNull.cause()).isInstanceOf(MissingFieldException.class);
    }

    @Test
    void aBlankIdIsRejected() {
        RecordFailure failure = rejected(identifier.identify(DataRecord.of(Map.of("id", "  ")), 0));

        assertThat(failure.recordId()).isEmpty();
        assertThat(failure.cause()).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("id");
    }

    @Test
    void anIdThatIsNotTextIsRejectedWithoutAnId() {
        RecordFailure failure = rejected(identifier.identify(DataRecord.of(Map.of("id", 42)), 0));

        assertThat(failure.recordId()).isEmpty();
        assertThat(failure.cause()).isInstanceOfSatisfying(FieldTypeException.class,
                cause -> assertThat(cause.fieldName()).isEqualTo("id"));
    }

    @Test
    void aCountryThatIsNotTextIsRejectedWithTheId() {
        RecordFailure failure = rejected(identifier.identify(DataRecord.of(Map.of("id", "r1", "country", 49)), 0));

        assertThat(failure.recordId()).contains("r1");
        assertThat(failure.cause()).isInstanceOfSatisfying(FieldTypeException.class,
                cause -> assertThat(cause.fieldName()).isEqualTo("country"));
    }

    @Test
    void aRecordThatThrowsWhenItsIdIsReadIsRejectedWithoutAnId() {
        IllegalStateException unreadable = new IllegalStateException("lazy field could not be loaded");

        RecordFailure failure = rejected(identifier.identify(throwingOn("id", unreadable), 4));

        assertThat(failure.recordId()).isEmpty();
        assertThat(failure.recordIndex()).isEqualTo(4);
        assertThat(failure.cause()).isSameAs(unreadable);
    }

    @Test
    void aRecordThatThrowsWhenItsCountryIsReadIsRejectedWithTheId() {
        IllegalStateException unreadable = new IllegalStateException("lazy field could not be loaded");

        RecordFailure failure = rejected(identifier.identify(throwingOn("country", unreadable), 0));

        assertThat(failure.recordId()).contains("r1");
        assertThat(failure.cause()).isSameAs(unreadable);
    }

    /** A host record whose id is "r1" and which throws when the given field is read. */
    private static DataRecord throwingOn(String field, RuntimeException exception) {
        return new DataRecord() {
            @Override
            public boolean contains(String name) {
                return true;
            }

            @Override
            public Object get(String name) {
                if (name.equals(field)) {
                    throw exception;
                }
                return "r1";
            }
        };
    }

    private static RecordFailure rejected(Identification identification) {
        assertThat(identification).isInstanceOf(Rejected.class);
        return ((Rejected) identification).failure();
    }

    private static Map<String, Object> withNull(String field, Map<String, ?> others) {
        Map<String, Object> fields = new HashMap<>(others);
        fields.put(field, null);
        return fields;
    }
}
