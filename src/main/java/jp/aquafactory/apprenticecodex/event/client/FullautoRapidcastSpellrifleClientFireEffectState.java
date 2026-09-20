package jp.aquafactory.apprenticecodex.event.client;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.item.fullautorapidcastspellrifle.FullautoRapidcastSpellrifle;
import jp.aquafactory.apprenticecodex.item.fullautorapidcastspellrifle.FullautoRapidcastSpellrifleRecoil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderFrameEvent;

import java.lang.ref.WeakReference;

@EventBusSubscriber(modid = ApprenticeCodex.MODID, value = Dist.CLIENT)
public final class FullautoRapidcastSpellrifleClientFireEffectState {
    private static final float RECOIL_DURATION_TICKS = 8.0F;
    private static final float RECOIL_HOLD_TICKS = 2.0F;

    private static long lastFireGameTime = Long.MIN_VALUE;
    private static WeakReference<LocalPlayer> recoilPlayer = new WeakReference<>(null);
    private static WeakReference<ClientLevel> recoilLevel = new WeakReference<>(null);
    private static FullautoRapidcastSpellrifleRecoil recoil = new FullautoRapidcastSpellrifleRecoil();
    private static float modelRecoilScale = 1.0F;

    private FullautoRapidcastSpellrifleClientFireEffectState() {
    }

    public static void beginRecoil() {
        var minecraft = Minecraft.getInstance();
        var player = minecraft.player;
        if (player == null || minecraft.level == null || minecraft.screen != null || minecraft.isPaused()
                || !player.isAlive() || player.isSpectator()
                || !(player.getMainHandItem().getItem() instanceof FullautoRapidcastSpellrifle)) {
            return;
        }
        if (recoilPlayer.get() != player || recoilLevel.get() != minecraft.level) {
            recoil = new FullautoRapidcastSpellrifleRecoil();
            recoilPlayer = new WeakReference<>(player);
            recoilLevel = new WeakReference<>(minecraft.level);
        }
        lastFireGameTime = resolveGameTime();
        if (FullautoRapidcastSpellrifle.hasRecoveryRune(player.getMainHandItem(), minecraft.level.registryAccess())) {
            recoil = new FullautoRapidcastSpellrifleRecoil();
            modelRecoilScale = 1.0F;
            return;
        }
        var pitch = recoil.fire(lastFireGameTime, FullautoRapidcastSpellrifleClientAdsState.shouldHandleAsAds(player));
        modelRecoilScale = 0.8F + pitch * 0.2F;
        recoil.addImpulse(pitch, System.nanoTime());
    }

    @SubscribeEvent
    public static void onRenderFrame(RenderFrameEvent.Pre event) {
        applyCameraRecoil();
    }

    public static void applyCameraRecoil() {
        var minecraft = Minecraft.getInstance();
        var player = minecraft.player;
        if (player == null || recoilPlayer.get() != player || recoilLevel.get() != minecraft.level
                || minecraft.screen != null || minecraft.isPaused() || !player.isAlive() || player.isSpectator()
                || !(player.getMainHandItem().getItem() instanceof FullautoRapidcastSpellrifle)
                || FullautoRapidcastSpellrifle.hasRecoveryRune(player.getMainHandItem(), player.level().registryAccess())) {
            recoil.clearImpulses();
            return;
        }
        var delta = recoil.advanceImpulses(System.nanoTime());
        var previousPitch = player.getXRot();
        player.setXRot(Mth.clamp(previousPitch - delta, -90.0F, 90.0F));
        // 描画ごとの差分を tick 補間で再度引き戻さない。マウス入力の差分はそのまま残す。
        player.xRotO += player.getXRot() - previousPitch;
    }

    public static float getRecoilAmount(float partialTick) {
        var gameTime = resolveGameTime();
        if (gameTime < 0L || lastFireGameTime == Long.MIN_VALUE
                || recoilPlayer.get() != Minecraft.getInstance().player || recoilLevel.get() != Minecraft.getInstance().level) {
            return 0.0F;
        }

        var age = gameTime + partialTick - lastFireGameTime;
        if (age < 0.0F || age >= RECOIL_DURATION_TICKS) {
            return 0.0F;
        }

        if (age <= RECOIL_HOLD_TICKS) {
            return modelRecoilScale;
        }

        var restoreProgress = (age - RECOIL_HOLD_TICKS) / (RECOIL_DURATION_TICKS - RECOIL_HOLD_TICKS);
        return (1.0F - restoreProgress) * modelRecoilScale;
    }

    private static long resolveGameTime() {
        var level = Minecraft.getInstance().level;
        return level == null ? -1L : level.getGameTime();
    }
}
