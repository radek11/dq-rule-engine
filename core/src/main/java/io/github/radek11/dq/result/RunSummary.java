package io.github.radek11.dq.result;

import io.github.radek11.dq.rule.Decision;
import io.github.radek11.dq.rule.Severity;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

/**
 * Counts for a finished run.
 *
 * <p>Note that the severity breakdown counts all results, including {@link Decision#VALID}
 * ones: {@code ERROR = 6} means six results of rules with severity ERROR, not six problems.
 *
 * @param results number of results
 * @param failures number of failures, of both kinds
 * @param byDecision number of results per decision; every decision is present
 * @param bySeverity number of results per severity; every severity is present
 */
public record RunSummary(
        long results,
        long failures,
        Map<Decision, Long> byDecision,
        Map<Severity, Long> bySeverity) {

    public RunSummary {
        byDecision = Collections.unmodifiableMap(new EnumMap<>(Objects.requireNonNull(byDecision, "byDecision")));
        bySeverity = Collections.unmodifiableMap(new EnumMap<>(Objects.requireNonNull(bySeverity, "bySeverity")));
    }
}
