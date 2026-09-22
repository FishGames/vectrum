package io.github.fishgames.vectrum.compat.emi;

import dev.emi.emi.api.EmiDragDropHandler;
import dev.emi.emi.api.EmiEntrypoint;
import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import io.github.fishgames.vectrum.client.EndpointScreen;
import io.github.fishgames.vectrum.gui.EndpointMenu;
import io.github.fishgames.vectrum.net.VectrumNet;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.inventory.Slot;

/** EMI: stacks (items, fluids, gases) can be dragged into the filter slots of the endpoint screen. */
@EmiEntrypoint
public final class VectrumEmiPlugin implements EmiPlugin {
    private static final int HIGHLIGHT = 0x8822BB33;

    @Override
    public void register(EmiRegistry registry) {
        registry.addDragDropHandler(EndpointScreen.class, new FilterSlots());
    }

    private static final class FilterSlots implements EmiDragDropHandler<EndpointScreen> {
        @Override
        public boolean dropStack(EndpointScreen screen, EmiIngredient dragged, int mouseX, int mouseY) {
            if (dragged.getEmiStacks().isEmpty() || !overSlot(screen, mouseX, mouseY)) {
                return false;
            }
            EmiStack stack = dragged.getEmiStacks().get(0);
            if (stack.isEmpty()) {
                return false;
            }
            VectrumNet.sendFilterId(stack.getId().toString());
            return true;
        }

        @Override
        public void render(EndpointScreen screen, EmiIngredient dragged, GuiGraphics graphics, int mouseX, int mouseY,
                           float delta) {
            for (Slot slot : screen.getMenu().slots) {
                if (slot.index < EndpointMenu.GHOSTS && slot.isActive()) {
                    int x = screen.left() + slot.x;
                    int y = screen.top() + slot.y;
                    graphics.fill(x, y, x + 16, y + 16, HIGHLIGHT);
                }
            }
        }

        private static boolean overSlot(EndpointScreen screen, int mouseX, int mouseY) {
            for (Slot slot : screen.getMenu().slots) {
                int x = screen.left() + slot.x;
                int y = screen.top() + slot.y;
                if (slot.index < EndpointMenu.GHOSTS && slot.isActive()
                        && mouseX >= x && mouseX < x + 16 && mouseY >= y && mouseY < y + 16) {
                    return true;
                }
            }
            return false;
        }
    }
}
