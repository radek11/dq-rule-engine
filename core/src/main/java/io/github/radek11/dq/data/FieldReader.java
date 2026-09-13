package io.github.radek11.dq.data;

import java.util.Optional;

/**
 * The only way rule logic reads a record. Every read is recorded and becomes the provenance of
 * the {@link io.github.radek11.dq.result.Result Result}, so provenance always matches what the rule actually looked at — including
 * reads of fields that are missing.
 *
 * <p>Instances are created by the engine for a single rule on a single record.
 */
public interface FieldReader {

    /**
     * Reads an optional text field.
     *
     * @param name field name
     * @return the value, or empty when the field is missing or {@code null}
     * @throws FieldTypeException when the field holds a non-text value
     */
    Optional<String> text(String name);

    /**
     * Reads a text field the rule cannot work without. A missing field becomes a failure of
     * this rule on this record, reported next to the results.
     *
     * @param name field name
     * @return the value
     * @throws MissingFieldException when the field is missing or {@code null}
     * @throws FieldTypeException when the field holds a non-text value
     */
    String requiredText(String name);
}
