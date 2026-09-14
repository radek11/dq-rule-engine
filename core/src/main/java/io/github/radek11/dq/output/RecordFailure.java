package io.github.radek11.dq.output;

import java.util.Objects;
import java.util.Optional;

/**
 * A record that could not be evaluated at all — no usable id (missing, {@code null} or blank),
 * an id or country that is not text, a record that throws when its id or country is read, or a
 * {@code null} input element. No rule ran on it.
 *
 * @param recordId id of the record, or empty when the record has no usable id
 * @param recordIndex zero-based position of the record in the run's input
 * @param cause what made the record unusable
 */
public record RecordFailure(Optional<String> recordId, long recordIndex, Exception cause) implements Failure {

    public RecordFailure {
        Objects.requireNonNull(recordId, "recordId");
        Objects.requireNonNull(cause, "cause");
        if (recordIndex < 0) {
            throw new IllegalArgumentException("recordIndex must not be negative, was " + recordIndex);
        }
    }
}
