# Task Assignment: Milestone M2 — Adaptive Planetary Gravity (R2) & Atmosphere Helper Resolution

## Context
You are worker_gravity_m2, a teamwork_preview_worker subagent implementing Milestone M2 for the Stellar Odyssey mod.
Your working directory is:
c:/Users/amaro/Documents/antigravity/blissful-lavoisier/.agents/teamwork/worker_gravity_m2

MANDATORY INTEGRITY WARNING:
DO NOT CHEAT. All implementations must be genuine. DO NOT hardcode test results, create dummy/facade implementations, or circumvent the intended task. A teamwork_preview_auditor will independently verify your work. Integrity violations WILL be detected and your work WILL be rejected.

## Inputs
- Authoritative requirements: `c:/Users/amaro/Documents/antigravity/blissful-lavoisier/.agents/teamwork/ORIGINAL_REQUEST.md` (read verbatim, section 2026-10-06T17:13:21Z).
- Project documentation: `c:/Users/amaro/Documents/antigravity/blissful-lavoisier/.agents/teamwork/orchestrator_gen2/PROJECT.md`.
- Survey findings: `c:/Users/amaro/Documents/antigravity/blissful-lavoisier/.agents/teamwork/explorer_survey_gravity/analysis.md` and `handoff.md`.
- Reviewer finding on AtmosphereHelper: `c:/Users/amaro/Documents/antigravity/blissful-lavoisier/.agents/teamwork/reviewer_oxygen_1/handoff.md`.

## Exclusive File Ownership
You exclusively own and may modify:
- `common/src/main/java/com/amaro/stellarodyssey/world/CelestialBodyRegistry.java`
- `common/src/main/java/com/amaro/stellarodyssey/world/PlanetaryGravityManager.java`
- `common/src/main/java/com/amaro/stellarodyssey/lifesupport/AtmosphereHelper.java`
- `common/src/test/java/com/amaro/stellarodyssey/world/AdaptivePlanetaryGravityTest.java`

## Implementation Tasks
1. **`CelestialBodyRegistry.java`**:
   - Update `PROXIMA_B` gravity multiplier from 0.40 to exactly `0.35f` as required by R2.
   - Verify `NEXUS_MOON` is `0.16f`.
2. **`AtmosphereHelper.java` Resolution**:
   - In `isVacuumEnvironment(Player player)` (lines 111-133), prioritize `CelestialBodyRegistry.getInstance().getBody(level.dimension())` BEFORE checking `VACUUM_DIMENSIONS` tag!
   - This ensures charted exoplanets like `EXOTIC_PRIME` (0.85 atm) and `PROXIMA_B` (0.15 atm) are recognized as unbreathable/toxic exoplanetary atmospheres rather than vacuum, while `NEXUS_MOON` (0.00 atm) is correctly recognized as vacuum.
   - Add `AtmosphereHelper.clearSealers()` called on server stopping if needed.
3. **`PlanetaryGravityManager.java` Adaptive Gravity Physics**:
   - Replace the static -0.60 modifier with dynamic gravity modifier calculation:
     `modifierValue = gravityMultiplier - 1.0f`
   - Dynamically look up the level dimension's body from `CelestialBodyRegistry.getInstance().getBody(dimensionKey)`:
     - Nexus Moon: 0.16g -> modifier `-0.84`
     - Proxima B: 0.35g -> modifier `-0.65`
     - Orbit ($Y \ge 320$): microgravity 0.08g -> modifier `-0.92`
     - Standard/Overworld: 1.0g -> modifier `0.0` (remove modifier cleanly)
   - Apply transient modifiers on the server (`!level.isClientSide()`):
     - `Attributes.GRAVITY`: base 0.08, modified by `gravityMultiplier - 1.0`.
     - `Attributes.SAFE_FALL_DISTANCE`: scaled up by `(3.0 / gravityMultiplier) - 3.0` (+15.75 blocks on Moon, +5.57 blocks on Proxima B) to prevent unrealistic fall damage in low terminal velocity regimes.
     - `Attributes.FALL_DAMAGE_MULTIPLIER`: scaled by `gravityMultiplier - 1.0` (reduces damage by 84% on Moon, 65% on Proxima B).
   - Hook `EntityEvent.ADD` (`dev.architectury.event.events.common.EntityEvent.ADD`) to apply gravity modifiers to all `LivingEntity` instances entering levels (mobs, animals, players), ensuring reduced gravity affects both players and entities!
   - Ensure clean removal of modifiers when returning to standard dimensions.
4. **Unit Tests & Verification**:
   - Create `AdaptivePlanetaryGravityTest.java` in `common/src/test/java/com/amaro/stellarodyssey/world/`:
     - Test `PROXIMA_B` has 0.35g and `NEXUS_MOON` has 0.16g.
     - Test modifier calculation formula for all celestial bodies.
     - Test safe fall distance and fall damage calculations.
     - Test `AtmosphereHelper.isVacuumEnvironment` vs `isToxicOrUnbreathable` across dimensions.
   - Run `./gradlew test --console=plain` and ensure 100% of tests pass!
   - Verify multi-loader build: `./gradlew :fabric:build :neoforge:build -x test --console=plain`.

## Deliverables
- Write `progress.md` and `handoff.md` in your working directory.
- Send a message to parent orchestrator with test results and summary of changes.


## 2026-10-06T19:50:22Z
You are worker_gravity_m2.
Your working directory is: c:/Users/amaro/Documents/antigravity/blissful-lavoisier/.agents/teamwork/worker_gravity_m2
Read your task instructions at: c:/Users/amaro/Documents/antigravity/blissful-lavoisier/.agents/teamwork/worker_gravity_m2/DISPATCH.md
MANDATORY: You MUST read the authoritative user request at c:/Users/amaro/Documents/antigravity/blissful-lavoisier/.agents/teamwork/ORIGINAL_REQUEST.md before starting work.
DO NOT CHEAT. All implementations must be genuine. DO NOT hardcode test results, create dummy/facade implementations, or circumvent the intended task. A teamwork_preview_auditor will independently verify your work. Integrity violations WILL be detected and your work WILL be rejected.
Also read c:/Users/amaro/Documents/antigravity/blissful-lavoisier/.agents/teamwork/orchestrator_gen2/PROJECT.md, c:/Users/amaro/Documents/antigravity/blissful-lavoisier/.agents/teamwork/explorer_survey_gravity/analysis.md, and c:/Users/amaro/Documents/antigravity/blissful-lavoisier/.agents/teamwork/reviewer_oxygen_1/handoff.md.
Implement Milestone M2 (Adaptive Planetary Gravity & AtmosphereHelper fix), verify with ./gradlew test, write handoff.md, and notify me with send_message.
