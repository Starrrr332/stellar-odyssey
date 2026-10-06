# Task Assignment: Milestone M3 — Rocket 3D Models & Emissive Rendering (R3)

## Context
You are worker_rocket_models_r3, a teamwork_preview_worker subagent implementing Requirement R3 (Rocket 3D Models & Emissive Rendering) for the Stellar Odyssey mod.
Your working directory is:
c:/Users/amaro/Documents/antigravity/blissful-lavoisier/.agents/teamwork/worker_rocket_models_r3

MANDATORY INTEGRITY WARNING:
DO NOT CHEAT. All implementations must be genuine. DO NOT hardcode test results, create dummy/facade implementations, or circumvent the intended task. A teamwork_preview_auditor will independently verify your work. Integrity violations WILL be detected and your work WILL be rejected.

## Inputs
- Authoritative requirements: `c:/Users/amaro/Documents/antigravity/blissful-lavoisier/.agents/teamwork/ORIGINAL_REQUEST.md` (read verbatim, section 2026-10-06T17:13:21Z).
- Project documentation: `c:/Users/amaro/Documents/antigravity/blissful-lavoisier/.agents/teamwork/orchestrator_gen2/PROJECT.md` and `RESUMEN_PARA_OTRA_IA.md`.
- Architectural specifications: `c:/Users/amaro/Documents/antigravity/blissful-lavoisier/.agents/teamwork/spec_miner_survey_rockets_starmap/spec_report.md` (Section 4).

## Exclusive File Ownership
You exclusively own and may modify or create:
- `common/src/main/java/com/amaro/stellarodyssey/client/model/RocketTier2Model.java`
- `common/src/main/java/com/amaro/stellarodyssey/client/model/RocketTier3Model.java`
- `common/src/main/java/com/amaro/stellarodyssey/client/renderer/RocketEntityRenderer.java`
- `common/src/main/java/com/amaro/stellarodyssey/client/StellarOdysseyClient.java` (only layer registration section)
- `tools/TextureGen.java` (adding rocket_t2, rocket_t2_emissive, rocket_t3, rocket_t3_emissive)
- `common/src/main/resources/assets/stellarodyssey/textures/entity/**`
- `common/src/test/java/com/amaro/stellarodyssey/client/RocketModelLayerRegistrationTest.java`

DO NOT modify files outside this ownership list.

## Implementation Tasks
1. **Java 3D Models**:
   - Create `RocketTier2Model.java` (Voyager): dual auxiliary side booster pods, enlarged heat deflection nose cone, dual heavy engine nozzles, conduit ribs, and fins. Layer location `new ModelLayerLocation(id("rocket_t2"), "main")`.
   - Create `RocketTier3Model.java` (Odyssey): command fuselage, hyperdrive focal dome, 4 radial outboard warp pods / nacelles, 4 Astralite crystalline radiator wings, tri-engine cluster, and panoramic pressurized bridge cupola. Layer location `new ModelLayerLocation(id("rocket_t3"), "main")`.
   - Register layer definitions in `StellarOdysseyClient.java` using `EntityModelLayerRegistry.register(...)`.
2. **Procedural Textures & Emissive Overlays**:
   - In `tools/TextureGen.java`, implement methods to generate:
     - `textures/entity/rocket_t2.png` (diffuse 128x64 or 128x128 with amber Celidium plating `#D97724`).
     - `textures/entity/rocket_t2_emissive.png` (transparent background, bioluminescent glowing Celidium conduits `#FBBF77` and flame nozzles).
     - `textures/entity/rocket_t3.png` (diffuse 128x128 with cyan Astralite `#06B6D4` and emerald Verdantite `#22C55E`).
     - `textures/entity/rocket_t3_emissive.png` (transparent background, bioluminescent glowing Astralite hyperdrive coils `#A5F3FC` and Verdantite circuits `#86EFAC`).
   - Run `java tools/TextureGen.java` to generate the PNG files directly into `common/src/main/resources/assets/stellarodyssey/textures/entity/`.
3. **`RocketEntityRenderer` Integration with `SubmitNodeCollector`**:
   - Bake all three models (`RocketModel`, `RocketTier2Model`, `RocketTier3Model`) in constructor via `context.bakeLayer(...)`.
   - In `submit(...)`:
     - Select the active model based on `state.tierLevel`.
     - Apply model scale via `poseStack.scale(-scale, -scale, scale)` from `RocketTier.modelScale()`.
     - Submit the diffuse model pass via `collector.submitModel(...)`.
     - For `state.tierLevel >= 2`, invoke `EmissiveModelLayer.submitEmissive(...)` using the respective `rocket_t{tierLevel}_emissive.png` texture at full brightness (`0x00F000F0`)!
4. **Unit Tests & Verification**:
   - Create `RocketModelLayerRegistrationTest.java` verifying layer locations are defined, non-null, and have unique paths.
   - Run `./gradlew test --rerun-tasks --console=plain` ensuring 100% test pass rate.
   - Run `./gradlew :fabric:build :neoforge:build -x test --console=plain` ensuring multi-loader clean build.

## Deliverables
- Write `progress.md` and `handoff.md` in your working directory.
- Send a message to parent orchestrator with test results and summary of changes.


## 2026-10-06T20:06:30Z
[Message] timestamp=2026-10-06T20:06:30Z sender=e6da9734-df75-4020-bdcc-13a0f39aae07 priority=MESSAGE_PRIORITY_HIGH content=You are worker_rocket_models_r3.
Your working directory is: c:/Users/amaro/Documents/antigravity/blissful-lavoisier/.agents/teamwork/worker_rocket_models_r3
Read your task instructions at: c:/Users/amaro/Documents/antigravity/blissful-lavoisier/.agents/teamwork/worker_rocket_models_r3/DISPATCH.md
MANDATORY: You MUST read the authoritative user request at c:/Users/amaro/Documents/antigravity/blissful-lavoisier/.agents/teamwork/ORIGINAL_REQUEST.md before starting work.
DO NOT CHEAT. All implementations must be genuine. DO NOT hardcode test results, create dummy/facade implementations, or circumvent the intended task. A teamwork_preview_auditor will independently verify your work. Integrity violations WILL be detected and your work WILL be rejected.
Also read c:/Users/amaro/Documents/antigravity/blissful-lavoisier/.agents/teamwork/orchestrator_gen2/PROJECT.md, RESUMEN_PARA_OTRA_IA.md, and c:/Users/amaro/Documents/antigravity/blissful-lavoisier/.agents/teamwork/spec_miner_survey_rockets_starmap/spec_report.md.
Implement Requirement R3 (Rocket Tier 2 & 3 Java 3D Models, procedural diffuse & emissive textures, and SubmitNodeCollector emissive pass), verify with ./gradlew test, write handoff.md, and notify me with send_message.
