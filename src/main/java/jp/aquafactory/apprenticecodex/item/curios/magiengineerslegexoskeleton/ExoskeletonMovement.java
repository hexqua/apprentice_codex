package jp.aquafactory.apprenticecodex.item.curios.magiengineerslegexoskeleton;

import jp.aquafactory.apprenticecodex.compat.magitech.ExoskeletonMagitechBridge;
import jp.aquafactory.apprenticecodex.config.ApprenticeCodexServerConfig;
import jp.aquafactory.apprenticecodex.item.curios.manathruster.ManaThrusterContext;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import top.theillusivec4.curios.api.CuriosApi;

public final class ExoskeletonMovement {
    private ExoskeletonMovement() {}

    public static boolean equipped(Player player) {
        return CuriosApi.getCuriosInventory(player).map(inventory -> inventory.isEquipped(
                stack -> stack.is(ItemRegistry.MAGI_ENGINEERS_LEG_EXOSKELETON.get()))).orElse(false);
    }

    public static boolean disabled(Player player) {
        return ManaThrusterContext.isDisabled(player) || player.isInWaterOrBubble() || player.isSwimming();
    }

    public static void apply(Player player, ExoskeletonAction action) {
        ExoskeletonMagitechBridge.apply(player, action, new ItemStack(ItemRegistry.MAGI_ENGINEERS_LEG_EXOSKELETON.get()));
    }

    public static void applyStriderCorrection(Player player, ExoskeletonAction action) {
        player.fallDistance = -5;
        if (action == ExoskeletonAction.AIR_STRIDER) {
            Vec3 motion = player.getDeltaMovement();
            // 上昇・落下の勢いは打ち消し、水平視線では高度維持を狙う。視線による上下の操作性は残す。
            player.setDeltaMovement(motion.x, 0.4 + player.getLookAngle().y * 0.2, motion.z);
        }
    }

    public static boolean accept(ServerPlayer player, ExoskeletonAction action) {
        if (!MagiEngineersLegExoskeleton.available() || disabled(player) || !equipped(player)) return false;
        if (action.isStrider() && player.getCooldowns().isOnCooldown(ItemRegistry.MAGI_ENGINEERS_LEG_EXOSKELETON.get())) return false;
        int cost = action.manaCost(ApprenticeCodexServerConfig.exoskeletonLifterManaCost(),
                ApprenticeCodexServerConfig.exoskeletonStriderManaCost());
        var magic = MagicData.getPlayerMagicData(player);
        if (magic.getMana() < cost) return false;
        // 接地・ダッシュは操作したclientの判断を採用し、本人の移動を応答待ちにしない。
        apply(player, action);
        magic.setMana(Math.max(0, magic.getMana() - cost));
        return true;
    }
}
