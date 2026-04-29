# Phase 3 Benchmarks

Phase 3 Task 3 adds a synthetic global benchmark suite for local protocol retrieval and mock visit reasoning.

These cases are synthetic protocol-scaffold tests. They are not clinical validation, do not use PHI, and do not claim the protocol pack is clinically complete.

## Runner

The unit-test runner executes each case through:

```text
ProtocolRetriever
-> VisitReasoningPipeline
-> MockGemmaAgent
```

The normal benchmark path does not require RealGemma, model files, audio files, cloud APIs, runtime downloads, vector search, or direct Gemma audio.

Per case, the runner records:

- retrieved/output citation strings,
- selected retrieval level: exact country, region, global core, or none,
- whether a referral flag was produced,
- whether the result stayed uncertain,
- clarification prompt presence when expected,
- pass/fail reason.

## Case Table

| Case | Context | Expected retrieval | Expected behavior | Proves |
| --- | --- | --- | --- | --- |
| India ANC danger signs | `IN / INDIA` | Exact country | Referral, not uncertain | Existing Meena-style danger-sign behavior is preserved |
| India normal ANC follow-up | `IN / INDIA` | Exact country | No referral, not uncertain | Routine India ANC support does not create a false referral |
| Bangladesh maternal danger sign | `BD / BANGLADESH` | Exact country | Referral, not uncertain | Bangladesh country chunks ground danger-sign support |
| Ethiopia HEW maternal danger sign | `ET / ETHIOPIA` | Exact country | Referral, not uncertain | Ethiopia country chunks ground danger-sign support |
| Africa-region fallback | `KE / AFRICA_REGION` | Region | Referral, not uncertain | Region fallback outranks global when country chunks are absent |
| South America-region fallback | `PE / SOUTH_AMERICA_REGION` | Region | Referral, not uncertain | South America regional fallback works without exact country chunks |
| Global core fallback | `NP / SOUTH_ASIA_REGION` | Global core | Referral, not uncertain | Global emergency chunks remain available when country/region are absent |
| Vague incomplete observation | Global context | None | No referral, uncertain, asks clarification | Missing protocol/details stays uncertain instead of guessing |
| No-danger-sign routine visit | Global context | Global core | No referral, not uncertain | Routine global ANC retrieval avoids false referral |
| Return visit with prior history | `IN / INDIA` | Exact country | Referral, not uncertain | Prior visit context flows through the local reasoning pipeline |

## Current Local/Mock Status

`GlobalSyntheticBenchmarkTest` verifies:

- at least 10 synthetic cases exist,
- required countries/regions are represented,
- each case declares citation, referral, uncertainty, retrieval, and notes expectations,
- all cases pass through `ProtocolRetriever + VisitReasoningPipeline + MockGemmaAgent`,
- danger-sign cases trigger referral,
- routine cases do not create false referral,
- incomplete/vague case remains uncertain and asks clarification,
- fallback cases retrieve region/global chunks correctly,
- existing Meena demo behavior is preserved,
- `AgentConfig.DEFAULT_MODE` remains `MOCK`,
- RealGemma developer mode remains gated.

## Accepted RealGemma Manual Benchmark

The accepted manual RealGemma text benchmark remains separate from the normal synthetic benchmark suite:

- `totalScenarios=3`
- `successCount=3`
- `parserSuccessCount=3`
- `referralCount=1`
- `citationCount=2`
- `singleCitationContractCount=3`
- `averageLatencyMs=15812`
- `maxLatencyMs=26272`

RealGemma benchmarks require explicit developer/manual setup and a sideloaded app-private model. The normal synthetic suite remains mock-backed and deterministic.
