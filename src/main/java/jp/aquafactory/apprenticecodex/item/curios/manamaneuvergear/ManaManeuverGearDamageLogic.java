package jp.aquafactory.apprenticecodex.item.curios.manamaneuvergear;

import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.network.SyncManaPacket;
import jp.aquafactory.apprenticecodex.config.ApprenticeCodexServerConfig;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageTypes;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.network.PacketDistributor;

public final class ManaManeuverGearDamageLogic {
    private static final float EPSILON = 1.0e-4F;

    private ManaManeuverGearDamageLogic() {
    }

    public static void reduceFallDamage(LivingIncomingDamageEvent event, ServerPlayer player) {
        if (event.isCanceled()
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

        var remainingDamage = incomingDamage - reducedDamage;
        if (remainingDamage <= EPSILON) {
            event.setCanceled(true);
        } else {
            event.setAmount(remainingDamage);
        }
    }

    private static void syncMana(ServerPlayer player, MagicData magicData) {
        if (!(player instanceof FakePlayer)) {
            PacketDistributor.sendToPlayer(player, new SyncManaPacket(magicData));
        }
    }
}
