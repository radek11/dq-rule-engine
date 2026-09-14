# Design

## 1. Public API and why it is shaped this way

One entry point, push-style output:

```java
RuleEngine engine = new RuleEngine(RuleCatalog.of(rules));
RunSummary summary = engine.run(records, RunOptions.defaults().withBatchSize(500), sink);
```

Packages follow the flow of a run, dependencies pointing one way (`input` and `output` depend on nothing):

| Package | Role | Public types | Hidden (package-private) |
|---|---|---|---|
| `dq.input` | what comes in | `DataRecord` — record by field name; `FieldReader` — what logic reads through; field exceptions | — |
| `dq.rule` | what is checked | `Rule` — evaluates itself on a record; `RuleLogic`, `DecisionMapping`, `Scope`, `RuleStatus`, `RuleCatalog`, `RuleFilters` | `RecordingFieldReader` — records reads as provenance |
| `dq` | how a run proceeds | `RuleEngine` — coordinates a run; `RunOptions` | `RuleSelection` — rules of a run; `RecordIdentifier` — id and country, or a rejection; `CountingSink` — host's sink with counting attached; `RunCounters` — summary counts |
| `dq.output` | what comes out | `Result`, `Failure` (sealed: `RuleFailure`, `RecordFailure`), `RunSummary`, `ResultSink`, `Decision`, `Severity`, `FieldRead` | — |

*Rejected:* one package per function (`selection`, `evaluation`, `counting`). Package-private
access ends at the package boundary, so the helpers would have to become public API.
`Decision` and `Severity` are also rule vocabulary, but they stay in `output`: moving them to
`rule` would make `output` depend on `rule`.

Who does what in a run:

```
RuleEngine.run
  RuleSelection.of(catalog, filter)      snapshot, duplicate ids, filter, RELEASED only
  new CountingSink(sink)                 everything the host accepts is counted
  per batch of batchSize records:
    per record:
      RecordIdentifier.identify          Identified(record, id, country) or Rejected(RecordFailure)
        Rejected → onFailure             no rule runs on the record
        Identified → per selected rule:
          rule.appliesTo(country)        scope decides
          rule.evaluate(record, id)      the only call inside try: logic → value → mapping → Result
          exception → RuleFailure        isolation stays in the engine; an Error is not caught
          onResult / onFailure           outside the try: a failing sink ends the run
    onBatchEnd(recordsSoFar)
  CountingSink.toSummary()
```

*Rejected:* counting next to each sink call in the engine — three places to keep in step, and
a missed one would make the summary disagree with the sink.

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

### Isolation: what becomes a failure, what ends the run

A failure belongs to one record or one pair of record and rule; it is reported and the run
goes on. A fault of the host or the platform ends the run.

| Isolated — a `Failure` in the sink | Ends the run — reaches the caller |
|---|---|
| a rule's logic throws (`RuleFailure`) | the input iterator throws |
| a record has no usable id, a non-text id or country, or throws when read (`RecordFailure`) | the sink throws |
| | the catalog or the filter throws, or two rules share an id — before any record is read |
| | a JVM `Error` |

In the rule loop only `rule.evaluate` sits inside the `try`, and it catches `Exception`. The
sink call is outside it.

*Rejected:* the sink call inside the `try`. A sink that fails to take a result would have its
exception reported as a `RuleFailure`: the rule is blamed for the host's fault, and the run
completes with that result lost. *Rejected:* `catch (Throwable)`. After an `OutOfMemoryError` or
`StackOverflowError` the JVM's state is unknown; reporting it as one rule's failure and going
on would hide it. Both alternatives were tried as mutations and each fails a named test.

A consequence to know: the engine reads a whole batch before evaluating it, so if the input
fails in the middle of a batch, the records already read in that batch are not evaluated.

### Memory: what the engine holds

During a run the engine holds:

- the selected rules — fixed for the run, as large as the catalog;
- the current batch — at most `batchSize` records;
- the counters for the summary — fixed-size arrays;
- one `Result` or `Failure` at a time, until the sink returns.

It does not hold results, failures or record ids. So it does not detect duplicate record ids:
that would need a set growing with the input.

Memory therefore grows with `batchSize` and the size of a record, not with the number of
records. What the sink keeps is the host's choice. *Assumption:* "bounded regardless of total
volume" is about the number of records; one huge record still takes what it weighs.

### Memory: measured

`./gradlew :core:memoryTest` runs 1,000,000 generated records through the three fixture rules
and one rule that fails on a missing VAT id (2,750,000 results, 500,000 failures), with the
default batch of 1,000 and a sink that only counts. The input is generated as it is read.

| Heap limit | Outcome |
|---|---|
| 64 MB | passes, 9 s |
| 16 MB | passes, 11–22 s over three runs — the task's limit |
| 12 MB | passes, 16 s |
| 8 MB | passes, 101 s: most of the time goes to garbage collection |
| 6 MB | `OutOfMemoryError` |

The limit is twice the smallest passing heap. These numbers include JUnit and the Gradle test
worker, so they are an upper bound for the engine itself. The test proves something only if a
wrong engine fails it: an engine that keeps every result fails with `OutOfMemoryError` at 16 MB
and at 64 MB. Time is reported, not promised — the task sets no throughput target and gives no
hardware.

Only one volume is measured. That memory does not grow with volume rests on the list above;
the test confirms that 1,000,000 records fit where keeping their results does not.

### Threads

One engine can serve concurrent runs. Its only field is the catalog, and all run state —
selected rules, batch, counters — is local to the call to `run`. This holds under three
conditions the host provides: the catalog can be read from several threads, rule logic keeps
no state, and each run gets its own sink (or a thread-safe one). *Rejected:* a test running
many threads on one engine. A race it misses still passes, so a green result would claim more
than it shows.

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
