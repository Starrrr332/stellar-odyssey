# Task Assignment: Milestone M4 — StarMap GUI & Destination Selection (R4 / S1-F3.3)

## Context
You are worker_starmap_m4, a teamwork_preview_worker subagent implementing Requirement R4 and Sprint S1-F3.3 for the Stellar Odyssey mod.
Your working directory is:
c:/Users/amaro/Documents/antigravity/blissful-lavoisier/.agents/teamwork/worker_starmap_m4

MANDATORY INTEGRITY WARNING:
DO NOT CHEAT. All implementations must be genuine. DO NOT hardcode test results, create dummy/facade implementations, or circumvent the intended task. A teamwork_preview_auditor will independently verify your work. Integrity violations WILL be detected and your work WILL be rejected.

## Inputs
- Authoritative requirements: `c:/Users/amaro/Documents/antigravity/blissful-lavoisier/.agents/teamwork/ORIGINAL_REQUEST.md` (read verbatim, section 2026-10-06T17:13:21Z).
- Project documentation: `c:/Users/amaro/Documents/antigravity/blissful-lavoisier/.agents/teamwork/orchestrator_gen2/PROJECT.md` and `RESUMEN_PARA_OTRA_IA.md`.
- Architectural specifications: `c:/Users/amaro/Documents/antigravity/blissful-lavoisier/.agents/teamwork/spec_miner_survey_rockets_starmap/spec_report.md` (Section 5).

## Exclusive File Ownership
You exclusively own and may modify or create:
- `common/src/main/java/com/amaro/stellarodyssey/network/ModNetworking.java`
- `common/src/main/java/com/amaro/stellarodyssey/network/FlightPhasePayload.java`
- `common/src/main/java/com/amaro/stellarodyssey/network/SelectDestinationPayload.java`
- `common/src/main/java/com/amaro/stellarodyssey/rocket/RocketEntity.java`
- `common/src/main/java/com/amaro/stellarodyssey/rocket/RocketTier.java` / `RocketTiers.java` (tier validation helper)
- `common/src/main/java/com/amaro/stellarodyssey/satellites/starmap/**`
- `common/src/test/java/com/amaro/stellarodyssey/rocket/RocketTierDestinationValidationTest.java`

DO NOT modify files outside this ownership list.

## Implementation Tasks
1. **Network Payloads on `ModNetworking`**:
   - `FlightPhasePayload` (S2C): transmits `(int entityId, RocketFlightPhase phase, int phaseTicks)` so client renderers, camera controllers, and HUD stay in lockstep.
   - `SelectDestinationPayload` (C2S): transmits `(int entityId, ResourceKey<Level> destinationDimension)`.
   - Register both payloads in `ModNetworking.registerPayloads()`.
2. **Strict Tier Destination Matrix**:
   - In `RocketTier` or `RocketTiers`:
     - Provide `getRequiredTier(ResourceKey<Level> destination)`:
       - `NEXUS_MOON` -> Tier 1 (Pioneer)
       - `PROXIMA_B` -> Tier 2 (Voyager)
       - `EXOTIC_PRIME` -> Tier 3 (Odyssey)
       - `GLIESE_DEEP` -> Tier 3 (Odyssey)
       - `OVERWORLD` -> Tier 1 (Any)
     - Provide `isDestinationAllowed(int tierLevel, ResourceKey<Level> destination)`.
3. **RocketEntity Launch Pad Mounting & Destination Selection**:
   - In `RocketEntity.java`:
     - When a player right-clicks an idle rocket on a complete Launch Pad (`LaunchPadBlock.isCompletePad`), mount the player and open the StarMap GUI for destination selection.
     - Add `targetDestination` variable with default fallback to `tier.destination()`.
     - When `SelectDestinationPayload` is received on the server:
       - Validate player is riding the rocket (`player.getVehicle() == rocket`).
       - Validate rocket is on a complete launch pad and not currently launching.
       - Validate `isDestinationAllowed(rocket.getTierLevel(), payload.destinationDimension())`.
       - If valid, set destination, start countdown/launch, play thrust sound. If invalid, reject and inform player.
4. **StarMapScreen Destination Selection Integration**:
   - Enable `StarMapScreen` to operate with an active rocket context (`rocketTier`, `rocketEntityId`).
   - For each celestial body:
     - Show `[UNLOCKED]` if `rocketTier >= requiredTier`, or `[LOCKED - REQUIRES TIER X]` if locked.
     - Add/update "ENGAGE LAUNCH SEQUENCE" button.
     - When clicked on an unlocked destination, send `SelectDestinationPayload` to server and close screen.
5. **Unit Tests & Verification**:
   - Create `RocketTierDestinationValidationTest.java` in `common/src/test/java/com/amaro/stellarodyssey/rocket/`:
     - Test tier destination matrix (T1 allowed Moon, rejected Proxima & Exotic; T2 allowed Moon & Proxima, rejected Exotic; T3 allowed all).
     - Test payload serialization/deserialization.
     - Test server validation logic for vehicle mounting and launch pad requirements.
   - Run `./gradlew test --rerun-tasks --console=plain` and ensure 100% tests pass.
   - Run `./gradlew :fabric:build :neoforge:build -x test --console=plain` ensuring clean multi-loader builds.

## Deliverables
- Write `progress.md` and `handoff.md` in your working directory.
- Send a message to parent orchestrator with test results and summary of changes.
