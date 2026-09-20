package jp.aquafactory.apprenticecodex.item.curios.manamaneuvergear;

import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.network.SyncManaPacket;
import jp.aquafactory.apprenticecodex.config.ApprenticeCodexServerConfig;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import io.redspace.ironsspellbooks.setup.PacketDistributor;

public final class ManaManeuverGearDamageLogic {
    private static final java.util.Set<java.util.UUID> RESIDUAL_DAMAGE_PLAYERS = new java.util.HashSet<>();
    private static final float EPSILON = 1.0e-4F;

    private ManaManeuverGearDamageLogic() {
    }

    public static void reduceFallDamage(LivingAttackEvent event, ServerPlayer player) {
        if (RESIDUAL_DAMAGE_PLAYERS.contains(player.getUUID())
                || event.isCanceled()
                || event.getAmount() <= 0.0F
                || !event.getSource().is(DamageTypes.FALL)
                || !ManaManeuverGearManager.isEquipped(player)) {
            return;
        }

        var magicData = MagicData.getPlayerMagicData(player);
        if (magicData == null) {
            return;
        }

        var incomingDamage = event.getAmount();
        var manaPerDamage = Math.max(0.0F, ApprenticeCodexServerConfig.manaManeuverGearManaPerDamage());
        if (manaPerDamage <= 0.0F) {
            ManaManeuverGearEffects.playFallDamageReduction(player, incomingDamage);
            event.setCanceled(true);
            return;
        }

        var currentMana = Math.max(0.0F, magicData.getMana());
        if (currentMana <= 0.0F) {
            return;
        }

        var reducedDamage = Math.min(incomingDamage, currentMana / manaPerDamage);
        var spentMana = reducedDamage * manaPerDamage;
        magicData.setMana(Math.max(0.0F, currentMana - spentMana));
        syncMana(player, magicData);
        ManaManeuverGearEffects.playFallDamageReduction(player, reducedDamage);

        var remainingDamage = incomingDamage - reducedDamage;
        if (remainingDamage <= EPSILON) {
            event.setCanceled(true);
        } else {
            // ForgeのLivingAttackEventは量を変更できないため、再入時の二重消費を防いで残ダメージを通常経路へ戻す。
            event.setCanceled(true);
            RESIDUAL_DAMAGE_PLAYERS.add(player.getUUID());
            try {
                player.hurt(event.getSource(), remainingDamage);
            } finally {
                RESIDUAL_DAMAGE_PLAYERS.remove(player.getUUID());
            }
        }
    }

    private static void syncMana(ServerPlayer player, MagicData magicData) {
        if (!(player instanceof FakePlayer)) {
            PacketDistributor.sendToPlayer(player, new SyncManaPacket(magicData));
        }
    }
}
