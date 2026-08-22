package jp.aquafactory.apprenticecodex.block.alchemybrewer;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.client.render.WaterCubeRenderTools;
import jp.aquafactory.apprenticecodex.network.packet.AlchemyBrewerWaterSupplyEffectPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;

@Mod.EventBusSubscriber(modid = ApprenticeCodex.MODID, value = Dist.CLIENT)
public final class AlchemyBrewerWaterSupplyRenderEvent {
    private static final int DURATION_TICKS = 10;
    private static final int CUBE_COUNT = 3;
    private static final int CUBE_DELAY_TICKS = 1;
    private static final float CUBE_DIAMETER = 2.0f / 16.0f;
    private static final float WATER_ALPHA = 0.78f;
    private static final int MAX_ACTIVE_EFFECTS = 32;
    private static final List<ActiveEffect> ACTIVE_EFFECTS = new ArrayList<>();

    private AlchemyBrewerWaterSupplyRenderEvent() {
    }

    public static void enqueueEffect(AlchemyBrewerWaterSupplyEffectPacket packet) {
        ACTIVE_EFFECTS.add(new ActiveEffect(packet));
        if (ACTIVE_EFFECTS.size() > MAX_ACTIVE_EFFECTS) {
            ACTIVE_EFFECTS.subList(0, ACTIVE_EFFECTS.size() - MAX_ACTIVE_EFFECTS).clear();
        }
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (Minecraft.getInstance().level == null) {
            ACTIVE_EFFECTS.clear();
        }
    }

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES || ACTIVE_EFFECTS.isEmpty()) {
            return;
        }

        var minecraft = Minecraft.getInstance();
        var level = minecraft.level;
        if (level == null) {
            ACTIVE_EFFECTS.clear();
            return;
        }
        var sprite = WaterCubeRenderTools.resolveWaterSprite();
        if (sprite == null) {
            ACTIVE_EFFECTS.clear();
            return;
        }

        var poseStack = event.getPoseStack();
        var cameraPosition = event.getCamera().getPosition();
        var bufferSource = minecraft.renderBuffers().bufferSource();
        var buffer = bufferSource.getBuffer(WaterCubeRenderTools.RENDER_TYPE);
        var gameTime = level.getGameTime();
        var partialTick = event.getPartialTick();

        poseStack.pushPose();
        poseStack.translate(-cameraPosition.x, -cameraPosition.y, -cameraPosition.z);
        var iterator = ACTIVE_EFFECTS.iterator();
        while (iterator.hasNext()) {
            var effect = iterator.next();
            var age = (float) (gameTime - effect.packet().startGameTime()) + partialTick;
            if (age < 0.0f) {
                continue;
            }
            if (age > DURATION_TICKS + (CUBE_COUNT - 1) * CUBE_DELAY_TICKS) {
                iterator.remove();
                continue;
            }

            var start = AlchemyBrewerWaterEffects.localToWorld(
                    effect.packet().brewerPos(),
                    effect.packet().brewerFacing(),
                    AlchemyBrewerWaterEffects.JAR_MOUTH_LOCAL
            );
            var end = Vec3.atCenterOf(effect.packet().targetPos());
            var horizontalMidpoint = start.lerp(end, 0.5d);
            var apex = new Vec3(horizontalMidpoint.x, Math.max(start.y, end.y) + 1.0d, horizontalMidpoint.z);
            for (var cubeIndex = 0; cubeIndex < CUBE_COUNT; ++cubeIndex) {
                var cubeAge = age - cubeIndex * CUBE_DELAY_TICKS;
                if (cubeAge < 0.0f || cubeAge > DURATION_TICKS) {
                    continue;
                }
                var progress = Mth.clamp(cubeAge / DURATION_TICKS, 0.0f, 1.0f);
                var position = arcThroughApex(start, apex, end, progress);
                var spin = cubeAge * 24.0f + cubeIndex * 35.0f;
                WaterCubeRenderTools.renderCube(
                        poseStack, buffer, sprite, position, CUBE_DIAMETER, WATER_ALPHA,
                        spin * 0.8f, spin, spin * 1.2f
                );
            }
        }
        poseStack.popPose();
        bufferSource.endBatch(WaterCubeRenderTools.RENDER_TYPE);
    }

    private static Vec3 arcThroughApex(Vec3 start, Vec3 apex, Vec3 end, double progress) {
        if (progress <= 0.5d) {
            var midpoint = start.lerp(apex, 0.5d);
            var control = new Vec3(midpoint.x, apex.y, midpoint.z);
            return quadraticBezier(start, control, apex, progress * 2.0d);
        }
        var midpoint = apex.lerp(end, 0.5d);
        var control = new Vec3(midpoint.x, apex.y, midpoint.z);
        return quadraticBezier(apex, control, end, (progress - 0.5d) * 2.0d);
    }

    private static Vec3 quadraticBezier(Vec3 start, Vec3 control, Vec3 end, double progress) {
        var inverse = 1.0d - progress;
        return start.scale(inverse * inverse)
                .add(control.scale(2.0d * inverse * progress))
                .add(end.scale(progress * progress));
    }

    private record ActiveEffect(AlchemyBrewerWaterSupplyEffectPacket packet) {
    }
}
