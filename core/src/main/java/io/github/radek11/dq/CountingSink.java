package io.github.radek11.dq;

import io.github.radek11.dq.output.Failure;
import io.github.radek11.dq.output.Result;
import io.github.radek11.dq.output.ResultSink;
import io.github.radek11.dq.output.RunSummary;

/**
 * The host's sink with counting attached: whatever reaches the host is counted, so the summary
 * matches the sink by construction rather than by every emitting line remembering to count.
 */
final class CountingSink implements ResultSink {

    private final ResultSink target;
    private final RunCounters counters = new RunCounters();

    /** @param target the host's sink */
    CountingSink(ResultSink target) {
        this.target = target;
    }

    @Override
    public void onResult(Result result) {
        target.onResult(result);
        counters.add(result);
    }

    @Override
    public void onFailure(Failure failure) {
        target.onFailure(failure);
        counters.add(failure);
    }

    @Override
    public void onBatchEnd(long recordsSoFar) {
        target.onBatchEnd(recordsSoFar);
    }

    /** @return the summary of everything the host's sink accepted */
    RunSummary toSummary() {
        return counters.toSummary();
    }
}
