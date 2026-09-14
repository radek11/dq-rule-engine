package io.github.radek11.dq;

import io.github.radek11.dq.input.DataRecord;
import io.github.radek11.dq.rule.DecisionMapping;
import io.github.radek11.dq.rule.Rule;
import io.github.radek11.dq.rule.RuleStatus;

import java.util.List;
import java.util.Map;

import static io.github.radek11.dq.output.Decision.INVALID;
import static io.github.radek11.dq.output.Decision.NOT_APPLICABLE;
import static io.github.radek11.dq.output.Decision.REVIEW;
import static io.github.radek11.dq.output.Decision.VALID;
import static io.github.radek11.dq.output.Severity.ERROR;
import static io.github.radek11.dq.output.Severity.WARNING;

/**
 * The scenario from the task, section 5: three records, three rules.
 *
 * <p>Adaptations, stated in DESIGN.md: severities are not given by the task
 * (countryBlocked ERROR, vatFormat ERROR, ibanFormat WARNING); vatFormat and ibanFormat get a
 * {@code REVIEW} default, because a mapping always has one — the fixture never reaches it.
 */
public final class Fixture {

    public static final DataRecord R1 = DataRecord.of(Map.of(
            "id", "r1", "vatId", "DE111111111", "country", "DE",
            "legalName", "ACME GmbH", "iban", "DE123456789"));

    public static final DataRecord R2 = DataRecord.of(Map.of(
            "id", "r2", "vatId", "FR22", "country", "FR",
            "legalName", "Bricolage SARL", "iban", "FR123456789"));

    public static final DataRecord R3 = DataRecord.of(Map.of(
            "id", "r3", "country", "ZZ",
            "legalName", "Nowhere Ltd", "iban", ""));

    public static final Rule COUNTRY_BLOCKED = Rule.builder("countryBlocked")
            .label("Country is not blocked")
            .status(RuleStatus.RELEASED)
            .severity(ERROR)
            .categories("compliance")
            .logic(fields -> fields.text("country").filter("ZZ"::equals).isPresent() ? "blocked" : "ok")
            .mapping(DecisionMapping.of(Map.of("blocked", INVALID), NOT_APPLICABLE))
            .build();

    public static final Rule VAT_FORMAT = Rule.builder("vatFormat")
            .label("VAT id has at least 9 characters")
            .status(RuleStatus.RELEASED)
            .severity(ERROR)
            .categories("format")
            .logic(fields -> fields.text("vatId").filter(vat -> vat.length() >= 9).isPresent() ? "ok" : "bad")
            .mapping(DecisionMapping.of(Map.of("ok", VALID, "bad", INVALID), REVIEW))
            .build();

    // Reads both fields every time, so provenance does not depend on short-circuiting.
    public static final Rule IBAN_FORMAT = Rule.builder("ibanFormat")
            .label("IBAN starts with the record's country")
            .status(RuleStatus.RELEASED)
            .severity(WARNING)
            .categories("format")
            .logic(fields -> {
                var iban = fields.text("iban").filter(value -> !value.isEmpty());
                var country = fields.text("country");
                return iban.isPresent() && country.isPresent() && iban.get().startsWith(country.get())
                        ? "ok" : "bad";
            })
            .mapping(DecisionMapping.of(Map.of("ok", VALID, "bad", INVALID), REVIEW))
            .build();

    public static final List<DataRecord> RECORDS = List.of(R1, R2, R3);

    public static final List<Rule> RULES = List.of(COUNTRY_BLOCKED, VAT_FORMAT, IBAN_FORMAT);

    private Fixture() {
    }
}
