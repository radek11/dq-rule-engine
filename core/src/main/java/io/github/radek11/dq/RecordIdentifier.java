package io.github.radek11.dq;

import io.github.radek11.dq.input.DataRecord;
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
        throw new UnsupportedOperationException("E2");
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
