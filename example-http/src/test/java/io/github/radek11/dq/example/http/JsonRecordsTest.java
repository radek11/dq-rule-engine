package io.github.radek11.dq.example.http;

import io.github.radek11.dq.input.DataRecord;
import org.junit.jupiter.api.Test;
import tools.jackson.core.JacksonException;
import tools.jackson.core.ObjectReadContext;
import tools.jackson.core.json.JsonFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JsonRecordsTest {

    private static final JsonFactory JSON = new JsonFactory();

    @Test
    void theFixtureArrayGivesOneRecordPerElementWithItsFields() throws IOException {
        List<DataRecord> records = readAll(records(Files.readAllBytes(Path.of("fixture/records.json"))));

        assertThat(records).extracting(record -> record.get("id")).containsExactly("r1", "r2", "r3");
        assertThat(records.get(0).get("vatId")).isEqualTo("DE111111111");
        assertThat(records.get(2).contains("vatId")).isFalse();
        assertThat(records.get(2).contains("iban")).isTrue();
        assertThat(records.get(2).get("iban")).isEqualTo("");
    }

    @Test
    void valuesKeepTheirJsonKind() {
        DataRecord record = records("""
                [{"text": "DE", "number": 12, "decimal": 1.5, "flag": true, "nothing": null,
                  "object": {"a": "b"}, "array": [1, "x"]}]""").next();

        assertThat(record.get("text")).isEqualTo("DE");
        assertThat(record.get("number")).isEqualTo(12);
        assertThat(record.get("decimal")).isEqualTo(1.5);
        assertThat(record.get("flag")).isEqualTo(true);
        assertThat(record.contains("nothing")).isTrue();
        assertThat(record.get("nothing")).isNull();
        assertThat(record.get("object")).isEqualTo(Map.of("a", "b"));
        assertThat(record.get("array")).isEqualTo(List.of(1, "x"));
    }

    @Test
    void anElementThatIsNotAnObjectBecomesARecordThatFailsWhenReadAndReadingContinues() {
        List<DataRecord> records = readAll(records("""
                [1, "r1", null, [{"id": "inside"}], {"id": "r2"}]"""));

        assertThat(records).hasSize(5);
        assertThatIllegalArgumentException().isThrownBy(() -> records.get(0).contains("id"))
                .withMessage("the array element is a number, not a JSON object");
        assertThatIllegalArgumentException().isThrownBy(() -> records.get(1).get("id"))
                .withMessage("the array element is a string, not a JSON object");
        assertThatIllegalArgumentException().isThrownBy(() -> records.get(2).contains("id"))
                .withMessage("the array element is null, not a JSON object");
        assertThatIllegalArgumentException().isThrownBy(() -> records.get(3).contains("id"))
                .withMessage("the array element is an array, not a JSON object");
        assertThat(records.get(4).get("id")).isEqualTo("r2");
    }

    @Test
    void aSyntaxErrorInsideAnElementIsThrownByNextNotSwallowed() {
        JsonRecords records = records("""
                [{"id": "r1"}, {"id": oops}]""");

        assertThat(records.next().get("id")).isEqualTo("r1");
        // hasNext reads only the opening brace of the second element; its value is read by next.
        assertThat(records.hasNext()).isTrue();
        assertThatThrownBy(records::next).isInstanceOf(JacksonException.class);
    }

    @Test
    void anInputThatIsNotAJsonArrayIsRejectedWhenOpened() {
        assertThatIllegalArgumentException().isThrownBy(() -> records("""
                {"id": "r1"}""")).withMessage("expected a JSON array, found an object");
        assertThatIllegalArgumentException().isThrownBy(() -> records(""))
                .withMessage("expected a JSON array, found the end of input");
    }

    @Test
    void aTruncatedArrayIsASyntaxError() {
        JsonRecords records = records("""
                [{"id": "r1"}""");

        assertThat(records.next().get("id")).isEqualTo("r1");
        assertThatThrownBy(records::hasNext).isInstanceOf(JacksonException.class);
    }

    @Test
    void contentAfterTheEndOfTheArrayIsRejected() {
        JsonRecords records = records("""
                [{"id": "r1"}] [{"id": "r2"}]""");

        assertThat(records.next().get("id")).isEqualTo("r1");
        assertThatIllegalArgumentException().isThrownBy(records::hasNext)
                .withMessage("unexpected content after the end of the JSON array");
    }

    private static JsonRecords records(String json) {
        return records(json.getBytes(StandardCharsets.UTF_8));
    }

    private static JsonRecords records(byte[] json) {
        return new JsonRecords(JSON.createParser(ObjectReadContext.empty(), new ByteArrayInputStream(json)));
    }

    private static List<DataRecord> readAll(JsonRecords records) {
        List<DataRecord> all = new ArrayList<>();
        records.forEachRemaining(all::add);
        return all;
    }
}
