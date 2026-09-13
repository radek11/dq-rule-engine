package io.github.radek11.dq.rule;

import io.github.radek11.dq.data.DataRecord;
import io.github.radek11.dq.data.MissingFieldException;
import io.github.radek11.dq.result.Decision;
import io.github.radek11.dq.result.FieldRead;
import io.github.radek11.dq.result.Result;
import io.github.radek11.dq.result.Severity;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static io.github.radek11.dq.Fixture.COUNTRY_BLOCKED;
import static io.github.radek11.dq.Fixture.IBAN_FORMAT;
import static io.github.radek11.dq.Fixture.R1;
import static io.github.radek11.dq.Fixture.R2;
import static io.github.radek11.dq.Fixture.R3;
import static io.github.radek11.dq.Fixture.VAT_FORMAT;
import static io.github.radek11.dq.result.Decision.INVALID;
import static io.github.radek11.dq.result.Decision.NOT_APPLICABLE;
import static io.github.radek11.dq.result.Decision.REVIEW;
import static io.github.radek11.dq.result.Decision.VALID;
import static io.github.radek11.dq.result.FieldRead.Presence.MISSING;
import static io.github.radek11.dq.result.FieldRead.Presence.PRESENT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Named.named;
import static org.junit.jupiter.params.provider.Arguments.arguments;

class RuleEvaluateTest {

    private static FieldRead present(String name, String value) {
        return new FieldRead(name, PRESENT, value);
    }

    /** The expected-results table from the task, section 5, with value and provenance made explicit. */
    static Stream<Arguments> fixtureTable() {
        return Stream.of(
                arguments(named("r1", R1), "r1", named(COUNTRY_BLOCKED.id(), COUNTRY_BLOCKED), "ok", NOT_APPLICABLE, List.of(present("country", "DE"))),
                arguments(named("r1", R1), "r1", named(VAT_FORMAT.id(), VAT_FORMAT), "ok", VALID, List.of(present("vatId", "DE111111111"))),
                arguments(named("r1", R1), "r1", named(IBAN_FORMAT.id(), IBAN_FORMAT), "ok", VALID,
                        List.of(present("iban", "DE123456789"), present("country", "DE"))),

                arguments(named("r2", R2), "r2", named(COUNTRY_BLOCKED.id(), COUNTRY_BLOCKED), "ok", NOT_APPLICABLE, List.of(present("country", "FR"))),
                arguments(named("r2", R2), "r2", named(VAT_FORMAT.id(), VAT_FORMAT), "bad", INVALID, List.of(present("vatId", "FR22"))),
                arguments(named("r2", R2), "r2", named(IBAN_FORMAT.id(), IBAN_FORMAT), "ok", VALID,
                        List.of(present("iban", "FR123456789"), present("country", "FR"))),

                arguments(named("r3", R3), "r3", named(COUNTRY_BLOCKED.id(), COUNTRY_BLOCKED), "blocked", INVALID, List.of(present("country", "ZZ"))),
                arguments(named("r3", R3), "r3", named(VAT_FORMAT.id(), VAT_FORMAT), "bad", INVALID, List.of(new FieldRead("vatId", MISSING, null))),
                arguments(named("r3", R3), "r3", named(IBAN_FORMAT.id(), IBAN_FORMAT), "bad", INVALID,
                        List.of(present("iban", ""), present("country", "ZZ"))));
    }

    @ParameterizedTest(name = "{0} × {2} → {4}")
    @MethodSource("fixtureTable")
    void fixtureRecordGetsTheExpectedDecisionValueAndProvenance(
            DataRecord record, String recordId, Rule rule, String value, Decision decision, List<FieldRead> reads) {

        Result result = rule.evaluate(record, recordId);

        assertThat(result.ruleId()).isEqualTo(rule.id());
        assertThat(result.recordId()).isEqualTo(recordId);
        assertThat(result.value()).isEqualTo(value);
        assertThat(result.decision()).isEqualTo(decision);
        assertThat(result.severity()).isEqualTo(rule.severity());
        assertThat(result.fieldsRead()).containsExactlyElementsOf(reads);
    }

    @Test
    void changingOnlyTheMappingChangesTheDecision() {
        Rule strict = ruleReturning("blocked", DecisionMapping.of(Map.of("blocked", INVALID), NOT_APPLICABLE));
        Rule lenient = ruleReturning("blocked", DecisionMapping.of(Map.of("blocked", REVIEW), NOT_APPLICABLE));

        assertThat(strict.evaluate(R1, "r1").decision()).isEqualTo(INVALID);
        assertThat(lenient.evaluate(R1, "r1").decision()).isEqualTo(REVIEW);
    }

    @Test
    void anUnexpectedValueGetsTheDefaultDecision() {
        Rule rule = ruleReturning("surprise", DecisionMapping.of(Map.of("ok", VALID), REVIEW));

        Result result = rule.evaluate(R1, "r1");

        assertThat(result.value()).isEqualTo("surprise");
        assertThat(result.decision()).isEqualTo(REVIEW);
    }

    @Test
    void aNullValueIsAFaultNamingTheRule() {
        Rule rule = ruleReturning(null, DecisionMapping.of(Map.of(), REVIEW));

        assertThatIllegalStateException()
                .isThrownBy(() -> rule.evaluate(R1, "r1"))
                .withMessageContaining(rule.id());
    }

    @Test
    void faultsFromTheLogicPropagateBecauseIsolationIsTheEnginesJob() {
        Rule rule = Rule.builder("broken").label("broken").status(RuleStatus.RELEASED).severity(Severity.INFO)
                .logic(fields -> fields.requiredText("doesNotExist"))
                .mapping(DecisionMapping.of(Map.of(), REVIEW))
                .build();

        assertThatThrownBy(() -> rule.evaluate(R1, "r1")).isInstanceOf(MissingFieldException.class);
    }

    @Test
    void recordAndRecordIdAreRequired() {
        assertThatNullPointerException().isThrownBy(() -> VAT_FORMAT.evaluate(null, "r1"));
        assertThatNullPointerException().isThrownBy(() -> VAT_FORMAT.evaluate(R1, null));
    }

    private static Rule ruleReturning(String value, DecisionMapping mapping) {
        return Rule.builder("test").label("test").status(RuleStatus.RELEASED).severity(Severity.INFO)
                .logic(fields -> value)
                .mapping(mapping)
                .build();
    }
}
