package jp.aquafactory.apprenticecodex.spell.precisionjack;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.damage.DamageTypes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = ApprenticeCodex.MODID)
public final class PrecisionJackLootingEvent {
    private PrecisionJackLootingEvent() {
    }

    @SubscribeEvent
    public static void onLivingDrops(LivingDropsEvent event) {
        var source = event.getSource();
        if (!source.is(DamageTypes.PRECISION_JACK)) {
            return;
        }

        if (event.getEntity() instanceof Player) {
            return;
        }

        if (!(source.getDirectEntity() instanceof PrecisionJackKnifeEntity knife)) {
            return;
        }

        var lootingBonus = knife.getLootingBonus();
        if (lootingBonus <= 0) {
            return;
        }

        var random = event.getEntity().getRandom();
        for (var drop : event.getDrops()) {
            ItemStack itemStack = drop.getItem();
            if (itemStack.isEmpty()) {
                continue;
            }

            var extra = random.nextInt(lootingBonus + 1);
            if (extra > 0) {
                itemStack.grow(extra);
            }
        }
    }
}

