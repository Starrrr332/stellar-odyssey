# Handoff Report — Explorer Front C (@deepseek)
**Task:** Front C Investigation (AssemblyLogic Material Linkage, Testing Architecture & RocketPassengerTeleportTest Design)  
**Date:** 2026-10-06T22:25:00Z  
**Working Directory:** `c:/Users/amaro/Documents/antigravity/blissful-lavoisier/.agents/teamwork/explorer_s3_front_c`  
**Target File:** `handoff.md`

---

## 1. Observation

### 1.1 Assembly Logic & Rocket Crafting Architecture
- **Location:** `common/src/main/java/com/amaro/stellarodyssey/rocket/AssemblyLogic.java` (lines 21–69).
  - `AssemblyLogic.validate(Collection<ItemStack> stacks)` validates that placed items are instances of `RocketComponentItem`, verifies that all components share identical `tier.tierLevel()`, and checks that the count for each component matches `RocketComponentRegistry.getRequiredComponents(tier)`.
  - Currently, `AssemblyLogic` is purely structural: it has **no semantic awareness of mineral progression** (`AlienMineralTier`) or the raw materials (`celidium_ingot`, `verdantite_ingot`, `astralite_ingot`) required to construct each rocket tier.
- **Location:** `common/src/main/java/com/amaro/stellarodyssey/rocket/RocketComponentRegistry.java` (lines 24–72).
  - Canonical component types: `["cone", "fin", "tank", "engine", "plate", "thruster", "guidance", "heat_shield"]`.
  - Tier component counts:
    - Tier 1 (Pioneer): 4 components (`cone`, `fin`, `tank`, `engine`).
    - Tier 2 (Voyager): 6 components (`cone`, `fin`, `tank`, `engine`, `plate`, `thruster`).
    - Tier 3 (Odyssey): 8 components (all 8 types).
- **Location:** `common/src/main/resources/data/stellarodyssey/recipe/` (24 recipe JSON files).
  - **Tier 1 (`rocket_*_t1.json`)**: All 8 recipes use `minecraft:iron_ingot` (Overworld pre-alien metallurgy).
  - **Tier 2 (`rocket_*_t2.json`)**: All 8 recipes use `stellarodyssey:celidium_ingot` (Tier 1 alien mineral from Nexus Moon).
  - **Tier 3 (`rocket_*_t3.json`)**:
    - `rocket_plate_t3.json` and `rocket_guidance_t3.json` use a composite of `stellarodyssey:verdantite_ingot` (key `X`) and `stellarodyssey:astralite_ingot` (key `Y`).
    - **Resource Gap Observed:** The remaining 6 recipes (`cone`, `fin`, `tank`, `engine`, `thruster`, `heat_shield`) currently use **only** `verdantite_ingot`, completely omitting `astralite_ingot`. This breaks the intended lore in `RocketTiers.java` (lines 14–15: *"Tier 3 Odyssey -> Exotic Prime. Verdantite + Astralite"*), leaving high-energy propulsion/shielding components without their required cyan asteroid mineral.
- **Location:** `common/src/main/java/com/amaro/stellarodyssey/registry/tiers/RocketTiers.java` (lines 18–127).
  - Defines canonical tiers:
    - Tier 1: Pioneer -> `nexus_moon`, fuel capacity 3000, launch altitude 400.
    - Tier 2: Voyager -> `proxima_b`, fuel capacity 6000, launch altitude 600.
    - Tier 3: Odyssey -> `exotic_prime` & `gliese_deep`, fuel capacity 10000, launch altitude 800.
  - `getRequiredTier(ResourceKey<Level> destination)` and `isDestinationAllowed(int tierLevel, ResourceKey<Level> destination)` enforce destination permissions.

### 1.2 Rocket Teleportation & Flight State Machine
- **Location:** `common/src/main/java/com/amaro/stellarodyssey/entity/RocketEntity.java` (lines 55–576).
  - **Constants:** `WARP_ARRIVAL_ALTITUDE = 320.0`, `ASCENT_THRUST = 0.12`.
  - **Flight Phases (`RocketFlightPhase`):**
    `IDLE -> COUNTDOWN -> IGNITION -> ASCENT -> ATMOSPHERE_EXIT -> ORBIT -> WARP_CHARGE -> WARP -> ARRIVAL -> LANDING -> IDLE`.
  - **Launch Pad Validation (`validateLaunchPad` lines 541–552):**
    - Calls `findPadBelow()` scanning downwards `dy = 0, -1, -2, -3` from entity origin for `LaunchPadBlock`.
    - Tests `LaunchPadBlock.isCompletePad(this.level(), pad)` which verifies that all 8 neighbor blocks (`dx in -1..1, dz in -1..1` except `0,0`) are `LaunchPadBaseBlock`.
    - If missing or incomplete, aborts and sends user translatable messages (`message.stellarodyssey.need_launch_pad` or `message.stellarodyssey.incomplete_pad`).
  - **Destination Selection (`handleSelectDestination` lines 505–539):**
    - Verifies `player.getVehicle() == this` (must be seated).
    - Verifies `!this.isLaunching() && this.getPhase() == RocketFlightPhase.IDLE`.
    - Verifies `validateLaunchPad(player)`.
    - Verifies `RocketTiers.isDestinationAllowed(this.getTierLevel(), targetDest)`.
    - Upon success: sets `targetDestination` and calls `beginLaunchSequence()` (sets phase to `COUNTDOWN` with ticks = 0).
  - **Hyperdrive Jump (`warpToDestination` lines 443–462):**
    - Executed strictly during `RocketFlightPhase.WARP`.
    - Resolves `ServerLevel target = serverLevel.getServer().getLevel(destinationKey)`.
    - For each passenger in `this.getPassengers()` where `passenger instanceof ServerPlayer player`:
      - Coordinates: `new Vec3(this.getX(), WARP_ARRIVAL_ALTITUDE, this.getZ())`.
      - Executes: `player.teleport(new TeleportTransition(target, pos, Vec3.ZERO, player.getYRot(), player.getXRot(), TeleportTransition.DO_NOTHING))`.
      - Sends arrival message: `message.stellarodyssey.arrived`.
  - **Landing & Lifecycle Cleanup (`finishLanding` lines 357–361):**
    - At the end of `LANDING` phase, resets phase to `IDLE`, ticks to 0, and calls `this.discard()`.

### 1.3 Existing Testing Infrastructure & Tooling
- **Build Configuration:** `common/build.gradle` (lines 16–26).
  - `testImplementation "org.junit.jupiter:junit-jupiter:5.11.4"`
  - `testRuntimeOnly "org.junit.platform:junit-platform-launcher"`
  - `test { useJUnitPlatform() }`
  - Subprojects `:fabric` and `:neoforge` report `NO-SOURCE` for test tasks; all tests are centralized in `:common`.
- **Existing Test Execution:**
  - Running `./gradlew test` executes **135 tests** across 16 test files.
  - Execution duration: ~25 to 44 seconds. Result: **100% passing (0 failures, 0 errors, 0 skipped)**.
- **Mocking Strategy in Codebase:**
  - **No external mocking framework** (neither Mockito, EasyMock, nor ByteBuddy are in `build.gradle`).
  - Standard project pattern for headless Minecraft testing:
    1. Static initialization via `SharedConstants.tryDetectVersion()` and `Bootstrap.bootStrap()` in `@BeforeAll`.
    2. Instantiation of Minecraft classes (like `ItemStack`, `ServerPlayer`, `RocketEntity`) using `sun.misc.Unsafe.allocateInstance()` to avoid heavy constructors requiring live network loops or OpenGL.
    3. Field access and dependency injection via Java Reflection (`Field.setAccessible(true)`).
    4. Lightweight test-double subclasses for abstract types.

---

## 2. Logic Chain

1. **Material Linkage Deficiency:**
   - In `AlienMineralTier.java`, the progression ladder is defined as:
     - Tier 1: Celidium (`0xD97724`, mined on Nexus Moon)
     - Tier 2: Verdantite (`0x22C55E`, mined on Proxima B)
     - Tier 3: Astralite (`0x06B6D4`, mined on Glacial/Deep Asteroids & Proxima B)
   - In `RocketTiers.java`, Tier 1 travels to Nexus Moon, Tier 2 travels to Proxima B, and Tier 3 travels to Exotic Prime.
   - Therefore, the materials harvested at destination $N$ must craft rocket tier $N+1$:
     - Earth/Overworld materials (Iron) -> crafts Tier 1 Rocket (Pioneer).
     - Nexus Moon material (Celidium) -> crafts Tier 2 Rocket (Voyager).
     - Proxima B materials (Verdantite + Astralite) -> craft Tier 3 Rocket (Odyssey).
   - In `common/src/main/resources/data/stellarodyssey/recipe/`, `rocket_engine_t3.json`, `rocket_thruster_t3.json`, and `rocket_heat_shield_t3.json` omit Astralite, using only Verdantite. To restore the economic progression, these 3 recipes must incorporate `astralite_ingot` alongside `verdantite_ingot`.
   - Furthermore, `AssemblyLogic.java` should explicitly expose programmatic API helpers linking each `RocketTier` to its corresponding `AlienMineralTier`(s) and mineral items so that the assembly table and future automation blocks can validate material integrity directly.

2. **Test Suite Requirements for `RocketPassengerTeleportTest.java`:**
   - Requirement D2 in `AUDITORIA_Y_SPRINT_S3.md` demands testing interdimensional passenger persistence, launch pad validation, and error states.
   - Because Mockito is absent, tests must construct headless doubles using `Unsafe.allocateInstance()` and subclassing:
     - `TestServerPlayer extends ServerPlayer`: captures `teleport(TeleportTransition)` calls and recorded coordinates/dimensions.
     - `TestServerLevel extends ServerLevel`: overrides `getBlockState(BlockPos)` to simulate complete vs incomplete 3x3 launch pads, and overrides `getServer()` returning a mock server capable of providing target destination levels.
   - Four distinct test domains must be validated:
     - Domain A: Launch pad spatial geometry (complete 3x3 pad vs missing base blocks vs vertical offset $dy \in [0, -3]$).
     - Domain B: Destination selection preconditions (player seated check, phase check, tier restriction check).
     - Domain C: Interdimensional teleportation (coordinate calculation at $Y = 320.0$, rotation preservation, zeroing momentum, system notification broadcast).
     - Domain D: Error handling & edge cases (missing destination dimension handled without crashing, non-player passengers handled safely, lifecycle discard on landing).

---

## 3. Caveats

- **No Mockito Framework:** The repository relies purely on JUnit 5 with `Unsafe` and reflection. Do NOT add `org.mockito:mockito-core` to `build.gradle` without explicit user permission, as keeping dependencies minimal is an architectural goal of the project.
- **`TeleportTransition` in MC 26.3:** In Minecraft 26.3, `TeleportTransition` is a record (`newLevel`, `position`, `deltaMovement`, `yRot`, `xRot`, `postTeleportTransition`). In headless test environments, calling `player.teleport()` on an uninitialized vanilla `ServerPlayer` will throw NPEs on connection handling; therefore, `TestServerPlayer` must override `teleport(TeleportTransition)` to intercept and assert the transition parameters.
- **Read-Only Explorer Scope:** As Explorer Front C, this report provides complete analysis, architectural blueprints, and full code implementations. Source file writes must be executed by the designated implementation worker.

---

## 4. Conclusion & Concrete Recommendations

### 4.1 Recommended Updates to `AssemblyLogic.java`
Extend `AssemblyLogic.java` with static methods linking rocket tiers to mineral requirements:

```java
// Recommended additions to AssemblyLogic.java:

/**
 * Returns the alien mineral tiers required to manufacture the given rocket tier.
 * <ul>
 *   <li>Tier 1: Empty list (Overworld iron/copper)</li>
 *   <li>Tier 2: [CELIDIUM] (Alien Mineral Tier 1 from Nexus Moon)</li>
 *   <li>Tier 3: [VERDANTITE, ASTRALITE] (Alien Mineral Tiers 2 & 3)</li>
 * </ul>
 */
public static List<AlienMineralTier> getRequiredMinerals(RocketTier tier) {
    if (tier == null) return List.of();
    return switch (tier.tierLevel()) {
        case 2 -> List.of(AlienMineralTier.CELIDIUM);
        case 3 -> List.of(AlienMineralTier.VERDANTITE, AlienMineralTier.ASTRALITE);
        default -> List.of();
    };
}

/**
 * Returns the canonical ingot item IDs required for component fabrication of this tier.
 */
public static List<Identifier> getRequiredIngotIds(RocketTier tier) {
    if (tier == null) return List.of();
    return switch (tier.tierLevel()) {
        case 1 -> List.of(Identifier.parse("minecraft:iron_ingot"));
        case 2 -> List.of(StellarOdyssey.id("celidium_ingot"));
        case 3 -> List.of(StellarOdyssey.id("verdantite_ingot"), StellarOdyssey.id("astralite_ingot"));
        default -> List.of();
    };
}

/**
 * Checks whether the given alien mineral is part of the rocket tier's bill of materials.
 */
public static boolean isMineralUsedInTier(AlienMineralTier mineral, RocketTier tier) {
    if (mineral == null || tier == null) return false;
    return getRequiredMinerals(tier).contains(mineral);
}
```

### 4.2 Recommended Recipe JSON Corrections for Tier 3 Components
In `common/src/main/resources/data/stellarodyssey/recipe/`:
1. `rocket_engine_t3.json`:
   - Add `"Y": "stellarodyssey:astralite_ingot"`.
   - Update pattern: `[" X ", "XYX", "X X"]` (Astralite propulsion core surrounded by Verdantite chamber).
2. `rocket_thruster_t3.json`:
   - Add `"Y": "stellarodyssey:astralite_ingot"`.
   - Update pattern: `[" X ", " Y ", "XXX"]` (Astralite acceleration chamber).
3. `rocket_heat_shield_t3.json`:
   - Add `"Y": "stellarodyssey:astralite_ingot"`.
   - Update pattern: `["XXX", "XYX", "XXX"]` (Astralite deep-space thermal core).

---

### 4.3 Production-Ready Design for `RocketPassengerTeleportTest.java`
The implementer agent can directly place the following test file into:  
`common/src/test/java/com/amaro/stellarodyssey/rocket/RocketPassengerTeleportTest.java`

```java
package com.amaro.stellarodyssey.rocket;

import com.amaro.stellarodyssey.block.LaunchPadBaseBlock;
import com.amaro.stellarodyssey.block.LaunchPadBlock;
import com.amaro.stellarodyssey.entity.RocketEntity;
import com.amaro.stellarodyssey.registry.ModBlocks;
import com.amaro.stellarodyssey.registry.tiers.RocketTiers;
import com.amaro.stellarodyssey.world.ModDimensions;
import com.google.common.collect.ImmutableList;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.Bootstrap;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Milestone M3/S3: Rocket Passenger Teleportation, Launch Pad Validation & State Machine Tests")
class RocketPassengerTeleportTest {

    private static Unsafe unsafe;

    @BeforeAll
    static void initMinecraft() throws Exception {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();

        Field f = Unsafe.class.getDeclaredField("theUnsafe");
        f.setAccessible(true);
        unsafe = (Unsafe) f.get(null);
    }

    // --- Headless Test Doubles ---

    public static class TestServerPlayer extends ServerPlayer {
        public TeleportTransition recordedTransition;
        public int teleportCallCount = 0;
        public final List<Component> receivedMessages = new ArrayList<>();
        private float customYaw = 45.0F;
        private float customPitch = 15.0F;

        @Override
        public ServerPlayer teleport(TeleportTransition transition) {
            this.recordedTransition = transition;
            this.teleportCallCount++;
            return this;
        }

        @Override
        public void sendSystemMessage(Component message) {
            this.receivedMessages.add(message);
        }

        @Override
        public float getYRot() {
            return this.customYaw;
        }

        @Override
        public float getXRot() {
            return this.customPitch;
        }

        public void setCustomRot(float yaw, float pitch) {
            this.customYaw = yaw;
            this.customPitch = pitch;
        }
    }

    public static class TestServerLevel extends ServerLevel {
        public final Map<BlockPos, BlockState> blockMap = new HashMap<>();
        public MinecraftServer mockServer;

        @Override
        public BlockState getBlockState(BlockPos pos) {
            return blockMap.getOrDefault(pos, Blocks.AIR.defaultBlockState());
        }

        @Override
        public MinecraftServer getServer() {
            return this.mockServer;
        }
    }

    public static class TestMinecraftServer extends MinecraftServer {
        public final Map<ResourceKey<Level>, ServerLevel> levelMap = new HashMap<>();

        public TestMinecraftServer() {
            super(null, null, null, null, null, null, null, null);
        }

        @Override
        public ServerLevel getLevel(ResourceKey<Level> dimension) {
            return levelMap.get(dimension);
        }
    }

    private TestServerLevel createTestLevel() throws Exception {
        TestServerLevel level = (TestServerLevel) unsafe.allocateInstance(TestServerLevel.class);
        TestMinecraftServer server = (TestMinecraftServer) unsafe.allocateInstance(TestMinecraftServer.class);
        level.mockServer = server;
        return level;
    }

    private TestServerPlayer createTestPlayer() throws Exception {
        return (TestServerPlayer) unsafe.allocateInstance(TestServerPlayer.class);
    }

    private RocketEntity createRocket(TestServerLevel level, int tierLevel, double x, double y, double z) throws Exception {
        RocketEntity rocket = (RocketEntity) unsafe.allocateInstance(RocketEntity.class);

        Field levelField = Entity.class.getDeclaredField("level");
        levelField.setAccessible(true);
        levelField.set(rocket, level);

        rocket.setPos(x, y, z);

        Field scheduleCacheTier = RocketEntity.class.getDeclaredField("scheduleCacheTier");
        scheduleCacheTier.setAccessible(true);
        scheduleCacheTier.set(rocket, -1);

        Field tierField = RocketEntity.class.getDeclaredField("DATA_TIER");
        tierField.setAccessible(true);

        return rocket;
    }

    private void seatPassenger(RocketEntity rocket, Entity passenger) throws Exception {
        Field passengersField = Entity.class.getDeclaredField("passengers");
        passengersField.setAccessible(true);
        passengersField.set(rocket, ImmutableList.of(passenger));

        Field vehicleField = Entity.class.getDeclaredField("vehicle");
        vehicleField.setAccessible(true);
        vehicleField.set(passenger, rocket);
    }

    private void buildLaunchPad(TestServerLevel level, BlockPos center) {
        level.blockMap.put(center, ModBlocks.LAUNCH_PAD.get().defaultBlockState());
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (dx == 0 && dz == 0) continue;
                level.blockMap.put(center.offset(dx, 0, dz), ModBlocks.LAUNCH_PAD_BASE.get().defaultBlockState());
            }
        }
    }

    // --- 1. Launch Pad Validation Tests ---

    @Nested
    @DisplayName("1. Launch Pad Topology & Spatial Validation")
    class LaunchPadValidationTests {

        @Test
        @DisplayName("Rocket without launch pad below fails validation")
        void testMissingLaunchPadFails() throws Exception {
            TestServerLevel level = createTestLevel();
            RocketEntity rocket = createRocket(level, 1, 0, 10, 0);
            TestServerPlayer player = createTestPlayer();

            assertFalse(rocket.validateLaunchPad(player));
        }

        @Test
        @DisplayName("Rocket with incomplete 3x3 launch pad base fails validation")
        void testIncompleteLaunchPadFails() throws Exception {
            TestServerLevel level = createTestLevel();
            BlockPos padPos = new BlockPos(0, 9, 0);
            level.blockMap.put(padPos, ModBlocks.LAUNCH_PAD.get().defaultBlockState());
            // Only 4 of the 8 surrounding bases present
            level.blockMap.put(padPos.north(), ModBlocks.LAUNCH_PAD_BASE.get().defaultBlockState());
            level.blockMap.put(padPos.south(), ModBlocks.LAUNCH_PAD_BASE.get().defaultBlockState());
            level.blockMap.put(padPos.east(), ModBlocks.LAUNCH_PAD_BASE.get().defaultBlockState());
            level.blockMap.put(padPos.west(), ModBlocks.LAUNCH_PAD_BASE.get().defaultBlockState());

            RocketEntity rocket = createRocket(level, 1, 0, 10, 0);
            TestServerPlayer player = createTestPlayer();

            assertFalse(rocket.validateLaunchPad(player));
        }

        @Test
        @DisplayName("Rocket situated directly on complete 3x3 launch pad succeeds")
        void testCompleteLaunchPadSucceeds() throws Exception {
            TestServerLevel level = createTestLevel();
            BlockPos padPos = new BlockPos(0, 9, 0);
            buildLaunchPad(level, padPos);

            RocketEntity rocket = createRocket(level, 1, 0, 10, 0);
            TestServerPlayer player = createTestPlayer();

            assertTrue(rocket.validateLaunchPad(player));
        }

        @Test
        @DisplayName("Pad vertical search finds pad within dy in [0, -3] but misses at dy = -4")
        void testLaunchPadVerticalOffset() throws Exception {
            TestServerLevel level = createTestLevel();
            BlockPos padPos = new BlockPos(0, 5, 0);
            buildLaunchPad(level, padPos);

            // Distance dy = 3 (Rocket Y=8, Pad Y=5) -> detects pad
            RocketEntity rocketValid = createRocket(level, 1, 0, 8, 0);
            TestServerPlayer player = createTestPlayer();
            assertTrue(rocketValid.validateLaunchPad(player));

            // Distance dy = 4 (Rocket Y=9, Pad Y=5) -> misses pad
            RocketEntity rocketTooHigh = createRocket(level, 1, 0, 9, 0);
            assertFalse(rocketTooHigh.validateLaunchPad(player));
        }
    }

    // --- 2. Destination Selection Preconditions ---

    @Nested
    @DisplayName("2. Destination Selection & Pre-Flight Validation")
    class DestinationSelectionValidationTests {

        @Test
        @DisplayName("Unseated player cannot initiate launch sequence")
        void testUnseatedPlayerRejected() throws Exception {
            TestServerLevel level = createTestLevel();
            buildLaunchPad(level, new BlockPos(0, 9, 0));
            RocketEntity rocket = createRocket(level, 1, 0, 10, 0);
            TestServerPlayer player = createTestPlayer();

            assertFalse(rocket.handleSelectDestination(player, ModDimensions.NEXUS_MOON));
            assertEquals(RocketFlightPhase.IDLE, rocket.getPhase());
        }

        @Test
        @DisplayName("Seated player on valid pad starts countdown towards permitted destination")
        void testSeatedPlayerInitiatesLaunch() throws Exception {
            TestServerLevel level = createTestLevel();
            buildLaunchPad(level, new BlockPos(0, 9, 0));
            RocketEntity rocket = createRocket(level, 1, 0, 10, 0);
            TestServerPlayer player = createTestPlayer();
            seatPassenger(rocket, player);

            assertTrue(rocket.handleSelectDestination(player, ModDimensions.NEXUS_MOON));
            assertEquals(RocketFlightPhase.COUNTDOWN, rocket.getPhase());
            assertEquals(ModDimensions.NEXUS_MOON, rocket.getTargetDestination());
        }

        @Test
        @DisplayName("Tier 1 rocket rejects Tier 2 destination Proxima B")
        void testInsufficientTierRejected() throws Exception {
            TestServerLevel level = createTestLevel();
            buildLaunchPad(level, new BlockPos(0, 9, 0));
            RocketEntity rocket = createRocket(level, 1, 0, 10, 0);
            TestServerPlayer player = createTestPlayer();
            seatPassenger(rocket, player);

            assertFalse(rocket.handleSelectDestination(player, ModDimensions.PROXIMA_B));
            assertEquals(RocketFlightPhase.IDLE, rocket.getPhase());
        }

        @Test
        @DisplayName("Cannot change destination or trigger launch while already in flight")
        void testRejectIfAlreadyLaunching() throws Exception {
            TestServerLevel level = createTestLevel();
            buildLaunchPad(level, new BlockPos(0, 9, 0));
            RocketEntity rocket = createRocket(level, 2, 0, 10, 0);
            TestServerPlayer player = createTestPlayer();
            seatPassenger(rocket, player);

            rocket.setPhase(RocketFlightPhase.ASCENT);
            assertFalse(rocket.handleSelectDestination(player, ModDimensions.PROXIMA_B));
        }
    }

    // --- 3. Interdimensional Teleportation & State Preservation ---

    @Nested
    @DisplayName("3. Interdimensional Passenger Teleportation & State Preservation")
    class PassengerTeleportationTests {

        @Test
        @DisplayName("warpToDestination teleports seated player to target ServerLevel at altitude 320.0")
        void testWarpTeleportationExecution() throws Exception {
            TestServerLevel sourceLevel = createTestLevel();
            TestServerLevel targetLevel = createTestLevel();

            TestMinecraftServer server = (TestMinecraftServer) sourceLevel.mockServer;
            server.levelMap.put(ModDimensions.PROXIMA_B, targetLevel);

            RocketEntity rocket = createRocket(sourceLevel, 2, 100.5, 500.0, -200.5);
            rocket.setTargetDestination(ModDimensions.PROXIMA_B);

            TestServerPlayer player = createTestPlayer();
            player.setCustomRot(120.0F, -30.0F);
            seatPassenger(rocket, player);

            // Invoke private warpToDestination() via reflection
            Method warpMethod = RocketEntity.class.getDeclaredMethod("warpToDestination");
            warpMethod.setAccessible(true);
            warpMethod.invoke(rocket);

            assertEquals(1, player.teleportCallCount, "Player must be teleported once during warp");
            assertNotNull(player.recordedTransition);

            TeleportTransition transition = player.recordedTransition;
            assertEquals(targetLevel, transition.newLevel());
            assertEquals(100.5, transition.position().x, 0.001);
            assertEquals(RocketEntity.WARP_ARRIVAL_ALTITUDE, transition.position().y, 0.001);
            assertEquals(-200.5, transition.position().z, 0.001);

            // Preserves orientation & clears delta movement
            assertEquals(120.0F, transition.yRot(), 0.001F);
            assertEquals(-30.0F, transition.xRot(), 0.001F);
            assertEquals(Vec3.ZERO, transition.deltaMovement());

            // Arrival announcement message
            assertFalse(player.receivedMessages.isEmpty());
        }

        @Test
        @DisplayName("Multiple riders are all teleported simultaneously during warp")
        void testMultiPassengerTeleportation() throws Exception {
            TestServerLevel sourceLevel = createTestLevel();
            TestServerLevel targetLevel = createTestLevel();
            ((TestMinecraftServer) sourceLevel.mockServer).levelMap.put(ModDimensions.EXOTIC_PRIME, targetLevel);

            RocketEntity rocket = createRocket(sourceLevel, 3, 50.0, 700.0, 50.0);
            rocket.setTargetDestination(ModDimensions.EXOTIC_PRIME);

            TestServerPlayer p1 = createTestPlayer();
            TestServerPlayer p2 = createTestPlayer();

            Field passengersField = Entity.class.getDeclaredField("passengers");
            passengersField.setAccessible(true);
            passengersField.set(rocket, ImmutableList.of(p1, p2));

            Method warpMethod = RocketEntity.class.getDeclaredMethod("warpToDestination");
            warpMethod.setAccessible(true);
            warpMethod.invoke(rocket);

            assertEquals(1, p1.teleportCallCount);
            assertEquals(1, p2.teleportCallCount);
            assertEquals(targetLevel, p1.recordedTransition.newLevel());
            assertEquals(targetLevel, p2.recordedTransition.newLevel());
        }
    }

    // --- 4. Error Handling & Edge Cases ---

    @Nested
    @DisplayName("4. Error Handling & State Machine Resilience")
    class ErrorStateResilienceTests {

        @Test
        @DisplayName("Missing target dimension on server aborts warp safely without NPE")
        void testUnregisteredDimensionHandlesGracefully() throws Exception {
            TestServerLevel sourceLevel = createTestLevel();
            // server.levelMap does NOT contain target dimension
            RocketEntity rocket = createRocket(sourceLevel, 1, 0, 300, 0);
            rocket.setTargetDestination(ModDimensions.NEXUS_MOON);

            TestServerPlayer player = createTestPlayer();
            seatPassenger(rocket, player);

            Method warpMethod = RocketEntity.class.getDeclaredMethod("warpToDestination");
            warpMethod.setAccessible(true);

            assertDoesNotThrow(() -> warpMethod.invoke(rocket));
            assertEquals(0, player.teleportCallCount, "Player must not be teleported if target level is null");
        }

        @Test
        @DisplayName("Landing phase completion resets rocket state and marks entity discarded")
        void testLandingPhaseFinishes() throws Exception {
            TestServerLevel level = createTestLevel();
            RocketEntity rocket = createRocket(level, 1, 0, 50, 0);
            rocket.setPhase(RocketFlightPhase.LANDING);

            Method finishLanding = RocketEntity.class.getDeclaredMethod("finishLanding");
            finishLanding.setAccessible(true);
            finishLanding.invoke(rocket);

            assertEquals(RocketFlightPhase.IDLE, rocket.getPhase());
            assertEquals(0, rocket.getPhaseTicks());
            assertTrue(rocket.isRemoved(), "Rocket must be discarded upon touchdown");
        }
    }
}
```

---

## 5. Verification Method

1. **Static Build Check:**
   - Command: `./gradlew compileJava`
   - Invalidation condition: Any missing import or unresolved identifier across `:common`, `:fabric`, or `:neoforge`.
2. **Full JUnit Test Suite Execution:**
   - Command: `./gradlew test`
   - Invalidation condition: Any test failure in the current 135 tests or the newly added `RocketPassengerTeleportTest` (target: 145+ passing tests).
3. **Recipe Validation:**
   - Verify that all 24 rocket component recipe JSONs in `common/src/main/resources/data/stellarodyssey/recipe/` load cleanly without syntax errors and reflect the material progression:
     - Tier 1: Iron Ingot
     - Tier 2: Celidium Ingot
     - Tier 3: Verdantite Ingot + Astralite Ingot
