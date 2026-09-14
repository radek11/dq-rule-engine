package io.github.radek11.dq.example.http;

import io.github.radek11.dq.output.Failure;
import io.github.radek11.dq.output.FieldRead;
import io.github.radek11.dq.output.RecordFailure;
import io.github.radek11.dq.output.Result;
import io.github.radek11.dq.output.ResultSink;
import io.github.radek11.dq.output.RuleFailure;
import io.github.radek11.dq.output.RunSummary;
import tools.jackson.core.JsonGenerator;
import tools.jackson.core.ObjectWriteContext;
import tools.jackson.core.json.JsonFactory;

import java.io.OutputStream;
import java.util.Map;

/**
 * Writes a run as NDJSON (D25): one line per result or failure in the order they are produced,
 * then one {@code summary} line — or one {@code error} line when the run ends with an exception
 * (D26). Nothing is collected: lines are written as they come and flushed to the client at the
 * end of every batch.
 *
 * <p>One sink per response; not thread-safe.
 */
final class NdjsonSink implements ResultSink, AutoCloseable {

    // No separator between root values: every line ends with its own newline, the last one too.
    private static final JsonFactory JSON = JsonFactory.builder().rootValueSeparator("").build();

    private final JsonGenerator json;

    NdjsonSink(OutputStream out) {
        this.json = JSON.createGenerator(ObjectWriteContext.empty(), out);
    }

    @Override
    public void onResult(Result result) {
        json.writeStartObject();
        json.writeStringProperty("type", "result");
        json.writeStringProperty("ruleId", result.ruleId());
        json.writeStringProperty("recordId", result.recordId());
        json.writeStringProperty("value", result.value());
        json.writeStringProperty("decision", result.decision().name());
        json.writeStringProperty("severity", result.severity().name());
        json.writeName("fieldsRead");
        json.writeStartArray();
        for (FieldRead read : result.fieldsRead()) {
            json.writeStartObject();
            json.writeStringProperty("name", read.name());
            json.writeStringProperty("presence", read.presence().name());
            if (read.value() != null) {
                json.writeStringProperty("value", read.value());
            }
            json.writeEndObject();
        }
        json.writeEndArray();
        endLine();
    }

    // The cause's type and message only. The library's own exceptions name fields, not values; the
    // message of an exception thrown by rule logic is up to the rule's author (D10).
    @Override
    public void onFailure(Failure failure) {
        json.writeStartObject();
        json.writeStringProperty("type", "failure");
        switch (failure) {
            case RuleFailure rule -> {
                json.writeStringProperty("kind", "rule");
                json.writeStringProperty("ruleId", rule.ruleId());
                json.writeStringProperty("recordId", rule.recordId());
            }
            case RecordFailure record -> {
                json.writeStringProperty("kind", "record");
                if (record.recordId().isPresent()) {
                    json.writeStringProperty("recordId", record.recordId().get());
                }
            }
        }
        json.writeNumberProperty("recordIndex", failure.recordIndex());
        json.writeStringProperty("error", failure.cause().getClass().getName());
        if (failure.cause().getMessage() != null) {
            json.writeStringProperty("message", failure.cause().getMessage());
        }
        endLine();
    }

    @Override
    public void onBatchEnd(long recordsSoFar) {
        json.flush();
    }

    /** Writes the last line of a complete run and flushes it. */
    void summary(RunSummary summary) {
        json.writeStartObject();
        json.writeStringProperty("type", "summary");
        json.writeNumberProperty("results", summary.results());
        json.writeNumberProperty("failures", summary.failures());
        json.writeName("byDecision");
        writeCounts(summary.byDecision());
        json.writeName("bySeverity");
        writeCounts(summary.bySeverity());
        endLine();
        json.flush();
    }

    /** Writes the last line of a run that ended with an exception and flushes it. */
    void error(String message) {
        json.writeStartObject();
        json.writeStringProperty("type", "error");
        json.writeStringProperty("message", message);
        endLine();
        json.flush();
    }

    /** Flushes and closes the generator together with the output stream. */
    @Override
    public void close() {
        json.close();
    }

    private void writeCounts(Map<? extends Enum<?>, Long> counts) {
        json.writeStartObject();
        counts.forEach((key, count) -> json.writeNumberProperty(key.name(), count));
        json.writeEndObject();
    }

    private void endLine() {
        json.writeEndObject();
        json.writeRaw('\n');
    }
}
