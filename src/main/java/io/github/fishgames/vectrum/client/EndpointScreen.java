package io.github.fishgames.vectrum.client;

import io.github.fishgames.vectrum.Vectrum;
import io.github.fishgames.vectrum.block.EndpointMode;
import io.github.fishgames.vectrum.core.diagnosis.FlowReason;
import io.github.fishgames.vectrum.core.routing.PortSettings;
import io.github.fishgames.vectrum.core.transport.TransportType;
import io.github.fishgames.vectrum.core.upgrade.UpgradeType;
import io.github.fishgames.vectrum.gui.EndpointKind;
import io.github.fishgames.vectrum.gui.EndpointMenu;
import io.github.fishgames.vectrum.net.VectrumNet;
import io.github.fishgames.vectrum.registry.ModItems;
import io.github.fishgames.vectrum.world.PortDiagnosis;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Endpoint screen: controls on the left, upgrades and live information on the right.
 * <ul>
 *   <li>Buttons form a fixed pool; every tick the layout decides which ones show and where.</li>
 *   <li>Texts and hover regions are rebuilt with the layout.</li>
 *   <li>Every control belongs to a page ({@link EndpointMenu.Element}); the advanced page is reached with the gear
 *       button.</li>
 *   <li>The panel is drawn without a texture.</li>
 * </ul>
 */
public final class EndpointScreen extends AbstractContainerScreen<EndpointMenu> {
    private static final int WIDTH = 320;
    private static final int HEIGHT = 224;
    private static final int BUTTON_HEIGHT = 14;
    private static final int ICON_BUTTON = 20;
    private static final int LEFT = 8;
    private static final int LEFT_WIDTH = 182;
    private static final int RIGHT = 196;
    private static final int RIGHT_WIDTH = 116;

    private static final int TEXT = 0xFF404040;
    private static final int MUTED = 0xFF707070;
    private static final int GOOD = 0xFF2E7D32;
    private static final int BAD = 0xFFB03030;

    private static final ResourceLocation ICON_COPY = Vectrum.id("textures/gui/copy.png");
    private static final ResourceLocation ICON_PASTE = Vectrum.id("textures/gui/paste.png");
    private static final ResourceLocation ICON_GEAR = Vectrum.id("textures/gui/gear.png");

    private record Text(FormattedCharSequence sequence, int x, int y, int color) {
    }

    private record Hot(int x, int y, int width, int height, List<Component> tooltip) {
    }

    /** Button with a 16x16 picture instead of a label. */
    private static final class IconButton extends Button {
        private final ResourceLocation icon;

        IconButton(ResourceLocation icon, OnPress onPress) {
            super(0, 0, ICON_BUTTON, ICON_BUTTON, Component.empty(), onPress, DEFAULT_NARRATION);
            this.icon = icon;
        }

        @Override
        public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            super.renderWidget(graphics, mouseX, mouseY, partialTick);
            graphics.setColor(1, 1, 1, active ? 1 : 0.4F);
            graphics.blit(icon, getX() + (getWidth() - 16) / 2, getY() + (getHeight() - 16) / 2, 0, 0, 16, 16, 16, 16);
            graphics.setColor(1, 1, 1, 1);
        }
    }

    private final Button[] buttons = new Button[EndpointMenu.BUTTON_COUNT];
    private Button advancedButton;
    private Button backButton;
    private EditBox nameBox;
    private EditBox priorityBox;
    /** Set while the screen itself writes into an edit box, so that this is not sent as an edit by the player. */
    private boolean updatingBoxes;
    private final List<Text> texts = new ArrayList<>();
    private final List<Hot> hots = new ArrayList<>();
    private EndpointKind tooltipKind;

    public EndpointScreen(EndpointMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = WIDTH;
        this.imageHeight = HEIGHT;
        this.titleLabelX = LEFT;
        this.titleLabelY = 5;
        this.inventoryLabelY = 10000;
    }

    /** Left edge of the panel on the screen. */
    public int left() {
        return leftPos;
    }

    /** Top edge of the panel on the screen. */
    public int top() {
        return topPos;
    }

    @Override
    protected void init() {
        super.init();
        for (int id = 0; id < buttons.length; id++) {
            int buttonId = id;
            Button button = switch (id) {
                case EndpointMenu.BUTTON_COPY -> new IconButton(ICON_COPY, pressed -> press(buttonId));
                case EndpointMenu.BUTTON_PASTE -> new IconButton(ICON_PASTE, pressed -> press(buttonId));
                default -> Button.builder(Component.empty(), pressed -> press(buttonId))
                        .bounds(leftPos, topPos, 20, BUTTON_HEIGHT).build();
            };
            String tip = tooltipKey(id);
            if (tip != null) {
                button.setTooltip(Tooltip.create(Component.translatable(tip)));
            }
            buttons[id] = addRenderableWidget(button);
        }
        advancedButton = addRenderableWidget(new IconButton(ICON_GEAR, pressed -> menu.setPage(EndpointMenu.Page.ADVANCED)));
        advancedButton.setTooltip(Tooltip.create(Component.translatable("gui.vectrum.advanced")));
        backButton = addRenderableWidget(Button.builder(Component.translatable("gui.vectrum.back"),
                pressed -> menu.setPage(EndpointMenu.Page.BASIC)).bounds(leftPos, topPos, 60, BUTTON_HEIGHT).build());

        nameBox = new EditBox(font, leftPos + LEFT, topPos + 4, LEFT_WIDTH, 12, Component.translatable("gui.vectrum.tip.name"));
        nameBox.setMaxLength(PortSettings.MAX_NAME_LENGTH);
        nameBox.setTooltip(Tooltip.create(Component.translatable("gui.vectrum.tip.name")));
        nameBox.setResponder(value -> {
            if (!updatingBoxes) {
                VectrumNet.sendPortName(value.equals(defaultName()) ? "" : value);
            }
        });
        addRenderableWidget(nameBox);

        priorityBox = new EditBox(font, leftPos + LEFT + 62, topPos + 72, 60, 12, Component.translatable("gui.vectrum.priority_label"));
        priorityBox.setMaxLength(5);
        priorityBox.setFilter(value -> value.matches("-?\\d{0,4}"));
        priorityBox.setTooltip(Tooltip.create(Component.translatable("gui.vectrum.tip.priority")));
        priorityBox.setResponder(value -> {
            if (updatingBoxes) {
                return;
            }
            try {
                VectrumNet.sendPortPriority(Integer.parseInt(value));
            } catch (NumberFormatException ignored) {
                // empty or just a minus sign: nothing to send yet
            }
        });
        addRenderableWidget(priorityBox);
        refreshLayout();
    }

    private String defaultName() {
        return title.getString();
    }

    private static String tooltipKey(int id) {
        return switch (id) {
            case EndpointMenu.BUTTON_ROLE -> "gui.vectrum.tip.role";
            case EndpointMenu.BUTTON_DISTRIBUTION -> "gui.vectrum.tip.distribution";
            case EndpointMenu.BUTTON_REDSTONE -> "gui.vectrum.tip.redstone";
            case EndpointMenu.BUTTON_FILTER_TYPE -> "gui.vectrum.tip.filter_type";
            case EndpointMenu.BUTTON_CLEAR_FILTER -> "gui.vectrum.tip.clear";
            case EndpointMenu.BUTTON_SIDE_PREVIOUS, EndpointMenu.BUTTON_SIDE_NEXT -> "gui.vectrum.tip.side";
            case EndpointMenu.BUTTON_COPY -> "gui.vectrum.tip.copy";
            case EndpointMenu.BUTTON_PASTE -> "gui.vectrum.tip.paste";
            default -> id >= EndpointMenu.BUTTON_LINK && id < EndpointMenu.BUTTON_LINK + EndpointMenu.TYPE_ORDER.size()
                    ? "gui.vectrum.tip.link" : null;
        };
    }

    private void press(int id) {
        if (minecraft != null && minecraft.gameMode != null) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
        }
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        nameBox.tick();
        priorityBox.tick();
        refreshLayout();
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // A focused edit box gets the keys first; otherwise the inventory key would close the screen while typing
        EditBox focused = nameBox.isFocused() ? nameBox : priorityBox.isFocused() ? priorityBox : null;
        if (focused != null && keyCode != 256) {
            return focused.keyPressed(keyCode, scanCode, modifiers) || focused.canConsumeInput()
                    || super.keyPressed(keyCode, scanCode, modifiers);
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    // Layout

    private void place(int id, int x, int y, int width, Component label, boolean active) {
        Button button = buttons[id];
        button.visible = true;
        button.active = active;
        button.setX(leftPos + x);
        button.setY(topPos + y);
        button.setWidth(width);
        button.setMessage(label);
    }

    private void text(Component component, int x, int y, int width, int color) {
        FormattedText shown = component;
        if (font.width(component) > width) {
            shown = FormattedText.composite(font.substrByWidth(component, width - font.width("...")),
                    FormattedText.of("..."));
        }
        texts.add(new Text(Language.getInstance().getVisualOrder(shown), x, y, color));
    }

    private void centered(Component component, int centerX, int y, int width, int color) {
        int used = Math.min(width, font.width(component));
        text(component, centerX - used / 2, y, width, color);
    }

    private void wrapped(Component component, int x, int y, int width, int color) {
        for (FormattedCharSequence line : font.split(component, width)) {
            texts.add(new Text(line, x, y, color));
            y += 10;
        }
    }

    private void refreshLayout() {
        if (buttons[0] == null || nameBox == null) {
            return;
        }
        for (Button button : buttons) {
            button.visible = false;
        }
        advancedButton.visible = false;
        backButton.visible = false;
        showBox(nameBox, false, 0, 0);
        showBox(priorityBox, false, 0, 0);
        texts.clear();
        hots.clear();
        EndpointKind kind = menu.kind();
        if (kind != tooltipKind) {
            tooltipKind = kind;
            Component roleTip = Component.translatable(kind == EndpointKind.ENERGY_CABLE
                    ? "gui.vectrum.tip.role_set_energy" : "gui.vectrum.tip.role_set");
            for (int i = 0; i < 3; i++) {
                buttons[EndpointMenu.BUTTON_ROLE_SET + i].setTooltip(Tooltip.create(roleTip));
            }
        }
        boolean cable = kind == EndpointKind.CABLE || kind == EndpointKind.ENERGY_CABLE;
        if (menu.page() == EndpointMenu.Page.ADVANCED && !cable) {
            menu.setPage(EndpointMenu.Page.BASIC);
        }
        if (cable) {
            if (menu.page() == EndpointMenu.Page.ADVANCED) {
                backButton.visible = true;
                backButton.setX(leftPos + LEFT);
                backButton.setY(topPos + 19);
                text(Component.translatable("gui.vectrum.advanced"), LEFT, 6, LEFT_WIDTH, TEXT);
            } else {
                advancedButton.visible = true;
                advancedButton.setX(leftPos + RIGHT + RIGHT_WIDTH - ICON_BUTTON);
                advancedButton.setY(topPos + 121);
            }
        }
        switch (kind) {
            case CABLE, ENERGY_CABLE -> {
                if (menu.shown(EndpointMenu.Element.NAME)) {
                    syncName();
                    showBox(nameBox, true, LEFT, 4);
                }
                if (menu.shown(EndpointMenu.Element.SIDE)) {
                    sideRow(19);
                }
                if (menu.shown(EndpointMenu.Element.ROLE)) {
                    roleRow(36);
                }
                if (menu.shown(EndpointMenu.Element.DISTRIBUTION)) {
                    boolean source = menu.role() == EndpointMode.SOURCE
                            || (kind == EndpointKind.ENERGY_CABLE && menu.role() != EndpointMode.OFF);
                    if (source) {
                        place(EndpointMenu.BUTTON_DISTRIBUTION, LEFT, 53, 98,
                                Component.translatable("gui.vectrum.distribution",
                                        Component.translatable("gui.vectrum.distribution." + menu.distribution().id())),
                                true);
                    }
                }
                if (menu.shown(EndpointMenu.Element.REDSTONE)) {
                    place(EndpointMenu.BUTTON_REDSTONE, LEFT + 100, 53, LEFT_WIDTH - 100,
                            Component.translatable("gui.vectrum.redstone",
                                    Component.translatable("gui.vectrum.redstone." + menu.redstone().id())), true);
                }
                if (menu.shown(EndpointMenu.Element.PRIORITY)) {
                    priorityRow(72);
                }
                if (menu.shown(EndpointMenu.Element.FILTER)) {
                    filterRow(104);
                }
                upgradeLabel();
                if (menu.shown(EndpointMenu.Element.STATS)) {
                    typeInfo();
                }
                if (menu.shown(EndpointMenu.Element.TOOLS)) {
                    copyPaste();
                }
                if (menu.page() == EndpointMenu.Page.ADVANCED && !anyOnAdvanced()) {
                    wrapped(Component.translatable("gui.vectrum.advanced_empty"), LEFT, 40, LEFT_WIDTH, MUTED);
                }
            }
            case REDSTONE -> {
                sideRow(17);
                roleRow(35);
                text(Component.translatable("gui.vectrum.signal", menu.signal()), LEFT, 57, LEFT_WIDTH, TEXT);
                copyPaste();
            }
            case WIRELESS -> {
                frequencyRow(17);
                boolean dense = shownTypes() > 4;
                int top = dense ? 32 : 35;
                int step = dense ? 15 : 18;
                int slot = 0;
                for (int i = 0; i < EndpointMenu.TYPE_ORDER.size(); i++) {
                    TransportType type = EndpointMenu.TYPE_ORDER.get(i);
                    if (menu.hasType(type)) {
                        place(EndpointMenu.BUTTON_LINK + i, LEFT + (slot % 2) * (LEFT_WIDTH / 2 + 1),
                                top + (slot / 2) * step, LEFT_WIDTH / 2 - 1,
                                Component.translatable("gui.vectrum.link", typeName(type),
                                        Component.translatable("gui.vectrum.link." + menu.linkMode(type).id())), true);
                        slot++;
                    }
                }
                if (dense) {
                    sideRow(77);
                    priorityRow(92);
                    filterRow(107);
                } else {
                    sideRow(71);
                    priorityRow(89);
                    filterRow(104);
                }
                upgradeLabel();
                typeInfo();
                copyPaste();
            }
            case CODER -> {
                frequencyRow(17);
                wrapped(Component.translatable("gui.vectrum.coder_hint"), LEFT, 40, LEFT_WIDTH, MUTED);
                copyPaste();
            }
        }
    }

    /** Whether a control is placed on the advanced page. */
    private boolean anyOnAdvanced() {
        return EndpointMenu.pageHasControls(EndpointMenu.Page.ADVANCED);
    }

    private void showBox(EditBox box, boolean visible, int x, int y) {
        box.visible = visible;
        box.active = visible;
        if (visible) {
            box.setX(leftPos + x);
            box.setY(topPos + y);
        } else if (box.isFocused()) {
            box.setFocused(false);
        }
    }

    /** Shows the name of the side, or the default name while none is set; not while the player types. */
    private void syncName() {
        if (nameBox.isFocused()) {
            return;
        }
        String shown = menu.name().isEmpty() ? defaultName() : menu.name();
        if (!nameBox.getValue().equals(shown)) {
            updatingBoxes = true;
            nameBox.setValue(shown);
            nameBox.moveCursorToStart();
            updatingBoxes = false;
        }
    }

    private static Component typeName(TransportType type) {
        String id = type.id();
        return Component.translatable("type.vectrum." + id.substring(id.indexOf(':') + 1));
    }

    private void sideRow(int y) {
        place(EndpointMenu.BUTTON_SIDE_PREVIOUS, LEFT, y, 16, Component.literal("<"), menu.canSwitchSide());
        place(EndpointMenu.BUTTON_SIDE_NEXT, LEFT + LEFT_WIDTH - 16, y, 16, Component.literal(">"),
                menu.canSwitchSide());
        centered(Component.translatable("gui.vectrum.side",
                Component.translatable("direction.vectrum." + menu.side().getName())),
                LEFT + LEFT_WIDTH / 2, y + 3, LEFT_WIDTH - 40, TEXT);
    }

    /** Role: three buttons side by side for a cable, the active one highlighted; one cycling button otherwise. */
    private void roleRow(int y) {
        EndpointKind kind = menu.kind();
        if (kind == EndpointKind.REDSTONE) {
            Component name = Component.translatable("gui.vectrum.role." + menu.role().shortName());
            place(EndpointMenu.BUTTON_ROLE, LEFT, y, LEFT_WIDTH, Component.translatable("gui.vectrum.role", name), true);
            return;
        }
        // Energy cable: push & pull, pull, off; other cables: pull, push, off
        EndpointMode[] order = kind == EndpointKind.ENERGY_CABLE
                ? new EndpointMode[]{EndpointMode.TARGET, EndpointMode.SOURCE, EndpointMode.OFF}
                : new EndpointMode[]{EndpointMode.SOURCE, EndpointMode.TARGET, EndpointMode.OFF};
        int width = (LEFT_WIDTH - 4) / 3;
        for (int i = 0; i < 3; i++) {
            boolean both = kind == EndpointKind.ENERGY_CABLE && order[i] == EndpointMode.TARGET;
            Component label = Component.translatable("gui.vectrum.role." + (both ? "both" : order[i].shortName()));
            boolean selected = menu.role() == order[i];
            place(EndpointMenu.BUTTON_ROLE_SET + i, LEFT + i * (width + 2), y, width,
                    selected ? label.copy().withStyle(ChatFormatting.YELLOW, ChatFormatting.UNDERLINE) : label, !selected);
        }
    }

    /** Row with -10, -1, a centered label, +1 and +10. */
    private void stepRow(int y, int[] ids, Component label) {
        place(ids[0], LEFT, y, 24, Component.literal("-10"), true);
        place(ids[1], LEFT + 26, y, 20, Component.literal("-1"), true);
        place(ids[2], LEFT + LEFT_WIDTH - 46, y, 20, Component.literal("+1"), true);
        place(ids[3], LEFT + LEFT_WIDTH - 24, y, 24, Component.literal("+10"), true);
        centered(label, LEFT + LEFT_WIDTH / 2, y + 3, LEFT_WIDTH - 100, TEXT);
    }

    private void frequencyRow(int y) {
        stepRow(y, new int[]{EndpointMenu.BUTTON_FREQUENCY + 2, EndpointMenu.BUTTON_FREQUENCY,
                EndpointMenu.BUTTON_FREQUENCY + 1, EndpointMenu.BUTTON_FREQUENCY + 3},
                Component.translatable("gui.vectrum.frequency", menu.frequency()));
    }

    /** Label and input field of the priority; a hint instead while the priority upgrade is missing. */
    private void priorityRow(int y) {
        if (!menu.priorityUnlocked()) {
            text(Component.translatable("gui.vectrum.priority_locked"), LEFT, y + 2, LEFT_WIDTH, MUTED);
            return;
        }
        text(Component.translatable("gui.vectrum.priority_label"), LEFT, y + 2, 56, TEXT);
        if (!priorityBox.isFocused()) {
            String shown = Integer.toString(menu.priority());
            if (!priorityBox.getValue().equals(shown)) {
                updatingBoxes = true;
                priorityBox.setValue(shown);
                updatingBoxes = false;
            }
        }
        showBox(priorityBox, true, LEFT + 62, y);
    }

    private void filterRow(int y) {
        if (!menu.filterUnlocked()) {
            return;
        }
        text(Component.translatable("gui.vectrum.filter"), LEFT, y + 3, 38, TEXT);
        Component type = Component.translatable(menu.blacklist() ? "gui.vectrum.blacklist" : "gui.vectrum.whitelist");
        if (menu.filterSize() > EndpointMenu.GHOSTS) {
            type = Component.translatable("gui.vectrum.filter_count", type, menu.filterSize());
        }
        place(EndpointMenu.BUTTON_FILTER_TYPE, LEFT + 40, y, LEFT_WIDTH - 88, type, true);
        place(EndpointMenu.BUTTON_CLEAR_FILTER, LEFT + LEFT_WIDTH - 44, y, 44, Component.translatable("gui.vectrum.clear"),
                menu.filterSize() > 0);
    }

    private void copyPaste() {
        place(EndpointMenu.BUTTON_COPY, RIGHT, 121, ICON_BUTTON, Component.empty(), true);
        place(EndpointMenu.BUTTON_PASTE, RIGHT + ICON_BUTTON + 4, 121, ICON_BUTTON, Component.empty(),
                menu.hasClipboard());
    }

    private void upgradeLabel() {
        if (!menu.shown(EndpointMenu.Element.UPGRADES)) {
            return;
        }
        for (UpgradeType type : UpgradeType.VALUES) {
            if (menu.hasUpgradeSlot(type)) {
                text(Component.translatable("gui.vectrum.upgrades"), RIGHT, 6, RIGHT_WIDTH, TEXT);
                return;
            }
        }
    }

    /** Number of types the menu shows. */
    private int shownTypes() {
        int count = 0;
        for (TransportType type : EndpointMenu.TYPE_ORDER) {
            if (menu.hasType(type)) {
                count++;
            }
        }
        return count;
    }

    /**
     * One block per type: rate line, diagnosis line and network line, with the details as hover text; with more
     * than two types one line per type, coloured by the diagnosis.
     */
    private void typeInfo() {
        int top = 42;
        int y = top;
        boolean dense = shownTypes() > 2;
        for (TransportType type : EndpointMenu.TYPE_ORDER) {
            if (!menu.hasType(type)) {
                continue;
            }
            List<FlowReason> reasons = menu.reasons(type);
            String shortName = type.id().substring(type.id().indexOf(':') + 1);
            int limit = menu.limit(type);
            String unit = Component.translatable("unit.vectrum." + shortName).getString();
            String current = String.format(Locale.ROOT, "%.1f", menu.rateTenths(type) / 10.0);
            Component rate = limit == Integer.MAX_VALUE || menu.interval() <= 0
                    ? Component.translatable("gui.vectrum.rate", typeName(type), current, unit)
                    : Component.translatable("gui.vectrum.rate_limit", typeName(type), current,
                    String.format(Locale.ROOT, "%.0f", limit * 20.0 / menu.interval()), unit);
            Component diagnosis = Component.empty();
            boolean problem = false;
            for (int i = 0; i < reasons.size(); i++) {
                diagnosis = i == 0 ? Component.translatable("diagnosis.vectrum." + reasons.get(i).id())
                        : Component.empty().append(diagnosis).append(", ")
                        .append(Component.translatable("diagnosis.vectrum." + reasons.get(i).id()));
                problem |= reasons.get(i).isProblem();
            }
            Component network = Component.translatable("gui.vectrum.network", menu.sources(type), menu.targets(type));
            text(rate, RIGHT, y, RIGHT_WIDTH, dense ? (problem ? BAD : TEXT) : TEXT);
            if (!dense) {
                text(diagnosis, RIGHT, y + 10, RIGHT_WIDTH, problem ? BAD : GOOD);
                text(network, RIGHT, y + 20, RIGHT_WIDTH, MUTED);
            }

            List<Component> tooltip = new ArrayList<>();
            tooltip.add(PortDiagnosis.text(new PortDiagnosis.Entry(type, reasons)));
            tooltip.add(limit == Integer.MAX_VALUE
                    ? Component.translatable("gui.vectrum.limit_unlimited")
                    : Component.translatable("gui.vectrum.limit", limit, menu.interval()));
            if (TransportType.ITEM.equals(type) && menu.kind() == EndpointKind.CABLE) {
                tooltip.add(Component.translatable("gui.vectrum.max_types", menu.maxTypes()));
            }
            if (menu.kind() != EndpointKind.WIRELESS) {
                tooltip.add(network);
            }
            hots.add(new Hot(RIGHT, y, RIGHT_WIDTH, dense ? 13 : 31, tooltip));
            y += dense ? 13 : 32;
        }
        if (y == top) {
            wrapped(Component.translatable("gui.vectrum.no_storage"), RIGHT, top, RIGHT_WIDTH, MUTED);
        }
    }

    // Drawing

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int x = leftPos;
        int y = topPos;
        graphics.fill(x, y, x + imageWidth, y + imageHeight, 0xFF000000);
        graphics.fill(x + 1, y + 1, x + imageWidth - 1, y + imageHeight - 1, 0xFFC6C6C6);
        graphics.fill(x + 1, y + 1, x + imageWidth - 1, y + 3, 0xFFFFFFFF);
        graphics.fill(x + 1, y + 1, x + 3, y + imageHeight - 1, 0xFFFFFFFF);
        graphics.fill(x + 1, y + imageHeight - 3, x + imageWidth - 1, y + imageHeight - 1, 0xFF555555);
        graphics.fill(x + imageWidth - 3, y + 1, x + imageWidth - 1, y + imageHeight - 1, 0xFF555555);
        for (Slot slot : menu.slots) {
            if (slot.isActive()) {
                int sx = x + slot.x - 1;
                int sy = y + slot.y - 1;
                graphics.fill(sx, sy, sx + 18, sy + 18, 0xFF373737);
                graphics.fill(sx + 1, sy + 1, sx + 18, sy + 18, 0xFFFFFFFF);
                graphics.fill(sx + 1, sy + 1, sx + 17, sy + 17, 0xFF8B8B8B);
                if (!slot.hasItem() && slot.index >= EndpointMenu.UPGRADE_SLOT_BASE
                        && slot.index < EndpointMenu.INVENTORY_BASE) {
                    // An empty upgrade slot shows a faint picture of what belongs in it
                    UpgradeType type = UpgradeType.VALUES[slot.index - EndpointMenu.UPGRADE_SLOT_BASE];
                    graphics.renderFakeItem(new ItemStack(ModItems.upgrade(type)), x + slot.x, y + slot.y);
                    graphics.fill(x + slot.x, y + slot.y, x + slot.x + 16, y + slot.y + 16, 0xCC8B8B8B);
                }
            }
        }
        for (Text text : texts) {
            graphics.drawString(font, text.sequence(), x + text.x(), y + text.y(), text.color(), false);
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        for (Hot hot : hots) {
            if (inside(mouseX, mouseY, hot.x(), hot.y(), hot.width(), hot.height())) {
                graphics.renderComponentTooltip(font, hot.tooltip(), mouseX, mouseY);
            }
        }
    }

    private boolean inside(int mouseX, int mouseY, int x, int y, int width, int height) {
        return mouseX >= leftPos + x && mouseX < leftPos + x + width
                && mouseY >= topPos + y && mouseY < topPos + y + height;
    }

    @Override
    protected void renderTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        if (hoveredSlot != null && hoveredSlot.isActive() && !hoveredSlot.hasItem() && menu.getCarried().isEmpty()) {
            if (hoveredSlot.index < EndpointMenu.GHOSTS) {
                graphics.renderTooltip(font, Component.translatable("gui.vectrum.tip.filter_slot"), mouseX, mouseY);
                return;
            }
            if (hoveredSlot.index < EndpointMenu.INVENTORY_BASE) {
                UpgradeType type = UpgradeType.VALUES[hoveredSlot.index - EndpointMenu.UPGRADE_SLOT_BASE];
                graphics.renderComponentTooltip(font, List.of(
                        Component.translatable(ModItems.upgrade(type).getDescriptionId()),
                        Component.translatable("gui.vectrum.tip.upgrade_slot", type.maxCount())
                                .withStyle(ChatFormatting.GRAY)), mouseX, mouseY);
                return;
            }
        }
        super.renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        // The name field takes the place of the title on cables
        if (menu.kind() != EndpointKind.CABLE && menu.kind() != EndpointKind.ENERGY_CABLE) {
            graphics.drawString(font, title, titleLabelX, titleLabelY, TEXT, false);
        }
    }
}
