package com.amaro.stellarodyssey.assembly;

import com.amaro.stellarodyssey.item.RocketComponentItem;
import com.amaro.stellarodyssey.registry.tiers.RocketTier;
import com.amaro.stellarodyssey.registry.tiers.RocketTierRegistry;
import com.amaro.stellarodyssey.rocket.AssemblyLogic;
import com.amaro.stellarodyssey.rocket.RocketComponentRegistry;
import net.minecraft.SharedConstants;
import net.minecraft.core.Holder;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Guards against the assembly duplication exploit.
 * <p>
 * The validator originally counted slot occurrences instead of item quantities, so a slot
 * holding a stack of components still satisfied the "exactly one of each component"
 * contract. Because taking the result only consumed a single unit per slot, the same items
 * could assemble an unlimited number of rockets.
 */
@DisplayName("Assembly Stack Size Duplication Guard")
class AssemblyStackSizeGuardTest {

    private static Unsafe unsafe;

    @BeforeAll
    static void initMinecraft() throws Exception {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();

        Field f = Unsafe.class.getDeclaredField("theUnsafe");
        f.setAccessible(true);
        unsafe = (Unsafe) f.get(null);
    }

    /**
     * Builds a real {@link RocketComponentItem} stack of the requested size without running
     * vanilla constructors (the codebase's established headless-test pattern).
     */
    private ItemStack component(RocketTier tier, String type, int count) throws Exception {
        RocketComponentItem item = (RocketComponentItem) unsafe.allocateInstance(RocketComponentItem.class);

        Field tierField = RocketComponentItem.class.getDeclaredField("tier");
        tierField.setAccessible(true);
        tierField.set(item, tier);

        Field typeField = RocketComponentItem.class.getDeclaredField("type");
        typeField.setAccessible(true);
        typeField.set(item, type);

        ItemStack stack = (ItemStack) unsafe.allocateInstance(ItemStack.class);
        for (Field field : ItemStack.class.getDeclaredFields()) {
            field.setAccessible(true);
            if (field.getType().equals(Holder.class)) {
                field.set(stack, Holder.direct(item));
            } else if (field.getType().equals(int.class)) {
                field.set(stack, 1);
            }
        }
        // Set the stack size explicitly so the guard does not depend on the field name.
        stack.setCount(count);
        return stack;
    }

    private List<ItemStack> completeTier1Set(int countPerSlot) throws Exception {
        RocketTier tier = RocketTierRegistry.getTier(1).orElseThrow();
        List<ItemStack> stacks = new ArrayList<>();
        for (String type : RocketComponentRegistry.getRequiredComponents(tier)) {
            stacks.add(component(tier, type, countPerSlot));
        }
        return stacks;
    }

    @Test
    @DisplayName("A complete set of single components still assembles a Tier 1 rocket")
    void singleComponentsStillValidate() throws Exception {
        Optional<RocketTier> result = AssemblyLogic.validate(completeTier1Set(1));
        assertTrue(result.isPresent(), "A single component per slot must still be valid");
        assertEquals(1, result.get().tierLevel());
    }

    @Test
    @DisplayName("Stacked components are rejected, closing the infinite rocket duplication exploit")
    void stackedComponentsAreRejected() throws Exception {
        assertTrue(AssemblyLogic.validate(completeTier1Set(2)).isEmpty(),
                "A two-item stack per slot must not assemble a rocket");
        assertTrue(AssemblyLogic.validate(completeTier1Set(64)).isEmpty(),
                "A full stack per slot must not assemble a rocket");
    }
}
