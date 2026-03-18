package jp.aquafactory.apprenticecodex.item;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.ShieldBlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = ApprenticeCodex.MODID)
public final class ImbueShieldBlockCastEvent {
    private ImbueShieldBlockCastEvent() {
    }

    @SubscribeEvent
    public static void onShieldBlock(ShieldBlockEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        if (player.level().isClientSide || event.getBlockedDamage() <= 0.0f || !player.isUsingItem()) {
            return;
        }

        var shieldStack = player.getUseItem();
        if (!(shieldStack.getItem() instanceof AbstractImbueShieldItem imbueShieldItem)) {
            return;
        }

        imbueShieldItem.tryTriggerImbuedSpellOnBlock(player, shieldStack, player.getUsedItemHand());
    }
}
