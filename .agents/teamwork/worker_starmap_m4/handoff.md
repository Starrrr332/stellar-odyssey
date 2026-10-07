# Handoff Report: Milestone M4 — StarMap GUI & Destination Selection (R4 / S1-F3.3)

## 1. Observation
- **Authoritative Dispatch & Scope**:
  - `DISPATCH.md` assigned Milestone M4 (Requirement R4 / S1-F3.3) covering:
    - Network payloads on `ModNetworking`: `FlightPhasePayload` (S2C transmitting `entityId, phase, phaseTicks`) and `SelectDestinationPayload` (C2S transmitting `entityId, destinationDimension`).
    - Strict Tier Destination Matrix (`getRequiredTier`, `isDestinationAllowed`) in `RocketTiers` / `RocketTier`.
    - `RocketEntity` launch pad mounting & destination selection (`handleSelectDestination` server-side validation).
    - `StarMapScreen` rocket destination selection integration with unlocked/locked visual badges and "ENGAGE LAUNCH SEQUENCE" button.
    - Comprehensive unit tests in `RocketTierDestinationValidationTest.java`.
- **Files Modified & Created**:
  - `common/src/main/java/com/amaro/stellarodyssey/network/FlightPhasePayload.java`: Updated primary record components to `(int entityId, RocketFlightPhase phase, int phaseTicks)` with StreamCodec mapping VAR_INT to `RocketFlightPhase`. Added backward-compatibility constructors for ordinal and destination ID.
  - `common/src/main/java/com/amaro/stellarodyssey/network/SelectDestinationPayload.java`: Updated primary record components to `(int entityId, ResourceKey<Level> destinationDimension)` using `ResourceKey.streamCodec(Registries.DIMENSION)`. Added backward-compatibility constructors and accessors (`dimensionKey()`, `dimensionId()`).
  - `common/src/main/java/com/amaro/stellarodyssey/network/ModNetworking.java`: Added `registerPayloads()` method called by `init()`. Updated S2C receiver to pass `payload.phase()` directly to `rocket.setPhase()`, and C2S receiver to dispatch `rocket.handleSelectDestination(serverPlayer, payload.destinationDimension())`.
  - `common/src/main/java/com/amaro/stellarodyssey/registry/tiers/RocketTiers.java` & `RocketTier.java`: Added `getRequiredTier(ResourceKey<Level>)` and `isDestinationAllowed(int tierLevel, ResourceKey<Level>)` mapping:
    - `NEXUS_MOON` -> 1
    - `OVERWORLD` -> 1
    - `PROXIMA_B` -> 2
    - `EXOTIC_PRIME` -> 3
    - `GLIESE_DEEP` -> 3
    - Unknown/null -> -1 / disallowed.
  - `common/src/main/java/com/amaro/stellarodyssey/rocket/RocketTiers.java`, `RocketTier.java`, `RocketEntity.java`: Added helper and re-export classes under `com.amaro.stellarodyssey.rocket`.
  - `common/src/main/java/com/amaro/stellarodyssey/entity/RocketEntity.java`:
    - Added `targetDestination` variable with default fallback to `tier.destination()`.
    - Persisted `targetDestination` in `readAdditionalSaveData` and `addAdditionalSaveData`.
    - In `interact()`: when player right-clicks an idle rocket on a complete Launch Pad (`LaunchPadBlock.isCompletePad`), mounts the player and opens the StarMap GUI on the client with active rocket context.
    - Added `handleSelectDestination(ServerPlayer player, ResourceKey<Level> targetDest)`: validates vehicle riding status (`player.getVehicle() == this`), launch pad completeness (`validateLaunchPad`), idle phase (`!isLaunching() && getPhase() == RocketFlightPhase.IDLE`), and tier permission (`isDestinationAllowed`). On success, sets target destination and begins countdown via `beginLaunchSequence()`.
    - Hardened `sendMessage()` to safely guard against null `connection` in headless unit test players.
  - `common/src/main/java/com/amaro/stellarodyssey/satellites/starmap/StarMapSatellite.java`: Added `openScreen(catalog, rocketTier, rocketEntityId)` overload for physical client invocation.
  - `common/src/main/java/com/amaro/stellarodyssey/satellites/starmap/render/StarMapSkyRenderer.java`: Added `renderCelestialNode` overload accepting `int rocketTier`. Displays `[UNLOCKED]` in green/cyan if `rocketTier >= reqTier` or `[LOCKED - REQUIRES TIER X]` in red if locked.
  - `common/src/main/java/com/amaro/stellarodyssey/satellites/starmap/screen/StarMapScreen.java`:
    - Added `rocketTier` and `rocketEntityId` context fields, constructors, and auto-detection from ridden rocket entity.
    - Updated bottom toolbar button to "ENGAGE LAUNCH SEQUENCE".
    - Dynamically enables/disables the button based on destination unlock status.
    - Updated node rendering and hover tooltips with unlock/lock badges.
    - Clicking sends `SelectDestinationPayload` and closes the screen.
  - `common/src/test/java/com/amaro/stellarodyssey/rocket/RocketTierDestinationValidationTest.java`: Added 15 JUnit 5 unit tests covering matrix mapping, tier permissions, payload serialization/deserialization codecs, destination fallback, vehicle riding validation, tier constraint enforcement, and runtime target destination updating.
- **Verification Commands and Outputs**:
  - Command: `./gradlew test --rerun-tasks --console=plain`
    - Result: `BUILD SUCCESSFUL in 15s`, 8 actionable tasks executed. All 135 unit tests passed (100% pass rate).
  - Command: `./gradlew :fabric:build :neoforge:build -x test --console=plain`
    - Result: `BUILD SUCCESSFUL in 13s`, 18 actionable tasks up-to-date. Multi-loader build clean without side-safety or compilation issues.

## 2. Logic Chain
1. From Observation 1, Requirement R4 dictates that mounting a rocket on a completed launch pad must allow celestial navigation via StarMap, validating the rocket's tier against destination requirements before launching.
2. The network communication requires synchronizing the authoritative phase to client renderers (`FlightPhasePayload`) and transmitting the player's destination selection to the server (`SelectDestinationPayload`).
3. Updating `FlightPhasePayload` to carry `RocketFlightPhase` and `SelectDestinationPayload` to carry `ResourceKey<Level>` creates a strongly typed protocol, while backward-compatibility constructors protect existing call sites.
4. Implementing `getRequiredTier` and `isDestinationAllowed` on `RocketTiers` establishes the strict progression matrix (T1: Moon/Overworld; T2: Moon/Overworld/Proxima; T3: Moon/Overworld/Proxima/Exotic/Gliese).
5. In `RocketEntity`, checking `validateLaunchPad` in `interact()` prevents launching from incomplete pads. Providing `handleSelectDestination` enforces that the selecting player is seated in the rocket, the rocket is idle on a valid pad, and the chosen destination is permitted for the rocket's tier.
6. In `StarMapScreen`, integrating `rocketTier` provides visual feedback (`[UNLOCKED]` vs `[LOCKED - REQUIRES TIER X]`), activates/deactivates the "ENGAGE LAUNCH SEQUENCE" button, and dispatches the payload to the server.
7. Verification with `./gradlew test` and multi-loader builds confirms that all 135 tests pass and both Fabric and NeoForge build cleanly without side-safety regressions.

## 3. Caveats
- Dedicated server testing relies on headless JUnit tests and build checks (`EnvExecutor` and side guards prevent client classes from loading on dedicated servers). In-game visual confirmation in a live client requires manual game launch.
- No other caveats; all requirements of Milestone M4 are fully implemented.

## 4. Conclusion
Requirement R4 and Sprint S1-F3.3 are completely and genuinely implemented according to architectural specifications:
- Strongly typed network payloads `FlightPhasePayload` and `SelectDestinationPayload` are registered on `ModNetworking.registerPayloads()`.
- Strict Tier Destination Matrix is active in `RocketTiers` and `RocketTier`.
- `RocketEntity` handles launch pad validation, mounting, StarMap opening, and server-side destination verification.
- `StarMapScreen` and `StarMapSkyRenderer` provide interactive destination selection with visual unlock badges and launch button engagement.
- Test suite is 100% green across 135 tests.

## 5. Verification Method
To independently verify:
1. Run all unit tests:
   ```bash
   ./gradlew test --rerun-tasks --console=plain
   ```
   Expected: 135 tests completed, 0 failed, exit code 0.
2. Run multi-loader build:
   ```bash
   ./gradlew :fabric:build :neoforge:build -x test --console=plain
   ```
   Expected: BUILD SUCCESSFUL, exit code 0.
3. Inspect `common/src/test/java/com/amaro/stellarodyssey/rocket/RocketTierDestinationValidationTest.java` to verify destination matrix rules, payload serialization, and server validation checks.
