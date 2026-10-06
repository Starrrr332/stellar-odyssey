package com.amaro.stellarodyssey.block.entity;

import com.amaro.stellarodyssey.block.AssemblyTableBlock;
import com.amaro.stellarodyssey.registry.ModItems;
import com.amaro.stellarodyssey.registry.ModMenuTypes;
import com.amaro.stellarodyssey.registry.tiers.RocketTier;
import com.amaro.stellarodyssey.rocket.AssemblyLogic;
import net.minecraft.util.Prediction;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;

/**
 * Container menu for the Rocket Assembly Table.
 * <p>
 * Eight component slots (2x4 grid) plus a result slot. When the components form a
 * valid rocket of some tier, the result slot shows the assembled rocket; taking it
 * consumes the components.
 * </p>
 */
public class AssemblyTableMenu extends AbstractContainerMenu {
    private static final int COMPONENT_SLOTS = 8;
    private static final int RESULT_SLOT = 8;
    private static final int SLOT_COUNT = 9;

    private final ContainerLevelAccess access;
    private final Container components;
    private final Container result;

    public AssemblyTableMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, new SimpleContainer(SLOT_COUNT), ContainerLevelAccess.NULL);
    }

    public AssemblyTableMenu(int containerId, Inventory playerInventory, Container container) {
        this(containerId, playerInventory, container, ContainerLevelAccess.NULL);
    }

    public AssemblyTableMenu(int containerId, Inventory playerInventory, Container container, ContainerLevelAccess access) {
        super(ModMenuTypes.ASSEMBLY_TABLE.get(), containerId);
        this.access = access;
        this.components = container;
        this.result = new SimpleContainer(1);

        // Component slots: 2 rows x 4 columns
        for (int row = 0; row < 2; row++) {
            for (int col = 0; col < 4; col++) {
                int index = row * 4 + col;
                this.addSlot(new Slot(container, index, 26 + col * 18, 18 + row * 18));
            }
        }
        // Result slot
        this.addSlot(new Slot(this.result, 0, 134, 27) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }

            @Override
            public void onTake(Player player, ItemStack stack) {
                super.onTake(player, stack);
                consumeComponents();
            }
        });

        // Player inventory (3x9) + hotbar
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInventory, col, 8 + col * 18, 142));
        }
    }

    /** Recomputes the result slot from the current components. */
    public void updateResult() {
        java.util.List<ItemStack> stacks = new java.util.ArrayList<>();
        for (int i = 0; i < COMPONENT_SLOTS; i++) {
            stacks.add(this.components.getItem(i));
        }
        Optional<RocketTier> tier = AssemblyLogic.validate(stacks);
        if (tier.isPresent()) {
            this.result.setItem(0, new ItemStack(ModItems.ROCKETS.get("rocket_t" + tier.get().tierLevel()).get(), 1));
        } else {
            this.result.setItem(0, ItemStack.EMPTY);
        }
    }

    @Override
    public void slotsChanged(Container container) {
        super.slotsChanged(container);
        if (container == this.components) {
            this.updateResult();
        }
    }

    private void consumeComponents() {
        for (int i = 0; i < COMPONENT_SLOTS; i++) {
            this.components.setItem(i, ItemStack.EMPTY);
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack stack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack current = slot.getItem();
            stack = current.copy();
            if (index < SLOT_COUNT) {
                if (!this.moveItemStackTo(current, SLOT_COUNT, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else if (!this.moveItemStackTo(current, 0, COMPONENT_SLOTS, false)) {
                return ItemStack.EMPTY;
            }
            if (current.isEmpty()) {
                slot.setByPlayer(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }
        return stack;
    }

    @Override
    public boolean stillValid(Player player) {
        return this.access.evaluate((level, pos) -> level.getBlockState(pos).getBlock() instanceof AssemblyTableBlock
                && player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= 64.0, true);
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        if (!this.access.evaluate((level, pos) -> level.isClientSide(), true)) {
            // Return any leftover components to the player
            for (int i = 0; i < COMPONENT_SLOTS; i++) {
                ItemStack stack = this.components.getItem(i);
                if (!stack.isEmpty()) {
                    player.getInventory().placeItemBackInInventory(stack, Prediction.SERVER_ONLY);
                }
            }
        }
    }
}
