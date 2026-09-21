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
    private static final SpellrifleModelRecoil MODEL_RECOIL = new SpellrifleModelRecoil();

    private static long lastFireGameTime = Long.MIN_VALUE;
    private static WeakReference<LocalPlayer> recoilPlayer = new WeakReference<>(null);
    private static WeakReference<ClientLevel> recoilLevel = new WeakReference<>(null);
    private static FullautoRapidcastSpellrifleRecoil recoil = new FullautoRapidcastSpellrifleRecoil();

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
            MODEL_RECOIL.clear();
            recoilPlayer = new WeakReference<>(player);
            recoilLevel = new WeakReference<>(minecraft.level);
        }
        SpellrifleSprintState.onShot();
        lastFireGameTime = resolveGameTime();
        if (FullautoRapidcastSpellrifle.hasRecoveryRune(player.getMainHandItem(), minecraft.level.registryAccess())) {
            recoil = new FullautoRapidcastSpellrifleRecoil();
            beginModelRecoil(1.0F);
            return;
        }
        var pitch = recoil.fire(lastFireGameTime, FullautoRapidcastSpellrifleClientAdsState.shouldHandleAsAds(player));
        beginModelRecoil(0.8F + pitch * 0.2F);
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

        return MODEL_RECOIL.amount((double) gameTime + partialTick);
    }

    private static void beginModelRecoil(float strength) {
        var minecraft = Minecraft.getInstance();
        boolean ads = FullautoRapidcastSpellrifleClientAdsState.shouldHandleAsAds(minecraft.player);
        // 描画の補間時刻で衝撃を開始し、受信したtick内の時刻による立ち上がりの飛びを避ける。
        double time = (double) resolveGameTime() + minecraft.getTimer().getGameTimeDeltaPartialTick(true);
        MODEL_RECOIL.fire(time, strength * (ads ? 0.55F : 1.0F));
    }

    private static long resolveGameTime() {
        var level = Minecraft.getInstance().level;
        return level == null ? -1L : level.getGameTime();
    }
}
