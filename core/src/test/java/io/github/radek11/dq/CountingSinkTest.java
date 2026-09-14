package io.github.radek11.dq;

import io.github.radek11.dq.input.MissingFieldException;
import io.github.radek11.dq.output.Failure;
import io.github.radek11.dq.output.Result;
import io.github.radek11.dq.output.ResultSink;
import io.github.radek11.dq.output.RuleFailure;
import io.github.radek11.dq.output.RunSummary;
import org.junit.jupiter.api.Test;

import java.util.List;

import static io.github.radek11.dq.output.Decision.VALID;
import static io.github.radek11.dq.output.Severity.ERROR;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CountingSinkTest {

    private static final Result RESULT = new Result("rule", "r1", "ok", VALID, ERROR, List.of());
    private static final Failure FAILURE = new RuleFailure("rule", "r2", 1, new MissingFieldException("vatId"));

    @Test
    void everyCallReachesTheHostsSinkInOrderAndIsCounted() {
        RecordingSink host = new RecordingSink();
        CountingSink sink = new CountingSink(host);

        sink.onResult(RESULT);
        sink.onFailure(FAILURE);
        sink.onBatchEnd(2);

        assertThat(host.events()).containsExactly(RESULT, FAILURE, new RecordingSink.BatchEnd(2));
        RunSummary summary = sink.toSummary();
        assertThat(summary.results()).isEqualTo(1);
        assertThat(summary.failures()).isEqualTo(1);
    }

    @Test
    void whatTheHostsSinkRejectsIsNotCounted() {
        IllegalStateException sinkDown = new IllegalStateException("sink down");
        CountingSink sink = new CountingSink(new ResultSink() {
            @Override
            public void onResult(Result result) {
                throw sinkDown;
            }

            @Override
            public void onFailure(Failure failure) {
                throw sinkDown;
            }
        });

        assertThatThrownBy(() -> sink.onResult(RESULT)).isSameAs(sinkDown);
        assertThatThrownBy(() -> sink.onFailure(FAILURE)).isSameAs(sinkDown);
        assertThat(sink.toSummary().results()).isZero();
        assertThat(sink.toSummary().failures()).isZero();
    }
}
