package io.github.radek11.dq.rule;

import io.github.radek11.dq.data.DataRecord;
import io.github.radek11.dq.data.FieldReader;
import io.github.radek11.dq.result.FieldRead;

import java.util.List;
import java.util.Optional;

/**
 * Field reader that records every read, for one rule on one record. Its reads become the
 * provenance of the result. Not thread-safe; one instance per evaluation.
 */
final class RecordingFieldReader implements FieldReader {

    private final DataRecord record;

    RecordingFieldReader(DataRecord record) {
        this.record = record;
    }

    @Override
    public Optional<String> text(String name) {
        throw new UnsupportedOperationException("E2");
    }

    @Override
    public String requiredText(String name) {
        throw new UnsupportedOperationException("E2");
    }

    /** @return the reads so far, in order */
    List<FieldRead> reads() {
        throw new UnsupportedOperationException("E2");
    }
}
