package io.github.radek11.dq.example.http;

import io.github.radek11.dq.rule.DecisionMapping;
import io.github.radek11.dq.rule.Rule;
import io.github.radek11.dq.rule.RuleCatalog;
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
 * The host's rule catalog: the three rules from the task, section 5. The host, not the library,
 * owns its rules (T-21); the core module's tests keep their own copy, and the HTTP end-to-end
 * test checks this one against the expected table independently.
 */
final class FixtureRules {

    static final List<Rule> RULES = List.of(
            Rule.builder("countryBlocked")
                    .label("Country is not blocked")
                    .status(RuleStatus.RELEASED)
                    .severity(ERROR)
                    .categories("compliance")
                    .logic(fields -> fields.text("country").filter("ZZ"::equals).isPresent() ? "blocked" : "ok")
                    .mapping(DecisionMapping.of(Map.of("blocked", INVALID), NOT_APPLICABLE))
                    .build(),
            Rule.builder("vatFormat")
                    .label("VAT id has at least 9 characters")
                    .status(RuleStatus.RELEASED)
                    .severity(ERROR)
                    .categories("format")
                    .logic(fields -> fields.text("vatId").filter(vat -> vat.length() >= 9).isPresent() ? "ok" : "bad")
                    .mapping(DecisionMapping.of(Map.of("ok", VALID, "bad", INVALID), REVIEW))
                    .build(),
            // Reads both fields every time, so provenance does not depend on short-circuiting.
            Rule.builder("ibanFormat")
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
                    .build());

    static RuleCatalog catalog() {
        return RuleCatalog.of(RULES);
    }

    private FixtureRules() {
    }
}
