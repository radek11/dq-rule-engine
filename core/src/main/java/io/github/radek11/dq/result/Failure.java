package io.github.radek11.dq.result;

import java.util.Objects;

/**
 * A fault isolated by the engine and reported next to the results. A failure never stops the
 * run.
 *
 * @param kind whether one rule failed on a record, or the whole record was rejected
 * @param ruleId id of the failed rule, or {@code null} for {@link Kind#RECORD}
 * @param recordId id of the record; {@code null} only for {@link Kind#RECORD} when the record
 *     has no usable id
 * @param recordIndex zero-based position of the record in the run's input
 * @param cause the exception that caused the failure; its message may contain field values
 *     when it comes from rule logic
 */
public record Failure(Kind kind, String ruleId, String recordId, long recordIndex, Throwable cause) {

    /** What failed. */
    public enum Kind {
        /** One rule failed on one record; other rules still ran on it. */
        RULE,
        /** The record could not be evaluated at all, for example because it has no id. */
        RECORD
    }

    public Failure {
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(cause, "cause");
        if (kind == Kind.RULE) {
            Objects.requireNonNull(ruleId, "ruleId");
            Objects.requireNonNull(recordId, "recordId");
        }
        if (recordIndex < 0) {
            throw new IllegalArgumentException("recordIndex must not be negative, was " + recordIndex);
        }
    }
}
