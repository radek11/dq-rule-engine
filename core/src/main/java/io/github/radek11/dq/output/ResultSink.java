package io.github.radek11.dq.output;

/**
 * Receives the output of a run as it is produced. The engine keeps nothing beyond the current
 * batch: what the sink does with results — write, count, forward or drop — decides the rest of
 * the host's memory use.
 *
 * <p>All methods are called on the thread that called
 * {@link io.github.radek11.dq.RuleEngine#run RuleEngine.run}, one at a time. A slow sink slows
 * the run down. An exception thrown by the sink is not isolated: it ends the run and reaches
 * the caller.
 */
public interface ResultSink {

    /** @param result a result, emitted as soon as it is computed */
    void onResult(Result result);

    /** @param failure a failure, emitted as soon as it occurs */
    void onFailure(Failure failure);

    /**
     * Called after each batch, including a final partial one — a point where a host can flush,
     * commit or checkpoint. Not called for an empty input.
     *
     * @param recordsSoFar number of records consumed by the run so far, rejected ones included
     */
    default void onBatchEnd(long recordsSoFar) {
    }
}
