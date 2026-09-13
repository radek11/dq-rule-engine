package io.github.radek11.dq.rule;

import io.github.radek11.dq.data.DataRecord;
import io.github.radek11.dq.data.FieldReader;
import io.github.radek11.dq.data.FieldTypeException;
import io.github.radek11.dq.data.MissingFieldException;
import io.github.radek11.dq.result.FieldRead;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static io.github.radek11.dq.result.FieldRead.Presence.MISSING;
import static io.github.radek11.dq.result.FieldRead.Presence.NULL;
import static io.github.radek11.dq.result.FieldRead.Presence.PRESENT;

/**
 * Field reader that records every read, for one rule on one record. Its reads become the
 * provenance of the result. Not thread-safe; one instance per evaluation.
 */
final class RecordingFieldReader implements FieldReader {

    private final DataRecord record;
    private final List<FieldRead> reads = new ArrayList<>();

    RecordingFieldReader(DataRecord record) {
        this.record = record;
    }

    @Override
    public Optional<String> text(String name) {
        FieldRead read = read(name);
        return read.presence() == PRESENT ? Optional.of(read.value()) : Optional.empty();
    }

    @Override
    public String requiredText(String name) {
        FieldRead read = read(name);
        if (read.presence() != PRESENT) {
            throw new MissingFieldException(name);
        }
        return read.value();
    }

    /** @return the reads so far, in order; a snapshot, later reads do not change it */
    List<FieldRead> reads() {
        return List.copyOf(reads);
    }

    /**
     * Reads a field and records the read. The returned read is exactly what goes into
     * provenance, so the rule's view and the recorded view cannot diverge. Missing and
     * {@code null} look the same to the rule but not in provenance: a missing field was never
     * sent, a {@code null} one was sent empty.
     *
     * @throws FieldTypeException when the field holds a non-text value; not recorded, because
     *     the read fails the rule and a failure carries no provenance
     */
    private FieldRead read(String name) {
        FieldRead read = inspect(name);
        reads.add(read);
        return read;
    }

    private FieldRead inspect(String name) {
        if (!record.contains(name)) {
            return new FieldRead(name, MISSING, null);
        }
        Object value = record.get(name);
        if (value == null) {
            return new FieldRead(name, NULL, null);
        }
        if (value instanceof String text) {
            return new FieldRead(name, PRESENT, text);
        }
        throw new FieldTypeException(name, value.getClass());
    }
}
