package io.github.radek11.dq.example.http;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/** The HTTP API end to end, on a real port, with the section 5 fixture sent as a JSON array. */
class HttpApiTest {

    private static final Pattern RESULT = Pattern.compile(
            "\\{\"type\":\"result\",\"ruleId\":\"(\\w+)\",\"recordId\":\"(\\w+)\",\"value\":\"\\w+\","
                    + "\"decision\":\"(\\w+)\",\"severity\":\"(\\w+)\",.*");

    // Section 5, "Expected results", one row per record; severities are the host's (D15).
    private static final List<String> FIXTURE_RESULTS = List.of(
            "r1 countryBlocked NOT_APPLICABLE ERROR", "r1 vatFormat VALID ERROR", "r1 ibanFormat VALID WARNING",
            "r2 countryBlocked NOT_APPLICABLE ERROR", "r2 vatFormat INVALID ERROR", "r2 ibanFormat VALID WARNING",
            "r3 countryBlocked INVALID ERROR", "r3 vatFormat INVALID ERROR", "r3 ibanFormat INVALID WARNING");

    private final HttpClient client = HttpClient.newHttpClient();
    private HttpServer server;

    @BeforeEach
    void startServer() throws IOException {
        server = Main.start(0);
    }

    @AfterEach
    void stopServer() {
        server.stop(0);
    }

    @Test
    void theFixtureGivesTheExpectedDecisionForEveryRecordAndRuleThenTheSummary() throws Exception {
        HttpResponse<String> response = post(fixture());

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.headers().firstValue("Content-Type")).hasValue("application/x-ndjson");
        List<String> lines = response.body().lines().toList();
        assertThat(lines.subList(0, 9)).map(HttpApiTest::result).containsExactlyElementsOf(FIXTURE_RESULTS);
        assertThat(lines.subList(9, lines.size())).containsExactly("""
                {"type":"summary","results":9,"failures":0,\
                "byDecision":{"VALID":3,"INVALID":4,"REVIEW":0,"NOT_APPLICABLE":2},\
                "bySeverity":{"ERROR":6,"WARNING":3,"INFO":0}}""");
    }

    @Test
    void aFailingRuleAndAnElementThatIsNotARecordAreReportedAndTheRestOfTheBatchCompletes() throws Exception {
        // r4 holds a number where vatFormat reads text; the fifth element is not an object at all.
        String body = fixture().stripTrailing();
        body = body.substring(0, body.length() - 1) + """
                , {"id": "r4", "vatId": 123, "country": "DE", "iban": "DE123456789"}, 42]""";

        List<String> lines = post(body).body().lines().toList();

        assertThat(lines.subList(0, 9)).map(HttpApiTest::result).containsExactlyElementsOf(FIXTURE_RESULTS);
        assertThat(lines.subList(9, lines.size())).containsExactly(
                "{\"type\":\"result\",\"ruleId\":\"countryBlocked\",\"recordId\":\"r4\",\"value\":\"ok\",\"decision\":\"NOT_APPLICABLE\",\"severity\":\"ERROR\",\"fieldsRead\":[{\"name\":\"country\",\"presence\":\"PRESENT\",\"value\":\"DE\"}]}",
                "{\"type\":\"failure\",\"kind\":\"rule\",\"ruleId\":\"vatFormat\",\"recordId\":\"r4\",\"recordIndex\":3,\"error\":\"io.github.radek11.dq.input.FieldTypeException\",\"message\":\"Field vatId is not text but Integer\"}",
                "{\"type\":\"result\",\"ruleId\":\"ibanFormat\",\"recordId\":\"r4\",\"value\":\"ok\",\"decision\":\"VALID\",\"severity\":\"WARNING\",\"fieldsRead\":[{\"name\":\"iban\",\"presence\":\"PRESENT\",\"value\":\"DE123456789\"},{\"name\":\"country\",\"presence\":\"PRESENT\",\"value\":\"DE\"}]}",
                "{\"type\":\"failure\",\"kind\":\"record\",\"recordIndex\":4,\"error\":\"java.lang.IllegalArgumentException\",\"message\":\"the array element is a number, not a JSON object\"}",
                "{\"type\":\"summary\",\"results\":11,\"failures\":2,\"byDecision\":{\"VALID\":4,\"INVALID\":4,\"REVIEW\":0,\"NOT_APPLICABLE\":3},\"bySeverity\":{\"ERROR\":7,\"WARNING\":4,\"INFO\":0}}");
    }

    @Test
    void aBodyThatIsNotAJsonArrayGets400WithAnErrorLine() throws Exception {
        HttpResponse<String> response = post("{\"id\": \"r1\"}");

        assertThat(response.statusCode()).isEqualTo(400);
        assertThat(response.body().lines())
                .containsExactly("{\"type\":\"error\",\"message\":\"expected a JSON array, found an object\"}");
    }

    @Test
    void anyMethodOtherThanPostGets405() throws Exception {
        HttpResponse<String> response = client.send(
                HttpRequest.newBuilder(uri()).GET().build(), HttpResponse.BodyHandlers.ofString());

        assertThat(response.statusCode()).isEqualTo(405);
        assertThat(response.headers().firstValue("Allow")).hasValue("POST");
    }

    private HttpResponse<String> post(String body) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(uri()).POST(HttpRequest.BodyPublishers.ofString(body)).build();
        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private URI uri() {
        return URI.create("http://localhost:" + server.getAddress().getPort() + "/validate");
    }

    private static String fixture() throws IOException {
        return Files.readString(Path.of("fixture/records.json"));
    }

    private static String result(String line) {
        Matcher m = RESULT.matcher(line);
        assertThat(m.matches()).as("a result line: %s", line).isTrue();
        return m.group(2) + " " + m.group(1) + " " + m.group(3) + " " + m.group(4);
    }
}
