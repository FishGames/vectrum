package io.github.fishgames.vectrum.compat.jei;

import io.github.fishgames.vectrum.Vectrum;
import io.github.fishgames.vectrum.client.EndpointScreen;
import io.github.fishgames.vectrum.gui.EndpointMenu;
import io.github.fishgames.vectrum.net.VectrumNet;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.gui.handlers.IGhostIngredientHandler;
import mezz.jei.api.ingredients.IIngredientHelper;
import mezz.jei.api.ingredients.ITypedIngredient;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import mezz.jei.api.runtime.IIngredientManager;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.Slot;

import java.util.ArrayList;
import java.util.List;

/** JEI: ingredients (items, fluids, gases) can be dragged into the filter slots of the endpoint screen. */
@JeiPlugin
public final class VectrumJeiPlugin implements IModPlugin {
    private IIngredientManager ingredients;

    @Override
    public ResourceLocation getPluginUid() {
        return Vectrum.id("jei");
    }

    @Override
    public void onRuntimeAvailable(IJeiRuntime runtime) {
        ingredients = runtime.getIngredientManager();
    }

    @Override
    public void onRuntimeUnavailable() {
        ingredients = null;
    }

    @Override
    public void registerGuiHandlers(IGuiHandlerRegistration registration) {
        registration.addGhostIngredientHandler(EndpointScreen.class, new FilterSlots());
    }

    private final class FilterSlots implements IGhostIngredientHandler<EndpointScreen> {
        @Override
        public <I> List<Target<I>> getTargetsTyped(EndpointScreen screen, ITypedIngredient<I> ingredient,
                                                   boolean doStart) {
            List<Target<I>> targets = new ArrayList<>();
            if (ingredients == null) {
                return targets;
            }
            IIngredientHelper<I> helper = ingredients.getIngredientHelper(ingredient.getType());
            ResourceLocation id = helper.getResourceLocation(ingredient.getIngredient());
            for (Slot slot : screen.getMenu().slots) {
                if (slot.index < EndpointMenu.GHOSTS && slot.isActive()) {
                    Rect2i area = new Rect2i(screen.left() + slot.x, screen.top() + slot.y, 16, 16);
                    targets.add(new Target<>() {
                        @Override
                        public Rect2i getArea() {
                            return area;
                        }

                        @Override
                        public void accept(I dropped) {
                            VectrumNet.sendFilterId(id.toString());
                        }
                    });
                }
            }
            return targets;
        }

        @Override
        public void onComplete() {
        }
    }
}
