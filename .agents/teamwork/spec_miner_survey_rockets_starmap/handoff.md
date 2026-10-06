# Handoff Report: Specification Survey for Rockets & StarMap (R3, R4)

**Agent:** `spec_miner_survey_rockets_starmap`  
**Recipient:** Orchestrator (`e6da9734-df75-4020-bdcc-13a0f39aae07`)  
**Timestamp:** 2026-10-06T17:31:00Z  
**Type:** Hard Handoff (Task Complete)  

---

## 1. Observation

1. **Rocket Models & Renderer**:
   - `RocketEntityRenderer.java:25`:
     ```java
     this.model = new RocketModel(context.bakeLayer(RocketModel.LAYER_LOCATION));
     ```
     Only a single model `RocketModel` is baked and used for all tiers.
   - `RocketEntityRenderer.java:49-51`:
     ```java
     Identifier texture = StellarOdyssey.id("textures/entity/rocket_t" + state.tierLevel + ".png");
     collector.submitModel(this.model, state, poseStack, texture, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
     ```
     No scale modifier from `RocketTier.modelScale` is applied. Only a single diffuse model pass is executed via `collector.submitModel`. There is no invocation of `EmissiveModelLayer.submitEmissive(...)`.
   - File listing in `assets/stellarodyssey/textures/entity/`:
     `rocket_t1.png` exists (6,920 bytes), but `rocket_t2.png` and `rocket_t3.png` do not exist.
     `textures/entity/rocket_t2_emissive.png` and `textures/entity/rocket_t3_emissive.png` do not exist.
   - `TextureGen.java:977-1040`:
     The `rocket()` procedural generator only emits `textures/entity/rocket/rocket.png` (Tier 1 128×64 diffuse texture). It does not generate entity textures or emissive maps for Tiers 2 and 3.

2. **Mounting & Launch Sequence Interaction**:
   - `RocketEntity.java:200-217`:
     ```java
     @Override
     public InteractionResult interact(Player player, InteractionHand hand, Vec3 hitPos) {
         if (this.level().isClientSide()) return InteractionResult.SUCCESS;
         if (this.isLaunching()) return InteractionResult.SUCCESS;
         if (this.getPassengers().isEmpty()) {
             player.startRiding(this);
             return InteractionResult.SUCCESS;
         }
         this.setLaunching(true);
         this.setLaunchTicks(0);
         ...
     }
     ```
     When a player mounts the rocket, no GUI opens. A second click blindly initiates liftoff.
   - `RocketEntity.java:177-182`:
     ```java
     Optional<RocketTier> tier = this.getTier();
     if (tier.isEmpty()) return;
     ResourceKey<Level> destination = tier.get().destination();
     ```
     The destination is hardcoded to a single fixed dimension per tier in `RocketTier` (`NEXUS_MOON` for T1, `PROXIMA_B` for T2, `EXOTIC_PRIME` for T3). There is no dynamic destination variable or setter on `RocketEntity`.

3. **StarMap Navigation & Networking**:
   - `StarMapScreen.java:150-157`:
     ```java
     private void plotCourse() {
         if (this.selectedBody != null) {
             this.navigationStatusMessage = Component.literal(
                     "COURSE PLOTTED: " + this.selectedBody.name().toUpperCase() + " [" + this.selectedBody.starSystemName() + "]");
         } else {
             this.navigationStatusMessage = Component.literal("NO TARGET SELECTED TO PLOT COURSE");
         }
     }
     ```
     The "PLOT COURSE" button only updates a local UI string. No packet is dispatched, no rocket tier validation is enforced.
   - `ModNetworking.java:10-18`:
     Only `OxygenSyncPayload` is registered. No C2S packet exists for destination selection or launch initiation.

4. **Build & Test Suite**:
   - Executed `./gradlew test --console=plain` in project root:
     Exit code 0, build successful in 1s.
     39 tests passed across 6 test classes (`AlienMineralTierExtensibilityTest`, `AlienMineralTierMatrixTest`, `AssemblyLogicTest`, `CoreLifecycleAndConstantsTest`, `DecoupledSatellitesContractTest`, `ModRegistriesBindingTest`).

5. **Obsidian Vault**:
   - Inspected `C:\Users\amaro\OneDrive\Documents\Obsidian Vault\`:
     Contains `Stellar Odyssey - Proyecto.md`, `Arquitectura & Subagentes.md`, `Progreso & Checkpoints.md`, `Agentes/STATUS.md`, `Agentes/DECISIONES.md`, and `Agentes/HANDOFFS/antigravity.md`.
     `DECISIONES.md` documents:
     `2026-10-06 — Star Map abre automáticamente al llegar a Y>=256 montado en la nave (fix de StarshipEntity).`

---

## 2. Logic Chain

1. **R3 Gaps & Requirements**:
   - From Observation 1, `RocketEntityRenderer` binds all tiers to `RocketModel` and expects `textures/entity/rocket_t{tierLevel}.png`.
   - Because `rocket_t2.png` and `rocket_t3.png` are missing, and no emissive textures exist, rendering Tier 2 or Tier 3 rockets currently produces missing textures and lacks bioluminescent glow.
   - Requirement R3 explicitly mandates distinct 3D models for Tier 2 (Voyager) and Tier 3 (Odyssey), and bioluminescent emissive layers linked to Celidium (amber) and Astralite/Verdantite (cyan/emerald) submitted via `SubmitNodeCollector`.
   - Therefore, implementation requires:
     (a) Two new Java model layer definitions: `RocketTier2Model` (with side booster pods and conduit ribs) and `RocketTier3Model` (with quad warp nacelles, crystalline radiator wings, and tri-engine bells).
     (b) Entity textures and emissive PNG textures generated in `tools/TextureGen.java` (or handcrafted).
     (c) `RocketEntityRenderer` updated to scale by `tier.modelScale()`, switch models, and call `EmissiveModelLayer.submitEmissive(...)` when `state.tierLevel >= 2`.

2. **R4 Gaps & Requirements**:
   - From Observation 2, mounting the rocket (`player.startRiding(rocket)`) performs no check to open `StarMapScreen`.
   - From Observation 2 and 3, destination is currently an immutable field on `RocketTier` rather than an entity destination, and `StarMapScreen` has no knowledge of the player's rocket tier or launch pad state.
   - Requirement R4 demands that mounting the rocket on the Launch Pad opens the StarMap GUI to choose a destination, with strict minimum tier validation:
     - Tier 1: Nexus Moon only.
     - Tier 2: Proxima B & Nexus Moon.
     - Tier 3: Exotic Prime, Proxima B, Nexus Moon (and Gliese Deep).
   - Therefore, implementation requires:
     (a) An S2C trigger (`OpenRocketStarMapPayload` or client vehicle check) opening `StarMapScreen` with the rocket's entity ID and tier.
     (b) `StarMapScreen` updated with destination locking/unlocking badges and an "ENGAGE LAUNCH SEQUENCE" button.
     (c) A C2S packet (`SelectRocketDestinationPayload`) transmitting the selected dimension key, validated securely on the server against the rocket's tier and pad integrity before commencing liftoff.

---

## 3. Caveats

- **No Code Modifications Made**: Consistent with the `spec_miner` read-only role, no Java source files or game assets were altered during this survey.
- **Client vs Server Isolation**: GUI components (`StarMapScreen`) and Model definitions (`RocketModel`, `RocketEntityRenderer`) must remain strictly in client packages or guarded by `EnvExecutor` to prevent dedicated server crashes.
- **Obsidian Vault Concurrency**: The Obsidian Vault at `C:\Users\amaro\OneDrive\Documents\Obsidian Vault\` is shared with OpenCode. Changes must strictly follow the rules in `Agentes/STATUS.md` (Antigravity writes to `HANDOFFS/antigravity.md`).

---

## 4. Conclusion

The specification survey for R3, R4, build/test, and Obsidian Vault synchronization is complete.
1. The codebase foundations (Architectury multi-loader, `RocketEntity`, `RocketTierRegistry`, `LaunchPadBlock`, `StarMapScreen`, `SubmitNodeCollector` pipeline, and JUnit test framework) are exceptionally clean, decoupled, and compiling without errors (39/39 tests pass).
2. The exact architectural blueprints, UV layouts, model geometry, tier validation rules, and network payloads are fully specified and documented in `spec_report.md`.
3. The next assigned worker agents can implement R3 and R4 in parallel without ambiguity or architectural friction.

---

## 5. Verification Method

To independently verify all observations and test results:
1. **Verify Test Suite**:
   ```powershell
   cd c:/Users/amaro/Documents/antigravity/blissful-lavoisier
   ./gradlew test --console=plain
   ```
   Expected: 39 tests executed, 0 failures, exit code 0.
2. **Inspect Specification Artifacts**:
   - View `c:/Users/amaro/Documents/antigravity/blissful-lavoisier/.agents/teamwork/spec_miner_survey_rockets_starmap/spec_report.md`
   - View `c:/Users/amaro/Documents/antigravity/blissful-lavoisier/.agents/teamwork/spec_miner_survey_rockets_starmap/handoff.md`
3. **Inspect Obsidian Vault**:
   ```powershell
   Get-ChildItem "C:\Users\amaro\OneDrive\Documents\Obsidian Vault\"
   Get-ChildItem "C:\Users\amaro\OneDrive\Documents\Obsidian Vault\Agentes"
   ```
