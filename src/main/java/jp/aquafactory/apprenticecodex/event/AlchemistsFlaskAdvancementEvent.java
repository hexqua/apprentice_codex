package jp.aquafactory.apprenticecodex.event;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.recipe.crafting.AlchemistsFlaskTippedArrowRecipe;
import jp.aquafactory.apprenticecodex.utility.AdvancementTools;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = ApprenticeCodex.MODID)
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
}
