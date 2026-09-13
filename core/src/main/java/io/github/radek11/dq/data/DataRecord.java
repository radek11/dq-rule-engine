package io.github.radek11.dq.data;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * One business-partner record, accessed by field name. The library does not parse JSON: the
 * host adapts whatever it reads into this interface.
 *
 * <p>Only top-level fields are addressed. A missing field, a field holding {@code null} and a
 * field holding an empty string are three different states.
 */
public interface DataRecord {

    /**
     * @param name field name
     * @return {@code true} when the field is present, even if its value is {@code null}
     */
    boolean contains(String name);

    /**
     * @param name field name
     * @return the field's value, or {@code null} when the field is missing or holds {@code null}
     */
    Object get(String name);

    /**
     * Wraps a map of field values. The map is copied; {@code null} values are allowed.
     *
     * @param fields field values by name
     * @return the record
     */
    static DataRecord of(Map<String, ?> fields) {
        Map<String, Object> copy = new HashMap<>(Objects.requireNonNull(fields, "fields"));
        return new DataRecord() {
            @Override
            public boolean contains(String name) {
                return copy.containsKey(name);
            }

            @Override
            public Object get(String name) {
                return copy.get(name);
            }

            // Field names only: records hold company and personal data that must not leak into logs.
            @Override
            public String toString() {
                return "DataRecord" + copy.keySet();
            }
        };
    }
}
