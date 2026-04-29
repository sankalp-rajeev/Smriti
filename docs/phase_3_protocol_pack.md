# Phase 3 Protocol Pack

Phase 3 Task 2 expands Smriti from the original demo ANC corpus into a local Global Protocol Pack v1 for country/region-aware CHW retrieval.

## What Changed

- The protocol corpus remains a local JSON asset at `app/src/main/assets/protocols/maternal_health_demo_protocols.json`.
- The pack now contains 46 structured maternal/ANC and CHW referral-support chunks.
- Required tags are present: `GLOBAL_CORE`, `INDIA`, `BANGLADESH`, `ETHIOPIA`, `AFRICA_REGION`, and `SOUTH_AMERICA_REGION`.
- Each chunk includes an id, region, country code when applicable, topic, keywords, citation, referral level when applicable, guidance text, and safety notes.

## Retrieval Behavior

`ProtocolRetriever.retrieve(...)` accepts an optional `ProtocolRetrievalContext`:

```text
countryCode: optional ISO-style country code
region: optional protocol region tag
```

When keyword matches are found, retrieval ranks local chunks in this order:

1. exact country match,
2. region match,
3. `GLOBAL_CORE` fallback.

Global emergency and danger-sign chunks remain available when country-specific chunks are absent. Retrieval is still deterministic keyword matching over a local JSON file. Smriti does not use cloud RAG, remote search, embeddings, or a vector database.

## Demo Context

The normal Meena demo remains India-focused:

```text
countryCode = IN
region = INDIA
```

VisitScreen shows this as `Protocol pack: India / Global fallback`. The existing Meena danger-sign transcript still retrieves the accepted demo danger-sign citation and produces non-diagnostic referral support.

## Scope And Safety

The pack supports global CHW settings as a scaffold, not as a clinically complete guideline library. Recommendations must be grounded in retrieved protocol chunks or marked uncertain. CHW review and confirm/save remain required before any generated visit record or referral flag is persisted.

## Tests

`ProtocolRetrieverTest` covers:

- at least 40 chunks load,
- required region tags exist,
- exact country match outranks region/global,
- region match outranks global,
- `GLOBAL_CORE` fallback works,
- the Meena danger-sign query still retrieves the accepted danger-sign citation,
- no network/vector dependency is introduced in the retriever.
