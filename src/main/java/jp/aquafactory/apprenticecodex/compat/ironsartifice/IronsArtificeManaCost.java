package jp.aquafactory.apprenticecodex.compat.ironsartifice;

import io.redspace.irons_artifice.gun.ShotProfile;
import io.redspace.irons_artifice.modifier.OnShotEffect;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.network.SyncManaPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.network.PacketDistributor;

public final class IronsArtificeManaCost implements OnShotEffect {
    @Override
    public void onShot(ServerLevel level, LivingEntity shooter, ShotProfile profile) {
        if (!(shooter instanceof Player player) || player.hasInfiniteMaterials()) return;
        int cost = profile.peek(IronsArtificeManaEvents.MANA_COST);
        if (cost <= 0) return;
        var data = MagicData.getPlayerMagicData(player);
        // 発射ゲートでは判定のみ。全キャンセル判定が終わった発射だけを一度課金する。
        data.setMana(Math.max(0, data.getMana() - cost));
        if (player instanceof ServerPlayer serverPlayer && !(player instanceof FakePlayer)) {
            PacketDistributor.sendToPlayer(serverPlayer, new SyncManaPacket(data));
        }
    }
}
