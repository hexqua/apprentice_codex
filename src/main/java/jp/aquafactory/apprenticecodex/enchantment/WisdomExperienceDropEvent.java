package jp.aquafactory.apprenticecodex.enchantment;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.registry.EnchantmentRegistry;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingExperienceDropEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = ApprenticeCodex.MODID)
public final class WisdomExperienceDropEvent {
    private static final int EXPERIENCE_BONUS_DENOMINATOR = 5;

    private WisdomExperienceDropEvent() {
    }

    @SubscribeEvent
    public static void onLivingExperienceDrop(LivingExperienceDropEvent event) {
        if (!EnchantmentRegistry.WISDOM.isPresent()) {
            return;
        }

        var attackingPlayer = event.getAttackingPlayer();
        if (attackingPlayer == null) {
            return;
        }

        var wisdomLevel = getApplicableWisdomLevel(attackingPlayer);
        if (wisdomLevel <= 0) {
            return;
        }

        var droppedExperience = event.getDroppedExperience();
        if (droppedExperience <= 0) {
            return;
        }

        event.setDroppedExperience(droppedExperience * (EXPERIENCE_BONUS_DENOMINATOR + wisdomLevel) / EXPERIENCE_BONUS_DENOMINATOR);
    }

    private static int getApplicableWisdomLevel(Player player) {
        // SpellOnCastEvent だけでは遅延着弾する spell まで追跡できないため、
        // Looting と同様に撃破時の手持ち spell gun を見る緩め判定を採用する。
        return Math.max(
                getWisdomLevel(player.getMainHandItem()),
                getWisdomLevel(player.getOffhandItem())
        );
    }

    private static int getWisdomLevel(ItemStack stack) {
        if (stack.isEmpty()
                || !MagicItemEnchantmentTargeting.isSupportedWisdomMagicItem(stack.getItem())) {
            return 0;
        }

        return stack.getEnchantmentLevel(EnchantmentRegistry.WISDOM.get());
    }
}
