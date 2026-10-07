package com.amaro.stellarodyssey.satellites.starmap.screen;

import com.amaro.stellarodyssey.api.celestial.ICelestialBody;
import com.amaro.stellarodyssey.api.celestial.ICelestialCatalog;
import com.amaro.stellarodyssey.entity.RocketEntity;
import com.amaro.stellarodyssey.network.SelectDestinationPayload;
import com.amaro.stellarodyssey.satellites.starmap.render.StarMapSkyRenderer;
import dev.architectury.networking.NetworkManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * Fullscreen celestial navigation UI querying {@link ICelestialCatalog} without direct
 * world generation, mob entity, or dimensions coupling.
 * <p>
 * Provides interactive galactic exploration, orbital trajectory mapping, sector coordinate telemetry,
 * and environmental hazard assessment.
 */
public class StarMapScreen extends Screen {
    private final ICelestialCatalog catalog;
    private final List<ICelestialBody> chartedBodies = new ArrayList<>();
    private final List<String> availableSystems = new ArrayList<>();
    private int systemFilterIndex = 0; // 0 = ALL

    private ICelestialBody selectedBody;
    private ICelestialBody hoveredBody;

    private double panX = 0.0;
    private double panY = 0.0;
    private double targetPanX = 0.0;
    private double targetPanY = 0.0;
    private double mapOffsetX;
    private float zoom = 1.0F;
    private float targetZoom = 1.0F;

    private boolean isDragging = false;
    private double animationTicks = 0.0;
    private float transitionProgress;
    private Component navigationStatusMessage = Component.literal("HYPERDRIVE READY // SELECT DESTINATION");
    private StarMapCoordinatesWidget coordinatesWidget;
    private Button launchButton;
    private int rocketTier = 0;
    private int rocketEntityId = -1;

    public StarMapScreen(ICelestialCatalog catalog) {
        this(Component.literal("Galactic Star Map Navigation"), catalog, 0, -1);
    }

    public StarMapScreen(Component title, ICelestialCatalog catalog) {
        this(title, catalog, 0, -1);
    }

    public StarMapScreen(ICelestialCatalog catalog, int rocketTier, int rocketEntityId) {
        this(Component.literal("Galactic Star Map Navigation"), catalog, rocketTier, rocketEntityId);
    }

    public StarMapScreen(Component title, ICelestialCatalog catalog, int rocketTier, int rocketEntityId) {
        super(title);
        this.catalog = catalog;
        this.rocketTier = rocketTier;
        this.rocketEntityId = rocketEntityId;
    }

    @Override
    protected void init() {
        super.init();
        loadCatalogData();
        this.targetZoom = this.zoom;
        this.targetPanX = this.panX;
        this.targetPanY = this.panY;

        if (this.rocketEntityId < 0) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null && mc.player.getVehicle() instanceof RocketEntity rocket) {
                this.rocketEntityId = rocket.getId();
                if (this.rocketTier <= 0) {
                    this.rocketTier = rocket.getTierLevel();
                }
            }
        }

        // Adapt the telemetry panel to the available GUI resolution and scale.
        int margin = Math.clamp(this.width / 48, 6, 12);
        int widgetWidth = Math.clamp(this.width / 3, 168, 230);
        int widgetHeight = Math.clamp(this.height - 104, 24, 175);
        double previousMapOffsetX = this.mapOffsetX;
        this.mapOffsetX = (widgetWidth + margin - 2.0) * 0.5;
        double mapOffsetDelta = this.mapOffsetX - previousMapOffsetX;
        this.panX += mapOffsetDelta;
        this.targetPanX += mapOffsetDelta;
        this.coordinatesWidget = new StarMapCoordinatesWidget(margin, 36, widgetWidth, widgetHeight);
        addRenderableWidget(this.coordinatesWidget);

        // Scale and distribute toolbar controls relative to the window, including compact resolutions.
        boolean compact = this.width < 360;
        int gap = compact ? 2 : 4;
        int controlWidth = compact ? 24 : (this.width < 440 ? 38 : 48);
        int filterWidth = compact ? 38 : (this.width < 440 ? 64 : 78);
        int launchWidth = Math.clamp(this.width / 4, compact ? 54 : 82, 112);
        int closeWidth = compact ? 32 : 44;
        int btnY = this.height - 28;
        int btnHeight = 20;
        int x = margin;

        addRenderableWidget(Button.builder(Component.literal(compact ? "R" : "RESET"), btn -> resetView())
                .bounds(x, btnY, controlWidth, btnHeight).build());
        x += controlWidth + gap;
        addRenderableWidget(Button.builder(Component.literal("+"), btn -> adjustZoom(0.25F))
                .bounds(x, btnY, controlWidth, btnHeight).build());
        x += controlWidth + gap;
        addRenderableWidget(Button.builder(Component.literal(compact ? "-" : "ZOOM -"), btn -> adjustZoom(-0.25F))
                .bounds(x, btnY, controlWidth, btnHeight).build());
        x += controlWidth + gap;
        addRenderableWidget(Button.builder(Component.literal(compact ? "SYS" : getSystemFilterLabel()), this::cycleSystemFilter)
                .bounds(x, btnY, filterWidth, btnHeight).build());

        this.launchButton = Button.builder(Component.literal(compact ? "GO" : "LAUNCH"), btn -> engageLaunchSequence())
                .bounds(this.width - margin - closeWidth - gap - launchWidth, btnY, launchWidth, btnHeight)
                .build();
        addRenderableWidget(this.launchButton);
        updateLaunchButtonState();
        addRenderableWidget(Button.builder(Component.literal(compact ? "X" : "CLOSE"), btn -> onClose())
                .bounds(this.width - margin - closeWidth, btnY, closeWidth, btnHeight).build());

        // Update widget with initial selection if available
        if (this.selectedBody != null) {
            StarMapSkyRenderer.SectorCoordinates coords = StarMapSkyRenderer.getCoordinates(this.selectedBody);
            this.coordinatesWidget.updateTarget(this.selectedBody, coords.x(), coords.y(), coords.z(), coords.sectorCode());
        }
    }

    private void loadCatalogData() {
        this.chartedBodies.clear();
        this.availableSystems.clear();
        this.availableSystems.add("ALL");
        this.systemFilterIndex = 0;

        if (this.catalog != null) {
            Collection<ICelestialBody> all = this.catalog.getAllBodies();
            if (all != null) {
                this.chartedBodies.addAll(all);
                for (ICelestialBody body : all) {
                    if (body != null && body.starSystemName() != null && !this.availableSystems.contains(body.starSystemName())) {
                        this.availableSystems.add(body.starSystemName());
                    }
                }
            }
        }

        this.chartedBodies.removeIf(Objects::isNull);
        this.availableSystems.removeIf(system -> system == null || system.isBlank());

        if (this.selectedBody == null && !this.chartedBodies.isEmpty()) {
            this.selectedBody = this.chartedBodies.getFirst();
        }
    }

    private void resetView() {
        this.targetPanX = this.mapOffsetX;
        this.targetPanY = 0.0;
        this.targetZoom = 1.0F;
    }

    private void adjustZoom(float delta) {
        this.targetZoom = (float) Math.clamp(this.targetZoom + delta, 0.4F, 3.0F);
    }

    private void focusOnBody(ICelestialBody body) {
        if (body == null) {
            return;
        }
        StarMapSkyRenderer.SectorCoordinates coords = StarMapSkyRenderer.getCoordinates(body, this.animationTicks);
        this.targetZoom = 1.45F;
        double pitch = Math.toRadians(25.0);
        double projectedY = coords.y() * Math.cos(pitch) - coords.z() * Math.sin(pitch);
        this.targetPanX = this.mapOffsetX - coords.x() * this.targetZoom;
        this.targetPanY = -projectedY * this.targetZoom;
    }

    private String getCurrentSystemFilter() {
        if (this.availableSystems.isEmpty()) {
            return "ALL";
        }
        int index = Math.clamp(this.systemFilterIndex, 0, this.availableSystems.size() - 1);
        return this.availableSystems.get(index);
    }

    private String getSystemFilterLabel() {
        String sys = getCurrentSystemFilter();
        return "SYS: " + (sys.length() > 8 ? sys.substring(0, 8) + ".." : sys);
    }

    private void cycleSystemFilter(Button button) {
        if (this.availableSystems.isEmpty()) {
            return;
        }
        this.systemFilterIndex = (this.systemFilterIndex + 1) % this.availableSystems.size();
        button.setMessage(Component.literal(this.width < 360 ? "SYS" : getSystemFilterLabel()));
    }

    private void updateLaunchButtonState() {
        if (this.launchButton == null) {
            return;
        }
        if (this.selectedBody == null) {
            this.launchButton.active = false;
            this.launchButton.setMessage(Component.literal(this.width < 360 ? "GO" : "LAUNCH"));
            return;
        }

        if (this.rocketTier > 0) {
            int reqTier = com.amaro.stellarodyssey.registry.tiers.RocketTiers.getRequiredTier(this.selectedBody.dimensionKey());
            boolean unlocked = reqTier > 0 && this.rocketTier >= reqTier;
            this.launchButton.active = unlocked;
            if (unlocked) {
                this.launchButton.setMessage(Component.literal(this.width < 360 ? "GO" : "LAUNCH"));
            } else {
                this.launchButton.setMessage(Component.literal(this.width < 360
                        ? "T" + (reqTier > 0 ? reqTier : "?") + " LOCK"
                        : "LOCKED [TIER " + (reqTier > 0 ? reqTier : "?") + "]"));
            }
        } else {
            this.launchButton.active = true;
            this.launchButton.setMessage(Component.literal(this.width < 360
                    ? "GO"
                    : (this.rocketEntityId >= 0 ? "ENGAGE LAUNCH SEQUENCE" : "PLOT COURSE")));
        }
    }

    private void engageLaunchSequence() {
        if (this.selectedBody == null) {
            this.navigationStatusMessage = Component.literal("NO TARGET SELECTED TO ENGAGE LAUNCH");
            return;
        }

        if (this.rocketTier > 0 && !com.amaro.stellarodyssey.registry.tiers.RocketTiers.isDestinationAllowed(this.rocketTier, this.selectedBody.dimensionKey())) {
            int reqTier = com.amaro.stellarodyssey.registry.tiers.RocketTiers.getRequiredTier(this.selectedBody.dimensionKey());
            this.navigationStatusMessage = Component.literal("Â§cACCESS DENIED: DESTINATION REQUIRES TIER " + reqTier + " ROCKET");
            return;
        }

        int targetId = this.rocketEntityId;
        Minecraft mc = Minecraft.getInstance();
        if (targetId < 0 && mc.player != null && mc.player.getVehicle() instanceof RocketEntity rocket) {
            targetId = rocket.getId();
        }

        if (targetId >= 0) {
            try {
                NetworkManager.sendToServer(new SelectDestinationPayload(
                        targetId,
                        this.selectedBody.dimensionKey()));
                this.navigationStatusMessage = Component.literal(
                        "LAUNCH SEQUENCE ENGAGED: " + this.selectedBody.name().toUpperCase() + " // DESTINATION LOCKED");
                this.onClose();
                return;
            } catch (Exception ignored) {
                // Headless test or client without active server connection
            }
        }

        this.navigationStatusMessage = Component.literal(
                "COURSE PLOTTED: " + this.selectedBody.name().toUpperCase() + " [" + this.selectedBody.starSystemName() + "]");
    }

    private void plotCourse() {
        engageLaunchSequence();
    }

    @Override
    public void tick() {
        super.tick();
        this.animationTicks += 1.0;
        this.transitionProgress = Math.min(1.0F, this.transitionProgress + 0.075F);

        // Exponential easing keeps zoom and panning fluid without retaining input history.
        this.zoom += (this.targetZoom - this.zoom) * 0.22F;
        this.panX += (this.targetPanX - this.panX) * 0.22;
        this.panY += (this.targetPanY - this.panY) * 0.22;
        if (Math.abs(this.targetZoom - this.zoom) < 0.0005F) {
            this.zoom = this.targetZoom;
        }
        if (Math.abs(this.targetPanX - this.panX) < 0.01) {
            this.panX = this.targetPanX;
        }
        if (Math.abs(this.targetPanY - this.panY) < 0.01) {
            this.panY = this.targetPanY;
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean isDouble) {
        if (super.mouseClicked(event, isDouble)) {
            return true;
        }

        double mx = event.x();
        double my = event.y();

        // 1. Check if clicking on any charted celestial node in the viewport
        ICelestialBody clickedBody = findHoveredBody(mx, my, this.animationTicks);
        if (clickedBody != null) {
            this.selectedBody = clickedBody;
            focusOnBody(clickedBody);
            StarMapSkyRenderer.SectorCoordinates coords = StarMapSkyRenderer.getCoordinates(clickedBody, this.animationTicks);
            this.coordinatesWidget.updateTarget(clickedBody, coords.x(), coords.y(), coords.z(), coords.sectorCode());
            if (this.rocketTier > 0) {
                int req = com.amaro.stellarodyssey.registry.tiers.RocketTiers.getRequiredTier(clickedBody.dimensionKey());
                if (req > 0 && this.rocketTier >= req) {
                    this.navigationStatusMessage = Component.literal("TARGET LOCKED: " + clickedBody.name().toUpperCase() + " [UNLOCKED // READY FOR LAUNCH]");
                } else {
                    this.navigationStatusMessage = Component.literal("TARGET LOCKED: " + clickedBody.name().toUpperCase() + " [LOCKED - REQUIRES TIER " + req + "]");
                }
            } else {
                this.navigationStatusMessage = Component.literal("TARGET LOCKED: " + clickedBody.name().toUpperCase());
            }
            updateLaunchButtonState();
            return true;
        }

        // 2. If clicking on empty map area (not interacting with widgets)
        int mapLeft = getMapViewportLeft();
        if (event.button() == 0 && mx >= mapLeft && mx < this.width - 10
                && my >= 34 && my < this.height - 40) {
            this.isDragging = true;
            this.targetPanX = this.panX;
            this.targetPanY = this.panY;
        }

        return false;
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        this.isDragging = false;
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        if (this.isDragging) {
            this.targetPanX += dragX;
            this.targetPanY += dragY;
            return true;
        }
        return super.mouseDragged(event, dragX, dragY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (mouseX < getMapViewportLeft() || mouseX >= this.width - 10
                || mouseY < 34 || mouseY >= this.height - 40) {
            return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
        }

        float previousZoom = this.targetZoom;
        adjustZoom((float) (scrollY * 0.15F));
        if (Math.abs(this.targetZoom - previousZoom) < 1.0E-4F) {
            return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
        }

        // Keep the world point beneath the cursor stationary while zooming.
        double worldX = (mouseX - this.width * 0.5 - this.targetPanX) / previousZoom;
        double worldY = (mouseY - this.height * 0.5 - this.targetPanY) / previousZoom;
        this.targetPanX = mouseX - this.width * 0.5 - worldX * this.targetZoom;
        this.targetPanY = mouseY - this.height * 0.5 - worldY * this.targetZoom;
        return true;
    }

    private int getMapViewportLeft() {
        return this.coordinatesWidget != null
                ? this.coordinatesWidget.getX() + this.coordinatesWidget.getWidth() + 8
                : Math.clamp(this.width / 3, 168, 230) + 20;
    }

    private ICelestialBody findHoveredBody(double mouseX, double mouseY, double renderTicks) {
        if (mouseX < getMapViewportLeft() || mouseX >= this.width - 10
                || mouseY < 34 || mouseY >= this.height - 40) {
            return null;
        }

        String filter = getCurrentSystemFilter();

        for (ICelestialBody body : this.chartedBodies) {
            if (!filter.equals("ALL") && !filter.equalsIgnoreCase(body.starSystemName())) {
                continue;
            }
            StarMapSkyRenderer.SectorCoordinates coords = StarMapSkyRenderer.getCoordinates(body, renderTicks);
            StarMapSkyRenderer.ProjectedPoint pt = StarMapSkyRenderer.project(
                    coords.x(), coords.y(), coords.z(),
                    this.width, this.height,
                    this.panX, this.panY,
                    this.zoom, 25.0F, 0.0F
            );

            if (pt.visible()) {
                double distSq = (mouseX - pt.screenX()) * (mouseX - pt.screenX()) + (mouseY - pt.screenY()) * (mouseY - pt.screenY());
                if (distSq <= 120.0) { // ~11px radius hit test
                    return body;
                }
            }
        }
        return null;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor gui, int mouseX, int mouseY, float partialTick) {
        double renderTicks = this.animationTicks + partialTick;
        this.hoveredBody = findHoveredBody(mouseX, mouseY, renderTicks);

        // 1. Procedural spiral galaxy with sub-tick motion interpolation and eased viewport controls.
        StarMapSkyRenderer.renderDeepSpaceBackground(gui, this.width, this.height,
                this.panX, this.panY, this.zoom, renderTicks);

        // 2. Galactic Coordinate Grid & Range Rings
        StarMapSkyRenderer.renderGalacticGrid(gui, this.width, this.height,
                this.panX, this.panY, this.zoom, 50);

        // 3. Render Hyperspace Lanes between bodies in the same star system
        renderHyperspaceLanes(gui, renderTicks);

        // Subtle custom HUD texture: corner brackets, moving scan line, and layered frame highlights.
        renderMapFrame(gui, renderTicks);

        // 4. Render Charted Celestial Nodes with dynamic orbital and axial rotation.
        String filter = getCurrentSystemFilter();

        for (ICelestialBody body : this.chartedBodies) {
            if (!filter.equals("ALL") && !filter.equalsIgnoreCase(body.starSystemName())) {
                continue;
            }
            StarMapSkyRenderer.SectorCoordinates coords = StarMapSkyRenderer.getCoordinates(body, renderTicks);
            StarMapSkyRenderer.ProjectedPoint pt = StarMapSkyRenderer.project(
                    coords.x(), coords.y(), coords.z(),
                    this.width, this.height,
                    this.panX, this.panY,
                    this.zoom, 25.0F, 0.0F
            );

            if (pt.visible()) {
                boolean isSelected = Objects.equals(this.selectedBody, body);
                boolean isHovered = Objects.equals(this.hoveredBody, body);
                StarMapSkyRenderer.renderCelestialNode(gui, this.font, pt.screenX(), pt.screenY(), body, isSelected, isHovered, renderTicks, this.rocketTier);
            }
        }

        // 5. Update Coordinates Widget if hovering or selecting
        ICelestialBody telemetryBody = this.hoveredBody != null ? this.hoveredBody : this.selectedBody;
        if (telemetryBody != null && this.coordinatesWidget != null) {
            StarMapSkyRenderer.SectorCoordinates coords = StarMapSkyRenderer.getCoordinates(telemetryBody, renderTicks);
            this.coordinatesWidget.updateTarget(telemetryBody, coords.x(), coords.y(), coords.z(), coords.sectorCode());
        }

        // Smooth screen-in reveal layered over the map, followed by the instrument chrome.
        int revealAlpha = Math.round(96.0F * (1.0F - this.transitionProgress));
        if (revealAlpha > 0) {
            gui.fill(0, 28, this.width, this.height - 34, (revealAlpha << 24) | 0x00020712);
        }

        // 6. Top Header Bar & Navigation Title
        int headerInset = Math.clamp(this.width / 60, 6, 14);
        gui.fill(headerInset, 0, this.width - headerInset, 28, 0xE8080E1C);
        gui.fill(headerInset, 27, this.width - headerInset, 28, 0xFF00E5FF);
        gui.fill(headerInset, 2, headerInset + 34, 3, 0xFF80DEEA);
        gui.fill(this.width - headerInset - 34, 2, this.width - headerInset, 3, 0xFF80DEEA);

        String mainTitle = this.width < 420 ? "STELLAR ODYSSEY // NAV" : "STELLAR ODYSSEY // NAV-COMPUTER";
        gui.text(this.font, Component.literal(mainTitle), headerInset + 6, 9, 0xFF00E5FF);

        if (this.width >= 420) {
            String statsBadge = "[CHARTED: " + this.chartedBodies.size() + " | SYS: " + getCurrentSystemFilter() + "]";
            gui.text(this.font, Component.literal(statsBadge), this.width - this.font.width(statsBadge) - headerInset - 6, 9, 0xFF80DEEA);
        }

        // 7. Bottom Navigation Status Strip
        gui.fill(headerInset, this.height - 34, this.width - headerInset, this.height, 0xE8080E1C);
        gui.fill(headerInset, this.height - 34, this.width - headerInset, this.height - 33, 0xFF00E5FF);
        gui.fill(headerInset, this.height - 2, this.width - headerInset, this.height - 1, 0xFF16404A);

        gui.text(this.font, this.navigationStatusMessage, headerInset + 4, this.height - 22, 0xFF00E676);

        // 8. Render Widgets (Coordinates Widget, Buttons)
        super.extractRenderState(gui, mouseX, mouseY, partialTick);

        // 9. Tooltip for hovered body
        if (this.hoveredBody != null) {
            List<Component> tooltip = new ArrayList<>(8);
            tooltip.add(Component.literal("Â§bÂ§l" + this.hoveredBody.name().toUpperCase()));
            tooltip.add(Component.literal("Â§7System: Â§f" + this.hoveredBody.starSystemName()));
            tooltip.add(Component.literal("Â§7Gravity: Â§f" + String.format("%.2fg", this.hoveredBody.gravityMultiplier())));
            tooltip.add(Component.literal("Â§7Atmosphere: Â§f" + (this.hoveredBody.hasBreathableAtmosphere() ? "Â§aBreathable" : "Â§cToxic / Vacuum")));
            tooltip.add(Component.literal("Â§7Radiation: Â§f" + String.format("%.2f rad", this.hoveredBody.solarRadiation())));
            tooltip.add(Component.literal(this.hoveredBody.isHazardous() ? "Â§4âš  HIGH HAZARD ENVIRONMENT" : "Â§2â— NOMINAL CONDITIONS"));

            if (this.rocketTier > 0) {
                int reqTier = com.amaro.stellarodyssey.registry.tiers.RocketTiers.getRequiredTier(this.hoveredBody.dimensionKey());
                if (reqTier > 0) {
                    if (this.rocketTier >= reqTier) {
                        tooltip.add(Component.literal("Â§a[UNLOCKED - TIER " + reqTier + " CLEARED]"));
                    } else {
                        tooltip.add(Component.literal("Â§c[LOCKED - REQUIRES TIER " + reqTier + "]"));
                    }
                }
            }

            gui.setComponentTooltipForNextFrame(this.font, tooltip, mouseX, mouseY);
        }
    }

    /**
     * Draws a bespoke, texture-like navigation frame from low-cost GUI primitives.
     * It adds a sweeping scanner, edge ticks and animated corner brackets without a shader dependency.
     */
    private void renderMapFrame(GuiGraphicsExtractor gui, double renderTicks) {
        int left = this.coordinatesWidget != null
                ? this.coordinatesWidget.getX() + this.coordinatesWidget.getWidth() + 8
                : Math.clamp(this.width / 3, 168, 230) + 20;
        int right = this.width - 10;
        int top = 34;
        int bottom = this.height - 40;
        if (right <= left || bottom <= top) {
            return;
        }

        int edge = 0x4037DFFF;
        int bright = 0xA037DFFF;
        int corner = Math.clamp(Math.min(right - left, bottom - top) / 10, 8, 18);
        gui.fill(left, top, right, top + 1, edge);
        gui.fill(left, bottom - 1, right, bottom, edge);
        gui.fill(left, top, left + 1, bottom, edge);
        gui.fill(right - 1, top, right, bottom, edge);

        gui.fill(left, top, left + corner, top + 2, bright);
        gui.fill(left, top, left + 2, top + corner, bright);
        gui.fill(right - corner, top, right, top + 2, bright);
        gui.fill(right - 2, top, right, top + corner, bright);
        gui.fill(left, bottom - 2, left + corner, bottom, bright);
        gui.fill(left, bottom - corner, left + 2, bottom, bright);
        gui.fill(right - corner, bottom - 2, right, bottom, bright);
        gui.fill(right - 2, bottom - corner, right, bottom, bright);

        int tickStep = Math.max(14, (right - left) / 24);
        for (int x = left + tickStep; x < right; x += tickStep) {
            gui.fill(x, top, x + 1, top + 3, edge);
            gui.fill(x, bottom - 3, x + 1, bottom, edge);
        }
        for (int y = top + tickStep; y < bottom; y += tickStep) {
            gui.fill(left, y, left + 3, y + 1, edge);
            gui.fill(right - 3, y, right, y + 1, edge);
        }

        int scanRange = Math.max(1, bottom - top - 8);
        int scanY = top + 4 + (int) ((renderTicks * 1.25) % scanRange);
        gui.fill(left + 2, scanY - 1, right - 2, scanY + 2, 0x1237DFFF);
        gui.fill(left + 2, scanY, right - 2, scanY + 1, 0x4037DFFF);
    }

    private void renderHyperspaceLanes(GuiGraphicsExtractor gui, double renderTicks) {
        String filter = getCurrentSystemFilter();

        for (int i = 0; i < this.chartedBodies.size(); i++) {
            ICelestialBody b1 = this.chartedBodies.get(i);
            if (!filter.equals("ALL") && !filter.equalsIgnoreCase(b1.starSystemName())) {
                continue;
            }
            for (int j = i + 1; j < this.chartedBodies.size(); j++) {
                ICelestialBody b2 = this.chartedBodies.get(j);
                if (Objects.equals(b1.starSystemName(), b2.starSystemName())) {
                    StarMapSkyRenderer.SectorCoordinates c1 = StarMapSkyRenderer.getCoordinates(b1, renderTicks);
                    StarMapSkyRenderer.SectorCoordinates c2 = StarMapSkyRenderer.getCoordinates(b2, renderTicks);

                    StarMapSkyRenderer.ProjectedPoint p1 = StarMapSkyRenderer.project(
                            c1.x(), c1.y(), c1.z(), this.width, this.height,
                            this.panX, this.panY, this.zoom, 25.0F, 0.0F);
                    StarMapSkyRenderer.ProjectedPoint p2 = StarMapSkyRenderer.project(
                            c2.x(), c2.y(), c2.z(), this.width, this.height,
                            this.panX, this.panY, this.zoom, 25.0F, 0.0F);

                    if (p1.visible() || p2.visible()) {
                        StarMapSkyRenderer.renderHyperspaceLane(gui, p1.screenX(), p1.screenY(), p2.screenX(), p2.screenY(), 0x3300E5FF, renderTicks);
                    }
                }
            }
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    public ICelestialCatalog getCatalog() {
        return this.catalog;
    }

    public ICelestialBody getSelectedBody() {
        return this.selectedBody;
    }

    public void setSelectedBody(ICelestialBody body) {
        this.selectedBody = body;
    }

    public int getRocketTier() {
        return this.rocketTier;
    }

    public int getRocketEntityId() {
        return this.rocketEntityId;
    }
}
