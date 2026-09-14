package io.github.radek11.dq;

import io.github.radek11.dq.input.DataRecord;
import io.github.radek11.dq.output.Failure;
import io.github.radek11.dq.output.RecordFailure;
import io.github.radek11.dq.output.Result;
import io.github.radek11.dq.output.ResultSink;
import io.github.radek11.dq.output.RuleFailure;
import io.github.radek11.dq.output.RunSummary;
import io.github.radek11.dq.rule.DecisionMapping;
import io.github.radek11.dq.rule.Rule;
import io.github.radek11.dq.rule.RuleCatalog;
import io.github.radek11.dq.rule.RuleStatus;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.stream.Stream;

import static io.github.radek11.dq.Fixture.RULES;
import static io.github.radek11.dq.output.Decision.INVALID;
import static io.github.radek11.dq.output.Decision.NOT_APPLICABLE;
import static io.github.radek11.dq.output.Decision.REVIEW;
import static io.github.radek11.dq.output.Decision.VALID;
import static io.github.radek11.dq.output.Severity.ERROR;
import static io.github.radek11.dq.output.Severity.INFO;
import static io.github.radek11.dq.output.Severity.WARNING;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

/**
 * K7: a run of 1,000,000 records completes under the heap limit set on the {@code memoryTest}
 * Gradle task. Run with {@code ./gradlew :core:memoryTest}; not part of {@code build}.
 *
 * <p>The input is generated as it is read and the sink only counts, so the heap holds what the
 * engine holds. The limit is chosen so that an engine keeping every result fails it.
 */
@Tag("memory")
class MillionRecordsMemoryTest {

    private static final int RECORDS = 1_000_000;
    private static final int CYCLES = RECORDS / 4;

    /** Fails on records without vatId, so the run also produces rule failures. */
    private static final Rule VAT_REQUIRED = Rule.builder("vatRequired")
            .label("VAT id is present")
            .status(RuleStatus.RELEASED)
            .severity(ERROR)
            .logic(fields -> fields.requiredText("vatId").isEmpty() ? "bad" : "ok")
            .mapping(DecisionMapping.of(Map.of("ok", VALID, "bad", INVALID), REVIEW))
            .build();

    @Test
    void aMillionRecordsRunUnderTheFixedHeapLimit() {
        RuleEngine engine = new RuleEngine(RuleCatalog.of(Stream.concat(RULES.stream(), Stream.of(VAT_REQUIRED)).toList()));
        CountingOnlySink sink = new CountingOnlySink();

        long start = System.nanoTime();
        RunSummary summary = engine.run(generated(), RunOptions.defaults(), sink);
        long millis = (System.nanoTime() - start) / 1_000_000;

        System.out.printf("K7: %,d records, %,d results, %,d failures, %,d ms, max heap %,d MB%n",
                RECORDS, summary.results(), summary.failures(), millis, Runtime.getRuntime().maxMemory() / (1024 * 1024));

        // Per cycle of four records: 3 accepted × 4 rules = 12 pairs, one of them a rule failure; one record rejected.
        assertThat(summary.results()).isEqualTo(11L * CYCLES);
        assertThat(summary.failures()).isEqualTo(2L * CYCLES);
        assertThat(summary.byDecision()).containsExactly(
                entry(VALID, 5L * CYCLES), entry(INVALID, 4L * CYCLES), entry(REVIEW, 0L), entry(NOT_APPLICABLE, 2L * CYCLES));
        assertThat(summary.bySeverity()).containsExactly(
                entry(ERROR, 8L * CYCLES), entry(WARNING, 3L * CYCLES), entry(INFO, 0L));
        assertThat(sink.results).isEqualTo(summary.results());
        assertThat(sink.ruleFailures).isEqualTo(CYCLES);
        assertThat(sink.recordFailures).isEqualTo(CYCLES);
        assertThat(sink.lastBatchEnd).isEqualTo(RECORDS);
    }

    /** Records like r1, r2, r3 from the fixture and one without an id, each created when read. */
    private static Iterator<DataRecord> generated() {
        return new Iterator<>() {
            private int index;

            @Override
            public boolean hasNext() {
                return index < RECORDS;
            }

            @Override
            public DataRecord next() {
                if (!hasNext()) {
                    throw new NoSuchElementException();
                }
                String id = "r" + index;
                DataRecord record = switch (index % 4) {
                    case 0 -> DataRecord.of(Map.of("id", id, "vatId", "DE111111111", "country", "DE", "iban", "DE123456789"));
                    case 1 -> DataRecord.of(Map.of("id", id, "vatId", "FR22", "country", "FR", "iban", "FR123456789"));
                    case 2 -> DataRecord.of(Map.of("id", id, "country", "ZZ", "iban", ""));
                    default -> DataRecord.of(Map.of("country", "DE", "vatId", "DE111111111"));
                };
                index++;
                return record;
            }
        };
    }

    /** Keeps counts only, so whatever grows on the heap is the engine's. */
    private static final class CountingOnlySink implements ResultSink {
        long results;
        long ruleFailures;
        long recordFailures;
        long lastBatchEnd;

        @Override
        public void onResult(Result result) {
            results++;
        }

        @Override
        public void onFailure(Failure failure) {
            switch (failure) {
                case RuleFailure ignored -> ruleFailures++;
                case RecordFailure ignored -> recordFailures++;
            }
        }

        @Override
        public void onBatchEnd(long recordsSoFar) {
            lastBatchEnd = recordsSoFar;
        }
    }
}
