package jp.aquafactory.apprenticecodex.block.atelierstation;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.client.render.WaterCubeRenderTools;
import jp.aquafactory.apprenticecodex.network.packet.AtelierStationFluidEffectPacket;
import jp.aquafactory.apprenticecodex.registry.SoundRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
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
public final class AtelierStationFluidRenderEvent {
    private static final Vec3 TANK_ANCHOR_LOCAL = new Vec3(12.5d / 16.0d, 16.5d / 16.0d, 12.5d / 16.0d);
    private static final Vec3 ALCHEMY_BREWER_SOURCE_LOCAL = new Vec3(12.0d / 16.0d, 0.5d, 4.0d / 16.0d);

    private static final Vec3 SUPPLY_SOURCE_LOCAL = new Vec3(1.65d / 16.0d, 14.0d / 16.0d, 14.3d / 16.0d);
    private static final int MAX_ACTIVE_COLLECTION_EFFECTS = 48;
    private static final int MAX_ACTIVE_SUPPLY_EFFECTS = 32;

    private static final List<ActiveCollectionEffect> ACTIVE_COLLECTION_EFFECTS = new ArrayList<>();
    private static final List<ActiveSupplyEffect> ACTIVE_SUPPLY_EFFECTS = new ArrayList<>();

    private AtelierStationFluidRenderEvent() {
    }

    public static void enqueueEffect(AtelierStationFluidEffectPacket packet) {
        var minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return;
        }

        switch (packet.kind()) {
            case CAULDRON_TO_STATION, ALCHEMY_BREWER_TO_STATION -> {
                ACTIVE_COLLECTION_EFFECTS.add(new ActiveCollectionEffect(
                        packet.kind(),
                        packet.stationPos(),
                        packet.stationFacing(),
                        packet.sourcePos(),
                        packet.sourceFacing(),
                        packet.startGameTime()
                ));
                trimEffects(ACTIVE_COLLECTION_EFFECTS, MAX_ACTIVE_COLLECTION_EFFECTS);
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
                playLocalSound(WaterCubeRenderTools.localToWorld(packet.stationPos(), packet.stationFacing(), SUPPLY_SOURCE_LOCAL),
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
            ACTIVE_COLLECTION_EFFECTS.clear();
            ACTIVE_SUPPLY_EFFECTS.clear();
        }
    }

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES
                || (ACTIVE_COLLECTION_EFFECTS.isEmpty() && ACTIVE_SUPPLY_EFFECTS.isEmpty())) {
            return;
        }

        var minecraft = Minecraft.getInstance();
        var level = minecraft.level;
        if (level == null) {
            ACTIVE_COLLECTION_EFFECTS.clear();
            ACTIVE_SUPPLY_EFFECTS.clear();
            return;
        }

        var sprite = WaterCubeRenderTools.resolveWaterSprite();
        if (sprite == null) {
            ACTIVE_COLLECTION_EFFECTS.clear();
            ACTIVE_SUPPLY_EFFECTS.clear();
            return;
        }

        var poseStack = event.getPoseStack();
        var bufferSource = minecraft.renderBuffers().bufferSource();
        var buffer = bufferSource.getBuffer(WaterCubeRenderTools.RENDER_TYPE);
        var cameraPosition = event.getCamera().getPosition();
        var gameTime = level.getGameTime();
        var partialTick = event.getPartialTick();

        poseStack.pushPose();
        poseStack.translate(-cameraPosition.x, -cameraPosition.y, -cameraPosition.z);

        var collectionIterator = ACTIVE_COLLECTION_EFFECTS.iterator();
        while (collectionIterator.hasNext()) {
            var effect = collectionIterator.next();
            var age = (float) (gameTime - effect.startGameTime()) + partialTick;
            if (age >= AtelierStationFluidEffectTuning.CAULDRON_TOTAL_TICKS) {
                collectionIterator.remove();
                continue;
            }

            renderCollectionEffect(poseStack, buffer, sprite, effect, age);
        }

        ACTIVE_SUPPLY_EFFECTS.removeIf(effect -> !renderSupplyEffect(level, poseStack, buffer, sprite, effect, gameTime, partialTick));
        poseStack.popPose();
        bufferSource.endBatch(WaterCubeRenderTools.RENDER_TYPE);
    }

    private static void renderCollectionEffect(PoseStack poseStack, VertexConsumer buffer, TextureAtlasSprite sprite,
                                               ActiveCollectionEffect effect, float age) {
        var fromAlchemyBrewer = effect.kind() == AtelierStationFluidEffectPacket.EffectKind.ALCHEMY_BREWER_TO_STATION;
        var start = fromAlchemyBrewer
                ? WaterCubeRenderTools.localToWorld(effect.sourcePos(), effect.sourceFacing(), ALCHEMY_BREWER_SOURCE_LOCAL)
                : Vec3.atCenterOf(effect.sourcePos());
        var hover = start.add(0.0d, 1.0d, 0.0d);
        var tankAnchor = WaterCubeRenderTools.localToWorld(effect.stationPos(), effect.stationFacing(), TANK_ANCHOR_LOCAL);
        var preDashTicks = AtelierStationFluidEffectTuning.CAULDRON_ASCEND_TICKS + AtelierStationFluidEffectTuning.CAULDRON_HOVER_TICKS;
        Vec3 position;
        float diameter;
        if (age < AtelierStationFluidEffectTuning.CAULDRON_ASCEND_TICKS) {
            var progress = age / AtelierStationFluidEffectTuning.CAULDRON_ASCEND_TICKS;
            position = start.lerp(hover, cubicEaseOut(progress));
            diameter = fromAlchemyBrewer
                    ? AtelierStationFluidEffectTuning.ALCHEMY_BREWER_PRE_DASH_DIAMETER
                    : Mth.lerp(age / preDashTicks,
                            AtelierStationFluidEffectTuning.CAULDRON_START_DIAMETER,
                            AtelierStationFluidEffectTuning.CAULDRON_PRE_DASH_DIAMETER);
        } else if (age < preDashTicks) {
            position = hover;
            diameter = fromAlchemyBrewer
                    ? AtelierStationFluidEffectTuning.ALCHEMY_BREWER_PRE_DASH_DIAMETER
                    : Mth.lerp(age / preDashTicks,
                            AtelierStationFluidEffectTuning.CAULDRON_START_DIAMETER,
                            AtelierStationFluidEffectTuning.CAULDRON_PRE_DASH_DIAMETER);
        } else {
            var dashAge = age - preDashTicks;
            var progress = Mth.clamp(dashAge / AtelierStationFluidEffectTuning.CAULDRON_DASH_TICKS, 0.0f, 1.0f);
            position = hover.lerp(tankAnchor, progress);
            diameter = Mth.lerp(progress,
                    fromAlchemyBrewer
                            ? AtelierStationFluidEffectTuning.ALCHEMY_BREWER_PRE_DASH_DIAMETER
                            : AtelierStationFluidEffectTuning.CAULDRON_PRE_DASH_DIAMETER,
                    AtelierStationFluidEffectTuning.CAULDRON_DASH_END_DIAMETER);
        }

        WaterCubeRenderTools.renderCube(poseStack, buffer, sprite, position, diameter,
                AtelierStationFluidEffectTuning.WATER_ALPHA,
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

        var origin = WaterCubeRenderTools.localToWorld(effect.stationPos(), effect.stationFacing(), SUPPLY_SOURCE_LOCAL);
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

            var anchor = origin.add(WaterCubeRenderTools.rotateVector(effect.stationFacing(), orb.controlOffset()));
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
            WaterCubeRenderTools.renderCube(poseStack, buffer, sprite, position,
                    AtelierStationFluidEffectTuning.SUPPLY_CUBE_DIAMETER, AtelierStationFluidEffectTuning.WATER_ALPHA,
                    spin * 0.85f, spin, spin * 1.15f);
            renderedAny = true;
        }

        return renderedAny || age <= latestEndTick;
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

    private record ActiveCollectionEffect(AtelierStationFluidEffectPacket.EffectKind kind, BlockPos stationPos,
                                          Direction stationFacing, BlockPos sourcePos, Direction sourceFacing,
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
