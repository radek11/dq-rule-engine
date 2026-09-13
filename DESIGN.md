# Design

## 1. Public API and why it is shaped this way

One entry point, push-style output:

```java
RuleEngine engine = new RuleEngine(RuleCatalog.of(rules));
RunSummary summary = engine.run(records, RunOptions.defaults().withBatchSize(500), sink);
```

| Type | Role |
|---|---|
| `RuleEngine` | Runs a catalog over records. Holds no run state, safe to share between threads. |
| `DataRecord` | A record accessed by field name. The host adapts its input (JSON, database row) to it. |
| `Rule` | Id, label, status, severity, scope, categories, `RuleLogic`, `DecisionMapping`. Immutable. |
| `RuleLogic` | `FieldReader → String` value. Computes a token such as `"ok"`; never a decision. |
| `DecisionMapping` | Value → `Decision`, as data, with a mandatory fallback. |
| `FieldReader` | The only way logic reads a record. Every read is recorded as provenance. |
| `ResultSink` | Receives each `Result` and `Failure` as it is produced, plus batch boundaries. |
| `RunSummary` | Counts of results and failures, results by decision and by severity. |
| `RunOptions` | Batch size, rule filter (`Predicate<Rule>`), id and country field names. |
| `RuleCatalog` | Source of rules; read once per run. |

Decisions, each with the alternative it beat:

- **Push, not pull.** `run(records, options, sink)` returns a summary and emits everything
  else to the sink. *Rejected:* `Stream<Outcome>` — the summary is only complete after a
  terminal operation and the stream must be closed. Push keeps memory bounded by
  construction (the engine keeps nothing) and a slow sink slows the run, because the engine
  calls it synchronously.
- **Records are field maps, not POJOs.** `DataRecord.get("vatId")`. *Rejected:* a
  `BusinessPartner` class — master data differs per country and per client, and a fixed
  class makes the library single-use. The core has no JSON dependency; parsing is the host's
  job.
- **Provenance is recorded, not declared.** Logic reads through `FieldReader`, which logs
  every read, including reads of missing fields. *Rejected:* rules declaring the fields they
  use — a declaration can drift from the code; a recording cannot.
- **Two kinds of read.** `text(name)` returns empty for a missing field; `requiredText(name)`
  throws `MissingFieldException`, which becomes a `Failure`. *Rejected:* a missing field is
  always a failure — the fixture expects `vatFormat` to return `INVALID` for a record without
  `vatId`. The rule decides whether absence is data or a fault.
- **Result carries the computed value.** *Rejected:* decision and fields only — without the
  value, provenance cannot explain which mapping entry produced the decision.
- **The mapping fallback is mandatory**, enforced by `DecisionMapping.of(cases, fallback)`.
  *Rejected:* optional fallback with unmapped values as failures — one typo in a token would
  produce a failure for every record.
- **Filters are `Predicate<Rule>`.** Composition (`and`, `or`, `negate`) comes from the JDK.
  *Rejected:* a filter over `(Rule, Record)` — rule selection would change per record, so the
  set of rules in a run would not be fixed.
- **Engine invariants cannot be filtered away:** only `RELEASED` rules run, and only on records
  their scope applies to. Filters can narrow a run, never widen it.
- **Failures are not called errors.** `Severity.ERROR` is rule metadata; `Failure` is a fault
  during evaluation. The type name also avoids a clash with `java.lang.Error`.

## 2. Fault isolation and bounded memory

*To be written with the implementation (E2–E3).*

## 3. Seams for host-supplied inputs

*To be written (E6).*

## 4. Trade-offs and what I would do next

*To be written (E6).*

## Assumptions

*To be written (E6).*
