package io.github.radek11.dq;

import io.github.radek11.dq.input.MissingFieldException;
import io.github.radek11.dq.output.Decision;
import io.github.radek11.dq.output.RecordFailure;
import io.github.radek11.dq.output.Result;
import io.github.radek11.dq.output.RuleFailure;
import io.github.radek11.dq.output.RunSummary;
import io.github.radek11.dq.output.Severity;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static io.github.radek11.dq.output.Decision.INVALID;
import static io.github.radek11.dq.output.Decision.NOT_APPLICABLE;
import static io.github.radek11.dq.output.Decision.VALID;
import static io.github.radek11.dq.output.Severity.ERROR;
import static io.github.radek11.dq.output.Severity.INFO;
import static io.github.radek11.dq.output.Severity.WARNING;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

class RunCountersTest {

    private final RunCounters counters = new RunCounters();

    @Test
    void nothingCountedGivesZeroForEveryDecisionAndSeverity() {
        RunSummary summary = counters.toSummary();

        assertThat(summary.results()).isZero();
        assertThat(summary.failures()).isZero();
        assertThat(summary.byDecision()).containsOnlyKeys(Decision.values())
                .allSatisfy((decision, count) -> assertThat(count).isZero());
        assertThat(summary.bySeverity()).containsOnlyKeys(Severity.values())
                .allSatisfy((severity, count) -> assertThat(count).isZero());
    }

    @Test
    void resultsAreCountedByDecisionAndBySeverity() {
        counters.add(result(VALID, ERROR));
        counters.add(result(VALID, WARNING));
        counters.add(result(INVALID, ERROR));
        counters.add(result(NOT_APPLICABLE, INFO));

        RunSummary summary = counters.toSummary();

        assertThat(summary.results()).isEqualTo(4);
        assertThat(summary.failures()).isZero();
        assertThat(summary.byDecision()).contains(entry(VALID, 2L), entry(INVALID, 1L), entry(NOT_APPLICABLE, 1L));
        assertThat(summary.bySeverity()).contains(entry(ERROR, 2L), entry(WARNING, 1L), entry(INFO, 1L));
    }

    @Test
    void failuresOfBothKindsAreCountedButDoNotChangeTheBreakdowns() {
        Exception cause = new MissingFieldException("vatId");
        counters.add(result(VALID, ERROR));
        RunSummary beforeFailures = counters.toSummary();
        counters.add(new RuleFailure("vatFormat", "r1", 0, cause));
        counters.add(new RecordFailure(Optional.empty(), 1, cause));

        RunSummary summary = counters.toSummary();

        assertThat(summary.results()).isEqualTo(1);
        assertThat(summary.failures()).isEqualTo(2);
        assertThat(summary.byDecision()).isEqualTo(beforeFailures.byDecision());
        assertThat(summary.bySeverity()).isEqualTo(beforeFailures.bySeverity());
    }

    @Test
    void countingContinuesAfterASummaryIsTaken() {
        counters.add(result(VALID, ERROR));
        RunSummary first = counters.toSummary();
        counters.add(result(VALID, ERROR));

        assertThat(first.results()).isEqualTo(1);
        assertThat(counters.toSummary().results()).isEqualTo(2);
    }

    private static Result result(Decision decision, Severity severity) {
        return new Result("rule", "record", "value", decision, severity, List.of());
    }
}
