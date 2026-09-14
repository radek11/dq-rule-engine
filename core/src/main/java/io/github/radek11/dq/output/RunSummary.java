package io.github.radek11.dq.output;

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
 * @param byDecision number of results per decision; every decision is present, missing keys
 *     are stored as zero
 * @param bySeverity number of results per severity; every severity is present, missing keys
 *     are stored as zero
 */
public record RunSummary(
        long results,
        long failures,
        Map<Decision, Long> byDecision,
        Map<Severity, Long> bySeverity) {

    public RunSummary {
        byDecision = complete(Decision.class, Objects.requireNonNull(byDecision, "byDecision"));
        bySeverity = complete(Severity.class, Objects.requireNonNull(bySeverity, "bySeverity"));
    }

    private static <K extends Enum<K>> Map<K, Long> complete(Class<K> type, Map<K, Long> counts) {
        EnumMap<K, Long> copy = new EnumMap<>(type);
        for (K key : type.getEnumConstants()) {
            copy.put(key, counts.getOrDefault(key, 0L));
        }
        return Collections.unmodifiableMap(copy);
    }
}
