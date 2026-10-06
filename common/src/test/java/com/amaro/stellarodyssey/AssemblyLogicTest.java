package com.amaro.stellarodyssey;

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

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Rocket Assembly Logic Unit Tests")
class AssemblyLogicTest {

    private static Unsafe unsafe;

    @BeforeAll
    static void initMinecraft() throws Exception {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();

        Field f = Unsafe.class.getDeclaredField("theUnsafe");
        f.setAccessible(true);
        unsafe = (Unsafe) f.get(null);
    }

    private ItemStack createComponentStack(RocketTier tier, String type) throws Exception {
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
        return stack;
    }

    @Test
    @DisplayName("Empty slots return empty validation result")
    void testEmptySlots() {
        List<ItemStack> stacks = List.of(ItemStack.EMPTY, ItemStack.EMPTY);
        Optional<RocketTier> result = AssemblyLogic.validate(stacks);
        assertTrue(result.isEmpty(), "Empty components should not form a rocket");
    }

    @Test
    @DisplayName("Valid Tier 1 component set validates successfully")
    void testValidTier1Assembly() throws Exception {
        Optional<RocketTier> t1 = RocketTierRegistry.getTier(1);
        assertTrue(t1.isPresent(), "Tier 1 must exist");

        RocketTier tier1 = t1.get();
        List<String> required = RocketComponentRegistry.getRequiredComponents(tier1);
        List<ItemStack> stacks = new ArrayList<>();
        for (String type : required) {
            stacks.add(createComponentStack(tier1, type));
        }

        Optional<RocketTier> result = AssemblyLogic.validate(stacks);
        assertTrue(result.isPresent(), "Valid Tier 1 set must validate");
        assertEquals(1, result.get().tierLevel(), "Validated tier level must be 1");
    }

    @Test
    @DisplayName("Missing required component fails validation")
    void testIncompleteTier1Assembly() throws Exception {
        Optional<RocketTier> t1 = RocketTierRegistry.getTier(1);
        assertTrue(t1.isPresent());

        RocketTier tier1 = t1.get();
        List<String> required = RocketComponentRegistry.getRequiredComponents(tier1);
        List<ItemStack> stacks = new ArrayList<>();
        for (int i = 0; i < required.size() - 1; i++) {
            stacks.add(createComponentStack(tier1, required.get(i)));
        }

        Optional<RocketTier> result = AssemblyLogic.validate(stacks);
        assertTrue(result.isEmpty(), "Incomplete component set must fail validation");
    }

    @Test
    @DisplayName("Mixed tier components fail validation")
    void testMixedTierAssembly() throws Exception {
        Optional<RocketTier> t1 = RocketTierRegistry.getTier(1);
        Optional<RocketTier> t2 = RocketTierRegistry.getTier(2);
        assertTrue(t1.isPresent() && t2.isPresent());

        ItemStack stack1 = createComponentStack(t1.get(), "cone");
        ItemStack stack2 = createComponentStack(t2.get(), "fin");

        List<ItemStack> stacks = List.of(stack1, stack2);
        Optional<RocketTier> result = AssemblyLogic.validate(stacks);
        assertTrue(result.isEmpty(), "Mixed tier components must fail validation");
    }
}
