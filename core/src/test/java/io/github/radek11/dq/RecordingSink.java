package io.github.radek11.dq;

import io.github.radek11.dq.output.Failure;
import io.github.radek11.dq.output.Result;
import io.github.radek11.dq.output.ResultSink;

import java.util.ArrayList;
import java.util.List;

/** Keeps every call in order, so tests can check what was emitted and when. */
final class RecordingSink implements ResultSink {

    /** A batch boundary, kept among results and failures to check their order. */
    record BatchEnd(long recordsSoFar) {
    }

    private final List<Object> events = new ArrayList<>();

    @Override
    public void onResult(Result result) {
        events.add(result);
    }

    @Override
    public void onFailure(Failure failure) {
        events.add(failure);
    }

    @Override
    public void onBatchEnd(long recordsSoFar) {
        events.add(new BatchEnd(recordsSoFar));
    }

    /** @return results, failures and batch ends in the order they arrived */
    List<Object> events() {
        return List.copyOf(events);
    }

    List<Result> results() {
        return only(Result.class);
    }

    List<Failure> failures() {
        return only(Failure.class);
    }

    List<Long> batchEnds() {
        return only(BatchEnd.class).stream().map(BatchEnd::recordsSoFar).toList();
    }

    private <T> List<T> only(Class<T> type) {
        return events.stream().filter(type::isInstance).map(type::cast).toList();
    }
}
