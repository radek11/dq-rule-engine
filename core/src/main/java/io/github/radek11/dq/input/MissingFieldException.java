package io.github.radek11.dq.input;

/** Thrown by {@link FieldReader#requiredText(String)} when a required field is absent. */
public final class MissingFieldException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final String fieldName;

    /** @param fieldName name of the missing field */
    public MissingFieldException(String fieldName) {
        super("Required field is missing: " + fieldName);
        this.fieldName = fieldName;
    }

    /** @return name of the missing field */
    public String fieldName() {
        return fieldName;
    }
}
