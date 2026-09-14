package io.github.radek11.dq.example.http;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.util.Objects;

/**
 * {@code POST /validate}: the body is a JSON array of records, the response is NDJSON (D25).
 *
 * <ul>
 *   <li>{@code 200} — one line per result or failure, then a {@code summary} line; an
 *       {@code error} line instead of the summary when the body breaks mid-run (D26);</li>
 *   <li>{@code 400} — the body is not a JSON array; one {@code error} line;</li>
 *   <li>{@code 405} — any other method.</li>
 * </ul>
 */
final class ValidateHandler implements HttpHandler {

    static final String NDJSON = "application/x-ndjson";

    private final BatchValidation validation;

    ValidateHandler(BatchValidation validation) {
        this.validation = Objects.requireNonNull(validation, "validation");
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        try (exchange) {
            if (!"POST".equals(exchange.getRequestMethod())) {
                exchange.getResponseHeaders().set("Allow", "POST");
                sendError(exchange, 405, "use POST with a JSON array of records");
                return;
            }
            JsonRecords records;
            try {
                records = validation.open(exchange.getRequestBody());
            } catch (BatchValidation.InvalidBodyException e) {
                sendError(exchange, 400, e.getMessage());
                return;
            }
            // Length 0 = chunked: the status goes out now, lines follow as the run produces them.
            exchange.getResponseHeaders().set("Content-Type", NDJSON);
            exchange.sendResponseHeaders(200, 0);
            validation.run(records, exchange.getResponseBody());
        }
    }

    private static void sendError(HttpExchange exchange, int status, String message) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", NDJSON);
        exchange.sendResponseHeaders(status, 0);
        try (NdjsonSink sink = new NdjsonSink(exchange.getResponseBody())) {
            sink.error(message);
        }
    }
}
