package jp.aquafactory.apprenticecodex.block.atelierstation;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.network.packet.AtelierStationFluidEffectPacket;
import jp.aquafactory.apprenticecodex.registry.SoundRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;

@Mod.EventBusSubscriber(modid = ApprenticeCodex.MODID, value = Dist.CLIENT)
public final class AtelierStationFluidRenderEvent {
    private static final RenderType WATER_CUBE_RENDER_TYPE = RenderType.entityTranslucent(InventoryMenu.BLOCK_ATLAS);
    private static final int WATER_TINT = 0x3F76E4;
    private static final float WATER_RED = ((WATER_TINT >> 16) & 0xFF) / 255.0f;
    private static final float WATER_GREEN = ((WATER_TINT >> 8) & 0xFF) / 255.0f;
    private static final float WATER_BLUE = (WATER_TINT & 0xFF) / 255.0f;
    private static final Vec3 TANK_ANCHOR_LOCAL = new Vec3(12.5d / 16.0d, 16.5d / 16.0d, 12.5d / 16.0d);

    private static final Vec3 SUPPLY_SOURCE_LOCAL = new Vec3(1.65d / 16.0d, 14.0d / 16.0d, 14.3d / 16.0d);
    private static final int MAX_ACTIVE_CAULDRON_EFFECTS = 48;
    private static final int MAX_ACTIVE_SUPPLY_EFFECTS = 32;

    private static final List<ActiveCauldronEffect> ACTIVE_CAULDRON_EFFECTS = new ArrayList<>();
    private static final List<ActiveSupplyEffect> ACTIVE_SUPPLY_EFFECTS = new ArrayList<>();

    private AtelierStationFluidRenderEvent() {
    }

    public static void enqueueEffect(AtelierStationFluidEffectPacket packet) {
        var minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return;
        }

        switch (packet.kind()) {
            case CAULDRON_TO_STATION -> {
                ACTIVE_CAULDRON_EFFECTS.add(new ActiveCauldronEffect(
                        packet.stationPos(),
                        packet.stationFacing(),
                        packet.sourcePos(),
                        packet.startGameTime()
                ));
                trimEffects(ACTIVE_CAULDRON_EFFECTS, MAX_ACTIVE_CAULDRON_EFFECTS);
                playLocalSound(Vec3.atCenterOf(packet.sourcePos()), SoundEvents.GENERIC_SPLASH, SoundSource.BLOCKS, 1.0f);
            }
            case STATION_TO_PLAYER -> {
                var orbs = packet.supplyOrbs().stream()
                        .map(orb -> new SupplyOrbState(
                                new Vec3(orb.controlOffsetX(), orb.controlOffsetY(), orb.controlOffsetZ()),
                                orb.startDelayTicks(),
                                orb.durationTicks(),
                                orb.spinOffsetDegrees(),
                                orb.spinSpeedDegreesPerTick()
                        ))
                        .toList();
                ACTIVE_SUPPLY_EFFECTS.add(new ActiveSupplyEffect(
                        packet.stationPos(),
                        packet.stationFacing(),
                        packet.targetEntityId(),
                        packet.startGameTime(),
                        orbs
                ));
                trimEffects(ACTIVE_SUPPLY_EFFECTS, MAX_ACTIVE_SUPPLY_EFFECTS);
                playLocalSound(localToWorld(packet.stationPos(), packet.stationFacing(), SUPPLY_SOURCE_LOCAL),
                        SoundEvents.GENERIC_SPLASH, SoundSource.BLOCKS, 1.0f);
            }
        }
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        if (Minecraft.getInstance().level == null) {
            ACTIVE_CAULDRON_EFFECTS.clear();
            ACTIVE_SUPPLY_EFFECTS.clear();
        }
    }

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES
                || (ACTIVE_CAULDRON_EFFECTS.isEmpty() && ACTIVE_SUPPLY_EFFECTS.isEmpty())) {
            return;
        }

        var minecraft = Minecraft.getInstance();
        var level = minecraft.level;
        if (level == null) {
            ACTIVE_CAULDRON_EFFECTS.clear();
            ACTIVE_SUPPLY_EFFECTS.clear();
            return;
        }

        var sprite = resolveWaterSprite();
        if (sprite == null) {
            ACTIVE_CAULDRON_EFFECTS.clear();
            ACTIVE_SUPPLY_EFFECTS.clear();
            return;
        }

        var poseStack = event.getPoseStack();
        var bufferSource = minecraft.renderBuffers().bufferSource();
        var buffer = bufferSource.getBuffer(WATER_CUBE_RENDER_TYPE);
        var cameraPosition = event.getCamera().getPosition();
        var gameTime = level.getGameTime();
        var partialTick = event.getPartialTick();

        poseStack.pushPose();
        poseStack.translate(-cameraPosition.x, -cameraPosition.y, -cameraPosition.z);

        var cauldronIterator = ACTIVE_CAULDRON_EFFECTS.iterator();
        while (cauldronIterator.hasNext()) {
            var effect = cauldronIterator.next();
            var age = (float) (gameTime - effect.startGameTime()) + partialTick;
            if (age >= AtelierStationFluidEffectTuning.CAULDRON_TOTAL_TICKS) {
                cauldronIterator.remove();
                continue;
            }

            renderCauldronEffect(poseStack, buffer, sprite, effect, age);
        }

        ACTIVE_SUPPLY_EFFECTS.removeIf(effect -> !renderSupplyEffect(level, poseStack, buffer, sprite, effect, gameTime, partialTick));
        poseStack.popPose();
        bufferSource.endBatch(WATER_CUBE_RENDER_TYPE);
    }

    private static void renderCauldronEffect(PoseStack poseStack, VertexConsumer buffer, TextureAtlasSprite sprite,
                                             ActiveCauldronEffect effect, float age) {
        var start = Vec3.atCenterOf(effect.cauldronPos());
        var hover = start.add(0.0d, 1.0d, 0.0d);
        var tankAnchor = localToWorld(effect.stationPos(), effect.stationFacing(), TANK_ANCHOR_LOCAL);
        var preDashTicks = AtelierStationFluidEffectTuning.CAULDRON_ASCEND_TICKS + AtelierStationFluidEffectTuning.CAULDRON_HOVER_TICKS;
        Vec3 position;
        float diameter;
        if (age < AtelierStationFluidEffectTuning.CAULDRON_ASCEND_TICKS) {
            var progress = age / AtelierStationFluidEffectTuning.CAULDRON_ASCEND_TICKS;
            position = start.lerp(hover, cubicEaseOut(progress));
            diameter = Mth.lerp(age / preDashTicks,
                    AtelierStationFluidEffectTuning.CAULDRON_START_DIAMETER,
                    AtelierStationFluidEffectTuning.CAULDRON_PRE_DASH_DIAMETER);
        } else if (age < preDashTicks) {
            position = hover;
            diameter = Mth.lerp(age / preDashTicks,
                    AtelierStationFluidEffectTuning.CAULDRON_START_DIAMETER,
                    AtelierStationFluidEffectTuning.CAULDRON_PRE_DASH_DIAMETER);
        } else {
            var dashAge = age - preDashTicks;
            var progress = Mth.clamp(dashAge / AtelierStationFluidEffectTuning.CAULDRON_DASH_TICKS, 0.0f, 1.0f);
            position = hover.lerp(tankAnchor, progress);
            diameter = Mth.lerp(progress,
                    AtelierStationFluidEffectTuning.CAULDRON_PRE_DASH_DIAMETER,
                    AtelierStationFluidEffectTuning.CAULDRON_DASH_END_DIAMETER);
        }

        renderCube(poseStack, buffer, sprite, position, diameter,
                age * AtelierStationFluidEffectTuning.CAULDRON_ROTATE_X,
                age * AtelierStationFluidEffectTuning.CAULDRON_ROTATE_Y,
                age * AtelierStationFluidEffectTuning.CAULDRON_ROTATE_Z);
    }

    private static boolean renderSupplyEffect(net.minecraft.client.multiplayer.ClientLevel level, PoseStack poseStack,
                                              VertexConsumer buffer, TextureAtlasSprite sprite,
                                              ActiveSupplyEffect effect, long gameTime, float partialTick) {
        var targetEntity = level.getEntity(effect.targetEntityId());
        if (targetEntity == null || !targetEntity.isAlive()) {
            return false;
        }

        var origin = localToWorld(effect.stationPos(), effect.stationFacing(), SUPPLY_SOURCE_LOCAL);
        var target = targetEntity.position().add(0.0d, targetEntity.getBbHeight() * 0.55d, 0.0d);
        var age = (float) (gameTime - effect.startGameTime()) + partialTick;
        var renderedAny = false;
        var latestEndTick = 0.0f;

        for (var orb : effect.orbs()) {
            var orbAge = age - orb.startDelayTicks();
            latestEndTick = Math.max(latestEndTick, orb.startDelayTicks() + orb.durationTicks());
            if (orbAge < 0.0f || orbAge > orb.durationTicks()) {
                continue;
            }

            var anchor = origin.add(rotateVector(effect.stationFacing(), orb.controlOffset()));
            var ascendTicks = Math.min(AtelierStationFluidEffectTuning.SUPPLY_ASCEND_TICKS, (float) orb.durationTicks());
            var hoverTicks = Math.min(AtelierStationFluidEffectTuning.SUPPLY_HOVER_TICKS,
                    Math.max(0.0f, orb.durationTicks() - ascendTicks));
            var dashTicks = Math.max(1.0f, orb.durationTicks() - ascendTicks - hoverTicks);
            var dashStartTick = ascendTicks + hoverTicks;
            if (!orb.isLaunchSoundPlayed() && orbAge >= dashStartTick) {
                playLocalSound(anchor, SoundRegistry.SIPHON_ORB_LAUNCH.get(), SoundSource.PLAYERS, 1.0f);
                orb.markLaunchSoundPlayed();
            }
            Vec3 position;
            if (orbAge < ascendTicks) {
                var progress = orbAge / ascendTicks;
                position = origin.lerp(anchor, cubicEaseOut(progress));
            } else if (orbAge < ascendTicks + hoverTicks) {
                position = anchor;
            } else {
                var dashAge = orbAge - ascendTicks - hoverTicks;
                var progress = Mth.clamp(dashAge / dashTicks, 0.0f, 1.0f);
                position = anchor.lerp(target, progress);
            }
            var spin = orb.spinOffsetDegrees() + orbAge * orb.spinSpeedDegreesPerTick();
            renderCube(poseStack, buffer, sprite, position, AtelierStationFluidEffectTuning.SUPPLY_CUBE_DIAMETER,
                    spin * 0.85f, spin, spin * 1.15f);
            renderedAny = true;
        }

        return renderedAny || age <= latestEndTick;
    }

    private static void renderCube(PoseStack poseStack, VertexConsumer buffer, TextureAtlasSprite sprite, Vec3 position,
                                   float diameter, float rotateX, float rotateY, float rotateZ) {
        poseStack.pushPose();
        poseStack.translate(position.x, position.y, position.z);
        poseStack.mulPose(Axis.XP.rotationDegrees(rotateX));
        poseStack.mulPose(Axis.YP.rotationDegrees(rotateY));
        poseStack.mulPose(Axis.ZP.rotationDegrees(rotateZ));
        drawTexturedCube(poseStack, buffer, sprite, diameter * 0.5f);
        poseStack.popPose();
    }

    private static void drawTexturedCube(PoseStack poseStack, VertexConsumer buffer, TextureAtlasSprite sprite, float half) {
        var pose = poseStack.last();
        var poseMatrix = pose.pose();
        var normalMatrix = pose.normal();
        var u0 = sprite.getU0();
        var u1 = sprite.getU1();
        var v0 = sprite.getV0();
        var v1 = sprite.getV1();

        face(buffer, poseMatrix, normalMatrix,
                new Vec3(-half, -half, half), new Vec3(half, -half, half),
                new Vec3(half, half, half), new Vec3(-half, half, half),
                u0, v1, u1, v1, u1, v0, u0, v0, new Vec3(0.0d, 0.0d, 1.0d));
        face(buffer, poseMatrix, normalMatrix,
                new Vec3(half, -half, -half), new Vec3(-half, -half, -half),
                new Vec3(-half, half, -half), new Vec3(half, half, -half),
                u0, v1, u1, v1, u1, v0, u0, v0, new Vec3(0.0d, 0.0d, -1.0d));
        face(buffer, poseMatrix, normalMatrix,
                new Vec3(-half, -half, -half), new Vec3(-half, -half, half),
                new Vec3(-half, half, half), new Vec3(-half, half, -half),
                u0, v1, u1, v1, u1, v0, u0, v0, new Vec3(-1.0d, 0.0d, 0.0d));
        face(buffer, poseMatrix, normalMatrix,
                new Vec3(half, -half, half), new Vec3(half, -half, -half),
                new Vec3(half, half, -half), new Vec3(half, half, half),
                u0, v1, u1, v1, u1, v0, u0, v0, new Vec3(1.0d, 0.0d, 0.0d));
        face(buffer, poseMatrix, normalMatrix,
                new Vec3(-half, half, half), new Vec3(half, half, half),
                new Vec3(half, half, -half), new Vec3(-half, half, -half),
                u0, v1, u1, v1, u1, v0, u0, v0, new Vec3(0.0d, 1.0d, 0.0d));
        face(buffer, poseMatrix, normalMatrix,
                new Vec3(-half, -half, -half), new Vec3(half, -half, -half),
                new Vec3(half, -half, half), new Vec3(-half, -half, half),
                u0, v1, u1, v1, u1, v0, u0, v0, new Vec3(0.0d, -1.0d, 0.0d));
    }

    private static void face(VertexConsumer buffer, Matrix4f poseMatrix, Matrix3f normalMatrix,
                             Vec3 p0, Vec3 p1, Vec3 p2, Vec3 p3,
                             float u0, float v0, float u1, float v1, float u2, float v2, float u3, float v3,
                             Vec3 normal) {
        vertex(buffer, poseMatrix, normalMatrix, p0, u0, v0, normal);
        vertex(buffer, poseMatrix, normalMatrix, p1, u1, v1, normal);
        vertex(buffer, poseMatrix, normalMatrix, p2, u2, v2, normal);
        vertex(buffer, poseMatrix, normalMatrix, p3, u3, v3, normal);
    }

    private static void vertex(VertexConsumer buffer, Matrix4f poseMatrix, Matrix3f normalMatrix, Vec3 position,
                               float u, float v, Vec3 normal) {
        buffer.vertex(poseMatrix, (float) position.x, (float) position.y, (float) position.z)
                .color(WATER_RED, WATER_GREEN, WATER_BLUE, AtelierStationFluidEffectTuning.WATER_ALPHA)
                .uv(u, v)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(LightTexture.FULL_BRIGHT)
                .normal(normalMatrix, (float) normal.x, (float) normal.y, (float) normal.z)
                .endVertex();
    }

    private static TextureAtlasSprite resolveWaterSprite() {
        var stillTexture = IClientFluidTypeExtensions.of(Fluids.WATER)
                .getStillTexture(new FluidStack(Fluids.WATER, 1000));
        if (stillTexture == null) {
            return null;
        }

        var sprite = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(stillTexture);
        return sprite.contents().name().equals(MissingTextureAtlasSprite.getLocation()) ? null : sprite;
    }

    private static Vec3 localToWorld(BlockPos blockPos, Direction facing, Vec3 localPoint) {
        var rotated = rotatePointAroundCenter(facing, localPoint);
        return new Vec3(
                blockPos.getX() + rotated.x,
                blockPos.getY() + rotated.y,
                blockPos.getZ() + rotated.z
        );
    }

    private static Vec3 rotatePointAroundCenter(Direction facing, Vec3 point) {
        var relativeX = point.x - 0.5d;
        var relativeZ = point.z - 0.5d;
        var rotated = rotateVector(facing, new Vec3(relativeX, point.y, relativeZ));
        return new Vec3(rotated.x + 0.5d, rotated.y, rotated.z + 0.5d);
    }

    private static Vec3 rotateVector(Direction facing, Vec3 vector) {
        return switch (facing) {
            case EAST -> new Vec3(-vector.z, vector.y, vector.x);
            case SOUTH -> new Vec3(-vector.x, vector.y, -vector.z);
            case WEST -> new Vec3(vector.z, vector.y, -vector.x);
            default -> vector;
        };
    }

    private static float cubicEaseOut(float progress) {
        var inverse = 1.0f - Mth.clamp(progress, 0.0f, 1.0f);
        return 1.0f - inverse * inverse * inverse;
    }

    private static void playLocalSound(Vec3 position, net.minecraft.sounds.SoundEvent soundEvent, SoundSource soundSource,
                                       float volume) {
        var level = Minecraft.getInstance().level;
        if (level == null) {
            return;
        }

        level.playLocalSound(position.x, position.y, position.z, soundEvent, soundSource, volume, 1.0f, false);
    }

    private static <T> void trimEffects(List<T> effects, int maxSize) {
        if (effects.size() <= maxSize) {
            return;
        }

        effects.subList(0, effects.size() - maxSize).clear();
    }

    private record ActiveCauldronEffect(BlockPos stationPos, Direction stationFacing, BlockPos cauldronPos,
                                        long startGameTime) {
    }

    private record ActiveSupplyEffect(BlockPos stationPos, Direction stationFacing, int targetEntityId,
                                      long startGameTime, List<SupplyOrbState> orbs) {
        private ActiveSupplyEffect {
            orbs = List.copyOf(orbs);
        }
    }

    private static final class SupplyOrbState {
        private final Vec3 controlOffset;
        private final int startDelayTicks;
        private final int durationTicks;
        private final float spinOffsetDegrees;
        private final float spinSpeedDegreesPerTick;
        private boolean launchSoundPlayed;

        private SupplyOrbState(Vec3 controlOffset, int startDelayTicks, int durationTicks, float spinOffsetDegrees,
                               float spinSpeedDegreesPerTick) {
            this.controlOffset = controlOffset;
            this.startDelayTicks = startDelayTicks;
            this.durationTicks = durationTicks;
            this.spinOffsetDegrees = spinOffsetDegrees;
            this.spinSpeedDegreesPerTick = spinSpeedDegreesPerTick;
        }

        private Vec3 controlOffset() {
            return controlOffset;
        }

        private int startDelayTicks() {
            return startDelayTicks;
        }

        private int durationTicks() {
            return durationTicks;
        }

        private float spinOffsetDegrees() {
            return spinOffsetDegrees;
        }

        private float spinSpeedDegreesPerTick() {
            return spinSpeedDegreesPerTick;
        }

        private boolean isLaunchSoundPlayed() {
            return launchSoundPlayed;
        }

        private void markLaunchSoundPlayed() {
            launchSoundPlayed = true;
        }
    }
}
