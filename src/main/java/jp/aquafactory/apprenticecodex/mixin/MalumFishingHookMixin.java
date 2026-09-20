package jp.aquafactory.apprenticecodex.mixin;

import com.sammy.malum.common.effect.rite.aura.soulwood.GoodTidesEffect;
import com.sammy.malum.registry.common.MalumMobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// Malumの既定priority=1000のRETURN処理より後に修復する。適用順は実体のGameTestでも検証する。
@Mixin(value = FishingHook.class, priority = 1100)
public abstract class MalumFishingHookMixin {
    @Shadow
    @Final
    @Mutable
    private int lureSpeed;

    @Inject(method = "<init>(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/level/Level;II)V",
            at = @At("RETURN"), require = 1)
    private void apprenticecodex$repairGoodTides(Player player, Level level, int luck,
                                                int originalLureSpeed, CallbackInfo ci) {
        // 1.8.2は効果なしでも0で上書きするため、コンストラクタ引数から本来の値を復元する。
        int originalTicks = Math.max(0, originalLureSpeed);
        if (!player.hasEffect(MalumMobEffects.GOOD_TIDES)) {
            lureSpeed = originalTicks;
            return;
        }

        // 1.21.1のフィールドはレベルではなくtick。1レベル=5秒=100tickへ換算する。
        long bonusTicks = GoodTidesEffect.increaseFishingStats(player).getSecond() * 100L;
        // 600tick以上だと待ち時間が常に非正になり進行しないため、効果併用時はV相当までにする。
        lureSpeed = (int) Math.min(500L, originalTicks + bonusTicks);
    }
}
