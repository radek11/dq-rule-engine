package io.github.radek11.dq.data;

import java.util.Objects;

/**
 * One field read by a rule — part of a result's provenance.
 *
 * @param name field name
 * @param presence whether the field was present, {@code null} or missing
 * @param value the text read, or {@code null} unless {@code presence} is {@link Presence#PRESENT}
 */
public record FieldRead(String name, Presence presence, String value) {

    /** State of a field at the moment it was read. */
    public enum Presence {
        PRESENT,
        NULL,
        MISSING
    }

    public FieldRead {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(presence, "presence");
    }
}
