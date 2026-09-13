package io.github.radek11.dq.result;

import java.util.Objects;

/**
 * One rule failed on one record. Other rules still ran on that record.
 *
 * @param ruleId id of the failed rule
 * @param recordId id of the record
 * @param recordIndex zero-based position of the record in the run's input
 * @param cause the exception thrown while evaluating the rule
 */
public record RuleFailure(String ruleId, String recordId, long recordIndex, Exception cause) implements Failure {

    public RuleFailure {
        Objects.requireNonNull(ruleId, "ruleId");
        Objects.requireNonNull(recordId, "recordId");
        Objects.requireNonNull(cause, "cause");
        if (recordIndex < 0) {
            throw new IllegalArgumentException("recordIndex must not be negative, was " + recordIndex);
        }
    }
}
