package io.github.radek11.dq;

import java.util.Objects;

/**
 * A fault isolated by the engine and reported next to the results. A failure never stops the
 * run.
 *
 * @param kind whether one rule failed on a record, or the whole record was rejected
 * @param ruleId id of the failed rule, or {@code null} for {@link Kind#RECORD}
 * @param recordId id of the record, or {@code null} when the record has no id
 * @param recordIndex zero-based position of the record in the run's input
 * @param cause the exception that caused the failure
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
        }
    }
}
