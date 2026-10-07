package com.amaro.stellarodyssey.satellites.starmap.screen;

import com.amaro.stellarodyssey.api.celestial.ICelestialBody;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

/**
 * Interactive UI widget displaying galactic sector coordinates and planetary hazard levels.
 * <p>
 * Displays real-time coordinate telemetry, gravitational metrics, atmospheric pressure ratings,
 * solar radiation flux, and breathable atmosphere status for charted celestial bodies.
 */
public class StarMapCoordinatesWidget extends AbstractWidget {
    private ICelestialBody targetBody;
    private double sectorX;
    private double sectorY;
    private double sectorZ;
    private String sectorCode = "SEC-00-PRIME";
    private boolean expanded = true;
    private int displayMode = 0; // 0: Comprehensive Telemetry, 1: Hazard Analysis, 2: Orbital Physics
    private float animation;

    public StarMapCoordinatesWidget(int x, int y, int width, int height) {
        super(x, y, width, height, Component.literal("Galactic Telemetry & Hazard Monitor"));
    }

    /**
     * Updates the widget with target celestial body and sector coordinates.
     */
    public void updateTarget(ICelestialBody body, double x, double y, double z, String code) {
        this.targetBody = body;
        this.sectorX = x;
        this.sectorY = y;
        this.sectorZ = z;
        if (code != null) {
            this.sectorCode = code;
        }
    }

    /**
     * Updates coordinates without changing target celestial body.
     */
    public void updateCoordinates(double x, double y, double z, String code) {
        this.sectorX = x;
        this.sectorY = y;
        this.sectorZ = z;
        if (code != null) {
            this.sectorCode = code;
        }
    }

    public ICelestialBody getTargetBody() {
        return this.targetBody;
    }

    public boolean isExpanded() {
        return this.expanded;
    }

    public void setExpanded(boolean expanded) {
        this.expanded = expanded;
    }

    @Override
    public void onClick(MouseButtonEvent event, boolean isDouble) {
        // Clicking toggles expanded mode or cycles display mode
        if (isDouble) {
            this.expanded = !this.expanded;
        } else {
            this.displayMode = (this.displayMode + 1) % 3;
        }
        AbstractWidget.playButtonClickSound(Minecraft.getInstance().getSoundManager());
    }

    @Override
    protected void extractWidgetRenderState(GuiGraphicsExtractor gui, int mouseX, int mouseY, float partialTick) {
        if (!this.visible) {
            return;
        }

        float target = this.expanded ? 1.0F : 0.0F;
        this.animation += (target - this.animation) * 0.24F;
        Minecraft mc = Minecraft.getInstance();
        Font font = mc.font;
        int x = getX();
        int y = getY();
        int w = getWidth();
        int h = Math.max(24, Math.round(24.0F + (getHeight() - 24.0F) * this.animation));

        // 1. Translucent sci-fi background panel
        gui.fill(x, y, x + w, y + h, 0xD0070D1A);

        // 2. High-tech border and left accent strip
        int borderColor = this.isHovered ? 0xFF00E5FF : 0x8000E5FF;
        gui.fill(x, y, x + w, y + 1, borderColor);
        gui.fill(x, y + h - 1, x + w, y + h, borderColor);
        gui.fill(x, y, x + 1, y + h, borderColor);
        gui.fill(x + w - 1, y, x + w, y + h, borderColor);
        gui.fill(x, y, x + 3, y + h, 0xFF00E5FF); // Primary cyan accent line

        // 3. Header title and status badge
        String headerTitle = "NAV-TELEMETRY // " + this.sectorCode;
        gui.text(font, Component.literal(headerTitle), x + 7, y + 5, 0xFF00E5FF);

        String modeBadge = this.expanded ? ("[M" + (this.displayMode + 1) + "]") : "[+]";
        gui.text(font, Component.literal(modeBadge), x + w - font.width(modeBadge) - 6, y + 5, 0xFF80DEEA);

        if (this.animation < 0.08F) {
            return;
        }

        int lineY = y + 20;
        int contentBottom = y + h - 12;
        int contentAlpha = Math.round(255.0F * this.animation);

        // 4. Sector Coordinates Matrix
        String coordText = String.format("X: %+.1f | Y: %+.1f | Z: %+.1f ly", this.sectorX, this.sectorY, this.sectorZ);
        if (lineY + 9 <= contentBottom) {
            gui.text(font, Component.literal(coordText), x + 7, lineY, 0xFFECEFF1);
        }
        lineY += 12;

        if (lineY + 1 <= contentBottom) {
            gui.fill(x + 7, lineY, x + w - 7, lineY + 1, 0x3000E5FF);
        }
        lineY += 5;

        // 5. Planetary Hazard & Environmental Metrics
        if (this.targetBody != null) {
            String nameStr = "TARGET: " + this.targetBody.name().toUpperCase() + " [" + this.targetBody.starSystemName() + "]";
            if (lineY + 9 <= contentBottom) {
                gui.text(font, Component.literal(nameStr), x + 7, lineY, (contentAlpha << 24) | 0x0080D8FF);
            }
            lineY += 13;

            // Hazard Assessment
            boolean hazardous = this.targetBody.isHazardous();
            boolean breathable = this.targetBody.hasBreathableAtmosphere();
            float rad = this.targetBody.solarRadiation();
            float pressure = this.targetBody.atmosphericPressure();
            double grav = this.targetBody.gravityMultiplier();

            String hazardClass;
            int hazardColor;
            if (!breathable && (rad > 2.0F || pressure < 0.1F || pressure > 4.0F)) {
                hazardClass = "HAZARD: CLASS IV [EXTREME/LETHAL]";
                hazardColor = 0xFFFF1744;
            } else if (hazardous) {
                hazardClass = "HAZARD: CLASS II [HOSTILE ENVIRONMENT]";
                hazardColor = 0xFFFF9100;
            } else {
                hazardClass = "HAZARD: CLASS I [SAFE / HABITABLE]";
                hazardColor = 0xFF00E676;
            }

            if (lineY + 9 <= contentBottom) {
                gui.text(font, Component.literal(hazardClass), x + 7, lineY, (contentAlpha << 24) | (hazardColor & 0x00FFFFFF));
            }
            lineY += 14;

            // Telemetry bars based on mode
            if (this.displayMode == 0 || this.displayMode == 1) {
                // Radiation Meter
                String radLabel = String.format("RAD: %.2f rad %s", rad, rad > 1.5F ? "[CRITICAL]" : "[NOMINAL]");
                if (lineY + 8 <= contentBottom) {
                    gui.text(font, Component.literal(radLabel), x + 7, lineY, (contentAlpha << 24) | 0x00B0BEC5);
                    renderMiniBar(gui, x + w - 65, lineY + 1, 58, 6, Math.min(1.0F, rad / 3.0F), rad > 1.5F ? 0xFFFF1744 : 0xFF00E5FF);
                }
                lineY += 12;

                // Atmospheric Pressure Meter
                String pressLabel = String.format("ATM: %.2f atm %s", pressure, this.targetBody.isVacuum() ? "[VACUUM]" : (pressure > 3.0F ? "[CRUSHING]" : "[STABLE]"));
                if (lineY + 8 <= contentBottom) {
                    gui.text(font, Component.literal(pressLabel), x + 7, lineY, (contentAlpha << 24) | 0x00B0BEC5);
                    renderMiniBar(gui, x + w - 65, lineY + 1, 58, 6, Math.min(1.0F, pressure / 4.0F), pressure < 0.2F ? 0xFFFF5252 : 0xFF00E5FF);
                }
                lineY += 12;
            }

            if (this.displayMode == 0 || this.displayMode == 2) {
                // Gravity Meter
                String gravLabel = String.format("GRAV: %.2fg %s", grav, grav < 0.8 ? "[LOW-G]" : (grav > 1.5 ? "[HEAVY-G]" : "[STD]"));
                if (lineY + 8 <= contentBottom) {
                    gui.text(font, Component.literal(gravLabel), x + 7, lineY, (contentAlpha << 24) | 0x00B0BEC5);
                    renderMiniBar(gui, x + w - 65, lineY + 1, 58, 6, (float) Math.min(1.0, grav / 2.5), 0xFF00E5FF);
                }
                lineY += 12;

                // Respiration status
                String respStr = breathable ? "● BREATHABLE ATMOSPHERE" : "⚠ SEALED SUIT REQUIRED";
                int respColor = breathable ? 0xFF00E676 : 0xFFFF5252;
                if (lineY + 9 <= contentBottom) {
                    gui.text(font, Component.literal(respStr), x + 7, lineY, (contentAlpha << 24) | (respColor & 0x00FFFFFF));
                }
                lineY += 12;
            }
        } else {
            if (lineY + 9 <= contentBottom) {
                gui.text(font, Component.literal("STATUS: SECTOR SCANNING..."), x + 7, lineY, 0xFF78909C);
            }
            lineY += 14;
            if (lineY + 9 <= contentBottom) {
                gui.text(font, Component.literal("NO CELESTIAL TARGET LOCKED"), x + 7, lineY, 0xFF546E7A);
            }
            lineY += 12;
            if (lineY + 9 <= contentBottom) {
                gui.text(font, Component.literal("SELECT PLANET TO ANALYZE HAZARD"), x + 7, lineY, 0xFF37474F);
            }
            lineY += 12;
        }

        // Bottom hint
        if (this.animation > 0.9F) {
            gui.text(font, Component.literal("◄ CLICK TO CYCLE / DBL-CLICK COLLAPSE ►"), x + 7, y + h - 11,
                    (contentAlpha << 24) | 0x00455A64);
        }
    }

    private void renderMiniBar(GuiGraphicsExtractor gui, int bx, int by, int bw, int bh, float fraction, int fillColor) {
        // Track
        gui.fill(bx, by, bx + bw, by + bh, 0xFF141F30);
        // Fill
        int filledW = Math.round(bw * Math.clamp(fraction, 0.0F, 1.0F));
        if (filledW > 0) {
            gui.fill(bx, by, bx + filledW, by + bh, fillColor);
            gui.fill(bx, by, bx + filledW, by + 1, 0x80FFFFFF);
        }
        // Border
        gui.fill(bx, by, bx + bw, by + 1, 0x40FFFFFF);
        gui.fill(bx, by + bh - 1, bx + bw, by + bh, 0x40FFFFFF);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        String narrText = String.format("Sector %s. Coordinates X %.1f, Y %.1f, Z %.1f.", this.sectorCode, this.sectorX, this.sectorY, this.sectorZ);
        if (this.targetBody != null) {
            narrText += String.format(" Target planet %s. Star system %s. %s.",
                    this.targetBody.name(),
                    this.targetBody.starSystemName(),
                    this.targetBody.isHazardous() ? "Hazardous environment" : "Safe environment");
        }
        output.add(NarratedElementType.TITLE, Component.literal(narrText));
    }
}
