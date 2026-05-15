# Smriti: Local Memory for the Health Worker Who Shows Up

For the ones who show up.

## 1. A Health Worker Carries Memory

A community health worker does not only carry forms.

She carries memory.

Who missed a follow-up. Who had danger signs last week. Which patient forgot the card. Which visit still needs to be explained to the family.

"If she forgets the card, we check it in our record," an ASHA worker in Udaipur told researchers. But if the record is not there, the memory breaks. Smriti is built for that moment. [Yale Global Health Review](https://yaleglobalhealthreview.com/2017/05/14/consider-the-asha-a-qualitative-analysis-of-accredited-social-health-activists-experiences-in-udaipur-india/)

Every two minutes, a woman dies from pregnancy or childbirth complications. WHO estimates about 260,000 maternal deaths in 2023; most were preventable, and approximately 92% occurred in low- and lower-middle-income countries. [WHO maternal mortality fact sheet](https://www.who.int/news-room/fact-sheets/detail/maternal-mortality) [WHO/UN maternal mortality report](https://www.who.int/publications/i/item/9789240108462)

Smriti means memory. It is offline local memory for the health worker who shows up: a CHW-facing Android app for visit documentation, local guidance, follow-up, patient communication, and supervisor visibility. It starts with the patient roster and ends with a reviewed record, follow-up task, patient message, and community view.

The patient in the demo, Meena, is synthetic. But the memory problem is real. A health worker can move between homes, paper records, family conversations, and the next person waiting; Smriti is built to make that memory visible and reviewable.

## 2. Why This Has to Work Offline

Hospital AI scribes assume doctors, EHRs, and stable WiFi. Cloud chatbots assume the internet will be there. Form tools collect data, but they do not close the field loop: patient history, local guidance, follow-up task, patient message, and supervisor view.

That gap matters because the last mile is often offline. ITU estimated that 2.6 billion people were offline in 2024. Globally, 83% of urban dwellers used the internet, compared with 48% of rural populations, and 1.8 billion of the people offline lived in rural areas. [ITU internet use](https://www.itu.int/itu-d/reports/statistics/2024/11/10/ff24-internet-use/) [ITU urban/rural gap](https://www.itu.int/itu-d/reports/statistics/2024/11/10/ff24-internet-use-in-urban-and-rural-areas/)

At the same time, WHO projects an 11 million health-worker shortfall by 2030, mostly in low- and lower-middle-income countries. India has more than one million ASHA workers connecting communities with health systems and supporting maternal care. [WHO health workforce](https://www.who.int/health-topics/health-workforce) [WHO India ASHA workers](https://www.who.int/india/india-asha-workers)

This is not an India-only pattern. Ethiopia's Health Extension Workers, Kenya's CHWs, Bangladesh's BRAC community health workers, Brazil's ACS agents, and Nigeria's frontline PHC workforce all point to the same need: local memory, local guidance, and follow-up support that can be adapted country by country.

Smriti is designed for supervisor-led setup: APK sideload, app-private model transfer, and no required internet after setup.

That setup choice keeps the technology in the hands of a local program instead of assuming every CHW has a cloud account, live connectivity, or an EHR login. After setup, the core visit path is local: roster, patient history, protocol pack, model, review, save, follow-up, and summary.

## 3. The Visit Loop

Smriti follows the rhythm of a field visit.

A CHW opens a patient, sees prior history, captures today's observation, checks local guidance, generates a cited structured visit note with on-device Gemma 4, reviews it, confirms it, and saves it locally. After the visit, Smriti creates the follow-up task, prepares an editable patient leave-behind message, updates the supervisor summary, and refreshes the Community Panel.

The path is simple: patient history -> today's observation -> local guidance -> on-device Gemma note -> CHW review/save -> follow-up task -> patient message -> community panel.

The app is Android native: Kotlin, Jetpack Compose, Room/SQLite local storage, a local JSON country/region-aware protocol pack, and LiteRT-LM Gemma 4 E2B for on-device text generation. Core runtime features do not require cloud APIs after setup.

This is why the first screen is a roster, not a chat box. A CHW starts from a person and a history, not from an empty prompt. Gemma 4 enters after the app already knows the patient, the history, and the local guidance.

## 4. Product Choices That Matter

Audio helps the worker type less; it does not create the record by itself. Audio transcription, when enabled, only populates the editable observation field; note generation and saving remain separate CHW actions.

Vision reads paper notes for data entry review. It is not patient identification.

Local guidance is retrieved deterministically before the model writes. The generated note must be structured, cited, and checked before it can be saved.

If a note cannot be parsed, cited, or checked, it is not saved. The worker keeps the transcript and can retry or continue manually.

The app does not diagnose, prescribe, calculate medication dosage, or decide referral action by itself. It provides local protocol-guided documentation and referral-support wording for CHW review.

A fast answer is not enough. The worker needs to see the source, edit the text, and decide whether it belongs in the record.

## 5. Where Gemma 4 Fits

Gemma 4 is used where it helps the visit, not where it would replace the worker.

On-device Gemma 4 through LiteRT-LM turns today's observation, local patient context, and cited local guidance into a structured visit note. The output then passes through parsing, citation, and referral-language checks before the CHW sees it on the review screen.

Selected demo languages are English, Hindi, Spanish, and Swahili. Gemma vision supports paper-note extraction for review only. Gemma audio transcription was validated as a LiteRT-LM 0.11.0 manual probe using `Content.AudioBytes` with `audioBackend=CPU`; the microphone path remains transcript-only and device-dependent. Native tool-calling was also validated as a manual probe: `lookupProtocol` returned the citation "Smriti Demo Maternal Health Protocol Danger Signs 1.1." The production flow remains local guidance first, Gemma note second, CHW review/save third.

## 6. Safety Is the Shape of the Product

When Smriti is uncertain, it stops.

There is a review screen before save. CHW confirmation is required. Citations are checked. Referral wording must agree with the structured flag. The consistency checks cover English, Hindi, Spanish, and Swahili, the selected demo languages.

Urgent Protocol Lookup shows local guidance, but it does not save anything by itself. Patient messages are editable and shared only by the user. Follow-up tasks are workflow support, not clinical decisions.

Smriti is safer because uncertainty is visible. If generation or validation fails, Smriti does not quietly produce a polished substitute. It leaves the observation available and lets the worker retry or continue manually. In a health workflow, a visible stop is better than a confident-looking record that should not be trusted.

## 7. Evidence and Limits

- On-device text generation runs around 15-22 seconds on CPU depending on scenario/device. The result is cited and CHW-reviewed.
- Local retrieval, validation, save, follow-up creation, summary, and panel refresh are milliseconds.
- Speculative decoding: 21787 ms baseline vs 22138 ms speculative, +351 ms, not used.
- Audio: manual `Content.AudioBytes` probe succeeded; microphone path remains transcript-only and device-dependent.
- Tool-calling: manual `lookupProtocol` probe returned cited guidance; production retrieval remains deterministic.
- Validation commands from the build history include `testDebugUnitTest`, `assembleDebug`, `compileDebugAndroidTestKotlin`, `runSmriti.ps1 -FinalUi`, and `runSmriti.ps1 -Logs`.

Smriti is not clinically validated and has not been field-deployed with CHWs. The protocol pack is a demo scaffold. The repository uses synthetic data only, with no real patient data or PHI. A model sideload is required. The selected demo languages are English, Hindi, Spanish, and Swahili. More countries and languages require reviewed protocol packs, translations, and referral pathways.

## 8. What Smriti Proves

Smriti proves that local patient memory, a country/region-aware protocol layer, on-device Gemma 4, and CHW review can become a realistic offline maternal-health workflow.

The patient leaves with a message. The CHW leaves with a task. The supervisor sees the day without needing the cloud.

The important part is not that the model can write. It is that the model is placed inside a workflow that knows when to retrieve, when to cite, when to ask for review, and when to fail visibly.

Smriti means memory. I built it so no health worker has to carry that memory alone.
