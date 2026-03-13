package jp.aquafactory.apprenticecodex.enchantment;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.item.AbstractSpellGunItem;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingExperienceDropEvent;

@EventBusSubscriber(modid = ApprenticeCodex.MODID)
public final class WisdomExperienceDropEvent {
    private static final int EXPERIENCE_BONUS_DENOMINATOR = 5;

    private WisdomExperienceDropEvent() {
    }

    @SubscribeEvent
    public static void onLivingExperienceDrop(LivingExperienceDropEvent event) {
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

        event.setDroppedExperience(
                droppedExperience * (EXPERIENCE_BONUS_DENOMINATOR + wisdomLevel) / EXPERIENCE_BONUS_DENOMINATOR
        );
    }

    private static int getApplicableWisdomLevel(Player player) {
        // 遅延着弾する spell まで厳密追跡するのは重いため、撃破時の手持ち spell gun を見る緩め判定に寄せる.
        return Math.max(
                getWisdomLevel(player.getMainHandItem()),
                getWisdomLevel(player.getOffhandItem())
        );
    }

    private static int getWisdomLevel(ItemStack stack) {
        if (stack.isEmpty() || !(stack.getItem() instanceof AbstractSpellGunItem)) {
            return 0;
        }

        return Enchantments.getLevel(stack, Enchantments.WISDOM);
    }
}
