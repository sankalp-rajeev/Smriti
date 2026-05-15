# Smriti System Architecture

Offline maternal-health memory for community health workers

```mermaid
flowchart LR
    Visit["CHW field visit<br/>Patient roster + local history + today's observation<br/>Synthetic demo data only"]

    Core["On-device Smriti core<br/>Room/SQLite local memory<br/>Local protocol pack<br/>Country/region-aware guidance<br/>No cloud required after setup<br/>Local guidance cited"]

    Gemma["LiteRT-LM Gemma 4 E2B<br/>Structured visit note<br/>from local context"]

    Gate["Review and save gate<br/>Citation + referral consistency checks<br/>Review screen<br/>CHW confirmation required"]

    Loop["After-visit loop<br/>Follow-up task<br/>Patient leave-behind message<br/>Supervisor summary<br/>Community Panel"]

    Probes["Validated Gemma 4 probes<br/>Paper-note vision extraction<br/>Audio Content.AudioBytes transcription probe<br/>Native lookupProtocol tool-calling probe<br/>Speculative decoding tested, +351 ms slower, not used"]

    Visit --> Core --> Gemma --> Gate --> Loop
    Probes -. "separate validated capabilities" .-> Gemma
```

Smriti starts from a CHW's field visit, not an empty chatbot prompt. Local memory and cited guidance shape the Gemma 4 note before the CHW reviews and saves it. The visit closes with follow-up, patient communication, supervisor visibility, and a Community Panel.

For README/media gallery, this Mermaid diagram can later be converted into a visual PNG/SVG.
