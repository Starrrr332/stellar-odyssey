# Task Assignment: Implement Milestone M1 — Oxygen & Planetary Atmosphere (R1)

## Context
You are worker_oxygen_m1, a teamwork_preview_worker subagent implementing Milestone M1 for the Stellar Odyssey space exploration mod (Minecraft 26.3, Java 25, Architectury multi-loader).
Your working directory is:
c:/Users/amaro/Documents/antigravity/blissful-lavoisier/.agents/teamwork/worker_oxygen_m1

MANDATORY INTEGRITY WARNING:
DO NOT CHEAT. All implementations must be genuine. DO NOT hardcode test results, create dummy/facade implementations, or circumvent the intended task. A teamwork_preview_auditor will independently verify your work. Integrity violations WILL be detected and your work WILL be rejected.

## Inputs
- Authoritative requirements: `c:/Users/amaro/Documents/antigravity/blissful-lavoisier/.agents/teamwork/ORIGINAL_REQUEST.md` (read verbatim, section 2026-10-06T17:13:21Z).
- Project documentation: `c:/Users/amaro/Documents/antigravity/blissful-lavoisier/.agents/teamwork/orchestrator_gen2/PROJECT.md` and `RESUMEN_PARA_OTRA_IA.md`.
- Survey findings: `c:/Users/amaro/Documents/antigravity/blissful-lavoisier/.agents/teamwork/explorer_survey_oxygen/analysis.md` and `handoff.md`.

## Exclusive File Ownership
You exclusively own and may modify or create:
- `common/src/main/java/com/amaro/stellarodyssey/lifesupport/**`
- `common/src/main/java/com/amaro/stellarodyssey/block/OxygenSealerBlock.java`
- `common/src/main/java/com/amaro/stellarodyssey/block/OxygenRefillerBlock.java`
- `common/src/main/java/com/amaro/stellarodyssey/block/entity/OxygenSealerBlockEntity.java`
- `common/src/main/java/com/amaro/stellarodyssey/registry/ModBlocks.java`
- `common/src/main/java/com/amaro/stellarodyssey/registry/ModItems.java`
- `common/src/main/java/com/amaro/stellarodyssey/registry/ModBlockEntityTypes.java`
- `common/src/main/resources/assets/stellarodyssey/models/block/oxygen_*.json`
- `common/src/main/resources/assets/stellarodyssey/models/item/oxygen_*.json`
- `common/src/main/resources/assets/stellarodyssey/items/oxygen_*.json` (MC 26.3 item descriptors!)
- `common/src/main/resources/assets/stellarodyssey/blockstates/oxygen_*.json`
- `common/src/test/java/com/amaro/stellarodyssey/lifesupport/LifeSupportSystemTest.java`

DO NOT modify files outside this ownership list.

## Implementation Tasks
1. **Atmosphere Differentiation**:
   - Refactor `AtmosphereHelper.isVacuumEnvironment(Player)` and add methods to query `CelestialBodyRegistry.getInstance().getBody(dimensionKey)`.
   - Hard vacuum (<0.05 atm on Nexus Moon) vs unbreathable/toxic atmosphere (0.85 atm on Exotic Prime, 0.15 atm on Proxima B).
   - Check sealed room status via `AtmosphereHelper.isRoomSealed(Level, BlockPos)`.
2. **Spacesuit Coupling & Oxygen Consumption**:
   - In `LifeSupportManager.java`, require `SpacesuitItem` chestplate (`ModItems.SPACESUIT_CHESTPLATE` - the canonical Oxygen Manifold) and helmet to distribute and consume oxygen from inventory `OxygenTankItem`.
   - Scale oxygen drain rate based on suit integrity (e.g. 2x drain for partial suit in vacuum) and apply decompression damage if no suit is equipped in hard vacuum or asphyxiation in toxic atmospheres.
3. **Oxygen Refiller Block & Logic**:
   - Implement `OxygenRefillerBlock` registered in `ModBlocks.OXYGEN_REFILLER` and item in `ModItems.OXYGEN_REFILLER`.
   - On right-click with an `OxygenTankItem`, restore tank oxygen (durability) to full (`CAPACITY`) and play sound.
4. **Oxygen Sealer Block & BlockEntity**:
   - Implement `OxygenSealerBlock` and `OxygenSealerBlockEntity` registered in `ModBlocks.OXYGEN_SEALER`, `ModItems.OXYGEN_SEALER`, and `ModBlockEntityTypes.OXYGEN_SEALER`.
   - Implement server-side 3D BFS room flood-fill (e.g. max 1024 blocks, max radius 16) to verify room is hermetically sealed (bounded by solid blocks). Cache sealed bounds and provide lookup for `AtmosphereHelper.isRoomSealed`.
5. **Assets (MC 26.3 format)**:
   - Provide models, blockstates, and critically `assets/stellarodyssey/items/oxygen_sealer.json` and `oxygen_refiller.json` descriptors so items never render purple/black missing textures!
6. **Unit Tests & Build**:
   - Create `LifeSupportSystemTest.java` verifying tank durability math, suit piece validation, catalog atmosphere queries, and sealed room BFS logic.
   - Run `./gradlew test --console=plain` and ensure 100% of tests pass.

## Deliverables
- Write `progress.md` and `handoff.md` in your working directory.
- Send a message to parent orchestrator with test results and summary of changes.


## 2026-10-06T17:36:43Z
You are worker_oxygen_m1.
Your working directory is: c:/Users/amaro/Documents/antigravity/blissful-lavoisier/.agents/teamwork/worker_oxygen_m1
Read your task instructions at: c:/Users/amaro/Documents/antigravity/blissful-lavoisier/.agents/teamwork/worker_oxygen_m1/DISPATCH.md
MANDATORY: You MUST read the authoritative user request at c:/Users/amaro/Documents/antigravity/blissful-lavoisier/.agents/teamwork/ORIGINAL_REQUEST.md before starting work.
DO NOT CHEAT. All implementations must be genuine. DO NOT hardcode test results, create dummy/facade implementations, or circumvent the intended task. A teamwork_preview_auditor will independently verify your work. Integrity violations WILL be detected and your work WILL be rejected.
Also read c:/Users/amaro/Documents/antigravity/blissful-lavoisier/.agents/teamwork/orchestrator_gen2/PROJECT.md, RESUMEN_PARA_OTRA_IA.md, and c:/Users/amaro/Documents/antigravity/blissful-lavoisier/.agents/teamwork/explorer_survey_oxygen/analysis.md.
Implement Milestone M1 (Oxygen & Planetary Atmosphere), verify with ./gradlew test, write handoff.md, and notify me with send_message.


## 2026-10-06T18:10:29Z
**Context**: Milestone M1 (Oxygen & Planetary Atmosphere) execution
**Content**: Heartbeat status check. Your progress.md was last updated at 17:38:00Z. What is your current status and which task from the checklist are you actively executing?
**Action**: Please report your current status or blockers and update your progress.md timestamp.
