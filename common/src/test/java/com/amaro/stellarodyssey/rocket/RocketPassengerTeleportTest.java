package com.amaro.stellarodyssey.rocket;

import com.amaro.stellarodyssey.block.LaunchPadBlock;
import com.amaro.stellarodyssey.entity.RocketEntity;
import com.amaro.stellarodyssey.registry.ModBlocks;
import com.amaro.stellarodyssey.registry.tiers.RocketTiers;
import com.amaro.stellarodyssey.world.ModDimensions;
import com.google.common.collect.ImmutableList;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.Bootstrap;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Milestone M3/S3: Rocket Passenger Teleportation, Launch Pad Validation & State Machine Tests")
class RocketPassengerTeleportTest {

    private static Unsafe unsafe;
    private static BlockState LAUNCH_PAD_STATE;
    private static BlockState LAUNCH_PAD_BASE_STATE;

    private static BlockState createMockBlockState(Block mockBlock) throws Exception {
        BlockState state = (BlockState) unsafe.allocateInstance(BlockState.class);
        Class<?> clazz = BlockState.class;
        while (clazz != null) {
            for (Field f : clazz.getDeclaredFields()) {
                if (!java.lang.reflect.Modifier.isStatic(f.getModifiers()) && (f.getType() == Object.class || f.getType() == Block.class)) {
                    f.setAccessible(true);
                    f.set(state, mockBlock);
                }
            }
            clazz = clazz.getSuperclass();
        }
        return state;
    }

    @BeforeAll
    static void initMinecraft() throws Exception {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();

        Field f = Unsafe.class.getDeclaredField("theUnsafe");
        f.setAccessible(true);
        unsafe = (Unsafe) f.get(null);

        com.amaro.stellarodyssey.block.LaunchPadBlock pad = (com.amaro.stellarodyssey.block.LaunchPadBlock) unsafe.allocateInstance(com.amaro.stellarodyssey.block.LaunchPadBlock.class);
        com.amaro.stellarodyssey.block.LaunchPadBaseBlock padBase = (com.amaro.stellarodyssey.block.LaunchPadBaseBlock) unsafe.allocateInstance(com.amaro.stellarodyssey.block.LaunchPadBaseBlock.class);

        LAUNCH_PAD_STATE = createMockBlockState(pad);
        LAUNCH_PAD_BASE_STATE = createMockBlockState(padBase);
    }

    // --- Headless Test Doubles ---

    public static class TestServerPlayer extends ServerPlayer {
        public TeleportTransition recordedTransition;
        public int teleportCallCount = 0;
        public List<Component> receivedMessages = new ArrayList<>();
        private float customYaw = 45.0F;
        private float customPitch = 15.0F;

        public TestServerPlayer() {
            super(null, null, null, null);
        }

        @Override
        public ServerPlayer teleport(TeleportTransition transition) {
            this.recordedTransition = transition;
            this.teleportCallCount++;
            return this;
        }

        @Override
        public void sendSystemMessage(Component message) {
            if (this.receivedMessages != null) {
                this.receivedMessages.add(message);
            }
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
        public Map<BlockPos, BlockState> blockMap = new HashMap<>();
        public MinecraftServer mockServer;
        private static int nextId = 0;

        public TestServerLevel() {
            super(null, null, null, null, null, null, false, 0L, null, false);
        }

        @Override
        public BlockState getBlockState(BlockPos pos) {
            if (this.blockMap == null) {
                return Blocks.AIR.defaultBlockState();
            }
            return this.blockMap.getOrDefault(pos, Blocks.AIR.defaultBlockState());
        }

        @Override
        public MinecraftServer getServer() {
            return this.mockServer;
        }

        public int getNextEntityId() {
            return nextId++;
        }
    }

    public static class TestMinecraftServer extends MinecraftServer {
        public Map<ResourceKey<Level>, ServerLevel> levelMap = new HashMap<>();

        public TestMinecraftServer() {
            super(null, null, null, null, null, null, null, null, null, false, null);
        }

        @Override
        public ServerLevel getLevel(ResourceKey<Level> dimension) {
            if (this.levelMap == null) {
                return null;
            }
            return this.levelMap.get(dimension);
        }

        @Override
        protected boolean initServer() {
            return true;
        }

        @Override
        public net.minecraft.server.permissions.LevelBasedPermissionSet operatorUserPermissions() {
            return null;
        }

        @Override
        public net.minecraft.server.permissions.PermissionSet getFunctionCompilationPermissions() {
            return null;
        }

        @Override
        public boolean shouldRconBroadcast() {
            return false;
        }

        @Override
        protected net.minecraft.util.debugchart.SampleLogger getTickTimeLogger() {
            return null;
        }

        @Override
        public boolean isTickTimeLoggingEnabled() {
            return false;
        }

        @Override
        public net.minecraft.SystemReport fillServerSystemReport(net.minecraft.SystemReport report) {
            return report;
        }

        @Override
        public boolean isDedicatedServer() {
            return true;
        }

        @Override
        public int getRateLimitPacketsPerSecond() {
            return 0;
        }

        @Override
        public int getCommandSpamThresholdSeconds() {
            return 0;
        }

        @Override
        public int getChatSpamThresholdSeconds() {
            return 0;
        }

        @Override
        public boolean useNativeTransport() {
            return false;
        }

        @Override
        public boolean isPublished() {
            return false;
        }

        @Override
        public boolean shouldInformAdmins() {
            return false;
        }

        @Override
        public boolean isSingleplayerOwner(net.minecraft.server.players.NameAndId nameAndId) {
            return false;
        }

        @Override
        public int getMaxPlayers() {
            return 20;
        }
    }

    private TestServerLevel createTestLevel() throws Exception {
        TestServerLevel level = (TestServerLevel) unsafe.allocateInstance(TestServerLevel.class);
        Field blockMapField = TestServerLevel.class.getDeclaredField("blockMap");
        blockMapField.setAccessible(true);
        blockMapField.set(level, new HashMap<BlockPos, BlockState>());

        TestMinecraftServer server = (TestMinecraftServer) unsafe.allocateInstance(TestMinecraftServer.class);
        Field levelMapField = TestMinecraftServer.class.getDeclaredField("levelMap");
        levelMapField.setAccessible(true);
        levelMapField.set(server, new HashMap<ResourceKey<Level>, ServerLevel>());

        level.mockServer = server;
        return level;
    }

    private TestServerPlayer createTestPlayer(TestServerLevel level) throws Exception {
        TestServerPlayer player = (TestServerPlayer) unsafe.allocateInstance(TestServerPlayer.class);
        Field levelField = Entity.class.getDeclaredField("level");
        levelField.setAccessible(true);
        levelField.set(player, level);

        Field msgField = TestServerPlayer.class.getDeclaredField("receivedMessages");
        msgField.setAccessible(true);
        msgField.set(player, new ArrayList<Component>());

        player.setCustomRot(45.0F, 15.0F);

        ServerGamePacketListenerImpl dummyConnection = (ServerGamePacketListenerImpl) unsafe.allocateInstance(ServerGamePacketListenerImpl.class);
        Field connectionField = ServerPlayer.class.getDeclaredField("connection");
        connectionField.setAccessible(true);
        connectionField.set(player, dummyConnection);

        return player;
    }

    private RocketEntity createRocket(TestServerLevel level, int tierLevel, double x, double y, double z) throws Exception {
        EntityType<RocketEntity> mockType = (EntityType<RocketEntity>) unsafe.allocateInstance(EntityType.class);
        Field dimensionsField = EntityType.class.getDeclaredField("dimensions");
        dimensionsField.setAccessible(true);
        dimensionsField.set(mockType, net.minecraft.world.entity.EntityDimensions.scalable(1.0f, 2.0f));

        RocketEntity rocket = new RocketEntity(mockType, level);

        rocket.setPos(x, y, z);

        Field bbField = Entity.class.getDeclaredField("bb");
        bbField.setAccessible(true);
        bbField.set(rocket, new net.minecraft.world.phys.AABB(x - 0.5, y, z - 0.5, x + 0.5, y + 2.0, z + 0.5));

        Field passengersField = Entity.class.getDeclaredField("passengers");
        passengersField.setAccessible(true);
        passengersField.set(rocket, ImmutableList.of());

        Field scheduleCacheTier = RocketEntity.class.getDeclaredField("scheduleCacheTier");
        scheduleCacheTier.setAccessible(true);
        scheduleCacheTier.set(rocket, -1);

        rocket.setTierLevel(tierLevel);

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
        level.blockMap.put(center, LAUNCH_PAD_STATE);
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (dx == 0 && dz == 0) continue;
                level.blockMap.put(center.offset(dx, 0, dz), LAUNCH_PAD_BASE_STATE);
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
            TestServerPlayer player = createTestPlayer(level);

            assertFalse(rocket.validateLaunchPad(player));
        }

        @Test
        @DisplayName("Rocket with incomplete 3x3 launch pad base fails validation")
        void testIncompleteLaunchPadFails() throws Exception {
            TestServerLevel level = createTestLevel();
            BlockPos padPos = new BlockPos(0, 9, 0);
            level.blockMap.put(padPos, LAUNCH_PAD_STATE);
            // Only 4 of the 8 surrounding bases present
            level.blockMap.put(padPos.north(), LAUNCH_PAD_BASE_STATE);
            level.blockMap.put(padPos.south(), LAUNCH_PAD_BASE_STATE);
            level.blockMap.put(padPos.east(), LAUNCH_PAD_BASE_STATE);
            level.blockMap.put(padPos.west(), LAUNCH_PAD_BASE_STATE);

            RocketEntity rocket = createRocket(level, 1, 0, 10, 0);
            TestServerPlayer player = createTestPlayer(level);

            assertFalse(rocket.validateLaunchPad(player));
        }

        @Test
        @DisplayName("Rocket situated directly on complete 3x3 launch pad succeeds")
        void testCompleteLaunchPadSucceeds() throws Exception {
            TestServerLevel level = createTestLevel();
            BlockPos padPos = new BlockPos(0, 9, 0);
            buildLaunchPad(level, padPos);

            RocketEntity rocket = createRocket(level, 1, 0, 10, 0);
            TestServerPlayer player = createTestPlayer(level);

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
            TestServerPlayer player = createTestPlayer(level);
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
            TestServerPlayer player = createTestPlayer(level);

            assertFalse(rocket.handleSelectDestination(player, ModDimensions.NEXUS_MOON));
            assertEquals(RocketFlightPhase.IDLE, rocket.getPhase());
        }

        @Test
        @DisplayName("Seated player on valid pad starts countdown towards permitted destination")
        void testSeatedPlayerInitiatesLaunch() throws Exception {
            TestServerLevel level = createTestLevel();
            buildLaunchPad(level, new BlockPos(0, 9, 0));
            RocketEntity rocket = createRocket(level, 1, 0, 10, 0);
            TestServerPlayer player = createTestPlayer(level);
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
            TestServerPlayer player = createTestPlayer(level);
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
            TestServerPlayer player = createTestPlayer(level);
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

            TestServerPlayer player = createTestPlayer(sourceLevel);
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

            TestServerPlayer p1 = createTestPlayer(sourceLevel);
            TestServerPlayer p2 = createTestPlayer(sourceLevel);

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

            TestServerPlayer player = createTestPlayer(sourceLevel);
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

        @Test
        @DisplayName("Non-player passengers in rocket are handled safely without crashing warpToDestination")
        void testNonPlayerPassengerHandledSafely() throws Exception {
            TestServerLevel sourceLevel = createTestLevel();
            TestServerLevel targetLevel = createTestLevel();
            ((TestMinecraftServer) sourceLevel.mockServer).levelMap.put(ModDimensions.NEXUS_MOON, targetLevel);

            RocketEntity rocket = createRocket(sourceLevel, 1, 10.0, 400.0, 20.0);
            rocket.setTargetDestination(ModDimensions.NEXUS_MOON);

            // Dummy non-player entity in passenger seat
            Entity nonPlayerPassenger = (Entity) unsafe.allocateInstance(RocketEntity.class);

            Field passengersField = Entity.class.getDeclaredField("passengers");
            passengersField.setAccessible(true);
            passengersField.set(rocket, ImmutableList.of(nonPlayerPassenger));

            Method warpMethod = RocketEntity.class.getDeclaredMethod("warpToDestination");
            warpMethod.setAccessible(true);

            assertDoesNotThrow(() -> warpMethod.invoke(rocket));
        }
    }
}
