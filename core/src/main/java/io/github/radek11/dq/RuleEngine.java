package io.github.radek11.dq;

import io.github.radek11.dq.data.DataRecord;
import io.github.radek11.dq.result.ResultSink;
import io.github.radek11.dq.result.RunSummary;
import io.github.radek11.dq.rule.RuleCatalog;

import java.util.Iterator;
import java.util.Objects;

/**
 * Runs data-quality rules over records. The entry point of the library.
 *
 * <p>An engine holds no run state and can be shared between threads; each call to
 * {@link #run} is independent. A single run is processed sequentially on the calling thread.
 */
public final class RuleEngine {

    private final RuleCatalog catalog;

    /** @param catalog source of rules, read at the start of every run */
    public RuleEngine(RuleCatalog catalog) {
        this.catalog = Objects.requireNonNull(catalog, "catalog");
    }

    /**
     * Validates records and streams every result and failure to the sink as it is produced.
     *
     * <p>A failing rule or a bad record is reported to the sink and the run continues. An
     * exception thrown by the input iterator itself (for example unreadable input) ends the run
     * and propagates to the caller.
     *
     * @param records the input, consumed once
     * @param options batch size, rule selection and field names
     * @param sink receives results, failures and batch boundaries
     * @return counts for the run
     */
    public RunSummary run(Iterator<? extends DataRecord> records, RunOptions options, ResultSink sink) {
        throw new UnsupportedOperationException("E2");
    }
}
