# Smriti System Architecture

Offline maternal-health memory for community health workers

```mermaid
flowchart LR
    CHW["CHW field visit<br/>Roster + local patient history<br/>Typed observation<br/>Synthetic demo data only"]

    Inputs["Field inputs<br/>Typed note<br/>Gemma audio transcript -> editable transcript only<br/>Paper-note scan -> data-entry support only"]

    Memory["On-device memory<br/>Room / SQLite<br/>Patients, visits, referrals, follow-ups<br/>Saved on this device"]

    Protocols["Local JSON protocol pack<br/>Country/region-aware retrieval<br/>Cited local guidance<br/>No cloud APIs after setup"]

    Gemma["Gemma 4 E2B through LiteRT-LM<br/>RealGemmaAgent<br/>Drafts structured cited note<br/>Runs on device after setup"]

    Checks["Parser + citation + safety checks<br/>No diagnosis<br/>No treatment/dosage<br/>No autonomous referral"]

    Review["ReviewScreen<br/>Generated note<br/>Local guidance / citation<br/>Referral-support status<br/>CHW confirm/save gate"]

    Loop["After-save loop<br/>Follow-up task<br/>Patient leave-behind message<br/>Supervisor summary<br/>Community Panel"]

    Proof["Offline Proof<br/>Local Room database<br/>Local protocol pack<br/>LiteRT-LM Gemma 4<br/>CHW review required"]

    CHW --> Inputs
    Inputs --> Memory
    Inputs --> Protocols
    Memory --> Gemma
    Protocols --> Gemma
    Gemma --> Checks
    Checks --> Review
    Review --> Memory
    Review --> Loop
    Memory --> Loop
    Protocols --> Proof
    Memory --> Proof
    Gemma --> Proof
```

## Safety Boundaries

- Smriti is documentation and referral support, not diagnosis.
- No treatment or dosage advice is generated as a product claim.
- No autonomous referral: the CHW reviews, edits, confirms, and saves.
- No cloud APIs are required after setup for the filmed runtime.
- Audio is transcript-only: it fills an editable transcript and does not save or generate clinical output by itself.
- Vision is data-entry support only: paper-note extraction goes to review and does not diagnose or create a referral from an image alone.
- Demo data is synthetic only.

For README or media gallery use, export this Mermaid diagram to PNG/SVG in a clean phone/product frame. Keep the safety labels visible enough that the visual reads as an offline field product, not a general chatbot.
