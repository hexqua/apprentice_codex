package jp.aquafactory.apprenticecodex.event.client;

import com.mojang.math.Axis;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.item.psychicdrivegauntlets.PsychicDriveGauntletsFollow;
import jp.aquafactory.apprenticecodex.item.psychicdrivegauntlets.PsychicDriveGauntletsArm.Phase;
import jp.aquafactory.apprenticecodex.item.psychicdrivegauntlets.PsychicDriveGauntletsCharge;
import jp.aquafactory.apprenticecodex.item.psychicdrivegauntlets.PsychicDriveGauntletsSelection;
import jp.aquafactory.apprenticecodex.item.psychicdrivegauntlets.PsychicDriveGauntletsSelection.Mode;
import jp.aquafactory.apprenticecodex.item.psychicdrivegauntlets.PsychicDriveGauntletsTransition;
import jp.aquafactory.apprenticecodex.renderer.PsychicDriveGauntletsFloatingRenderer;
import jp.aquafactory.apprenticecodex.renderer.PsychicDriveGauntletsHandRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderHandEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@EventBusSubscriber(modid = ApprenticeCodex.MODID, value = Dist.CLIENT)
public final class PsychicDriveGauntletsClientEvents {
    private static final Map<UUID, Presentation> PRESENTATIONS = new HashMap<>();
    private static ClientLevel presentationLevel;
    private static PsychicDriveGauntletsFloatingRenderer renderer;

    private PsychicDriveGauntletsClientEvents() {
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        var minecraft = Minecraft.getInstance();
        var level = minecraft.level;
        resetForLevel(level);
        PsychicDriveGauntletsClientState.prune();
        if (level == null) {
            return;
        }
        PRESENTATIONS.entrySet().removeIf(entry -> level.getPlayerByUUID(entry.getKey()) != entry.getValue().player);
        for (var player : level.players()) {
            update(player, level.getGameTime());
        }
    }

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES) {
            return;
        }
        var minecraft = Minecraft.getInstance();
        var level = minecraft.level;
        resetForLevel(level);
        if (level == null) {
            return;
        }
        var partialTick = event.getPartialTick().getGameTimeDeltaPartialTick(true);
        var time = level.getGameTime() + (double) partialTick;
        var cameraPosition = event.getCamera().getPosition();
        var buffers = minecraft.renderBuffers().bufferSource();
        var poseStack = event.getPoseStack();
        if (renderer == null) {
            renderer = new PsychicDriveGauntletsFloatingRenderer();
        }

        for (var player : level.players()) {
            var presentation = update(player, time);
            if (presentation == null) {
                continue;
            }
            var anchor = player.getEyePosition(partialTick);
            var bodyYaw = Mth.rotLerp(partialTick, player.yBodyRotO, player.yBodyRot);
            // 自分の視線yawはtick間で補間すると一人称カメラより遅れるため、現在値を使う。
            var viewYaw = player == minecraft.player ? player.getYRot() : player.getViewYRot(partialTick);
            var packedLight = LevelRenderer.getLightColor(level, player.blockPosition());
            var drawnAmount = presentation.transition.amount(time);
            var yaw = Mth.rotLerp(drawnAmount, bodyYaw, viewYaw);
            presentation.left.setTick(level.getGameTime());
            presentation.right.setTick(level.getGameTime());

            for (var side = 0; side < 2; side++) {
                var mirrored = side == 1;
                var visual = mirrored ? presentation.right : presentation.left;
                var follow = mirrored ? presentation.rightFollow : presentation.leftFollow;
                var attack = PsychicDriveGauntletsClientState.get(player.getUUID(), mirrored, partialTick);
                if (attack == null && PsychicDriveGauntletsClientState.mode(player)
                        == Mode.HIDDEN) continue;
                var offset = PsychicDriveGauntletsFloatingRenderer.offset(drawnAmount, mirrored)
                        .yRot((180.0F - yaw) * Mth.DEG_TO_RAD);
                var attachedPosition = follow.follow(anchor.add(offset), anchor, time, drawnAmount);
                boolean charging = attack != null && attack.phase() == Phase.CHARGING;
                if (charging) {
                    var ticks = (double) attack.sampleTick() - attack.launchTick() + partialTick;
                    attachedPosition = attachedPosition.add(PsychicDriveGauntletsCharge.offset(ticks)
                            .yRot((180 - viewYaw) * Mth.DEG_TO_RAD));
                }
                var position = (attack == null || charging ? attachedPosition : attack.position()).subtract(cameraPosition);
                poseStack.pushPose();
                poseStack.translate(position.x, position.y, position.z);
                // 向きは遅らせず、納刀の身体yawと抜刀の視線yawをモード遷移に合わせて繋ぐ。
                poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - (attack == null ? yaw : attack.yaw())));
                if (attack != null) poseStack.mulPose(Axis.XP.rotationDegrees(-attack.pitch()));
                renderer.renderSide(poseStack, buffers, visual, attack == null ? drawnAmount : attack.drawnAmount(),
                        mirrored, packedLight, partialTick);
                poseStack.popPose();
            }
        }
        // 他の描画のバッファはflushせず、今回使用した描画種別だけを確定する。
        if (!PRESENTATIONS.isEmpty()) {
            buffers.endBatch(renderer.renderType(PRESENTATIONS.values().iterator().next().left));
        }
    }

    @SubscribeEvent
    public static void onRenderHand(RenderHandEvent event) {
        if (!PsychicDriveGauntletsSelection.isGauntlets(event.getItemStack())) {
            return;
        }
        event.setCanceled(true);
        var minecraft = Minecraft.getInstance();
        var player = minecraft.player;
        if (player != null && event.getHand() == InteractionHand.MAIN_HAND) {
            PsychicDriveGauntletsHandRenderer.render(event, player);
        }
    }

    private static Presentation update(AbstractClientPlayer player, double time) {
        var mode = PsychicDriveGauntletsClientState.mode(player);
        if (mode == Mode.DISABLED || (mode == Mode.HIDDEN && !PsychicDriveGauntletsClientState.active(player.getUUID()))
                || player.isInvisible() || player.isSpectator() || !player.isAlive()) {
            PRESENTATIONS.remove(player.getUUID());
            return null;
        }
        var presentation = PRESENTATIONS.get(player.getUUID());
        if (presentation == null || presentation.player != player) {
            presentation = new Presentation(player);
            PRESENTATIONS.put(player.getUUID(), presentation);
        }
        presentation.transition.update(mode == Mode.HIDDEN ? Mode.SHEATHED : mode, time);
        return presentation;
    }

    private static void resetForLevel(ClientLevel level) {
        if (presentationLevel != level) {
            PRESENTATIONS.clear();
            presentationLevel = level;
        }
    }

    private static final class Presentation {
        private final AbstractClientPlayer player;
        private final PsychicDriveGauntletsTransition transition = new PsychicDriveGauntletsTransition();
        private final PsychicDriveGauntletsFollow leftFollow = new PsychicDriveGauntletsFollow();
        private final PsychicDriveGauntletsFollow rightFollow = new PsychicDriveGauntletsFollow();
        private final PsychicDriveGauntletsFloatingRenderer.Visual left;
        private final PsychicDriveGauntletsFloatingRenderer.Visual right;

        private Presentation(AbstractClientPlayer player) {
            this.player = player;
            left = new PsychicDriveGauntletsFloatingRenderer.Visual((long) player.getId() * 2);
            right = new PsychicDriveGauntletsFloatingRenderer.Visual((long) player.getId() * 2 + 1);
        }
    }
}
