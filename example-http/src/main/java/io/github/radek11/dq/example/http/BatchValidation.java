package io.github.radek11.dq.example.http;

import io.github.radek11.dq.RuleEngine;
import io.github.radek11.dq.RunOptions;
import io.github.radek11.dq.output.RunSummary;
import tools.jackson.core.JsonParser;
import tools.jackson.core.ObjectReadContext;
import tools.jackson.core.TokenStreamLocation;
import tools.jackson.core.exc.StreamReadException;
import tools.jackson.core.json.JsonFactory;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Objects;

/**
 * Validates one request body into one response body, in two steps so the HTTP status can depend
 * on the first: {@link #open} rejects a body that is not a JSON array before anything is sent;
 * {@link #run} streams the lines. Independent of the HTTP server, so it is testable on plain streams.
 */
final class BatchValidation {

    private static final JsonFactory JSON = new JsonFactory();

    private final RuleEngine engine;
    private final RunOptions options;

    BatchValidation(RuleEngine engine, RunOptions options) {
        this.engine = Objects.requireNonNull(engine, "engine");
        this.options = Objects.requireNonNull(options, "options");
    }

    /**
     * Reads the body up to the opening bracket.
     *
     * @throws InvalidBodyException when the body is not a JSON array; the message names where and
     *                              why, never the content
     */
    JsonRecords open(InputStream body) {
        JsonParser parser = JSON.createParser(ObjectReadContext.empty(), body);
        try {
            return new JsonRecords(parser);
        } catch (IllegalArgumentException | StreamReadException e) {
            parser.close();
            throw new InvalidBodyException(describe(e), e);
        }
    }

    /**
     * Runs the engine over the records and writes NDJSON lines to the response, ending with a
     * summary line, or with an error line when the input breaks mid-run (D23, D26). Closes the
     * response.
     *
     * @throws RuntimeException when the response cannot be written, for example the client left
     */
    void run(JsonRecords records, OutputStream response) {
        try (NdjsonSink sink = new NdjsonSink(response)) {
            RunSummary summary;
            try {
                summary = engine.run(records, options, sink);
            } catch (IllegalArgumentException | StreamReadException e) {
                sink.error(describe(e));
                return;
            }
            sink.summary(summary);
        }
    }

    // Jackson's own message may quote the offending input, which is company data (D10): only the
    // location is kept. JsonRecords' messages name the kind of JSON value, never the value.
    private static String describe(RuntimeException e) {
        if (e instanceof StreamReadException read) {
            TokenStreamLocation at = read.getLocation();
            return at == null ? "malformed JSON"
                    : "malformed JSON at line " + at.getLineNr() + ", column " + at.getColumnNr();
        }
        return e.getMessage();
    }

    /** The request body cannot start a run; the message is safe to return to the client. */
    static final class InvalidBodyException extends RuntimeException {

        private static final long serialVersionUID = 1L;

        InvalidBodyException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
