package com.amaro.stellarodyssey.client.screen;

import com.amaro.stellarodyssey.StellarOdyssey;
import com.amaro.stellarodyssey.block.entity.AssemblyTableMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

/**
 * GUI for the Rocket Assembly Table.
 * <p>
 * MC 26.3 render pipeline: the background is drawn in {@link #extractBackground}
 * (the modern replacement of {@code renderBg}); slots, labels and tooltips are
 * handled by the inherited {@code extractSlots}/{@code extractLabels}/{@code extractTooltip}.
 * </p>
 */
public class AssemblyTableScreen extends AbstractContainerScreen<AssemblyTableMenu> {
    private static final Identifier TEXTURE = StellarOdyssey.id("textures/gui/assembly_table.png");
    private static final int IMAGE_WIDTH = 176;
    private static final int IMAGE_HEIGHT = 166;

    public AssemblyTableScreen(AssemblyTableMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, IMAGE_WIDTH, IMAGE_HEIGHT);
        this.titleLabelX = 8;
        this.titleLabelY = 6;
        this.inventoryLabelX = 8;
        this.inventoryLabelY = 71;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        int x = this.leftPos;
        int y = this.topPos;
        graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, x, y, 0.0F, 0.0F,
                this.imageWidth, this.imageHeight, 256, 256);
    }
}
