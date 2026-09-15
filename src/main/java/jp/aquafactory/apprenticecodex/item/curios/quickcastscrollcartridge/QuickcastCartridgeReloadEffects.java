package jp.aquafactory.apprenticecodex.item.curios.quickcastscrollcartridge;

import jp.aquafactory.apprenticecodex.particle.AdditiveGlowParticleOptions;
import jp.aquafactory.apprenticecodex.registry.ParticleRegistry;
import jp.aquafactory.apprenticecodex.registry.SoundRegistry;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;

/** 開始・終了はサーバーの状態遷移から呼び、表示同期の再送では再生しない。 */
final class QuickcastCartridgeReloadEffects {
    private QuickcastCartridgeReloadEffects() {}

    static void start(ServerPlayer player) {
        player.displayClientMessage(Component.translatable(
                "ui.apprenticecodex.quickcast_scroll_cartridge.start_manual_reload").withStyle(ChatFormatting.AQUA), true);
        sound(player, SoundRegistry.VANILLA_COLLECT_MANA.get());
        charging(player);
    }

    static void charging(ServerPlayer player) {
        particles(player, false);
    }

    static void complete(ServerPlayer player) {
        sound(player, SoundRegistry.SPELLCHARGE.get());
        particles(player, true);
    }

    private static void sound(ServerPlayer player, SoundEvent sound) {
        player.serverLevel().playSound(null, player.getX(), player.getY(), player.getZ(),
                sound, SoundSource.PLAYERS, 1.0F, 1.0F);
    }

    private static void particles(ServerPlayer player, boolean complete) {
        var level = player.serverLevel();
        var random = level.random;
        int count = complete ? 6 : 2;
        double phase = random.nextDouble() * Math.PI * 2;
        for (int i = 0; i < count; i++) {
            double angle = phase + Math.PI * 2 * i / count;
            double x = Math.cos(angle);
            double z = Math.sin(angle);
            // ManaThrusterと同じ水色～紫。視界を塞がないよう胸より下へ小さく出す。
            float color = random.nextFloat();
            var options = new AdditiveGlowParticleOptions(
                    complete ? ParticleRegistry.ADDITIVE_RHOMBUS.get() : ParticleRegistry.ADDITIVE_SPARK.get(),
                    complete ? 0.16F : 0.12F + random.nextFloat() * 0.06F,
                    Mth.lerp(color, 0.28F, 0.62F), Mth.lerp(color, 0.78F, 0.36F),
                    Mth.lerp(color, 1.0F, 0.95F), 2,
                    complete ? 10 : 8 + random.nextInt(5), 0,
                    1.0F, 1.0F, 0.7F, 0.9F, 0.02F, 0.58F, 0.45F, true);
            double speed = complete ? 0.04 : -0.03;
            // count=0の単一粒子送信ではoffsetが速度になるため、内向きの流れを保てる。
            level.sendParticles(options, player.getX() + x * 0.65,
                    player.getY() + player.getBbHeight() * (0.45 + random.nextDouble() * 0.2),
                    player.getZ() + z * 0.65, 0, x * speed, 0, z * speed, 1);
        }
    }
}
