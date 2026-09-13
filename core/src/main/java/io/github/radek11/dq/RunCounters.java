package io.github.radek11.dq;

import io.github.radek11.dq.result.Result;
import io.github.radek11.dq.result.RunSummary;

/**
 * Mutable counters of one run. Constant memory: a fixed number of counters, whatever the volume.
 * Not thread-safe; owned by the thread running the run.
 */
final class RunCounters {

    /** @param result a result emitted to the sink */
    void add(Result result) {
        throw new UnsupportedOperationException("E2");
    }

    /** Counts a failure of either kind. */
    void addFailure() {
        throw new UnsupportedOperationException("E2");
    }

    /** @return the summary of everything counted so far */
    RunSummary toSummary() {
        throw new UnsupportedOperationException("E2");
    }
}
