package io.github.radek11.dq.input;

/** Thrown by {@link FieldReader} when a field holds a value of an unexpected type. */
public final class FieldTypeException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final String fieldName;

    /**
     * @param fieldName name of the field
     * @param actualType type found in the record; the value itself is not included
     */
    public FieldTypeException(String fieldName, Class<?> actualType) {
        super("Field " + fieldName + " is not text but " + actualType.getSimpleName());
        this.fieldName = fieldName;
    }

    /** @return name of the field */
    public String fieldName() {
        return fieldName;
    }
}
