package io.github.radek11.dq.output;

/**
 * A fault isolated by the engine and reported next to the results. A failure never stops the
 * run.
 *
 * <p>Two kinds, told apart by type rather than by nullable fields:
 * <pre>{@code
 * switch (failure) {
 *     case RuleFailure f   -> log("rule " + f.ruleId() + " failed on " + f.recordId());
 *     case RecordFailure f -> log("record at " + f.recordIndex() + " rejected");
 * }
 * }</pre>
 */
public sealed interface Failure permits RuleFailure, RecordFailure {

    /** @return zero-based position of the record in the run's input */
    long recordIndex();

    /**
     * @return the exception that caused the failure; its message may contain field values when
     *     it comes from rule logic
     */
    Exception cause();
}
