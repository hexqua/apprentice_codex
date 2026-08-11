package jp.aquafactory.apprenticecodex.compat.malum;

import com.sammy.malum.common.item.curiosities.weapons.staff.AbstractStaffItem;
import com.sammy.malum.common.item.curiosities.weapons.staff.HexStaffItem;
import com.sammy.malum.registry.client.ParticleRegistry;
import com.sammy.malum.registry.common.SoundRegistry;
import com.sammy.malum.registry.common.item.ItemRegistry;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

final class MalumMnemonicBladeBridgeImpl {
    private MalumMnemonicBladeBridgeImpl() {
    }

    static void fire(ServerPlayer player, ItemStack stack, int projectileCount) {
        var hexStaff = (HexStaffItem) ItemRegistry.MNEMONIC_HEX_STAFF.get();
        for (var index = 0; index < projectileCount; ++index) {
            // 本家の発射単位を直接使い、弾速・散開・3tick刻みのバーストを同一に保つ。
            // 1.21.1ではHexBoltEntity#setDataがOverkeen Eyeを読み取るため、Geas追尾は追加処理なしで有効になる。
            // このGeas接続だけは1.20.1に存在せずbackport対象外。発射自体はtarget側Malum APIで再実装する。
            hexStaff.fireProjectile(player, stack, player.serverLevel(), InteractionHand.MAIN_HAND, 1.0F, index);
        }
    }

    static void playMeleeHitEffect(ServerPlayer attacker, LivingEntity target) {
        var level = attacker.serverLevel();
        level.playSound(
                null,
                target.blockPosition(),
                SoundRegistry.STAFF_STRIKES.get(),
                attacker.getSoundSource(),
                0.75F,
                Mth.nextFloat(level.random, 0.5F, 1.0F)
        );
        // 1.20.1版Malum自身のstaff命中演出と同じ共通ヘルパーを使う。
        AbstractStaffItem.spawnSweepParticles(attacker, ParticleRegistry.STAFF_SLAM_PARTICLE.get());
    }
}
