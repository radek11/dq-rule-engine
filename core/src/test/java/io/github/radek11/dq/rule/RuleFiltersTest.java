package io.github.radek11.dq.rule;

import io.github.radek11.dq.output.Severity;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import static io.github.radek11.dq.output.Decision.REVIEW;
import static io.github.radek11.dq.rule.RuleStatus.DEPRECATED;
import static io.github.radek11.dq.rule.RuleStatus.DRAFT;
import static io.github.radek11.dq.rule.RuleStatus.RELEASED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

class RuleFiltersTest {

    private static final Scope DE = Scope.country("DE");
    private static final Scope FR = Scope.country("FR");

    private final Rule released = rule("released").status(RELEASED).build();
    private final Rule draft = rule("draft").status(DRAFT).build();
    private final Rule deprecated = rule("deprecated").status(DEPRECATED).build();

    private final Rule format = rule("format").categories("format").build();
    private final Rule compliance = rule("compliance").categories("compliance").build();
    private final Rule both = rule("both").categories("format", "compliance").build();
    private final Rule uncategorized = rule("uncategorized").build();

    private final Rule world = rule("world").build();
    private final Rule german = rule("german").scope(DE).build();
    private final Rule french = rule("french").scope(FR).build();

    @Test
    void statusSelectsRulesWithOneOfTheStatuses() {
        assertThat(select(RuleFilters.status(DRAFT, DEPRECATED), released, draft, deprecated))
                .containsExactly("draft", "deprecated");
    }

    @Test
    void categorySelectsRulesWithAtLeastOneOfTheCategories() {
        assertThat(select(RuleFilters.category("format", "unused"), format, compliance, both))
                .containsExactly("format", "both");
    }

    @Test
    void categoryNeverSelectsARuleWithoutCategories() {
        assertThat(select(RuleFilters.category("format", "compliance"), uncategorized)).isEmpty();
    }

    @Test
    void categoriesAreMatchedExactlyIncludingCase() {
        assertThat(select(RuleFilters.category("Format", "format "), format)).isEmpty();
    }

    @Test
    void scopeSelectsRulesWhoseDeclaredScopeIsOneOfTheGivenScopes() {
        assertThat(select(RuleFilters.scope(Scope.country("de")), world, german, french))
                .containsExactly("german");
        assertThat(select(RuleFilters.scope(Scope.WORLD, FR), world, german, french))
                .containsExactly("world", "french");
    }

    @Test
    void noArgumentsSelectNoRule() {
        assertThat(select(RuleFilters.status(), released, draft, deprecated)).isEmpty();
        assertThat(select(RuleFilters.category(), format, both)).isEmpty();
        assertThat(select(RuleFilters.scope(), world, german)).isEmpty();
    }

    @Test
    void aNullArgumentIsRejectedWhenTheFilterIsCreated() {
        assertThatNullPointerException().isThrownBy(() -> RuleFilters.status(RELEASED, null));
        assertThatNullPointerException().isThrownBy(() -> RuleFilters.category("format", null));
        assertThatNullPointerException().isThrownBy(() -> RuleFilters.scope(Scope.WORLD, null));
        assertThatNullPointerException().isThrownBy(() -> RuleFilters.status((RuleStatus[]) null));
        assertThatNullPointerException().isThrownBy(() -> RuleFilters.category((String[]) null));
        assertThatNullPointerException().isThrownBy(() -> RuleFilters.scope((Scope[]) null));
    }

    @Test
    void laterChangesToTheArgumentArrayDoNotAffectTheFilter() {
        RuleStatus[] statuses = {RELEASED};
        String[] categories = {"format"};
        Scope[] scopes = {DE};
        Predicate<Rule> byStatus = RuleFilters.status(statuses);
        Predicate<Rule> byCategory = RuleFilters.category(categories);
        Predicate<Rule> byScope = RuleFilters.scope(scopes);

        statuses[0] = DRAFT;
        categories[0] = "compliance";
        scopes[0] = FR;

        assertThat(select(byStatus, released, draft)).containsExactly("released");
        assertThat(select(byCategory, format, compliance)).containsExactly("format");
        assertThat(select(byScope, german, french)).containsExactly("german");
    }

    private static List<String> select(Predicate<Rule> filter, Rule... rules) {
        return List.of(rules).stream().filter(filter).map(Rule::id).toList();
    }

    private static Rule.Builder rule(String id) {
        return Rule.builder(id).label(id).status(RELEASED).severity(Severity.INFO)
                .logic(fields -> "ok")
                .mapping(DecisionMapping.of(Map.of(), REVIEW));
    }
}
