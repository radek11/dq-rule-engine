package io.github.radek11.dq.result;

import java.util.Objects;

/**
 * One field read by a rule — part of a result's provenance.
 *
 * @param name field name
 * @param presence whether the field was present, {@code null} or missing
 * @param value the text read; non-{@code null} exactly when {@code presence} is
 *     {@link Presence#PRESENT}, so {@code presence} alone tells the cases apart
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
        if ((presence == Presence.PRESENT) != (value != null)) {
            throw new IllegalArgumentException(
                    "value must be set exactly when presence is PRESENT; presence was " + presence);
        }
    }
}
