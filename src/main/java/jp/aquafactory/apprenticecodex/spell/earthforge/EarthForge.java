package jp.aquafactory.apprenticecodex.spell.earthforge;

import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.spells.*;
import io.redspace.ironsspellbooks.api.util.AnimationHolder;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.registry.SoundRegistry;
import jp.aquafactory.apprenticecodex.utility.BlockTargetingHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class EarthForge extends AbstractSpell implements jp.aquafactory.apprenticecodex.spell.IClientBlockTargetingSpell {
    private final ResourceLocation spellId = ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "earth_forge");

    private final DefaultConfig config = new DefaultConfig()
            .setMinRarity(SpellRarity.UNCOMMON)
            .setSchoolResource(SchoolRegistry.NATURE_RESOURCE)
            .setMaxLevel(1)
            .setCooldownSeconds(1)
            .build();

    public EarthForge() {
        baseSpellPower = 100;
        spellPowerPerLevel = 0;
        baseManaCost = 20;
        manaCostPerLevel = 0;
        castTime = 0;
    }

    @Override
    public ResourceLocation getSpellResource() {
        return spellId;
    }

    @Override
    public DefaultConfig getDefaultConfig() {
        return config;
    }

    @Override
    public CastType getCastType() {
        return CastType.INSTANT;
    }

    @Override
    public Optional<SoundEvent> getCastStartSound() {
        return Optional.of(SoundRegistry.TERRAIN.get());
    }

    @Override
    public Optional<SoundEvent> getCastFinishSound() {
        return Optional.empty();
    }

    @Override
    public AnimationHolder getCastStartAnimation() {
        return AnimationHolder.pass();
    }

    @Override
    public AnimationHolder getCastFinishAnimation() {
        return SpellAnimations.ANIMATION_INSTANT_CAST;
    }

    private double getRange(int spellLevel, LivingEntity entity) {
        return 8.0 * getSpellPower(spellLevel, entity) / 100.0;
    }

    private int getRadius(int spellLevel, LivingEntity entity) {
        return Math.max(1, 1 + (int) Math.floor(getSpellPower(spellLevel, entity) / 100.0));
    }

    @Override
    public double getClientBlockTargetingRange(int spellLevel, LivingEntity entity) {
        return getRange(spellLevel, entity);
    }

    @Override
    public final ICastDataSerializable getEmptyCastData() {
        return new EarthForgeCastData();
    }

    @Override
    public final boolean checkPreCastConditions(Level level, int spellLevel, LivingEntity entity, MagicData playerMagicData) {
        var placementPlan = buildPlacementPlan(level, spellLevel, entity);
        if (placementPlan.isEmpty()) {
            sendCantPlaceMessage(entity);
            return false;
        }

        var castData = new EarthForgeCastData();
        castData.centerPos = placementPlan.get().center();
        castData.effectDirection = placementPlan.get().effectDirection();
        castData.radius = placementPlan.get().radius();
        playerMagicData.setAdditionalCastData(castData);
        return true;
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        if (level instanceof ServerLevel serverLevel) {
            var placementPlan = restorePlacementPlan(level, playerMagicData);
            if (placementPlan.isEmpty()) {
                placementPlan = buildPlacementPlan(level, spellLevel, entity);
            }
            if (placementPlan.isEmpty()) {
                sendCantPlaceMessage(entity);
            } else {
                var plan = placementPlan.get();
                EarthForgeJobManager.submit(
                        serverLevel,
                        new EarthForgeJob(plan.center(), plan.placeablePositions(), plan.effectDirection(), serverLevel.getGameTime())
                );
            }
        }

        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }

    private Optional<PlacementPlan> buildPlacementPlan(Level level, int spellLevel, LivingEntity entity) {
        var placementTarget = resolvePlacementTarget(level, spellLevel, entity);
        if (placementTarget.isEmpty()) {
            return Optional.empty();
        }

        var hit = placementTarget.get();
        var centerPos = resolveCenterPos(level, hit.hitPos(), hit.hitFace());
        var radius = entity.isShiftKeyDown() ? 1 : getRadius(spellLevel, entity);
        var placeablePositions = collectPlaceablePositions(level, centerPos, hit.hitFace(), radius);
        if (placeablePositions.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(new PlacementPlan(centerPos, placeablePositions, hit.hitFace(), radius));
    }

    private Optional<PlacementPlan> restorePlacementPlan(Level level, MagicData playerMagicData) {
        if (!(playerMagicData.getAdditionalCastData() instanceof EarthForgeCastData castData)) {
            return Optional.empty();
        }
        if (castData.centerPos == null || castData.effectDirection == null || castData.radius <= 0) {
            return Optional.empty();
        }

        var placeablePositions = collectPlaceablePositions(level, castData.centerPos, castData.effectDirection, castData.radius);
        if (placeablePositions.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(new PlacementPlan(castData.centerPos, placeablePositions, castData.effectDirection, castData.radius));
    }

    private Optional<PlacementTarget> resolvePlacementTarget(Level level, int spellLevel, LivingEntity entity) {
        var clientTarget = BlockTargetingHelper.getValidatedPendingTarget(level, entity, getSpellResource(), getRange(spellLevel, entity));
        if (clientTarget.isPresent()) {
            var target = clientTarget.get();
            return Optional.of(new PlacementTarget(target.getHitBlockPos(), target.getHitFace()));
        }

        return raycastTargetBlock(level, spellLevel, entity)
                .map(hit -> new PlacementTarget(hit.getBlockPos(), hit.getDirection()));
    }

    private Optional<BlockHitResult> raycastTargetBlock(Level level, int spellLevel, LivingEntity entity) {
        var start = entity.getEyePosition(1.0F);
        var end = start.add(entity.getViewVector(1.0F).scale(getRange(spellLevel, entity)));
        var hit = level.clip(new ClipContext(
                start,
                end,
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
                entity
        ));

        if (hit.getType() != HitResult.Type.BLOCK) {
            return Optional.empty();
        }

        return Optional.of(hit);
    }

    private BlockPos resolveCenterPos(Level level, BlockPos hitPos, Direction hitFace) {
        var hitState = level.getBlockState(hitPos);
        if (hitState.canBeReplaced()) {
            return hitPos.immutable();
        }

        return hitPos.relative(hitFace).immutable();
    }

    private List<BlockPos> collectPlaceablePositions(Level level, BlockPos centerPos, Direction normal, int radius) {
        var positions = new ArrayList<BlockPos>();
        var span = Math.max(0, radius - 1);
        for (var axisA = -span; axisA <= span; axisA++) {
            for (var axisB = -span; axisB <= span; axisB++) {
                var pos = offsetOnParallelPlane(centerPos, normal, axisA, axisB);
                if (!EarthForgePlacementRules.canReplaceWithDirt(level.getBlockState(pos))) {
                    continue;
                }
                positions.add(pos.immutable());
            }
        }

        return positions;
    }

    private static BlockPos offsetOnParallelPlane(BlockPos centerPos, Direction normal, int axisA, int axisB) {
        return switch (normal.getAxis()) {
            case X -> centerPos.offset(0, axisA, axisB);
            case Y -> centerPos.offset(axisA, 0, axisB);
            case Z -> centerPos.offset(axisA, axisB, 0);
        };
    }

    private void sendCantPlaceMessage(LivingEntity entity) {
        if (entity instanceof ServerPlayer serverPlayer) {
            serverPlayer.connection.send(new ClientboundSetActionBarTextPacket(
                    Component.translatable("ui.apprenticecodex.cant_place", this.getDisplayName(serverPlayer))
                            .withStyle(ChatFormatting.RED)
            ));
        }
    }

    private record PlacementPlan(BlockPos center, List<BlockPos> placeablePositions, Direction effectDirection, int radius) {
    }

    private record PlacementTarget(BlockPos hitPos, Direction hitFace) {
    }

    public static class EarthForgeCastData implements ICastDataSerializable {
        private BlockPos centerPos;
        private Direction effectDirection;
        private int radius;

        @Override
        public void writeToBuffer(FriendlyByteBuf friendlyByteBuf) {
            var center = centerPos;
            var direction = effectDirection;
            var castRadius = radius;
            friendlyByteBuf.writeBoolean(center != null && direction != null && castRadius > 0);
            if (center == null || direction == null || castRadius <= 0) {
                return;
            }

            friendlyByteBuf.writeBlockPos(center);
            friendlyByteBuf.writeInt(direction.get3DDataValue());
            friendlyByteBuf.writeInt(castRadius);
        }

        @Override
        public void readFromBuffer(FriendlyByteBuf friendlyByteBuf) {
            if (!friendlyByteBuf.readBoolean()) {
                reset();
                return;
            }

            centerPos = friendlyByteBuf.readBlockPos();
            effectDirection = Direction.from3DDataValue(friendlyByteBuf.readInt());
            radius = friendlyByteBuf.readInt();
        }

        @Override
        public void reset() {
            centerPos = null;
            effectDirection = null;
            radius = 0;
        }

        @Override
        public CompoundTag serializeNBT() {
            var center = centerPos;
            var direction = effectDirection;
            var castRadius = radius;
            var tag = new CompoundTag();
            if (center == null || direction == null || castRadius <= 0) {
                return tag;
            }

            tag.putInt("CenterX", center.getX());
            tag.putInt("CenterY", center.getY());
            tag.putInt("CenterZ", center.getZ());
            tag.putInt("EffectDirection", direction.get3DDataValue());
            tag.putInt("Radius", castRadius);
            return tag;
        }

        @Override
        public void deserializeNBT(CompoundTag nbt) {
            if (!nbt.contains("CenterX")) {
                reset();
                return;
            }

            centerPos = new BlockPos(nbt.getInt("CenterX"), nbt.getInt("CenterY"), nbt.getInt("CenterZ"));
            effectDirection = Direction.from3DDataValue(nbt.getInt("EffectDirection"));
            radius = nbt.getInt("Radius");
        }
    }
}
