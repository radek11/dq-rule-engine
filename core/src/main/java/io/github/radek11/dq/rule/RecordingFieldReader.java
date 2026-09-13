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
        return Optional.ofNullable(read(name));
    }

    @Override
    public String requiredText(String name) {
        String value = read(name);
        if (value == null) {
            throw new MissingFieldException(name);
        }
        return value;
    }

    /** @return the reads so far, in order; a snapshot, later reads do not change it */
    List<FieldRead> reads() {
        return List.copyOf(reads);
    }

    /**
     * Reads a field and records the read. Missing and {@code null} look the same to the rule
     * but not in provenance: a missing field was never sent, a {@code null} one was sent empty.
     *
     * @return the text, or {@code null} when the field is missing or holds {@code null}
     * @throws FieldTypeException when the field holds a non-text value; not recorded, because
     *     the read fails the rule and a failure carries no provenance
     */
    private String read(String name) {
        if (!record.contains(name)) {
            reads.add(new FieldRead(name, MISSING, null));
            return null;
        }
        Object value = record.get(name);
        if (value == null) {
            reads.add(new FieldRead(name, NULL, null));
            return null;
        }
        if (!(value instanceof String text)) {
            throw new FieldTypeException(name, value.getClass());
        }
        reads.add(new FieldRead(name, PRESENT, text));
        return text;
    }
}
