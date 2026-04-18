package jp.aquafactory.apprenticecodex.event;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.recipe.crafting.AlchemistsFlaskTippedArrowRecipe;
import jp.aquafactory.apprenticecodex.utility.AdvancementTools;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

@EventBusSubscriber(modid = ApprenticeCodex.MODID)
public final class AlchemistsFlaskAdvancementEvent {
    private AlchemistsFlaskAdvancementEvent() {
    }

    @SubscribeEvent
    public static void onItemCrafted(PlayerEvent.ItemCraftedEvent event) {
        if (event.getEntity().level().isClientSide()) {
            return;
        }

        if (!(event.getEntity() instanceof ServerPlayer serverPlayer)) {
            return;
        }

        if (!shouldAward(event.getCrafting(), event.getInventory())) {
            return;
        }

        AdvancementTools.award(
                serverPlayer,
                AdvancementTools.CRAFT_TIPPED_ARROW_BY_FLASK,
                AdvancementTools.CRAFT_TIPPED_ARROW_BY_FLASK_CRITERION
        );
    }

    public static boolean shouldAward(ItemStack craftedStack, Container container) {
        return craftedStack.is(Items.TIPPED_ARROW) && AlchemistsFlaskTippedArrowRecipe.matchesContainer(container);
    }

    public static boolean shouldAward(ItemStack craftedStack, CraftingInput input) {
        return craftedStack.is(Items.TIPPED_ARROW) && AlchemistsFlaskTippedArrowRecipe.matchesInput(input);
    }
}
