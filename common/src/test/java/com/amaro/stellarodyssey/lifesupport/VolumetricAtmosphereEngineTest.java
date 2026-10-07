package com.amaro.stellarodyssey.lifesupport;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit test verifying Volumetric Atmosphere Engine calculations and decompression forces.
 */
class VolumetricAtmosphereEngineTest {

    @Test
    @DisplayName("A completely enclosed 3x3x3 room is detected as sealed")
    void testEnclosedRoomIsSealed() {
        BlockPos origin = new BlockPos(0, 0, 0);
        Set<BlockPos> walls = new HashSet<>();

        // Create a 5x5x5 hollow cube where -2..2 are walls and -1..1 are interior air
        for (int x = -2; x <= 2; x++) {
            for (int y = -2; y <= 2; y++) {
                for (int z = -2; z <= 2; z++) {
                    if (Math.abs(x) == 2 || Math.abs(y) == 2 || Math.abs(z) == 2) {
                        walls.add(new BlockPos(x, y, z));
                    }
                }
            }
        }

        var result = VolumetricAtmosphereEngine.evaluateRoom(
                origin, 1000, 10, 10, walls::contains
        );

        assertTrue(result.isSealed(), "Room should be hermetically sealed");
        assertNull(result.breachOrigin(), "No breach should be found");
        assertEquals(27, result.volume(), "3x3x3 interior should have 27 blocks of air");
        assertNotNull(result.bounds());
    }

    @Test
    @DisplayName("A room with a missing wall block detects the leak and breach coordinate")
    void testBreachDetection() {
        BlockPos origin = new BlockPos(0, 0, 0);
        Set<BlockPos> walls = new HashSet<>();

        // Create box with a hole at (2, 0, 0)
        for (int x = -2; x <= 2; x++) {
            for (int y = -2; y <= 2; y++) {
                for (int z = -2; z <= 2; z++) {
                    if (Math.abs(x) == 2 || Math.abs(y) == 2 || Math.abs(z) == 2) {
                        if (!(x == 2 && y == 0 && z == 0)) { // hole
                            walls.add(new BlockPos(x, y, z));
                        }
                    }
                }
            }
        }

        var result = VolumetricAtmosphereEngine.evaluateRoom(
                origin, 50, 5, 5, walls::contains
        );

        assertFalse(result.isSealed(), "Room with hole should not be sealed");
        assertNotNull(result.breachOrigin(), "Breach coordinate should be identified");
    }

    @Test
    @DisplayName("Decompression force pulls towards breach position with correct direction")
    void testDecompressionForceVector() {
        Vec3 playerPos = new Vec3(0.5, 0.5, 0.5);
        BlockPos breachPos = new BlockPos(5, 0, 0);

        Vec3 force = VolumetricAtmosphereEngine.calculateDecompressionForce(playerPos, breachPos, 1.0);

        assertTrue(force.x > 0, "Force X should point towards positive X breach");
        assertEquals(0.0, force.y, 1e-4, "Force Y should be 0");
        assertEquals(0.0, force.z, 1e-4, "Force Z should be 0");
        assertTrue(force.length() > 0.0 && force.length() <= 1.2, "Force magnitude should be bounded");
    }
}
