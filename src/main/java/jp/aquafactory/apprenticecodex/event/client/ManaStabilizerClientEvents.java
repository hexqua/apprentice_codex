package jp.aquafactory.apprenticecodex.event.client;

import io.redspace.ironsspellbooks.player.ClientMagicData;
import jp.aquafactory.apprenticecodex.compat.ironsartifice.ManaStabilizerClientBridge;
import jp.aquafactory.apprenticecodex.compat.ironsartifice.ManaStabilizerModuleEvents;
import jp.aquafactory.apprenticecodex.item.modifier.manastabilizermodule.ManaStabilizerModuleConfigState;
import jp.aquafactory.apprenticecodex.item.modifier.manastabilizermodule.ManaStabilizerRecovery;
import jp.aquafactory.apprenticecodex.item.multipurposestaffrifle.MultipurposeStaffrifle;
import jp.aquafactory.apprenticecodex.network.Networks;
import jp.aquafactory.apprenticecodex.network.packet.ClientManaStabilizerInputPacket;
import jp.aquafactory.apprenticecodex.network.packet.SyncManaStabilizerRecoilPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec2;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ComputeFovModifierEvent;
import net.neoforged.neoforge.client.event.InputEvent;

public final class ManaStabilizerClientEvents {
    private static LocalPlayer lastPlayer;
    private static boolean sentAiming;
    private static long hookTick = Long.MIN_VALUE, shot, lastRevision;
    private static SyncManaStabilizerRecoilPacket correction;
    private static long crosshairAssistUntil = Long.MIN_VALUE;
    private ManaStabilizerClientEvents() {}

    public static void register() {
        ManaStabilizerClientBridge.aiming = ManaStabilizerClientEvents::aiming;
    }

    public static boolean aiming(LivingEntity entity) {
        var minecraft = Minecraft.getInstance();
        return entity == minecraft.player && minecraft.screen == null && !minecraft.isPaused()
                && minecraft.options.keyUse.isDown() && ManaStabilizerModuleEvents.eligible(entity);
    }

    private static boolean hookActive() {
        var player = Minecraft.getInstance().player;
        return player != null && player.level().getGameTime() - hookTick >= 0
                && player.level().getGameTime() - hookTick <= 2;
    }

    public static void fired() {
        var player = Minecraft.getInstance().player;
        if (player == null || ManaStabilizerModuleEvents.count(player.getMainHandItem()) == 0) return;
        shot++;
        // 上流の発射packetの直後に送るため、拒否された射撃も確認番号を進めて補正できる。
        Networks.sendToServer(new ClientManaStabilizerInputPacket(aiming(player), hookActive(), shot));
    }

    public static void accept(SyncManaStabilizerRecoilPacket packet) {
        if (packet.revision() <= lastRevision) return;
        lastRevision = packet.revision();
        if (packet.shot() == shot && sentAiming) correction = packet;
    }

    public static Vec2 recover(float pitch, float yaw) {
        var player = Minecraft.getInstance().player;
        if (player == null) return new Vec2(pitch, yaw);
        long now = player.level().getGameTime();
        hookTick = now;
        boolean aiming = aiming(player);
        if (!aiming) {
            correction = null;
            crosshairAssistUntil = Long.MIN_VALUE;
            return new Vec2(pitch, yaw);
        }
        var values = ManaStabilizerModuleConfigState.values();
        int count = ManaStabilizerModuleEvents.count(player.getMainHandItem());
        double step = values.recoveryDegreesPerTick() * count;
        if (correction != null) {
            var packet = correction;
            correction = null;
            // 古いサーバー状態で、未確認の新しい発射反動を消さない。
            if (packet.shot() == shot) {
                int elapsed = (int) Math.min(20, Math.max(0, now - packet.tick()));
                Vec2 replay = new Vec2(packet.pitch(), packet.yaw());
                for (int tick = 0; tick < elapsed; tick++) {
                    replay = replay.scale(0.75f);
                    if (aiming) replay = ManaStabilizerRecovery.recover(replay.x, replay.y, step, packet.paid());
                }
                return trackRecovery(pitch, yaw, replay, packet.paid() && step > 0);
            }
        }
        double cost = values.manaCostPerTick() * count;
        boolean paid = player.hasInfiniteMaterials() || cost == 0 || ClientMagicData.getPlayerMana() >= cost;
        boolean assisted = step > 0 && (paid || !ManaStabilizerRecovery.needsPayment(pitch, yaw, step));
        return trackRecovery(pitch, yaw, ManaStabilizerRecovery.recover(pitch, yaw, step, paid), assisted);
    }

    private static Vec2 trackRecovery(float pitch, float yaw, Vec2 result, boolean assisted) {
        var player = Minecraft.getInstance().player;
        if (assisted && player != null && result.lengthSquared() < pitch * pitch + yaw * yaw - 1.0e-6f) {
            // 残留反動がゼロになった後も、遅れている表示が追いつくための短い猶予を残す。
            crosshairAssistUntil = player.level().getGameTime() + 4;
        }
        return result;
    }

    public static boolean accelerateCrosshairClosing() {
        var player = Minecraft.getInstance().player;
        return player != null && aiming(player) && hookActive()
                && player.level().getGameTime() <= crosshairAssistUntil;
    }

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        var player = Minecraft.getInstance().player;
        if (player != lastPlayer) { reset(); lastPlayer = player; }
        if (player == null) return;
        boolean aiming = aiming(player);
        if (aiming || sentAiming) Networks.sendToServer(new ClientManaStabilizerInputPacket(aiming, hookActive(), shot));
        sentAiming = aiming;
        if (aiming) player.setSprinting(false);
    }

    @SubscribeEvent
    public static void input(InputEvent.InteractionKeyMappingTriggered event) {
        var player = Minecraft.getInstance().player;
        if (event.isUseItem() && player != null && aiming(player)) {
            event.setCanceled(true);
            event.setSwingHand(false);
        }
    }

    @SubscribeEvent
    public static void fov(ComputeFovModifierEvent event) {
        if (aiming(event.getPlayer())) {
            double factor = Mth.lerp(Minecraft.getInstance().options.fovEffectScale().get(), 1, MultipurposeStaffrifle.getAdsFovModifier());
            event.setNewFovModifier((float) (event.getNewFovModifier() * factor));
        }
    }

    @SubscribeEvent
    public static void logout(ClientPlayerNetworkEvent.LoggingOut event) { reset(); }

    private static void reset() {
        lastPlayer = null;
        sentAiming = false;
        hookTick = Long.MIN_VALUE;
        shot = lastRevision = 0;
        correction = null;
        crosshairAssistUntil = Long.MIN_VALUE;

    }
}
