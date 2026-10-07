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
    private float zoom = 1.0F;

    private boolean isDragging = false;
    private double lastDragMouseX;
    private double lastDragMouseY;

    private long animationTicks = 0L;
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

        if (this.rocketEntityId < 0) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null && mc.player.getVehicle() instanceof RocketEntity rocket) {
                this.rocketEntityId = rocket.getId();
                if (this.rocketTier <= 0) {
                    this.rocketTier = rocket.getTierLevel();
                }
            }
        }

        // 1. Interactive coordinates and hazard widget
        int widgetWidth = 230;
        int widgetHeight = 175;
        this.coordinatesWidget = new StarMapCoordinatesWidget(12, 36, widgetWidth, widgetHeight);
        addRenderableWidget(this.coordinatesWidget);

        // 2. Control buttons in the bottom toolbar
        int btnY = this.height - 28;
        int btnHeight = 20;

        // Reset View Button
        addRenderableWidget(Button.builder(Component.literal("RESET VIEW"), btn -> resetView())
                .bounds(12, btnY, 80, btnHeight)
                .build());

        // Zoom In / Out Buttons
        addRenderableWidget(Button.builder(Component.literal("ZOOM +"), btn -> adjustZoom(0.25F))
                .bounds(96, btnY, 55, btnHeight)
                .build());

        addRenderableWidget(Button.builder(Component.literal("ZOOM -"), btn -> adjustZoom(-0.25F))
                .bounds(155, btnY, 55, btnHeight)
                .build());

        // Star System Filter Button
        addRenderableWidget(Button.builder(Component.literal(getSystemFilterLabel()), this::cycleSystemFilter)
                .bounds(214, btnY, 110, btnHeight)
                .build());

        // Engage Launch Sequence Button
        this.launchButton = Button.builder(Component.literal("ENGAGE LAUNCH SEQUENCE"), btn -> engageLaunchSequence())
                .bounds(this.width - 270, btnY, 170, btnHeight)
                .build();
        addRenderableWidget(this.launchButton);
        updateLaunchButtonState();

        // Close Screen Button
        addRenderableWidget(Button.builder(Component.literal("CLOSE"), btn -> onClose())
                .bounds(this.width - 92, btnY, 80, btnHeight)
                .build());

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

        if (this.selectedBody == null && !this.chartedBodies.isEmpty()) {
            this.selectedBody = this.chartedBodies.getFirst();
        }
    }

    private void resetView() {
        this.panX = 0.0;
        this.panY = 0.0;
        this.zoom = 1.0F;
    }

    private void adjustZoom(float delta) {
        this.zoom = (float) Math.clamp(this.zoom + delta, 0.4F, 3.0F);
    }

    private String getSystemFilterLabel() {
        String sys = this.availableSystems.get(this.systemFilterIndex);
        return "SYS: " + (sys.length() > 8 ? sys.substring(0, 8) + ".." : sys);
    }

    private void cycleSystemFilter(Button button) {
        if (this.availableSystems.isEmpty()) {
            return;
        }
        this.systemFilterIndex = (this.systemFilterIndex + 1) % this.availableSystems.size();
        button.setMessage(Component.literal(getSystemFilterLabel()));
    }

    private void updateLaunchButtonState() {
        if (this.launchButton == null) {
            return;
        }
        if (this.selectedBody == null) {
            this.launchButton.active = false;
            return;
        }
        if (this.rocketTier > 0) {
            int reqTier = com.amaro.stellarodyssey.registry.tiers.RocketTiers.getRequiredTier(this.selectedBody.dimensionKey());
            boolean unlocked = reqTier > 0 && this.rocketTier >= reqTier;
            this.launchButton.active = unlocked;
            if (unlocked) {
                this.launchButton.setMessage(Component.literal("ENGAGE LAUNCH SEQUENCE"));
            } else {
                this.launchButton.setMessage(Component.literal("LOCKED [TIER " + (reqTier > 0 ? reqTier : "?") + "]"));
            }
        } else {
            this.launchButton.active = true;
            this.launchButton.setMessage(Component.literal(this.rocketEntityId >= 0 ? "ENGAGE LAUNCH SEQUENCE" : "PLOT COURSE"));
        }
    }

    private void engageLaunchSequence() {
        if (this.selectedBody == null) {
            this.navigationStatusMessage = Component.literal("NO TARGET SELECTED TO ENGAGE LAUNCH");
            return;
        }

        if (this.rocketTier > 0 && !com.amaro.stellarodyssey.registry.tiers.RocketTiers.isDestinationAllowed(this.rocketTier, this.selectedBody.dimensionKey())) {
            int reqTier = com.amaro.stellarodyssey.registry.tiers.RocketTiers.getRequiredTier(this.selectedBody.dimensionKey());
            this.navigationStatusMessage = Component.literal("§cACCESS DENIED: DESTINATION REQUIRES TIER " + reqTier + " ROCKET");
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
        this.animationTicks++;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean isDouble) {
        double mx = event.x();
        double my = event.y();

        // 1. Check if clicking on any charted celestial node in the viewport
        ICelestialBody clickedBody = findHoveredBody(mx, my);
        if (clickedBody != null) {
            this.selectedBody = clickedBody;
            StarMapSkyRenderer.SectorCoordinates coords = StarMapSkyRenderer.getCoordinates(clickedBody);
            this.coordinatesWidget.updateTarget(clickedBody, coords.x(), coords.y(), coords.z(), coords.sectorCode());
            if (this.rocketTier > 0) {
                int req = com.amaro.stellarodyssey.registry.tiers.RocketTiers.getRequiredTier(clickedBody.dimensionKey());
                if (this.rocketTier >= req) {
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
        if (event.button() == 0 && (my > 30 && my < this.height - 32) && (mx > 250 || my > 220)) {
            this.isDragging = true;
            this.lastDragMouseX = mx;
            this.lastDragMouseY = my;
        }

        return super.mouseClicked(event, isDouble);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        this.isDragging = false;
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        if (this.isDragging) {
            this.panX += dragX;
            this.panY += dragY;
            return true;
        }
        return super.mouseDragged(event, dragX, dragY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        adjustZoom((float) (scrollY * 0.15F));
        return true;
    }

    private ICelestialBody findHoveredBody(double mouseX, double mouseY) {
        String filter = this.availableSystems.get(this.systemFilterIndex);

        for (ICelestialBody body : this.chartedBodies) {
            if (!filter.equals("ALL") && !filter.equalsIgnoreCase(body.starSystemName())) {
                continue;
            }
            StarMapSkyRenderer.SectorCoordinates coords = StarMapSkyRenderer.getCoordinates(body, this.animationTicks);
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
        // 1. Procedural Logarithmic Spiral Galaxy Skybox Background (coupled to viewport pan and zoom)
        StarMapSkyRenderer.renderDeepSpaceBackground(gui, this.width, this.height, this.panX, this.panY, this.zoom, this.animationTicks);

        // 2. Galactic Coordinate Grid & Range Rings
        StarMapSkyRenderer.renderGalacticGrid(gui, this.width, this.height, this.panX, this.panY, this.zoom, 50);

        // 3. Render Hyperspace Lanes between bodies in the same star system
        renderHyperspaceLanes(gui);

        // 4. Render Charted Celestial Nodes with dynamic orbital and axial rotation
        this.hoveredBody = findHoveredBody(mouseX, mouseY);
        String filter = this.availableSystems.get(this.systemFilterIndex);

        for (ICelestialBody body : this.chartedBodies) {
            if (!filter.equals("ALL") && !filter.equalsIgnoreCase(body.starSystemName())) {
                continue;
            }
            StarMapSkyRenderer.SectorCoordinates coords = StarMapSkyRenderer.getCoordinates(body, this.animationTicks);
            StarMapSkyRenderer.ProjectedPoint pt = StarMapSkyRenderer.project(
                    coords.x(), coords.y(), coords.z(),
                    this.width, this.height,
                    this.panX, this.panY,
                    this.zoom, 25.0F, 0.0F
            );

            if (pt.visible()) {
                boolean isSelected = Objects.equals(this.selectedBody, body);
                boolean isHovered = Objects.equals(this.hoveredBody, body);
                StarMapSkyRenderer.renderCelestialNode(gui, this.font, pt.screenX(), pt.screenY(), body, isSelected, isHovered, this.animationTicks, this.rocketTier);
            }
        }

        // 5. Update Coordinates Widget if hovering or selecting
        if (this.hoveredBody != null && this.coordinatesWidget != null) {
            StarMapSkyRenderer.SectorCoordinates coords = StarMapSkyRenderer.getCoordinates(this.hoveredBody, this.animationTicks);
            this.coordinatesWidget.updateTarget(this.hoveredBody, coords.x(), coords.y(), coords.z(), coords.sectorCode());
        }

        // 6. Top Header Bar & Navigation Title
        gui.fill(0, 0, this.width, 28, 0xEE080E1C);
        gui.fill(0, 27, this.width, 28, 0xFF00E5FF);

        String mainTitle = "STELLAR ODYSSEY // CELESTIAL CARTOGRAPHY & HYPERSPACE NAVIGATION";
        gui.text(this.font, Component.literal(mainTitle), 14, 9, 0xFF00E5FF);

        String statsBadge = String.format("[CHARTED: %d | ZOOM: %.2fx | SYS: %s]",
                this.chartedBodies.size(), this.zoom, this.availableSystems.get(this.systemFilterIndex));
        gui.text(this.font, Component.literal(statsBadge), this.width - this.font.width(statsBadge) - 14, 9, 0xFF80DEEA);

        // 7. Bottom Navigation Status Strip
        gui.fill(0, this.height - 34, this.width, this.height, 0xEE080E1C);
        gui.fill(0, this.height - 34, this.width, this.height - 33, 0x8000E5FF);

        gui.text(this.font, this.navigationStatusMessage, 14, this.height - 22, 0xFF00E676);

        // 8. Render Widgets (Coordinates Widget, Buttons)
        super.extractRenderState(gui, mouseX, mouseY, partialTick);

        // 9. Tooltip for hovered body
        if (this.hoveredBody != null) {
            List<Component> tooltip = new ArrayList<>();
            tooltip.add(Component.literal("§b§l" + this.hoveredBody.name().toUpperCase()));
            tooltip.add(Component.literal("§7System: §f" + this.hoveredBody.starSystemName()));
            tooltip.add(Component.literal("§7Gravity: §f" + String.format("%.2fg", this.hoveredBody.gravityMultiplier())));
            tooltip.add(Component.literal("§7Atmosphere: §f" + (this.hoveredBody.hasBreathableAtmosphere() ? "§aBreathable" : "§cToxic / Vacuum")));
            tooltip.add(Component.literal("§7Radiation: §f" + String.format("%.2f rad", this.hoveredBody.solarRadiation())));
            tooltip.add(Component.literal(this.hoveredBody.isHazardous() ? "§4⚠ HIGH HAZARD ENVIRONMENT" : "§2● NOMINAL CONDITIONS"));

            if (this.rocketTier > 0) {
                int reqTier = com.amaro.stellarodyssey.registry.tiers.RocketTiers.getRequiredTier(this.hoveredBody.dimensionKey());
                if (reqTier > 0) {
                    if (this.rocketTier >= reqTier) {
                        tooltip.add(Component.literal("§a[UNLOCKED - TIER " + reqTier + " CLEARED]"));
                    } else {
                        tooltip.add(Component.literal("§c[LOCKED - REQUIRES TIER " + reqTier + "]"));
                    }
                }
            }

            gui.setComponentTooltipForNextFrame(this.font, tooltip, mouseX, mouseY);
        }
    }

    private void renderHyperspaceLanes(GuiGraphicsExtractor gui) {
        String filter = this.availableSystems.get(this.systemFilterIndex);

        for (int i = 0; i < this.chartedBodies.size(); i++) {
            ICelestialBody b1 = this.chartedBodies.get(i);
            if (!filter.equals("ALL") && !filter.equalsIgnoreCase(b1.starSystemName())) {
                continue;
            }
            for (int j = i + 1; j < this.chartedBodies.size(); j++) {
                ICelestialBody b2 = this.chartedBodies.get(j);
                if (Objects.equals(b1.starSystemName(), b2.starSystemName())) {
                    StarMapSkyRenderer.SectorCoordinates c1 = StarMapSkyRenderer.getCoordinates(b1, this.animationTicks);
                    StarMapSkyRenderer.SectorCoordinates c2 = StarMapSkyRenderer.getCoordinates(b2, this.animationTicks);

                    StarMapSkyRenderer.ProjectedPoint p1 = StarMapSkyRenderer.project(
                            c1.x(), c1.y(), c1.z(), this.width, this.height, this.panX, this.panY, this.zoom, 25.0F, 0.0F);
                    StarMapSkyRenderer.ProjectedPoint p2 = StarMapSkyRenderer.project(
                            c2.x(), c2.y(), c2.z(), this.width, this.height, this.panX, this.panY, this.zoom, 25.0F, 0.0F);

                    if (p1.visible() || p2.visible()) {
                        StarMapSkyRenderer.renderHyperspaceLane(gui, p1.screenX(), p1.screenY(), p2.screenX(), p2.screenY(), 0x3300E5FF, this.animationTicks);
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
