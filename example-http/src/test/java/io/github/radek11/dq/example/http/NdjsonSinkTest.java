package io.github.radek11.dq.example.http;

import io.github.radek11.dq.input.MissingFieldException;
import io.github.radek11.dq.output.FieldRead;
import io.github.radek11.dq.output.RecordFailure;
import io.github.radek11.dq.output.Result;
import io.github.radek11.dq.output.RuleFailure;
import io.github.radek11.dq.output.RunSummary;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static io.github.radek11.dq.output.Decision.INVALID;
import static io.github.radek11.dq.output.Decision.VALID;
import static io.github.radek11.dq.output.FieldRead.Presence.MISSING;
import static io.github.radek11.dq.output.FieldRead.Presence.NULL;
import static io.github.radek11.dq.output.FieldRead.Presence.PRESENT;
import static io.github.radek11.dq.output.Severity.ERROR;
import static io.github.radek11.dq.output.Severity.WARNING;
import static org.assertj.core.api.Assertions.assertThat;

class NdjsonSinkTest {

    private final ByteArrayOutputStream client = new ByteArrayOutputStream();
    private final NdjsonSink sink = new NdjsonSink(client);

    @Test
    void aResultIsOneLineWithItsProvenanceAndAValueOnlyForAPresentField() {
        sink.onResult(new Result("vatFormat", "r3", "bad", INVALID, ERROR, List.of(
                new FieldRead("vatId", MISSING, null),
                new FieldRead("legalName", NULL, null),
                new FieldRead("country", PRESENT, "ZZ"))));
        sink.close();

        assertThat(written()).isEqualTo("""
                {"type":"result","ruleId":"vatFormat","recordId":"r3","value":"bad","decision":"INVALID",\
                "severity":"ERROR","fieldsRead":[{"name":"vatId","presence":"MISSING"},\
                {"name":"legalName","presence":"NULL"},{"name":"country","presence":"PRESENT","value":"ZZ"}]}
                """);
    }

    @Test
    void aRuleFailureNamesTheRuleTheRecordItsPositionAndTheCause() {
        sink.onFailure(new RuleFailure("ibanFormat", "r2", 1, new MissingFieldException("iban")));
        sink.close();

        assertThat(written()).isEqualTo("""
                {"type":"failure","kind":"rule","ruleId":"ibanFormat","recordId":"r2","recordIndex":1,\
                "error":"io.github.radek11.dq.input.MissingFieldException","message":"%s"}
                """.formatted(new MissingFieldException("iban").getMessage()));
    }

    @Test
    void aRecordFailureWithoutAnIdOrMessageLeavesThoseFieldsOut() {
        sink.onFailure(new RecordFailure(Optional.empty(), 3, new NullPointerException()));
        sink.onFailure(new RecordFailure(Optional.of("r9"), 4, new IllegalArgumentException("blank")));
        sink.close();

        assertThat(written()).isEqualTo("""
                {"type":"failure","kind":"record","recordIndex":3,"error":"java.lang.NullPointerException"}
                {"type":"failure","kind":"record","recordId":"r9","recordIndex":4,\
                "error":"java.lang.IllegalArgumentException","message":"blank"}
                """);
    }

    @Test
    void theSummaryLineCountsEveryDecisionAndSeverityIncludingZerosAndIsFlushed() {
        sink.summary(new RunSummary(3, 1, Map.of(VALID, 2L, INVALID, 1L), Map.of(ERROR, 2L, WARNING, 1L)));

        assertThat(written()).isEqualTo("""
                {"type":"summary","results":3,"failures":1,\
                "byDecision":{"VALID":2,"INVALID":1,"REVIEW":0,"NOT_APPLICABLE":0},\
                "bySeverity":{"ERROR":2,"WARNING":1,"INFO":0}}
                """);
    }

    @Test
    void theErrorLineIsWrittenAndFlushedWithoutClosing() {
        sink.error("malformed JSON at line 3, column 7");

        assertThat(written()).isEqualTo("""
                {"type":"error","message":"malformed JSON at line 3, column 7"}
                """);
    }

    @Test
    void linesWrittenSoFarReachTheClientAtTheEndOfABatch() {
        sink.onResult(new Result("countryBlocked", "r1", "ok", VALID, ERROR, List.of()));
        sink.onBatchEnd(1);

        assertThat(written()).startsWith("{\"type\":\"result\",\"ruleId\":\"countryBlocked\"").endsWith("}\n");
    }

    private String written() {
        return client.toString(StandardCharsets.UTF_8);
    }
}
