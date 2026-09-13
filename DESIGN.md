# Design

## 1. Public API and why it is shaped this way

One entry point, push-style output:

```java
RuleEngine engine = new RuleEngine(RuleCatalog.of(rules));
RunSummary summary = engine.run(records, RunOptions.defaults().withBatchSize(500), sink);
```

Packages, dependencies pointing one way (`data` and `result` depend on nothing):

| Package | Public types | Hidden (package-private) |
|---|---|---|
| `dq` | `RuleEngine` — coordinates a run; `RunOptions` | `RuleSelection` — rules of a run; `RunCounters` — summary counts |
| `dq.rule` | `Rule` — evaluates itself on a record; `RuleLogic`, `DecisionMapping`, `Scope`, `RuleStatus`, `RuleCatalog`, `RuleFilters` | `RecordingFieldReader` — records reads as provenance |
| `dq.data` | `DataRecord` — record by field name; `FieldReader` — what logic reads through; field exceptions | — |
| `dq.result` | `Result`, `Failure` (sealed: `RuleFailure`, `RecordFailure`), `RunSummary`, `ResultSink`, `Decision`, `Severity`, `FieldRead` | — |

Who does what in a run:

```
RuleEngine.run
  RuleSelection.of(catalog, filter)      snapshot, duplicate ids, filter, RELEASED only
  per batch, per record:                 id and country, or a RecordFailure
    per selected rule:
      rule.appliesTo(country)            scope decides
      rule.evaluate(record, id)          logic via recording reader → value → mapping → Result
      failure → RuleFailure              isolation stays in the engine
      sink.onResult / onFailure          outside the try: a failing sink ends the run
      RunCounters.add
    sink.onBatchEnd
  RunCounters.toSummary()
```

Decisions, each with the alternative it beat:

- **Behaviour lives in the objects that own the data.** `Rule` is a class whose logic and
  mapping are private; `rule.evaluate(record, id)` is the only way to run it, so the mapping
  and the "null value is a fault" contract cannot be bypassed. Start-of-run invariants live in
  `RuleSelection`, counting in `RunCounters`; the engine only coordinates and isolates.
  *Rejected:* `Rule` as a record read by the engine (`rule.logic()`, `rule.mapping()`) — every
  invariant would sit in one long method, testable only through a full run.
- **Values crossing the boundary are records.** `Result`, `RuleFailure`, `RecordFailure`, `RunSummary`, `FieldRead`,
  `RunOptions` are data the host reads, serializes and asserts on. Kinds of failure are told
  apart by type (sealed `Failure`), never by nullable fields. *Rejected:* hiding them
  behind interfaces — no behaviour to protect, only more code for the host.

- **Push, not pull.** `run(records, options, sink)` returns a summary and emits everything
  else to the sink. *Rejected:* `Stream<Outcome>` — the summary is only complete after a
  terminal operation and the stream must be closed. With push the engine keeps nothing beyond
  the current batch; what the sink keeps is the host's choice. A slow sink slows the run,
  because the engine calls it synchronously.
- **Clear line between isolated and fatal.** A failing rule or an unusable record becomes a
  `Failure` and the run goes on. Unreadable input, a failing sink, a broken catalog and JVM
  errors end the run — they are faults of the host or the platform, and reporting them as
  rule failures would hide them.
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

*To be written (E6).* Notes collected so far:

- **Parallelism within a run.** A run is sequential on the caller's thread; separate runs can
  share one engine. Not parallel yet: no throughput target, rules are cheap string checks and
  the sink (I/O) is the likely bottleneck. The design keeps the change local: `Rule.evaluate`
  shares no state, and the batch is a natural unit of work. Plan: an optional host-supplied
  `Executor` in `RunOptions`; evaluate a batch in parallel, then emit its outcomes to the sink
  in input order on the caller's thread. The sink stays single-threaded and ordered; memory
  grows to batch size × rules. Only `RuleEngine.run` and counter merging change.

## Assumptions

*To be written (E6).*
