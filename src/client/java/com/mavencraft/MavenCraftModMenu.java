package com.mavencraft;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import java.util.Comparator;
import java.util.List;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class MavenCraftModMenu implements ModMenuApi {

    private static final int ORE_LIST_TOP = 128;
    private static final int ORE_LIST_BOTTOM_PADDING = 38;
    private static final int ORE_ROW_HEIGHT = 21;
    private static final int ORE_COLUMN_COUNT = 2;
    private static final int ORE_BUTTON_WIDTH = 146;
    private static final int ORE_COLUMN_GAP = 8;
    private static final List<String> ORE_IDS = MavenCraftConfig.ORE_COLORS.stream()
            .map(OreColor::blockId)
            .sorted(Comparator.naturalOrder())
            .toList();

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return MavenCraftConfigScreen::new;
    }

    private static final class MavenCraftConfigScreen extends Screen {
        private final Screen parent;
        private int oreScrollOffset;

        private MavenCraftConfigScreen(Screen parent) {
            super(Component.literal("MavenCraft Settings"));
            this.parent = parent;
        }

        @Override
        protected void init() {
            int center = width / 2;
            int top = 34;
            int halfWidth = 150;
            int gap = 6;

            addRenderableWidget(Button.builder(xrayLabel(), button -> {
                        MavenCraftRenderer.setEnabled(!MavenCraftRenderer.enabled);
                        button.setMessage(xrayLabel());
                    })
                    .bounds(center - halfWidth - gap / 2, top, halfWidth, 20)
                    .build());
            addRenderableWidget(Button.builder(radarLabel(), button -> {
                        RadarManager.enabled = !RadarManager.enabled;
                        button.setMessage(radarLabel());
                    })
                    .bounds(center + gap / 2, top, halfWidth, 20)
                    .build());
            top += 24;
            addRenderableWidget(Button.builder(aimLabel(), button -> {
                        ProjectilePredictor.enabled = !ProjectilePredictor.enabled;
                        button.setMessage(aimLabel());
                    })
                    .bounds(center - halfWidth - gap / 2, top, halfWidth, 20)
                    .build());
            addRenderableWidget(new ScanRadiusSlider(
                    center + gap / 2, top, halfWidth, 20, (MavenCraftRenderer.getScanRadius() - 16) / 240.0));

            addRenderableWidget(Button.builder(breadcrumbLabel(), button -> {
                        BreadcrumbTrail.setEnabled(!BreadcrumbTrail.enabled);
                        button.setMessage(breadcrumbLabel());
                    })
                    .bounds(center - 150, 82, 300, 20)
                    .build());

            int visibleRows = visibleOreRows();
            int visibleCount = visibleRows * ORE_COLUMN_COUNT;
            int endIndex = Math.min(ORE_IDS.size(), oreScrollOffset + visibleCount);
            String range = ORE_IDS.isEmpty()
                    ? "No ores configured"
                    : "Enabled ores · " + (oreScrollOffset + 1) + "–" + endIndex + " of " + ORE_IDS.size()
                            + " · scroll to browse";
            Button rangeLabel = Button.builder(Component.literal(range), button -> {})
                    .bounds(center - 150, 106, 300, 18)
                    .build();
            rangeLabel.active = false;
            addRenderableWidget(rangeLabel);

            int buttonHeight = 19;
            for (int row = 0; row < visibleRows; row++) {
                for (int column = 0; column < ORE_COLUMN_COUNT; column++) {
                    int index = oreScrollOffset + row * ORE_COLUMN_COUNT + column;
                    if (index >= ORE_IDS.size()) continue;

                    String id = ORE_IDS.get(index);
                    String name = oreName(id);
                    int x = center
                            - ORE_BUTTON_WIDTH
                            - ORE_COLUMN_GAP / 2
                            + column * (ORE_BUTTON_WIDTH + ORE_COLUMN_GAP);
                    int y = ORE_LIST_TOP + row * ORE_ROW_HEIGHT;
                    addRenderableWidget(Button.builder(oreLabel(name, id), button -> {
                                String toggleId = id.equals("minecraft:ancient_debris") ? "netherite" : id;
                                if (MavenCraftRenderer.enabledOres.contains(id)) {
                                    MavenCraftRenderer.disableOre(toggleId);
                                } else {
                                    MavenCraftRenderer.enableOre(toggleId);
                                }
                                MavenCraftRenderer.invalidateCache();
                                MavenCraftRenderer.forceRescan();
                                button.setMessage(oreLabel(name, id));
                            })
                            .bounds(x, y, ORE_BUTTON_WIDTH, buttonHeight)
                            .build());
                }
            }

            addRenderableWidget(Button.builder(Component.translatable("gui.done"), button -> onClose())
                    .bounds(center - 100, height - 28, 200, 20)
                    .build());
        }

        @Override
        public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
            int listBottom = height - ORE_LIST_BOTTOM_PADDING;
            if (mouseX < width / 2.0 - 160
                    || mouseX > width / 2.0 + 160
                    || mouseY < ORE_LIST_TOP
                    || mouseY >= listBottom
                    || verticalAmount == 0) {
                return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
            }

            int visibleCount = visibleOreRows() * ORE_COLUMN_COUNT;
            int maxOffset = Math.max(0, ORE_IDS.size() - visibleCount);
            oreScrollOffset = (int) Math.clamp(
                    oreScrollOffset + (verticalAmount < 0 ? ORE_COLUMN_COUNT : -ORE_COLUMN_COUNT), 0, maxOffset);
            rebuildWidgets();
            return true;
        }

        @Override
        public void onClose() {
            minecraft.setScreenAndShow(parent);
        }

        private static Component xrayLabel() {
            return stateLabel("X-ray", MavenCraftRenderer.enabled);
        }

        private static Component radarLabel() {
            return stateLabel("Radar", RadarManager.enabled);
        }

        private static Component aimLabel() {
            return stateLabel("Projectile aim", ProjectilePredictor.enabled);
        }

        private static Component breadcrumbLabel() {
            return stateLabel("Breadcrumb trail", BreadcrumbTrail.enabled);
        }

        private int visibleOreRows() {
            return Math.max(1, (height - ORE_LIST_TOP - ORE_LIST_BOTTOM_PADDING) / ORE_ROW_HEIGHT);
        }

        private static Component stateLabel(String name, boolean enabled) {
            return Component.literal(name + ": " + (enabled ? "On" : "Off"));
        }

        private static Component oreLabel(String name, String id) {
            return stateLabel(name, MavenCraftRenderer.enabledOres.contains(id));
        }

        private static String oreName(String id) {
            if (id.equals("minecraft:ancient_debris")) return "netherite";
            return id.substring(id.indexOf(':') + 1).replace('_', ' ');
        }
    }

    private static final class ScanRadiusSlider extends AbstractSliderButton {
        private static final int STEPS = 15;

        private ScanRadiusSlider(int x, int y, int width, int height, double value) {
            super(x, y, width, height, Component.empty(), value);
            setValue(value);
            updateMessage();
        }

        @Override
        protected void setValue(double value) {
            super.setValue(Math.round(value * STEPS) / (double) STEPS);
        }

        @Override
        protected void updateMessage() {
            setMessage(Component.literal("Scan radius: " + radius() + " blocks"));
        }

        @Override
        protected void applyValue() {
            MavenCraftRenderer.setScanRadius(radius());
        }

        private int radius() {
            return 16 + (int) Math.round(value * STEPS) * 16;
        }
    }
}
