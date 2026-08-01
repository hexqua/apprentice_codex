package jp.aquafactory.apprenticecodex.compat.malum;

import com.sammy.malum.common.item.curiosities.weapons.staff.HexStaffItem;
import com.sammy.malum.registry.common.MalumAttributes;
import com.sammy.malum.registry.common.MalumParticleEffectTypes;
import com.sammy.malum.registry.common.MalumSoundEvents;
import com.sammy.malum.registry.common.item.MalumItems;
import com.sammy.malum.registry.common.magic.MalumSpiritTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import team.lodestar.lodestone.helpers.RandomHelper;
import team.lodestar.lodestone.helpers.SoundHelper;

final class MalumMnemonicBladeBridgeImpl {
    private MalumMnemonicBladeBridgeImpl() {
    }

    static double getChargeRecoveryRate(LivingEntity entity) {
        // CHARGE_RECOVERY_RATEは1.21.1にしかないAttributeではあるがnullチェックでそのまま通すかは1.20.1backport対応時に検討.
        var attribute = entity.getAttribute(MalumAttributes.CHARGE_RECOVERY_RATE);
        return attribute == null ? 1.0D : attribute.getValue();
    }

    static void fire(ServerPlayer player, ItemStack stack, int projectileCount) {
        var hexStaff = (HexStaffItem) MalumItems.MNEMONIC_HEX_STAFF.get();
        for (var index = 0; index < projectileCount; ++index) {
            // 本家の発射単位を直接使い、弾速・散開・3tick刻みのバーストを同一に保つ。
            // 1.21.1ではHexBoltEntity#setDataがOverkeen Eyeを読み取るため、Geas追尾は追加処理なしで有効になる。
            // このGeas接続だけは1.20.1に存在せずbackport対象外。発射自体はtarget側Malum APIで再実装する。
            hexStaff.fireProjectile(player, stack, player.serverLevel(), InteractionHand.MAIN_HAND, index);
        }
    }

    static void playMeleeHitEffect(ServerPlayer attacker, LivingEntity target) {
        var level = attacker.serverLevel();
        SoundHelper.playSound(
                target,
                MalumSoundEvents.STAFF_STRIKES.get(),
                attacker.getSoundSource(),
                2.0F,
                RandomHelper.randomBetween(level.random, 0.85F, 1.25F)
        );
        MalumParticleEffectTypes.STAFF_SLAM.createEffect()
                .originatesFrom(attacker)
                .targets(target)
                .color(MalumSpiritTypes.WICKED_SPIRIT)
                .forwardOffset(1.4F)
                .upwardOffset(0.3F)
                .spawn(level);
    }
}
