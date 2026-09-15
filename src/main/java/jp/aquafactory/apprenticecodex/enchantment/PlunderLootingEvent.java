package jp.aquafactory.apprenticecodex.enchantment;

import net.minecraft.core.Holder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import org.jetbrains.annotations.Nullable;

public final class PlunderLootingEvent {
    private PlunderLootingEvent() {
    }

    public static Object resolveLootingAttacker(Object original, Holder<Enchantment> enchantment, LootContext context) {
        if (original instanceof LivingEntity || !enchantment.is(net.minecraft.world.item.enchantment.Enchantments.LOOTING)) {
            return original;
        }

        // 生物の攻撃者は通常Lootingの計算用に保持し、炎上や所有者不在の召喚武器の場合だけ補完する。
        var attacker = resolvePlunderPlayer(context);
        return attacker != null && getApplicablePlunderLevel(attacker) > 0 ? attacker : original;
    }

    private static @Nullable Player resolvePlunderPlayer(LootContext context) {
        var source = context.getParamOrNull(LootContextParams.DAMAGE_SOURCE);
        if (source != null && source.getEntity() instanceof Player player) {
            return player;
        }
        // 召喚Mobや矢の所有者が非Playerでも、被害者のプレイヤー撃破帰属を略奪の判定に使う。
        // 所有者を独自に推測せず、従来のDamageSource -> kill creditの優先順位を維持する。
        var target = context.getParamOrNull(LootContextParams.THIS_ENTITY);
        return target instanceof LivingEntity living && living.getKillCredit() instanceof Player player
                ? player : null;
    }

    public static int applyLootingLevel(int original, Holder<Enchantment> enchantment, LootContext context) {
        if (!enchantment.is(net.minecraft.world.item.enchantment.Enchantments.LOOTING)) {
            return original;
        }
        var attacker = resolvePlunderPlayer(context);
        return attacker != null ? Math.max(original, getApplicablePlunderLevel(attacker)) : original;
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
