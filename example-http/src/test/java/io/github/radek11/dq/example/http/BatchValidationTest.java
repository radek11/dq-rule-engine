package io.github.radek11.dq.example.http;

import io.github.radek11.dq.RuleEngine;
import io.github.radek11.dq.RunOptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BatchValidationTest {

    private static final String R1 = """
            {"id": "r1", "vatId": "DE111111111", "country": "DE", "iban": "DE123456789"}""";
    private static final String R2 = """
            {"id": "r2", "vatId": "FR22", "country": "FR", "iban": "FR123456789"}""";

    private final BatchValidation validation =
            new BatchValidation(new RuleEngine(FixtureRules.catalog()), RunOptions.defaults().withBatchSize(1));

    @Test
    @Timeout(10)
    void theResultsOfTheFirstRecordReachTheClientBeforeTheSecondRecordIsSent() throws Exception {
        PipedOutputStream upload = new PipedOutputStream();
        PipedInputStream body = new PipedInputStream(upload);
        FlushedLines client = new FlushedLines();
        CompletableFuture<Void> server = CompletableFuture.runAsync(() -> validation.run(validation.open(body), client));

        send(upload, "[" + R1 + ",");
        List<String> firstRecord = List.of(client.nextLine(), client.nextLine(), client.nextLine());
        assertThat(firstRecord).allMatch(line -> line.contains("\"recordId\":\"r1\""));

        send(upload, R2 + "]");
        upload.close();
        server.get(5, TimeUnit.SECONDS);
        assertThat(client.remaining()).hasSize(4).last().asString().startsWith("{\"type\":\"summary\",\"results\":6,");
    }

    @Test
    void aSyntaxErrorAfterSomeRecordsEndsTheResponseWithAnErrorLineInsteadOfASummary() {
        List<String> lines = validate("[" + R1 + ",\n{\"id\": oops}]");

        assertThat(lines).hasSize(4);
        assertThat(lines.subList(0, 3)).allMatch(line -> line.startsWith("{\"type\":\"result\""));
        assertThat(lines.get(3)).isEqualTo("{\"type\":\"error\",\"message\":\"malformed JSON at line 2, column 8\"}");
    }

    @Test
    void aBodyCutOffMidRecordAlsoEndsWithAnErrorLine() {
        List<String> lines = validate("[" + R1 + ", {\"id\": \"r2\"");

        assertThat(lines).hasSize(4);
        assertThat(lines.get(3)).startsWith("{\"type\":\"error\",\"message\":\"malformed JSON at line 1, column ");
    }

    @Test
    void contentAfterTheArrayEndsTheResponseWithTheReasonInsteadOfASummary() {
        List<String> lines = validate("[" + R1 + "] []");

        assertThat(lines).hasSize(4);
        assertThat(lines.get(3))
                .isEqualTo("{\"type\":\"error\",\"message\":\"unexpected content after the end of the JSON array\"}");
    }

    @Test
    void aRecordOverTheParserNestingLimitEndsTheResponseWithAnErrorLineInsteadOfASummary() {
        List<String> lines = validate("[" + R1 + ", {\"id\": \"r2\", \"deep\": " + nested(600) + "}]");

        assertThat(lines).hasSize(4);
        assertThat(lines.get(3)).startsWith("{\"type\":\"error\",\"message\":\"JSON exceeds a parser limit");
    }

    @Test
    void aBodyThatIsANumberOverTheParserLengthLimitIsRejectedWhenOpened() {
        assertThatThrownBy(() -> validation.open(body("1".repeat(1001))))
                .isInstanceOf(BatchValidation.InvalidBodyException.class)
                .hasMessageStartingWith("JSON exceeds a parser limit");
    }

    @Test
    void aBodyThatIsNotAnArrayIsRejectedBeforeAnythingIsWrittenAndTheMessageHidesTheContent() {
        assertThatThrownBy(() -> validation.open(body("{\"id\": \"r1\"}")))
                .isInstanceOf(BatchValidation.InvalidBodyException.class)
                .hasMessage("expected a JSON array, found an object");
        assertThatThrownBy(() -> validation.open(body("ACME GmbH")))
                .isInstanceOf(BatchValidation.InvalidBodyException.class)
                .hasMessageStartingWith("malformed JSON at line 1, column ")
                .hasMessageNotContaining("ACME");
    }

    private List<String> validate(String json) {
        ByteArrayOutputStream client = new ByteArrayOutputStream();
        validation.run(validation.open(body(json)), client);
        return client.toString(StandardCharsets.UTF_8).lines().toList();
    }

    // Jackson's default StreamReadConstraints allow a nesting depth of 500.
    private static String nested(int depth) {
        return "[".repeat(depth) + "]".repeat(depth);
    }

    private static ByteArrayInputStream body(String json) {
        return new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));
    }

    private static void send(OutputStream upload, String json) throws IOException {
        upload.write(json.getBytes(StandardCharsets.UTF_8));
        upload.flush();
    }

    /** A client that sees only what was flushed to it, line by line. */
    private static final class FlushedLines extends OutputStream {

        private final ByteArrayOutputStream pending = new ByteArrayOutputStream();
        private final LinkedBlockingQueue<String> lines = new LinkedBlockingQueue<>();

        @Override
        public synchronized void write(int b) {
            pending.write(b);
        }

        @Override
        public synchronized void write(byte[] b, int off, int len) {
            pending.write(b, off, len);
        }

        @Override
        public synchronized void flush() {
            pending.toString(StandardCharsets.UTF_8).lines().forEach(lines::add);
            pending.reset();
        }

        String nextLine() throws InterruptedException {
            String line = lines.poll(5, TimeUnit.SECONDS);
            assertThat(line).as("a flushed line within 5 s").isNotNull();
            return line;
        }

        List<String> remaining() {
            return List.copyOf(lines);
        }
    }
}
