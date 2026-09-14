package io.github.radek11.dq;

import io.github.radek11.dq.input.DataRecord;
import io.github.radek11.dq.input.FieldTypeException;
import io.github.radek11.dq.input.MissingFieldException;
import io.github.radek11.dq.output.RecordFailure;

import java.util.Optional;

/**
 * Reads what the engine needs before any rule runs on a record: its id and its country. A record
 * without a usable id cannot be named in a result, so it is rejected as a whole.
 */
final class RecordIdentifier {

    private final String idField;
    private final String countryField;

    /**
     * @param idField name of the field holding the record id
     * @param countryField name of the field holding the record country
     */
    RecordIdentifier(String idField, String countryField) {
        this.idField = idField;
        this.countryField = countryField;
    }

    /**
     * @param record an input element, possibly {@code null}
     * @param index zero-based position of the record in the run's input
     * @return the record with its id and country, or the reason it was rejected
     */
    Identification identify(DataRecord record, long index) {
        if (record == null) {
            return rejected(Optional.empty(), index, new NullPointerException("Input element is null"));
        }
        // Reads can fail in the host's DataRecord as well as with FieldTypeException; either way
        // it is a bad record, isolated like a failing rule.
        Optional<String> id;
        try {
            id = text(record, idField);
        } catch (Exception e) {
            return rejected(Optional.empty(), index, e);
        }
        if (id.isEmpty()) {
            return rejected(Optional.empty(), index, new MissingFieldException(idField));
        }
        if (id.get().isBlank()) {
            return rejected(Optional.empty(), index,
                    new IllegalArgumentException("Record id in field " + idField + " is blank"));
        }
        try {
            return new Identified(record, id.get(), text(record, countryField));
        } catch (Exception e) {
            return rejected(id, index, e);
        }
    }

    /** @return the field's text, empty when it is missing or {@code null} */
    private static Optional<String> text(DataRecord record, String field) {
        if (!record.contains(field)) {
            return Optional.empty();
        }
        Object value = record.get(field);
        if (value == null) {
            return Optional.empty();
        }
        if (value instanceof String text) {
            return Optional.of(text);
        }
        throw new FieldTypeException(field, value.getClass());
    }

    private static Rejected rejected(Optional<String> recordId, long index, Exception cause) {
        return new Rejected(new RecordFailure(recordId, index, cause));
    }

    /** What identification found; the two cases are told apart by type. */
    sealed interface Identification permits Identified, Rejected {
    }

    /**
     * @param record the record
     * @param id non-blank record id
     * @param country the record's country as given, empty when the field is missing or {@code null}
     */
    record Identified(DataRecord record, String id, Optional<String> country) implements Identification {
    }

    /** @param failure why no rule can run on the record */
    record Rejected(RecordFailure failure) implements Identification {
    }
}
