package io.github.radek11.dq.example.http;

import io.github.radek11.dq.input.DataRecord;
import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

/**
 * Reads a JSON array of records one element at a time: only the element being returned is held in
 * memory, so a request body of any length can feed the engine.
 *
 * <p>JSON values keep their kind — string, number, boolean, {@code null}, object as {@link Map},
 * array as {@link List} — so a rule reading a non-text field gets the library's type error
 * instead of a silently converted string. A repeated key keeps its last value.
 *
 * <p>Consumed once, not thread-safe. A syntax error ends the iteration with Jackson's exception.
 */
final class JsonRecords implements Iterator<DataRecord> {

    private final JsonParser parser;
    // First token of the next element, END_ARRAY at the end, null when not read yet.
    private JsonToken lookahead;

    /**
     * Reads up to the opening bracket, so a body that is not an array is rejected before anything
     * is returned.
     *
     * @throws IllegalArgumentException when the input is not a JSON array
     * @throws tools.jackson.core.JacksonException when the input is not valid JSON
     */
    JsonRecords(JsonParser parser) {
        this.parser = parser;
        JsonToken first = parser.nextToken();
        if (first != JsonToken.START_ARRAY) {
            throw new IllegalArgumentException("expected a JSON array, found " + kind(first));
        }
    }

    @Override
    public boolean hasNext() {
        if (lookahead == null) {
            lookahead = parser.nextToken();
            if (lookahead == JsonToken.END_ARRAY && parser.nextToken() != null) {
                throw new IllegalArgumentException("unexpected content after the end of the JSON array");
            }
        }
        return lookahead != JsonToken.END_ARRAY;
    }

    @Override
    public DataRecord next() {
        if (!hasNext()) {
            throw new NoSuchElementException();
        }
        JsonToken token = lookahead;
        lookahead = null;
        if (token == JsonToken.START_OBJECT) {
            return DataRecord.of(readObject());
        }
        parser.skipChildren();
        return new NotAnObject(kind(token));
    }

    private Map<String, Object> readObject() {
        Map<String, Object> fields = new LinkedHashMap<>();
        while (parser.nextToken() == JsonToken.PROPERTY_NAME) {
            String name = parser.currentName();
            fields.put(name, readValue(parser.nextToken()));
        }
        return fields;
    }

    private List<Object> readArray() {
        List<Object> values = new ArrayList<>();
        JsonToken token;
        while ((token = parser.nextToken()) != JsonToken.END_ARRAY) {
            values.add(readValue(token));
        }
        return values;
    }

    private Object readValue(JsonToken token) {
        return switch (token) {
            case VALUE_STRING -> parser.getString();
            case VALUE_NUMBER_INT, VALUE_NUMBER_FLOAT -> parser.getNumberValue();
            case VALUE_TRUE -> Boolean.TRUE;
            case VALUE_FALSE -> Boolean.FALSE;
            case VALUE_NULL -> null;
            case START_OBJECT -> readObject();
            case START_ARRAY -> readArray();
            default -> throw new IllegalStateException("unexpected JSON token " + token);
        };
    }

    // The kind only, never the value: request bodies hold company data (the library's rule, D10).
    private static String kind(JsonToken token) {
        if (token == null) {
            return "the end of input";
        }
        return switch (token) {
            case START_OBJECT -> "an object";
            case START_ARRAY -> "an array";
            case VALUE_STRING -> "a string";
            case VALUE_NUMBER_INT, VALUE_NUMBER_FLOAT -> "a number";
            case VALUE_TRUE, VALUE_FALSE -> "a boolean";
            case VALUE_NULL -> "null";
            default -> token.name();
        };
    }

    /**
     * An array element that is not an object. Reading it throws, which the engine reports as a
     * record failure for this element while the run continues (D27).
     */
    private record NotAnObject(String kind) implements DataRecord {

        @Override
        public boolean contains(String name) {
            throw failure();
        }

        @Override
        public Object get(String name) {
            throw failure();
        }

        private IllegalArgumentException failure() {
            return new IllegalArgumentException("the array element is " + kind + ", not a JSON object");
        }
    }
}
