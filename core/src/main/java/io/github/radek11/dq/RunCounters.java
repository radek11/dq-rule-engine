package io.github.radek11.dq;

import io.github.radek11.dq.output.Decision;
import io.github.radek11.dq.output.Failure;
import io.github.radek11.dq.output.Result;
import io.github.radek11.dq.output.RunSummary;
import io.github.radek11.dq.output.Severity;

import java.util.EnumMap;
import java.util.Map;

/**
 * Mutable counters of one run. Constant memory: a fixed number of counters, whatever the volume.
 * Not thread-safe; owned by the thread running the run.
 */
final class RunCounters {

    // Indexed by ordinal: plain longs, no boxing on the path taken for every result.
    private final long[] byDecision = new long[Decision.values().length];
    private final long[] bySeverity = new long[Severity.values().length];
    private long results;
    private long failures;

    /** @param result a result emitted to the sink */
    void add(Result result) {
        results++;
        byDecision[result.decision().ordinal()]++;
        bySeverity[result.severity().ordinal()]++;
    }

    /** @param failure a failure of either kind emitted to the sink */
    void add(Failure failure) {
        failures++;
    }

    /** @return the summary of everything counted so far */
    RunSummary toSummary() {
        return new RunSummary(results, failures, toMap(Decision.class, byDecision), toMap(Severity.class, bySeverity));
    }

    private static <K extends Enum<K>> Map<K, Long> toMap(Class<K> type, long[] counts) {
        Map<K, Long> map = new EnumMap<>(type);
        for (K key : type.getEnumConstants()) {
            map.put(key, counts[key.ordinal()]);
        }
        return map;
    }
}
