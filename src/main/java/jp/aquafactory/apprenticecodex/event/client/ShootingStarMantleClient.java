package jp.aquafactory.apprenticecodex.event.client;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.item.curios.shootingstarmantle.MantleMovement;
import jp.aquafactory.apprenticecodex.item.curios.shootingstarmantle.ShootingStarMantleRuntime;
import jp.aquafactory.apprenticecodex.network.Networks;
import jp.aquafactory.apprenticecodex.network.packet.ClientMantleImpulsePacket;
import jp.aquafactory.apprenticecodex.network.packet.SyncMantlePacket;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.MovementInputUpdateEvent;

import java.util.Map;
import java.util.WeakHashMap;

@EventBusSubscriber(modid = ApprenticeCodex.MODID, value = Dist.CLIENT)
public final class ShootingStarMantleClient {
    private static final int WING_TRANSITION_TICKS = 6;
    private static final Map<Player, WingAnimation> WING_ANIMATIONS = new WeakHashMap<>();
    private static boolean jumpHeld;
    private static long nextSequence;
    private static long pendingSequence = -1;

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
        state.equipped = packet.equipped();
        state.energy = packet.energy();
        state.recovering = packet.recovering();
        state.hovering = packet.hovering();
        if (packet.blink()) state.blinkTicks = 20;
        if (!packet.hovering()) {
            // serverの終了通知が予測の最終tickより先に届くと、travel側の減速処理を通らない。
            // 枯渇終了では残りtickを消す前に止め、競合する飛行能力や上下速度は維持する。
            if (player == minecraft.player && state.dashTicks > 0 && packet.recovering() && packet.equipped()
                    && !ShootingStarMantleRuntime.conflict(player) && !ShootingStarMantleRuntime.invalidHoverContext(player)) {
                player.setDeltaMovement(0, player.getDeltaMovement().y, 0);
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
                player.setDeltaMovement(0, player.getDeltaMovement().y, 0);
            }
        }
    }

    @SubscribeEvent
    public static void input(MovementInputUpdateEvent event) {
        var player = event.getEntity();
        var input = event.getInput();
        boolean jump = input.jumping;
        var state = ShootingStarMantleRuntime.state(player);
        if (ShootingStarMantleRuntime.isHovering(player)) {
            if (jump && !jumpHeld && state.dashTicks == 0 && pendingSequence < 0 && !state.recovering
                    && Minecraft.getInstance().screen == null) {
                pendingSequence = nextSequence++;
                state.dashDirection = MantleMovement.direction(input.forwardImpulse, input.leftImpulse, player.getYRot());
                state.dashTicks = 5;
                Networks.sendToServer(new ClientMantleImpulsePacket(pendingSequence, input.forwardImpulse, input.leftImpulse));
            }
            // 通常ジャンプ・エリトラ開始へ同じ押下を渡さない。
            input.jumping = false;
        }
        jumpHeld = jump;
    }

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        var minecraft = Minecraft.getInstance();
        var player = minecraft.player;
        if (player != null) {
            var state = ShootingStarMantleRuntime.state(player);
            if (state.blinkTicks > 0) state.blinkTicks--;
            if (state.hovering && !ShootingStarMantleRuntime.isHovering(player)) {
                state.hovering = false;
                state.dashTicks = 0;
            }
        }
        tickWingAnimations(minecraft);
    }

    @SubscribeEvent
    public static void logout(ClientPlayerNetworkEvent.LoggingOut event) {
        ShootingStarMantleRuntime.clearClient();
        WING_ANIMATIONS.clear();
        jumpHeld = false;
        pendingSequence = -1;
        nextSequence = 0;
    }
}
