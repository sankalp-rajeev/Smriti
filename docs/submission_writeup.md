# Smriti: Local Memory for the Health Worker Who Shows Up

For the ones who show up.

## 1. A Health Worker Carries Memory

A community health worker does not only carry forms.

She carries memory.

Who missed a follow-up. Who had danger signs last week. Which patient forgot the card. Which visit still needs to be explained to the family.

"If she forgets the card, we check it in our record," an ASHA worker in Udaipur told researchers. But if the record is not there, the memory breaks. Smriti is built for that moment. [Yale Global Health Review](https://yaleglobalhealthreview.com/2017/05/14/consider-the-asha-a-qualitative-analysis-of-accredited-social-health-activists-experiences-in-udaipur-india/)

Every two minutes, a woman dies from pregnancy or childbirth complications. WHO estimates about 260,000 maternal deaths in 2023; most were preventable, and approximately 92% occurred in low- and lower-middle-income countries. [WHO maternal mortality fact sheet](https://www.who.int/news-room/fact-sheets/detail/maternal-mortality) [WHO/UN maternal mortality report](https://www.who.int/publications/i/item/9789240108462)

Smriti means memory. It is offline local memory for the health worker who shows up: a CHW-facing Android app for visit documentation, local guidance, follow-up, patient communication, and supervisor visibility. It starts with the roster and turns one home visit into a reviewed note, a follow-up task, a patient message, and a community view.

The demo patient, Meena, is synthetic. The memory problem is real. A worker can move between homes, paper records, family conversations, and the next person waiting; Smriti makes that memory visible and reviewable.

## 2. Why This Has to Work Offline

Hospital AI scribes assume doctors, EHRs, and stable WiFi. Cloud chatbots assume the internet will be there. Form tools collect data, but they often stop before the field loop closes: history, guidance, follow-up, patient message, supervisor view.

That gap matters because the last mile is often offline. ITU estimated that 2.6 billion people were offline in 2024. Globally, 83% of urban dwellers used the internet, compared with 48% of rural populations, and 1.8 billion of the people offline lived in rural areas. [ITU internet use](https://www.itu.int/itu-d/reports/statistics/2024/11/10/ff24-internet-use/) [ITU urban/rural gap](https://www.itu.int/itu-d/reports/statistics/2024/11/10/ff24-internet-use-in-urban-and-rural-areas/)

WHO projects an 11 million health-worker shortfall by 2030, mostly in low- and lower-middle-income countries. India has more than one million ASHA workers connecting communities with health systems and supporting maternal care. [WHO health workforce](https://www.who.int/health-topics/health-workforce) [WHO India ASHA workers](https://www.who.int/india/india-asha-workers)

This is not an India-only pattern. Ethiopia's Health Extension Workers, Kenya's CHWs, Bangladesh's BRAC community health workers, Brazil's ACS agents, and Nigeria's frontline PHC workforce all point to the same need: local memory, local guidance, and follow-up support that can be adapted country by country.

Smriti is designed for supervisor-led setup: APK sideload, app-private model transfer, and no required internet after setup. The core visit path is local: roster, patient history, protocol pack, model, review, save, follow-up, and summary.

## 3. The Visit Loop

Smriti follows the rhythm of a field visit.

A CHW opens a patient, sees prior history, captures today's observation, checks local guidance, generates a cited structured visit note with on-device Gemma 4, reviews it, confirms it, and saves it locally. After the visit, Smriti creates the follow-up task, prepares an editable patient leave-behind message, updates the supervisor summary, and refreshes the Community Panel.

This is why the first screen is a roster, not a chat box. A CHW starts from a person and a history, not from an empty prompt. Meena's prior ANC history is already on the device. Today's observation includes danger signs such as severe headache, blurred vision, high blood pressure, and reduced fetal movement. Smriti retrieves local danger-sign guidance before drafting referral-support wording for review.

The filmed loop is deliberately concrete: roster, Meena, today's observation, local guidance, on-device cited note, ReviewScreen, confirm/save, follow-up task, patient leave-behind, supervisor summary, and Community Panel. It is a product loop, not a model demo with a health label.

The app is Android native: Kotlin, Jetpack Compose, Room/SQLite local storage, a local JSON country/region-aware protocol pack, and LiteRT-LM Gemma 4 E2B for on-device text generation.

## 4. Where Gemma 4 Fits

Gemma 4 is inside the workflow, not exposed as open chat.

The transformation is:

`observation + local patient history + cited protocol guidance -> structured visit note for CHW review`

RealGemmaAgent runs Gemma 4 E2B through LiteRT-LM after local setup is complete. The generated note then goes through parser, citation, and safety checks before it reaches ReviewScreen and the CHW confirm-save gate.

The hard part was not calling a model. The hard part was making a health workflow refuse bad output. LiteRT-LM setup is gated. The model is large and sideloaded app-privately, not bundled or downloaded at runtime. RealGemmaAgent is app-facing only when setup is complete. Invalid structure is rejected. Invented or missing citations are rejected. Transcripts are preserved on failure. Confirm/save is a local Room/SQLite write only. Smriti does not fall back to mock clinical output.

That boundary is visible in the app: unavailable reasoning produces setup/retry messaging, not a fabricated clinical note, and the model is never treated as the source of truth.

Selected demo note languages are English, Hindi, Spanish, and Swahili; full app UI translation and broad all-language support are not claimed. Gemma audio fills an editable transcript only. Gemma vision supports synthetic paper-note data-entry support only. Native tool-calling was validated as a manual probe, but production retrieval remains deterministic through ProtocolRetriever.

## 5. Safety Is the Shape of the Product

When Smriti is uncertain, it stops.

There is a review screen before save. CHW confirmation is required. Citations are checked. Referral wording must agree with the structured flag. Urgent Protocol Lookup shows local health guidance, but it does not save anything by itself. Patient messages are editable and shared only by the user. Follow-up tasks are workflow support, not clinical decisions.

Smriti does not diagnose, prescribe, calculate dosage, decide referral action by itself, auto-send messages, use real patient data, or claim clinical validation. Paper-note scan is not clinical image diagnosis or paper-note OCR diagnosis. It is reviewed data entry.

A fast answer is not enough. The worker needs to see the source, edit the text, and decide whether it belongs in the record. In a health workflow, a visible stop is better than a confident-looking record that should not be trusted.

## 6. Evidence and Limits

- LiteRT-LM 0.11.0 with Gemma 4 E2B through RealGemmaAgent.
- Local JSON protocol pack with country/region-aware retrieval.
- On-device text generation runs around 15-22 seconds on CPU depending on scenario/device.
- Local retrieval, validation, save, follow-up creation, summary, and panel refresh are milliseconds.
- Speculative decoding was tested: 21787 ms baseline vs 22138 ms speculative, +351 ms slower, not used.
- Audio: `Content.AudioBytes` probe succeeded; microphone path remains editable transcript only, not direct clinical reasoning.
- Vision: synthetic paper-note extraction supports CHW-reviewed data entry only.
- Tool-calling: manual `lookupProtocol` probe returned cited guidance; production retrieval remains deterministic ProtocolRetriever.
- Validation/build history includes `testDebugUnitTest`, `assembleDebug`, `compileDebugAndroidTestKotlin`, `runSmriti.ps1 -FinalUi`, and `runSmriti.ps1 -Logs`.

The measured pattern matters: model generation is the expensive step; retrieval, validation, local save, follow-up creation, summary refresh, and panel refresh are small local operations. That separation keeps the CHW confirm/save gate fast and keeps the app honest about where the latency comes from.

Smriti is not clinically validated and has not been field-deployed with CHWs. The protocol pack is a scaffold and needs expert review before real deployment. The repository uses synthetic data only, with no PHI. A model sideload is required.

## 7. Visual Proof

The submission media gallery should make the project understandable at a glance: the CHW memory gap, the visit-loop visual, the Smriti system architecture, one full product loop, a technical evidence card, a safety boundary card, and final app screenshots. The goal is for judges to see both the human story and the engineered refusal boundaries without reading every line first.

## 8. What Smriti Proves

Smriti proves that local patient memory, a country/region-aware protocol layer, on-device Gemma 4, and CHW review can become a realistic offline maternal-health workflow.

The patient leaves with a message. The CHW leaves with a task. The supervisor sees the day without needing the cloud.

The important part is not that the model can write. It is that the model is placed inside a workflow that knows when to retrieve, when to cite, when to ask for review, and when to fail visibly.

Smriti means memory. I built it so no health worker has to carry that memory alone.
