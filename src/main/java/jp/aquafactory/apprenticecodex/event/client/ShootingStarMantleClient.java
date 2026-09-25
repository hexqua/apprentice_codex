package jp.aquafactory.apprenticecodex.event.client;

import io.redspace.ironsspellbooks.player.ClientMagicData;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.item.curios.shootingstarmantle.MantleCalibration;
import jp.aquafactory.apprenticecodex.item.curios.shootingstarmantle.MantleMovement;
import jp.aquafactory.apprenticecodex.item.curios.shootingstarmantle.ShootingStarMantleRuntime;
import jp.aquafactory.apprenticecodex.network.Networks;
import jp.aquafactory.apprenticecodex.network.packet.ClientMantleImpulsePacket;
import jp.aquafactory.apprenticecodex.network.packet.ClientMantleDashInputPacket;
import jp.aquafactory.apprenticecodex.network.packet.ClientMantleFireworkInputPacket;
import jp.aquafactory.apprenticecodex.network.packet.SyncMantleDashPacket;
import jp.aquafactory.apprenticecodex.network.packet.SyncMantlePacket;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.client.event.MovementInputUpdateEvent;

import java.util.Map;
import java.util.WeakHashMap;

@EventBusSubscriber(modid = ApprenticeCodex.MODID, value = Dist.CLIENT)
public final class ShootingStarMantleClient {
    private static final int WING_TRANSITION_TICKS = 6;
    private static final Map<Player, WingAnimation> WING_ANIMATIONS = new WeakHashMap<>();
    private static boolean jumpHeld;
    private static long nextSequence;
    private static long nextDashSequence;
    private static boolean sentDashInput;
    private static long nextFireworkSequence;
    private static boolean sentFireworkInput;
    private static long pendingSequence = -1;
    private static ItemStack selectionSnapshot = ItemStack.EMPTY;

    private ShootingStarMantleClient() { }

    private static final class WingAnimation {
        private int previous;
        private int current;

        private void tick(boolean hovering) {
            previous = current;
            current = Mth.clamp(current + (hovering ? 1 : -1), 0, WING_TRANSITION_TICKS);
        }

        private float amount(float partialTicks) {
            float progress = Mth.lerp(Mth.clamp(partialTicks, 0, 1), previous, current) / WING_TRANSITION_TICKS;
            return progress * progress * (3 - 2 * progress);
        }
    }

    public static float wingOpenAmount(Player player, float partialTicks) {
        var animation = WING_ANIMATIONS.get(player);
        return animation == null ? 0 : animation.amount(partialTicks);
    }

    private static void tickWingAnimations(Minecraft minecraft) {
        if (minecraft.level == null) {
            WING_ANIMATIONS.clear();
            return;
        }
        if (minecraft.isPaused()) return;
        WING_ANIMATIONS.keySet().removeIf(player -> player.isRemoved() || player.level() != minecraft.level);
        // rendererは複数playerで共有されるため、進行度はentity単位で保持する。
        // 描画回数ではなくclient tickで進め、視界外やFPSの違いでも開閉時間を揃える。
        for (var player : minecraft.level.players()) {
            boolean hovering = ShootingStarMantleRuntime.state(player).hovering;
            if (hovering || WING_ANIMATIONS.containsKey(player)) {
                var animation = WING_ANIMATIONS.computeIfAbsent(player, ignored -> new WingAnimation());
                animation.tick(hovering);
                if (animation.previous == 0 && animation.current == 0) WING_ANIMATIONS.remove(player);
            }
        }
    }

    public static void accept(SyncMantlePacket packet) {
        var minecraft = Minecraft.getInstance();
        if (minecraft.level == null || !(minecraft.level.getEntity(packet.entityId()) instanceof Player player)) return;
        var state = ShootingStarMantleRuntime.state(player);
        boolean ownPrediction = player == minecraft.player && state.blink.start() >= 0
                && state.blink.sequence() == packet.blinkSequence();
        boolean olderBlink = player == minecraft.player && pendingSequence >= 0 && state.blink.start() >= 0
                && state.blink.sequence() == pendingSequence && packet.blinkSequence() != pendingSequence
                && packet.sequence() != pendingSequence && packet.hovering() && packet.equipped();
        // 本人の受理応答で予測の時系列を巻き戻さない。追跡側はserverの開始時刻で再現する。
        if (!olderBlink && packet.blinkStart() >= 0) {
            if (!ownPrediction && (state.blink.start() != packet.blinkStart()
                    || state.blink.sequence() != packet.blinkSequence())) {
                state.blink.accept(packet.blinkStart(), packet.blinkSequence(), packet.blinkHeight(), packet.blinkDirection());
            }
        } else if (!olderBlink && (player != minecraft.player || pendingSequence < 0 || packet.sequence() == pendingSequence)) {
            state.blink.cancel();
        }
        state.equipped = packet.equipped();
        state.energy = packet.energy();
        state.maxEnergy = packet.maxEnergy();
        state.recovering = packet.recovering();
        boolean finishPredictedBlink = ownPrediction && packet.blinkStart() >= 0 && packet.equipped()
                && packet.recovering() && state.blink.active(player.level().getGameTime());
        state.hovering = packet.hovering() || finishPredictedBlink;
        if (packet.blink()) state.blinkTicks = 20;
        if (!packet.hovering()) {
            // serverの終了通知が予測の最終tickより先に届くと、travel側の減速処理を通らない。
            // 枯渇終了では残りtickを消す前に止め、競合する飛行能力や上下速度は維持する。
            if (player == minecraft.player && state.dashTicks > 0 && packet.recovering() && packet.equipped()
                    && !ShootingStarMantleRuntime.conflict(player) && !ShootingStarMantleRuntime.invalidHoverContext(player)) {
                MantleMovement.finishImpulse(player);
            }
            state.dashTicks = 0;
            state.lastPosition = null;
            state.movingTicks = 0;
            if (player == minecraft.player) pendingSequence = -1;
        }
        if (player == minecraft.player && packet.sequence() == pendingSequence && packet.sequence() >= 0) {
            pendingSequence = -1;
            if (!packet.accepted()) {
                state.dashTicks = 0;
                state.blink.cancel();
                player.setDeltaMovement(0, player.getDeltaMovement().y, 0);
            }
        }
    }

    public static void acceptDash(SyncMantleDashPacket packet) {
        var minecraft = Minecraft.getInstance();
        if (minecraft.level == null || !(minecraft.level.getEntity(packet.entityId()) instanceof Player player)) return;
        var dash = ShootingStarMantleRuntime.state(player).elemental;
        if (dash.accept(packet) && player == minecraft.player) {
            // BurningDashの通常cast packetを使わず、serverの決定した速度を一度だけ適用する。
            var motion = packet.motion();
            player.setDeltaMovement(packet.kind() != 0 && packet.hover()
                    ? new Vec3(motion.x, player.getDeltaMovement().y, motion.z) : motion);
        }
    }

    @SubscribeEvent
    public static void input(MovementInputUpdateEvent event) {
        var player = event.getEntity();
        var input = event.getInput();
        boolean jump = input.jumping;
        var state = ShootingStarMantleRuntime.state(player);
        boolean elemental = MantleCalibration.elementalKind(ShootingStarMantleRuntime.findEquipped(player)) != 0;
        boolean firework = MantleCalibration.usesFirework(ShootingStarMantleRuntime.findEquipped(player));
        if (firework || sentFireworkInput) {
            Networks.sendToServer(new ClientMantleFireworkInputPacket(nextFireworkSequence++,
                    firework && Minecraft.getInstance().screen == null && jump));
            sentFireworkInput = firework;
        }
        if (elemental || sentDashInput) {
            boolean usableInput = elemental && Minecraft.getInstance().screen == null;
            if (usableInput && jump && !jumpHeld && ShootingStarMantleRuntime.isHovering(player)
                    && !state.recovering && state.energy > 0) {
                state.elemental.predictHover(player, MantleCalibration.elementalKind(ShootingStarMantleRuntime.findEquipped(player)),
                        input.forwardImpulse, input.leftImpulse);
            }
            Networks.sendToServer(new ClientMantleDashInputPacket(nextDashSequence++, usableInput && jump,
                    usableInput ? input.forwardImpulse : 0, usableInput ? input.leftImpulse : 0));
            sentDashInput = elemental;
        }
        if (ShootingStarMantleRuntime.isHovering(player)) {
            if (!elemental && jump && !jumpHeld && state.dashTicks == 0 && !state.blink.active(player.level().getGameTime())
                    && pendingSequence < 0 && !state.recovering
                    && Minecraft.getInstance().screen == null) {
                pendingSequence = nextSequence++;
                state.dashDirection = MantleMovement.direction(input.forwardImpulse, input.leftImpulse, player.getYRot());
                if (MantleCalibration.usesBlink(ShootingStarMantleRuntime.findEquipped(player))) {
                    state.blink.begin(player, pendingSequence, state.dashDirection);
                } else {
                    state.blink.cancel();
                    state.dashTicks = 5;
                }
                Networks.sendToServer(new ClientMantleImpulsePacket(pendingSequence, input.forwardImpulse, input.leftImpulse));
            }
            // 通常ジャンプ・エリトラ開始へ同じ押下を渡さない。
            input.jumping = false;
        }
        jumpHeld = jump;
    }

    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        var minecraft = Minecraft.getInstance();
        var player = minecraft.player;
        var equipped = player == null ? ItemStack.EMPTY : ShootingStarMantleRuntime.findEquipped(player);
        if (player != null && !MantleCalibration.hasSameScrollSelection(selectionSnapshot, equipped, player.level().registryAccess())) {
            // 通知よりCuriosの装備同期が遅れても更新する。魔力消費だけでは再構築しない。
            ClientMagicData.updateSpellSelectionManager();
        }
        selectionSnapshot = equipped.copy();
        if (player != null) {
            var state = ShootingStarMantleRuntime.state(player);
            state.blink.update(player, state);
            if (state.blinkTicks > 0) state.blinkTicks--;
            if (state.hovering && !ShootingStarMantleRuntime.isHovering(player)) {
                state.hovering = false;
                state.dashTicks = 0;
                if (state.blink.active(player.level().getGameTime())) state.blink.cancel();
            }
        }
        tickWingAnimations(minecraft);
        if (!minecraft.isPaused()) ShootingStarMantleParticles.tick(player);
    }

    @SubscribeEvent
    public static void logout(ClientPlayerNetworkEvent.LoggingOut event) {
        ShootingStarMantleRuntime.clearClient();
        WING_ANIMATIONS.clear();
        jumpHeld = false;
        pendingSequence = -1;
        nextSequence = 0;
        nextDashSequence = 0;
        sentDashInput = false;
        nextFireworkSequence = 0;
        sentFireworkInput = false;
        selectionSnapshot = ItemStack.EMPTY;
        ShootingStarMantleParticles.reset();
    }
}
