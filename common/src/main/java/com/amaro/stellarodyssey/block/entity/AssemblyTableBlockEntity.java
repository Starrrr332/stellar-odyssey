package com.amaro.stellarodyssey.block.entity;

import com.amaro.stellarodyssey.registry.ModBlockEntityTypes;
import com.amaro.stellarodyssey.registry.tiers.RocketTier;
import com.amaro.stellarodyssey.rocket.AssemblyLogic;
import dev.architectury.registry.menu.ExtendedMenuProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Optional;

/**
 * Block entity for the Rocket Assembly Table.
 * <p>
 * Holds up to 8 component slots (one per {@code RocketComponentRegistry.COMPONENT_TYPES}).
 * When the placed components form a valid rocket of some tier, the table reports the
 * assembled tier so the menu can show a "Launch" result slot.
 * </p>
 */
public class AssemblyTableBlockEntity extends BaseContainerBlockEntity implements ExtendedMenuProvider {
    private static final int SLOT_COUNT = 8;
    private NonNullList<ItemStack> items = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);

    public AssemblyTableBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntityTypes.ASSEMBLY_TABLE.get(), pos, state);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.stellarodyssey.assembly_table");
    }

    @Override
    public int getContainerSize() {
        return SLOT_COUNT;
    }

    @Override
    protected NonNullList<ItemStack> getItems() {
        return this.items;
    }

    @Override
    protected void setItems(NonNullList<ItemStack> items) {
        this.items = items;
    }

    @Override
    protected AbstractContainerMenu createMenu(int containerId, Inventory inventory) {
        return new AssemblyTableMenu(containerId, inventory, this);
    }

    /** Sends this table's position to the client so the menu can bind to the same container. */
    @Override
    public void saveExtraData(FriendlyByteBuf buf) {
        buf.writeBlockPos(this.worldPosition);
    }

    /** @return the tier assembled by the current contents, if any. */
    public Optional<RocketTier> assembledTier() {
        return AssemblyLogic.validate(this.items);
    }

    @Override
    public void setChanged() {
        super.setChanged();
        if (this.level != null && !this.level.isClientSide()) {
            this.level.blockEntityChanged(this.worldPosition);
        }
    }

    @Override
    public void loadAdditional(net.minecraft.world.level.storage.ValueInput input) {
        super.loadAdditional(input);
        this.items = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(input, this.items);
    }

    @Override
    protected void saveAdditional(net.minecraft.world.level.storage.ValueOutput output) {
        super.saveAdditional(output);
        ContainerHelper.saveAllItems(output, this.items);
    }
}
