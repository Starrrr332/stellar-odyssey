package com.amaro.stellarodyssey.client.gui;

import com.amaro.stellarodyssey.client.ClientOxygenData;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

/**
 * Sci-fi HUD overlay displaying the player's current oxygen reserves and life support status.
 * Inspired by the No Man's Sky hazard/life-support visor aesthetic.
 */
public final class OxygenHudOverlay {
    private OxygenHudOverlay() {
    }

    public static void render(GuiGraphicsExtractor gui, DeltaTracker deltaTracker) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.gui.hud.isHidden() || mc.player == null || mc.player.isSpectator()) {
            return;
        }

        int oxygen = ClientOxygenData.getOxygen();
        int maxOxygen = ClientOxygenData.getMaxOxygen();
        boolean inHazard = ClientOxygenData.isInHazard();

        // Only show if the player carries oxygen tanks or is actively in an unbreathable environment
        if (maxOxygen <= 0 && !inHazard) {
            return;
        }

        Font font = mc.font;
        float percent = ClientOxygenData.getPercentage();

        int x = 12;
        int y = 12;
        int width = 124;
        int height = 24;

        // Color palettes based on status and remaining oxygen
        int accentColor;
        int barColor;
        long time = System.currentTimeMillis();
        boolean pulse = (time / 400) % 2 == 0;

        if (inHazard && oxygen <= 0) {
            // Depleted in hazard: critical blinking red
            accentColor = pulse ? 0xFFFF1744 : 0xFF7A091E;
            barColor = 0xFFFF1744;
        } else if (percent < 0.20F) {
            // Low oxygen warning: amber/red
            accentColor = pulse ? 0xFFFF5252 : 0xFFFF9100;
            barColor = 0xFFFF5252;
        } else if (percent < 0.50F) {
            // Moderate reserves
            accentColor = 0xFFFFAB00;
            barColor = 0xFFFFAB00;
        } else {
            // Safe / full: signature sci-fi cyan
            accentColor = 0xFF00E5FF;
            barColor = 0xFF00E5FF;
        }

        // 1. Panel Background (translucent deep space obsidian)
        gui.fill(x, y, x + width, y + height, 0xC0060A14);

        // 2. High-tech left accent strip
        gui.fill(x, y, x + 2, y + height, accentColor);

        // 3. Top frame header text
        if (inHazard && oxygen <= 0) {
            gui.text(font, Component.literal("O₂ CRITICAL [0%]"), x + 6, y + 3, accentColor);
        } else {
            int pctInt = Math.round(percent * 100);
            String title = "O₂ LIFE SUPPORT [" + pctInt + "%]";
            gui.text(font, Component.literal(title), x + 6, y + 3, 0xFFE0F7FA);
        }

        // 4. Progress bar track & fill
        int barX = x + 6;
        int barY = y + 14;
        int barWidth = width - 12;
        int barHeight = 5;

        // Background track (dark inset)
        gui.fill(barX, barY, barX + barWidth, barY + barHeight, 0xFF141F30);

        // Filled bar width
        int filledWidth = Math.round(barWidth * percent);
        if (filledWidth > 0) {
            gui.fill(barX, barY, barX + filledWidth, barY + barHeight, barColor);
            // Highlight shine line
            gui.fill(barX, barY, barX + filledWidth, barY + 1, 0x80FFFFFF);
        }

        // 5. Environmental hazard alert banner (when in vacuum or underwater)
        if (inHazard) {
            String hazardLabel = oxygen > 0 ? "● HAZARD ACTIVE" : "⚠ SUFFOCATING";
            int hazardColor = oxygen > 0 ? 0xFF00E5FF : (pulse ? 0xFFFF1744 : 0xFFFFFFFF);
            gui.text(font, Component.literal(hazardLabel), x + 6, y + height + 3, hazardColor);
        }
    }
}
