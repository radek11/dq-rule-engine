package io.github.radek11.dq;

import io.github.radek11.dq.RecordingSink.BatchEnd;
import io.github.radek11.dq.input.DataRecord;
import io.github.radek11.dq.input.MissingFieldException;
import io.github.radek11.dq.output.Failure;
import io.github.radek11.dq.output.RecordFailure;
import io.github.radek11.dq.output.Result;
import io.github.radek11.dq.output.ResultSink;
import io.github.radek11.dq.output.RuleFailure;
import io.github.radek11.dq.output.RunSummary;
import io.github.radek11.dq.output.Severity;
import io.github.radek11.dq.rule.DecisionMapping;
import io.github.radek11.dq.rule.Rule;
import io.github.radek11.dq.rule.RuleCatalog;
import io.github.radek11.dq.rule.RuleLogic;
import io.github.radek11.dq.rule.RuleStatus;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static io.github.radek11.dq.Fixture.COUNTRY_BLOCKED;
import static io.github.radek11.dq.Fixture.IBAN_FORMAT;
import static io.github.radek11.dq.Fixture.R1;
import static io.github.radek11.dq.Fixture.R2;
import static io.github.radek11.dq.Fixture.R3;
import static io.github.radek11.dq.Fixture.RECORDS;
import static io.github.radek11.dq.Fixture.RULES;
import static io.github.radek11.dq.Fixture.VAT_FORMAT;
import static io.github.radek11.dq.output.Decision.INVALID;
import static io.github.radek11.dq.output.Decision.NOT_APPLICABLE;
import static io.github.radek11.dq.output.Decision.REVIEW;
import static io.github.radek11.dq.output.Decision.VALID;
import static io.github.radek11.dq.output.Severity.ERROR;
import static io.github.radek11.dq.output.Severity.INFO;
import static io.github.radek11.dq.output.Severity.WARNING;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.entry;
import static org.assertj.core.api.Assertions.tuple;

class RuleEngineTest {

    /** Fails on r3, which has no vatId — "a missing field" from F3. */
    private static final Rule VAT_REQUIRED = rule("vatRequired", RuleStatus.RELEASED,
            fields -> fields.requiredText("vatId").isEmpty() ? "bad" : "ok");

    /** Has country and vatId, but no id — a record the engine cannot name in a result. */
    private static final DataRecord NO_ID = DataRecord.of(Map.of("country", "DE", "vatId", "DE111111111"));

    private final RecordingSink sink = new RecordingSink();

    // K3 — the table from section 5, through the engine

    @Test
    void theFixtureRunGivesTheExpectedDecisionForEveryRecordAndRule() {
        engine(RULES).run(RECORDS.iterator(), RunOptions.defaults(), sink);

        assertThat(sink.results())
                .extracting(Result::recordId, Result::ruleId, Result::decision)
                .containsExactly(
                        tuple("r1", "countryBlocked", NOT_APPLICABLE), tuple("r1", "vatFormat", VALID), tuple("r1", "ibanFormat", VALID),
                        tuple("r2", "countryBlocked", NOT_APPLICABLE), tuple("r2", "vatFormat", INVALID), tuple("r2", "ibanFormat", VALID),
                        tuple("r3", "countryBlocked", INVALID), tuple("r3", "vatFormat", INVALID), tuple("r3", "ibanFormat", INVALID));
        assertThat(sink.failures()).isEmpty();
    }

    /** Value and provenance per cell are asserted in RuleEvaluateTest; the engine must not change them. */
    @Test
    void theEngineEmitsExactlyWhatEachRuleEvaluatesToIncludingValueAndProvenance() {
        engine(RULES).run(RECORDS.iterator(), RunOptions.defaults(), sink);

        List<Result> expected = RECORDS.stream()
                .flatMap(record -> RULES.stream().map(rule -> rule.evaluate(record, (String) record.get("id"))))
                .toList();
        assertThat(sink.results()).containsExactlyElementsOf(expected);
    }

    // K5 — fault isolation

    @Test
    void aFailingRuleAndAnUnusableRecordAreReportedNextToTheResultsAndTheRunCompletes() {
        List<DataRecord> records = List.of(R1, NO_ID, R2, R3);

        RunSummary summary = engine(append(RULES, VAT_REQUIRED))
                .run(records.iterator(), RunOptions.defaults().withBatchSize(2), sink);

        assertThat(sink.results())
                .extracting(Result::recordId, Result::ruleId)
                .containsExactly(
                        tuple("r1", "countryBlocked"), tuple("r1", "vatFormat"), tuple("r1", "ibanFormat"), tuple("r1", "vatRequired"),
                        tuple("r2", "countryBlocked"), tuple("r2", "vatFormat"), tuple("r2", "ibanFormat"), tuple("r2", "vatRequired"),
                        tuple("r3", "countryBlocked"), tuple("r3", "vatFormat"), tuple("r3", "ibanFormat"));

        List<Failure> failures = sink.failures();
        assertThat(failures).hasSize(2);
        assertThat(failures.get(0)).isInstanceOfSatisfying(RecordFailure.class, failure -> {
            assertThat(failure.recordId()).isEmpty();
            assertThat(failure.recordIndex()).isEqualTo(1);
            assertThat(failure.cause()).isInstanceOfSatisfying(MissingFieldException.class,
                    cause -> assertThat(cause.fieldName()).isEqualTo("id"));
        });
        assertThat(failures.get(1)).isInstanceOfSatisfying(RuleFailure.class, failure -> {
            assertThat(failure.ruleId()).isEqualTo("vatRequired");
            assertThat(failure.recordId()).isEqualTo("r3");
            assertThat(failure.recordIndex()).isEqualTo(3);
            assertThat(failure.cause()).isInstanceOfSatisfying(MissingFieldException.class,
                    cause -> assertThat(cause.fieldName()).isEqualTo("vatId"));
        });

        assertThat(sink.batchEnds()).containsExactly(2L, 4L);
        assertThat(summary.results()).isEqualTo(sink.results().size());
        assertThat(summary.failures()).isEqualTo(2);
    }

    @Test
    void aRuleFailureIsEmittedWhereItHappenedBetweenTheResults() {
        engine(List.of(COUNTRY_BLOCKED, VAT_REQUIRED, IBAN_FORMAT)).run(List.of(R3).iterator(), RunOptions.defaults(), sink);

        assertThat(sink.events())
                .extracting(RuleEngineTest::describe)
                .containsExactly("result countryBlocked", "failure vatRequired", "result ibanFormat", "batch end 1");
    }

    // K6 — summary

    @Test
    void theFixtureSummaryCountsResultsByDecisionAndBySeverity() {
        RunSummary summary = engine(RULES).run(RECORDS.iterator(), RunOptions.defaults(), sink);

        assertThat(summary.results()).isEqualTo(9);
        assertThat(summary.failures()).isZero();
        assertThat(summary.byDecision()).containsExactly(
                entry(VALID, 3L), entry(INVALID, 4L), entry(REVIEW, 0L), entry(NOT_APPLICABLE, 2L));
        assertThat(summary.bySeverity()).containsExactly(entry(ERROR, 6L), entry(WARNING, 3L), entry(INFO, 0L));
        assertThat(sum(summary.byDecision())).isEqualTo(summary.results());
        assertThat(sum(summary.bySeverity())).isEqualTo(summary.results());
    }

    // K6a — completeness, counted by the test's own sink

    @Test
    void everyPairOfAcceptedRecordAndSelectedRuleEndsAsAResultOrARuleFailureOrASkip() {
        Rule draft = rule("draftCheck", RuleStatus.DRAFT, fields -> "ok");
        List<DataRecord> records = List.of(R1, NO_ID, R2, R3);

        engine(append(RULES, VAT_REQUIRED, draft)).run(records.iterator(), RunOptions.defaults().withBatchSize(3), sink);

        long consumed = sink.batchEnds().getLast();
        long rejected = sink.failures().stream().filter(RecordFailure.class::isInstance).count();
        long ruleFailures = sink.failures().stream().filter(RuleFailure.class::isInstance).count();
        long selectedRules = 4;  // the three fixture rules and vatRequired; the draft never runs
        long skipped = 0;        // every rule is WORLD; skips by country come with E4

        assertThat(consumed).isEqualTo(records.size());
        assertThat(sink.results()).extracting(Result::ruleId).doesNotContain("draftCheck");
        assertThat(sink.results().size() + ruleFailures + skipped).isEqualTo((consumed - rejected) * selectedRules);
    }

    // The run contract: what ends the run

    @Test
    void anExceptionFromTheSinkEndsTheRunAndIsNotReportedAsARuleFailure() {
        IllegalStateException sinkDown = new IllegalStateException("sink down");
        List<Failure> reported = new ArrayList<>();
        ResultSink failing = new ResultSink() {
            @Override
            public void onResult(Result result) {
                throw sinkDown;
            }

            @Override
            public void onFailure(Failure failure) {
                reported.add(failure);
            }
        };

        assertThatThrownBy(() -> engine(RULES).run(RECORDS.iterator(), RunOptions.defaults(), failing))
                .isSameAs(sinkDown);
        assertThat(reported).isEmpty();
    }

    @Test
    void aDuplicateRuleIdFailsTheRunBeforeTheFirstRecordIsRead() {
        Rule draftTwin = rule(VAT_FORMAT.id(), RuleStatus.DRAFT, fields -> "ok");
        RuleEngine engine = new RuleEngine(() -> List.of(COUNTRY_BLOCKED, VAT_FORMAT, draftTwin));

        assertThatIllegalArgumentException()
                .isThrownBy(() -> engine.run(neverRead(), RunOptions.defaults(), sink))
                .withMessageContaining(VAT_FORMAT.id());
        assertThat(sink.events()).isEmpty();
    }

    @Test
    void anExceptionFromTheCatalogFailsTheRunBeforeTheFirstRecordIsRead() {
        IllegalStateException catalogDown = new IllegalStateException("catalog down");
        RuleEngine engine = new RuleEngine(() -> {
            throw catalogDown;
        });

        assertThatThrownBy(() -> engine.run(neverRead(), RunOptions.defaults(), sink)).isSameAs(catalogDown);
        assertThat(sink.events()).isEmpty();
    }

    @Test
    void anExceptionFromTheInputEndsTheRun() {
        UncheckedIOException unreadable = new UncheckedIOException(new IOException("truncated input"));
        Iterator<DataRecord> records = Stream.<DataRecord>iterate(R1, record -> {
            throw unreadable;
        }).iterator();

        assertThatThrownBy(() -> engine(RULES).run(records, RunOptions.defaults(), sink)).isSameAs(unreadable);
    }

    @Test
    void aJvmErrorInARuleEndsTheRun() {
        StackOverflowError error = new StackOverflowError("simulated");
        Rule overflowing = rule("overflowing", RuleStatus.RELEASED, fields -> {
            throw error;
        });

        assertThatThrownBy(() -> engine(List.of(overflowing)).run(RECORDS.iterator(), RunOptions.defaults(), sink))
                .isSameAs(error);
        assertThat(sink.failures()).isEmpty();
    }

    // The run contract: batches

    @Test
    void theBatchEndIsSignalledAfterEachBatchIncludingTheLastPartialOne() {
        List<DataRecord> records = List.of(R1, R2, R3, R1, R2);

        engine(List.of(VAT_FORMAT)).run(records.iterator(), RunOptions.defaults().withBatchSize(2), sink);

        assertThat(sink.events())
                .extracting(RuleEngineTest::describe)
                .containsExactly(
                        "result vatFormat", "result vatFormat", "batch end 2",
                        "result vatFormat", "result vatFormat", "batch end 4",
                        "result vatFormat", "batch end 5");
    }

    @Test
    void inputThatFillsTheLastBatchExactlyGetsNoExtraBatchEnd() {
        engine(RULES).run(List.of(R1, R2, R3, R1).iterator(), RunOptions.defaults().withBatchSize(2), sink);

        assertThat(sink.batchEnds()).containsExactly(2L, 4L);
    }

    @Test
    void anEmptyInputGivesNoBatchEndAndAnEmptySummary() {
        RunSummary summary = engine(RULES).run(Collections.emptyIterator(), RunOptions.defaults(), sink);

        assertThat(sink.events()).isEmpty();
        assertThat(summary.results()).isZero();
        assertThat(summary.failures()).isZero();
    }

    /** A caller may pass the largest batch size to mean "one batch"; memory must follow the input, not the option. */
    @Test
    void theLargestBatchSizeRunsTheInputAsOneBatch() {
        RunSummary summary = engine(RULES).run(RECORDS.iterator(), RunOptions.defaults().withBatchSize(Integer.MAX_VALUE), sink);

        assertThat(summary.results()).isEqualTo(9);
        assertThat(sink.batchEnds()).containsExactly(3L);
    }

    // The run contract: options and reuse

    @Test
    void theCallersFilterNarrowsTheRun() {
        engine(RULES).run(RECORDS.iterator(),
                RunOptions.defaults().withRuleFilter(rule -> rule.severity() == WARNING), sink);

        assertThat(sink.results()).extracting(Result::ruleId).containsOnly(IBAN_FORMAT.id()).hasSize(3);
    }

    @Test
    void theIdIsReadFromTheConfiguredField() {
        DataRecord record = DataRecord.of(Map.of("partnerId", "p7", "land", "DE", "vatId", "DE111111111"));

        engine(List.of(VAT_FORMAT)).run(List.of(record).iterator(),
                RunOptions.defaults().withIdField("partnerId").withCountryField("land"), sink);

        assertThat(sink.results()).extracting(Result::recordId).containsExactly("p7");
    }

    @Test
    void anEngineKeepsNoStateBetweenRuns() {
        RuleEngine engine = engine(RULES);

        RunSummary first = engine.run(RECORDS.iterator(), RunOptions.defaults(), new RecordingSink());
        RunSummary second = engine.run(RECORDS.iterator(), RunOptions.defaults(), new RecordingSink());

        assertThat(second).isEqualTo(first);
    }

    @Test
    void runRequiresRecordsOptionsAndSink() {
        RuleEngine engine = engine(RULES);

        assertThatNullPointerException().isThrownBy(() -> engine.run(null, RunOptions.defaults(), sink));
        assertThatNullPointerException().isThrownBy(() -> engine.run(RECORDS.iterator(), null, sink));
        assertThatNullPointerException().isThrownBy(() -> engine.run(RECORDS.iterator(), RunOptions.defaults(), null));
    }

    private static RuleEngine engine(List<Rule> rules) {
        return new RuleEngine(RuleCatalog.of(rules));
    }

    private static Rule rule(String id, RuleStatus status, RuleLogic logic) {
        return Rule.builder(id).label(id).status(status).severity(Severity.INFO)
                .logic(logic)
                .mapping(DecisionMapping.of(Map.of("ok", VALID, "bad", INVALID), REVIEW))
                .build();
    }

    private static List<Rule> append(List<Rule> rules, Rule... more) {
        return Stream.concat(rules.stream(), Stream.of(more)).toList();
    }

    private static <K> long sum(Map<K, Long> counts) {
        return counts.values().stream().mapToLong(Long::longValue).sum();
    }

    /** An input the engine must not touch. */
    private static Iterator<DataRecord> neverRead() {
        return new Iterator<>() {
            @Override
            public boolean hasNext() {
                throw new AssertionError("the input was read");
            }

            @Override
            public DataRecord next() {
                throw new AssertionError("the input was read");
            }
        };
    }

    private static String describe(Object event) {
        return switch (event) {
            case Result result -> "result " + result.ruleId();
            case RuleFailure failure -> "failure " + failure.ruleId();
            case RecordFailure failure -> "rejected " + failure.recordIndex();
            case BatchEnd end -> "batch end " + end.recordsSoFar();
            default -> throw new IllegalArgumentException("unexpected event " + event);
        };
    }
}
