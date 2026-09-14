package io.github.radek11.dq;

import io.github.radek11.dq.RecordIdentifier.Identified;
import io.github.radek11.dq.RecordIdentifier.Rejected;
import io.github.radek11.dq.input.DataRecord;
import io.github.radek11.dq.input.FieldTypeException;
import io.github.radek11.dq.output.Failure;
import io.github.radek11.dq.output.RecordFailure;
import io.github.radek11.dq.output.Result;
import io.github.radek11.dq.output.ResultSink;
import io.github.radek11.dq.output.RuleFailure;
import io.github.radek11.dq.output.RunSummary;
import io.github.radek11.dq.rule.Rule;
import io.github.radek11.dq.rule.RuleCatalog;
import io.github.radek11.dq.rule.RuleLogic;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
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
        Objects.requireNonNull(records, "records");
        Objects.requireNonNull(options, "options");
        Objects.requireNonNull(sink, "sink");

        RuleSelection selection = RuleSelection.of(catalog, options.ruleFilter());
        RecordIdentifier identifier = new RecordIdentifier(options.idField(), options.countryField());
        RunCounters counters = new RunCounters();
        long index = 0;
        while (records.hasNext()) {
            for (DataRecord record : nextBatch(records, options.batchSize())) {
                switch (identifier.identify(record, index)) {
                    case Identified identified -> evaluate(identified, index, selection, sink, counters);
                    case Rejected rejected -> {
                        sink.onFailure(rejected.failure());
                        counters.add(rejected.failure());
                    }
                }
                index++;
            }
            sink.onBatchEnd(index);
        }
        return counters.toSummary();
    }

    private static List<DataRecord> nextBatch(Iterator<? extends DataRecord> records, int batchSize) {
        List<DataRecord> batch = new ArrayList<>(batchSize);
        while (batch.size() < batchSize && records.hasNext()) {
            batch.add(records.next());
        }
        return batch;
    }

    // Only rule.evaluate is inside the try: a sink that throws must end the run, not become a
    // RuleFailure, and an Error is never isolated.
    private static void evaluate(
            Identified record, long index, RuleSelection selection, ResultSink sink, RunCounters counters) {
        String country = record.country().orElse(null);
        for (Rule rule : selection.rules()) {
            if (!rule.appliesTo(country)) {
                continue;
            }
            Result result;
            try {
                result = rule.evaluate(record.record(), record.id());
            } catch (Exception e) {
                RuleFailure failure = new RuleFailure(rule.id(), record.id(), index, e);
                sink.onFailure(failure);
                counters.add(failure);
                continue;
            }
            sink.onResult(result);
            counters.add(result);
        }
    }
}
