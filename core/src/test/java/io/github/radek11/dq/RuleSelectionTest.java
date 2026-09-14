package io.github.radek11.dq;

import io.github.radek11.dq.output.Severity;
import io.github.radek11.dq.rule.DecisionMapping;
import io.github.radek11.dq.rule.Rule;
import io.github.radek11.dq.rule.RuleCatalog;
import io.github.radek11.dq.rule.RuleFilters;
import io.github.radek11.dq.rule.RuleStatus;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static io.github.radek11.dq.Fixture.COUNTRY_BLOCKED;
import static io.github.radek11.dq.Fixture.IBAN_FORMAT;
import static io.github.radek11.dq.Fixture.RULES;
import static io.github.radek11.dq.Fixture.VAT_FORMAT;
import static io.github.radek11.dq.output.Decision.REVIEW;
import static io.github.radek11.dq.rule.RuleStatus.DEPRECATED;
import static io.github.radek11.dq.rule.RuleStatus.DRAFT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class RuleSelectionTest {

    @Test
    void releasedRulesAreSelectedInCatalogOrder() {
        RuleSelection selection = RuleSelection.of(RuleCatalog.of(RULES), RuleFilters.all());

        assertThat(selection.rules()).containsExactly(COUNTRY_BLOCKED, VAT_FORMAT, IBAN_FORMAT);
    }

    @Test
    void draftAndDeprecatedRulesAreNeverSelectedEvenWhenTheFilterAcceptsThem() {
        Rule draft = rule("draft", DRAFT);
        Rule deprecated = rule("deprecated", DEPRECATED);

        RuleSelection selection = RuleSelection.of(
                RuleCatalog.of(List.of(draft, VAT_FORMAT, deprecated)), RuleFilters.all());

        assertThat(selection.rules()).containsExactly(VAT_FORMAT);
    }

    @Test
    void theFilterNarrowsTheSelection() {
        RuleSelection selection = RuleSelection.of(
                RuleCatalog.of(RULES), rule -> rule.severity() == Severity.WARNING);

        assertThat(selection.rules()).containsExactly(IBAN_FORMAT);
    }

    @Test
    void aFilterSelectingNothingGivesAnEmptySelection() {
        RuleSelection selection = RuleSelection.of(RuleCatalog.of(RULES), rule -> false);

        assertThat(selection.rules()).isEmpty();
    }

    @Test
    void aDuplicateIdIsRejectedEvenWhenOneOfTheRulesWouldNotBeSelected() {
        Rule draftTwin = rule(VAT_FORMAT.id(), DRAFT);
        RuleCatalog catalog = () -> List.of(COUNTRY_BLOCKED, VAT_FORMAT, draftTwin);

        assertThatIllegalArgumentException()
                .isThrownBy(() -> RuleSelection.of(catalog, rule -> false))
                .withMessageContaining(VAT_FORMAT.id());
    }

    @Test
    void theCatalogIsReadOnceAndLaterChangesDoNotAffectTheSelection() {
        AtomicInteger reads = new AtomicInteger();
        List<Rule> live = new ArrayList<>(RULES);
        RuleCatalog catalog = () -> {
            reads.incrementAndGet();
            return live;
        };

        RuleSelection selection = RuleSelection.of(catalog, RuleFilters.all());
        live.clear();

        assertThat(reads).hasValue(1);
        assertThat(selection.rules()).containsExactly(COUNTRY_BLOCKED, VAT_FORMAT, IBAN_FORMAT);
    }

    private static Rule rule(String id, RuleStatus status) {
        return Rule.builder(id).label(id).status(status).severity(Severity.INFO)
                .logic(fields -> "ok")
                .mapping(DecisionMapping.of(Map.of(), REVIEW))
                .build();
    }
}
