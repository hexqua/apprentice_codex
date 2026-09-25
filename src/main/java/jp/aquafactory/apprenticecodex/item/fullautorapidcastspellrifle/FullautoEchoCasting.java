package jp.aquafactory.apprenticecodex.item.fullautorapidcastspellrifle;

import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import jp.aquafactory.apprenticecodex.config.ApprenticeCodexServerConfig;
import jp.aquafactory.apprenticecodex.item.CalibrationAdjustmentStorage;
import jp.aquafactory.apprenticecodex.item.multicastechostaff.MulticastEchoStaffAttackHandler;
import jp.aquafactory.apprenticecodex.item.multicastechostaff.MulticastEchoStaffAttackHandler.AttackOrigin;
import jp.aquafactory.apprenticecodex.item.multicastechostaff.MulticastEchoStaffAttackHandler.CastScope;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import net.minecraft.core.HolderLookup;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.server.ServerLifecycleHooks;

public final class FullautoEchoCasting {
    private FullautoEchoCasting() {}

    // 同一JVMの統合サーバーでも、clientの表示は同期済みの値を使う。
    public static boolean enabledForCurrentThread() {
        var server = ServerLifecycleHooks.getCurrentServer();
        return server != null && server.isSameThread()
                ? ApprenticeCodexServerConfig.fullautoRapidcastSpellrifleEchoCastEnabled()
                : FullautoEchoConfigState.enabled();
    }

    public static double manaMultiplierForCurrentThread() {
        var server = ServerLifecycleHooks.getCurrentServer();
        return server != null && server.isSameThread()
                ? ApprenticeCodexServerConfig.fullautoRapidcastSpellrifleEchoCastManaCostMultiplier()
                : FullautoEchoConfigState.manaMultiplier();
    }

    public static boolean hasStaff(ItemStack stack, HolderLookup.Provider lookup) {
        if (!(stack.getItem() instanceof FullautoRapidcastSpellrifle)) return false;
        for (int slot = 0; slot < FullautoRapidcastSpellrifle.CALIBRATION_ADJUSTMENT_SLOT_COUNT; slot++) {
            if (CalibrationAdjustmentStorage.get(stack, slot,
                    FullautoRapidcastSpellrifle.CALIBRATION_ADJUSTMENT_SLOT_COUNT)
                    .is(ItemRegistry.MULTICAST_ECHO_STAFF.get())) return true;
        }
        return false;
    }

    public static double manaMultiplier(Player player, ItemStack stack, AbstractSpell spell) {
        return player instanceof ServerPlayer
                && FullautoRapidcastSpellrifleCastContext.isActiveFor(player.getUUID(), stack, spell)
                && ApprenticeCodexServerConfig.fullautoRapidcastSpellrifleEchoCastEnabled()
                && hasStaff(stack, player.level().registryAccess())
                ? ApprenticeCodexServerConfig.fullautoRapidcastSpellrifleEchoCastManaCostMultiplier() : 1.0D;
    }

    public static int scaleMana(int mana, double multiplier) {
        return (int) Math.min(Integer.MAX_VALUE, Math.ceil(Math.max(0, mana) * multiplier));
    }

    public static CastScope openAttackScope(ServerPlayer player, ItemStack stack, AbstractSpell spell) {
        if (!ApprenticeCodexServerConfig.fullautoRapidcastSpellrifleEchoCastEnabled()
                || !FullautoRapidcastSpellrifleCastContext.isActiveFor(player.getUUID(), stack, spell)
                || !hasStaff(stack, player.level().registryAccess())) return () -> {};
        return MulticastEchoStaffAttackHandler.openCast(player, spell, AttackOrigin.RIFLE);
    }

    public static CastScope openAttackScope(ServerPlayer player, AbstractSpell spell) {
        return openAttackScope(player, MagicData.getPlayerMagicData(player).getPlayerCastingItem(), spell);
    }
}
