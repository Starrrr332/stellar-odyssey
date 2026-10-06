# Task Assignment: Survey Rocket Models, Emissives, StarMap GUI & Obsidian Sync (R3, R4)

## Context
You are spec_miner_survey_rockets_starmap, a teamwork_preview_spec_miner subagent working on the Stellar Odyssey space exploration mod (Architectury multi-loader, Minecraft 26.3, Java 25).
Your working directory is:
c:/Users/amaro/Documents/antigravity/blissful-lavoisier/.agents/teamwork/spec_miner_survey_rockets_starmap

## Inputs
- Authoritative requirements: `c:/Users/amaro/Documents/antigravity/blissful-lavoisier/.agents/teamwork/ORIGINAL_REQUEST.md` (specifically section 2026-10-06T17:13:21Z).
- Project documentation: `c:/Users/amaro/Documents/antigravity/blissful-lavoisier/PROJECT.md` and `RESUMEN_PARA_OTRA_IA.md`.
- Codebase packages:
  - `common/src/main/java/com/amaro/stellarodyssey/rocket/`
  - `common/src/main/java/com/amaro/stellarodyssey/client/`
  - `common/src/main/java/com/amaro/stellarodyssey/satellites/starmap/`
  - `common/src/main/resources/assets/stellarodyssey/`
  - Build files (`build.gradle`, tests in `common/src/test/java/`)
  - Target Obsidian Vault: `C:\Users\amaro\OneDrive\Documents\Obsidian Vault\`

## Objective
Thoroughly explore the existing codebase and extract specifications for R3 and R4:
1. R3. Modelos 3D & Renderizado para Cohetes Tier 2 & Tier 3:
   - What rocket models and entities currently exist? (`RocketEntity`, `RocketTierRegistry`, `RocketModel` or renderers).
   - How are Java 3D models and UV atlases structured for Tier 1 (Pioneer), and how should Tier 2 (Voyager) and Tier 3 (Odyssey) models be registered/rendered?
   - How does `SubmitNodeCollector` or the emissive rendering pipeline work for Celidium (T2) and Astralite/Verdantite (T3)?
2. R4. Interfaz de Navegación Estelar (StarMap GUI) & Selección de Destino:
   - What currently happens when mounting the rocket on a Launch Pad (`launch_pad`)?
   - How does `StarMapScreen` work? How can it be opened when mounted in the rocket?
   - What are the strict tier validation rules? (e.g. Tier 1 -> Moon only; Tier 2 -> Proxima B & Moon; Tier 3 -> Exotic Prime, Proxima B, Moon).
3. Build, Acceptance Criteria & Obsidian Vault:
   - How do tests run in Gradle? (e.g. `./gradlew test`)
   - What are the requirements for notes synchronization to `C:\Users\amaro\OneDrive\Documents\Obsidian Vault\`? Inspect what notes structure exists or should be created.

## Deliverables
- Write `spec_report.md` with complete specifications and technical requirements.
- Write `handoff.md` with sections: Observation, Logic Chain, Caveats, Conclusion, Verification Method.
- Send a short notification message to your parent orchestrator via `send_message`.
DO NOT modify any Java or game files. You are an exploratory read-only agent.


## 2026-10-06T17:18:36Z
[Message] timestamp=2026-10-06T17:18:36Z sender=e6da9734-df75-4020-bdcc-13a0f39aae07 priority=MESSAGE_PRIORITY_HIGH content=You are spec_miner_survey_rockets_starmap.
Your working directory is: c:/Users/amaro/Documents/antigravity/blissful-lavoisier/.agents/teamwork/spec_miner_survey_rockets_starmap
Read your task instructions at: c:/Users/amaro/Documents/antigravity/blissful-lavoisier/.agents/teamwork/spec_miner_survey_rockets_starmap/DISPATCH.md
MANDATORY: You MUST read the authoritative user request at c:/Users/amaro/Documents/antigravity/blissful-lavoisier/.agents/teamwork/ORIGINAL_REQUEST.md before starting work.
Also read c:/Users/amaro/Documents/antigravity/blissful-lavoisier/PROJECT.md and RESUMEN_PARA_OTRA_IA.md.
Extract specifications for R3 (Rocket Models & Emissives), R4 (StarMap GUI & Destination Selection), build/tests, and Obsidian Vault sync. Write spec_report.md and handoff.md in your working directory.
Notify me with send_message when complete.
