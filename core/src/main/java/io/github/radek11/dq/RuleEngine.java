package io.github.radek11.dq;

import io.github.radek11.dq.data.DataRecord;
import io.github.radek11.dq.data.FieldTypeException;
import io.github.radek11.dq.result.Failure;
import io.github.radek11.dq.result.RecordFailure;
import io.github.radek11.dq.result.ResultSink;
import io.github.radek11.dq.result.RuleFailure;
import io.github.radek11.dq.result.RunSummary;
import io.github.radek11.dq.rule.RuleCatalog;
import io.github.radek11.dq.rule.RuleLogic;

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
     * <p>What is isolated — reported to the sink as a {@link Failure}, the run continues:
     * <ul>
     *   <li>a rule failing on a record, as described in {@link RuleLogic#compute} — a
     *       {@link RuleFailure};</li>
     *   <li>a record that cannot be evaluated: a {@code null} element, no id, or an id or
     *       country that is not text ({@link FieldTypeException}) — one {@link RecordFailure},
     *       and no rule runs on that record.</li>
     * </ul>
     *
     * <p>What ends the run — propagates to the caller, no summary is returned:
     * <ul>
     *   <li>an exception thrown by the input iterator, for example unreadable input;</li>
     *   <li>an exception thrown by the sink — it is the host's fault, not the rule's;</li>
     *   <li>an exception thrown by the catalog, or two rules with the same id
     *       ({@link IllegalArgumentException}), both before any record is read;</li>
     *   <li>a JVM {@link Error}.</li>
     * </ul>
     *
     * <p>Records are read in batches of {@link RunOptions#batchSize()}. After each batch,
     * including a final partial one, the sink's {@link ResultSink#onBatchEnd} is called; an
     * empty input produces no call.
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
