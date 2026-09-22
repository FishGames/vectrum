package io.github.fishgames.vectrum.client;

import io.github.fishgames.vectrum.block.EndpointMode;
import io.github.fishgames.vectrum.core.diagnosis.FlowReason;
import io.github.fishgames.vectrum.core.transport.TransportType;
import io.github.fishgames.vectrum.core.upgrade.UpgradeType;
import io.github.fishgames.vectrum.gui.EndpointKind;
import io.github.fishgames.vectrum.gui.EndpointMenu;
import io.github.fishgames.vectrum.registry.ModItems;
import io.github.fishgames.vectrum.world.PortDiagnosis;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Endpoint screen: controls on the left, live information on the right.
 * <ul>
 *   <li>Buttons form a fixed pool; every tick the layout decides which ones show and where.</li>
 *   <li>Texts, upgrade icons and hover regions are rebuilt with the layout.</li>
 *   <li>The panel is drawn without a texture.</li>
 * </ul>
 */
public final class EndpointScreen extends AbstractContainerScreen<EndpointMenu> {
    private static final int WIDTH = 320;
    private static final int HEIGHT = 224;
    private static final int BUTTON_HEIGHT = 14;
    private static final int LEFT = 8;
    private static final int LEFT_WIDTH = 182;
    private static final int RIGHT = 196;
    private static final int RIGHT_WIDTH = 116;

    private static final int TEXT = 0xFF404040;
    private static final int MUTED = 0xFF707070;
    private static final int GOOD = 0xFF2E7D32;
    private static final int BAD = 0xFFB03030;

    private record Text(FormattedCharSequence sequence, int x, int y, int color) {
    }

    private record Icon(ItemStack stack, int x, int y, List<Component> tooltip) {
    }

    private record Hot(int x, int y, int width, int height, List<Component> tooltip) {
    }

    private final Button[] buttons = new Button[20];
    private final List<Text> texts = new ArrayList<>();
    private final List<Icon> icons = new ArrayList<>();
    private final List<Hot> hots = new ArrayList<>();

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
            Button button = Button.builder(Component.empty(), pressed -> press(buttonId))
                    .bounds(leftPos, topPos, 20, BUTTON_HEIGHT).build();
            String tip = tooltipKey(id);
            if (tip != null) {
                button.setTooltip(Tooltip.create(Component.translatable(tip)));
            }
            buttons[id] = addRenderableWidget(button);
        }
        refreshLayout();
    }

    private static String tooltipKey(int id) {
        return switch (id) {
            case EndpointMenu.BUTTON_ROLE -> "gui.vectrum.tip.role";
            case EndpointMenu.BUTTON_DISTRIBUTION -> "gui.vectrum.tip.distribution";
            case EndpointMenu.BUTTON_FILTER_TYPE -> "gui.vectrum.tip.filter_type";
            case EndpointMenu.BUTTON_CLEAR_FILTER -> "gui.vectrum.tip.clear";
            case EndpointMenu.BUTTON_SIDE_PREVIOUS, EndpointMenu.BUTTON_SIDE_NEXT -> "gui.vectrum.tip.side";
            case EndpointMenu.BUTTON_COPY -> "gui.vectrum.tip.copy";
            case EndpointMenu.BUTTON_PASTE -> "gui.vectrum.tip.paste";
            case EndpointMenu.BUTTON_LINK, EndpointMenu.BUTTON_LINK + 1, EndpointMenu.BUTTON_LINK + 2,
                 EndpointMenu.BUTTON_LINK + 3 -> "gui.vectrum.tip.link";
            default -> null;
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
        refreshLayout();
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
        if (buttons[0] == null) {
            return;
        }
        for (Button button : buttons) {
            button.visible = false;
        }
        texts.clear();
        icons.clear();
        hots.clear();
        EndpointKind kind = menu.kind();
        switch (kind) {
            case CABLE -> {
                sideRow(17);
                roleRow(35);
                if (menu.role() == EndpointMode.IN) {
                    place(EndpointMenu.BUTTON_DISTRIBUTION, LEFT, 53, LEFT_WIDTH,
                            Component.translatable("gui.vectrum.distribution",
                                    Component.translatable("gui.vectrum.distribution." + menu.distribution().id())),
                            true);
                }
                priorityRow(71);
                filterRow(104);
                upgrades();
                typeInfo();
                copyPaste();
            }
            case REDSTONE -> {
                sideRow(17);
                roleRow(35);
                text(Component.translatable("gui.vectrum.signal", menu.signal()), LEFT, 57, LEFT_WIDTH, TEXT);
                copyPaste();
            }
            case WIRELESS -> {
                frequencyRow(17);
                for (int i = 0; i < EndpointMenu.TYPE_ORDER.size(); i++) {
                    TransportType type = EndpointMenu.TYPE_ORDER.get(i);
                    if (menu.hasType(type)) {
                        place(EndpointMenu.BUTTON_LINK + i, LEFT + (i % 2) * (LEFT_WIDTH / 2 + 1), 35 + (i / 2) * 18,
                                LEFT_WIDTH / 2 - 1, Component.translatable("gui.vectrum.link", typeName(type),
                                        Component.translatable("gui.vectrum.link." + menu.linkMode(type).id())), true);
                    }
                }
                sideRow(71);
                priorityRow(89);
                filterRow(104);
                upgrades();
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

    private void roleRow(int y) {
        Component name = Component.translatable("gui.vectrum.role." + menu.role().name().toLowerCase(Locale.ROOT));
        place(EndpointMenu.BUTTON_ROLE, LEFT, y, LEFT_WIDTH, Component.translatable("gui.vectrum.role", name), true);
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

    private void priorityRow(int y) {
        if (!menu.priorityUnlocked()) {
            return;
        }
        stepRow(y, new int[]{EndpointMenu.BUTTON_PRIORITY_DOWN_10, EndpointMenu.BUTTON_PRIORITY_DOWN,
                EndpointMenu.BUTTON_PRIORITY_UP, EndpointMenu.BUTTON_PRIORITY_UP_10},
                Component.translatable("gui.vectrum.priority", menu.priority()));
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
        place(EndpointMenu.BUTTON_COPY, RIGHT, 121, 56, Component.translatable("gui.vectrum.copy"), true);
        place(EndpointMenu.BUTTON_PASTE, RIGHT + 60, 121, 56, Component.translatable("gui.vectrum.paste"),
                menu.hasClipboard());
    }

    private void upgrades() {
        int x = RIGHT;
        for (UpgradeType type : UpgradeType.VALUES) {
            int count = menu.upgradeCount(type);
            if (count <= 0) {
                continue;
            }
            ItemStack stack = new ItemStack(ModItems.upgrade(type), count);
            List<Component> tooltip = new ArrayList<>();
            tooltip.add(stack.getHoverName());
            tooltip.add(Component.translatable("gui.vectrum.upgrade_count", count, type.maxCount())
                    .withStyle(style -> style.withColor(0xAAAAAA)));
            icons.add(new Icon(stack, x, 16, tooltip));
            x += 19;
        }
        if (icons.isEmpty()) {
            text(Component.translatable("gui.vectrum.no_upgrades"), RIGHT, 20, RIGHT_WIDTH, MUTED);
        }
    }

    /** One block per type: rate line and diagnosis line, with the details as hover text. */
    private void typeInfo() {
        int y = 38;
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
            text(rate, RIGHT, y, RIGHT_WIDTH, TEXT);
            Component diagnosis = Component.empty();
            boolean problem = false;
            for (int i = 0; i < reasons.size(); i++) {
                diagnosis = i == 0 ? Component.translatable("diagnosis.vectrum." + reasons.get(i).id())
                        : Component.empty().append(diagnosis).append(", ")
                        .append(Component.translatable("diagnosis.vectrum." + reasons.get(i).id()));
                problem |= reasons.get(i).isProblem();
            }
            text(diagnosis, RIGHT, y + 10, RIGHT_WIDTH, problem ? BAD : GOOD);

            List<Component> tooltip = new ArrayList<>();
            tooltip.add(PortDiagnosis.text(new PortDiagnosis.Entry(type, reasons)));
            tooltip.add(limit == Integer.MAX_VALUE
                    ? Component.translatable("gui.vectrum.limit_unlimited")
                    : Component.translatable("gui.vectrum.limit", limit, menu.interval()));
            if (TransportType.ITEM.equals(type) && menu.kind() == EndpointKind.CABLE) {
                tooltip.add(Component.translatable("gui.vectrum.max_types", menu.maxTypes()));
            }
            hots.add(new Hot(RIGHT, y, RIGHT_WIDTH, 20, tooltip));
            y += 20;
        }
        if (y == 38) {
            wrapped(Component.translatable("gui.vectrum.no_storage"), RIGHT, 38, RIGHT_WIDTH, MUTED);
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
            }
        }
        for (Text text : texts) {
            graphics.drawString(font, text.sequence(), x + text.x(), y + text.y(), text.color(), false);
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        for (Icon icon : icons) {
            graphics.renderItem(icon.stack(), leftPos + icon.x(), topPos + icon.y());
            graphics.renderItemDecorations(font, icon.stack(), leftPos + icon.x(), topPos + icon.y());
        }
        for (Icon icon : icons) {
            if (inside(mouseX, mouseY, icon.x(), icon.y(), 16, 16)) {
                graphics.renderComponentTooltip(font, icon.tooltip(), mouseX, mouseY);
            }
        }
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
        if (hoveredSlot != null && hoveredSlot.index < EndpointMenu.GHOSTS && hoveredSlot.isActive()
                && !hoveredSlot.hasItem() && menu.getCarried().isEmpty()) {
            graphics.renderTooltip(font, Component.translatable("gui.vectrum.tip.filter_slot"), mouseX, mouseY);
            return;
        }
        super.renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, titleLabelX, titleLabelY, TEXT, false);
    }
}
