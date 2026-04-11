package jp.aquafactory.apprenticecodex.enchantment;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.item.OffhandMagicCompatibleItem;
import jp.aquafactory.apprenticecodex.item.curios.CuriosSlotConstants;
import jp.aquafactory.apprenticecodex.registry.EnchantmentRegistry;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingExperienceDropEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import top.theillusivec4.curios.api.CuriosApi;

import java.util.List;

@Mod.EventBusSubscriber(modid = ApprenticeCodex.MODID)
public final class WisdomExperienceDropEvent {
    private static final int EXPERIENCE_BONUS_DENOMINATOR = 20;
    private static final int HELD_WISDOM_BONUS_UNITS_PER_LEVEL = 4;
    private static final int ARMOR_WISDOM_BONUS_UNITS_PER_LEVEL = 1;

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

        var bonusUnits = getApplicableHeldWisdomLevel(attackingPlayer) * HELD_WISDOM_BONUS_UNITS_PER_LEVEL
                + (getApplicableArmorWisdomLevel(attackingPlayer) + getApplicableCurioWisdomLevel(attackingPlayer)) * ARMOR_WISDOM_BONUS_UNITS_PER_LEVEL;
        if (bonusUnits <= 0) {
            return;
        }

        var droppedExperience = event.getDroppedExperience();
        if (droppedExperience <= 0) {
            return;
        }

        event.setDroppedExperience(droppedExperience * (EXPERIENCE_BONUS_DENOMINATOR + bonusUnits) / EXPERIENCE_BONUS_DENOMINATOR);
    }

    private static int getApplicableHeldWisdomLevel(Player player) {
        // SpellOnCastEvent だけでは遅延着弾する spell まで追跡できないため、
        // Looting と同様に撃破時の手持ち spell gun を見る緩め判定を採用する。
        return Math.max(
                getHeldWisdomLevel(player.getMainHandItem()),
                getHeldWisdomLevel(player.getOffhandItem())
        );
    }

    private static int getApplicableArmorWisdomLevel(Player player) {
        var wisdom = EnchantmentRegistry.WISDOM.get();
        var totalLevel = 0;
        for (var stack : player.getArmorSlots()) {
            if (!stack.isEmpty()) {
                totalLevel += stack.getEnchantmentLevel(wisdom);
            }
        }
        return totalLevel;
    }

    private static int getApplicableCurioWisdomLevel(Player player) {
        var wisdom = EnchantmentRegistry.WISDOM.get();
        return CuriosApi.getCuriosInventory(player)
                .map(inventory -> inventory.findCurios(stack -> stack.getItem() instanceof OffhandMagicCompatibleItem))
                .orElse(List.of())
                .stream()
                .filter(slotResult -> CuriosSlotConstants.HEAD.equals(slotResult.slotContext().identifier()))
                .mapToInt(slotResult -> slotResult.stack().getEnchantmentLevel(wisdom))
                .sum();
    }

    private static int getHeldWisdomLevel(ItemStack stack) {
        if (stack.isEmpty()
                || !MagicItemEnchantmentTargeting.isSupportedHeldWisdomMagicItem(stack.getItem())) {
            return 0;
        }

        return stack.getEnchantmentLevel(EnchantmentRegistry.WISDOM.get());
    }
}
