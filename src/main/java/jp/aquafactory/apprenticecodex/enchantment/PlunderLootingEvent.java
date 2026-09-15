package jp.aquafactory.apprenticecodex.enchantment;

import net.minecraft.core.Holder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;

public final class PlunderLootingEvent {
    private PlunderLootingEvent() {
    }

    public static Object resolveLootingAttacker(Object original, Holder<Enchantment> enchantment, LootContext context) {
        if (original != null || !enchantment.is(net.minecraft.world.item.enchantment.Enchantments.LOOTING)) {
            return original;
        }

        // 炎上など攻撃者を直接持たない死亡では、従来どおり撃破帰属と現在の手持ちから略奪を判定する。
        var source = context.getParamOrNull(LootContextParams.DAMAGE_SOURCE);
        var target = context.getParamOrNull(LootContextParams.THIS_ENTITY);
        var attacker = source != null && source.getEntity() instanceof Player player
                ? player
                : target instanceof LivingEntity living && living.getKillCredit() instanceof Player player
                ? player : null;
        return attacker != null && getApplicablePlunderLevel(attacker) > 0 ? attacker : original;
    }

    public static int applyLootingLevel(int original, Holder<Enchantment> enchantment, LootContext context) {
        if (!enchantment.is(net.minecraft.world.item.enchantment.Enchantments.LOOTING)) {
            return original;
        }
        var attacker = resolveLootingAttacker(context.getParamOrNull(LootContextParams.ATTACKING_ENTITY), enchantment, context);
        return attacker instanceof Player player ? Math.max(original, getApplicablePlunderLevel(player)) : original;
    }

    private static int getApplicablePlunderLevel(Player player) {
        return Math.max(
                getActivePlunderLevel(player.getMainHandItem()),
                getActivePlunderLevel(player.getOffhandItem())
        );
    }

    public static int getActivePlunderLevel(ItemStack stack) {
        if (stack.isEmpty() || !(stack.getItem() instanceof PlunderTarget)) {
            return 0;
        }
        return Enchantments.getLevel(stack, Enchantments.PLUNDER);
    }
}
