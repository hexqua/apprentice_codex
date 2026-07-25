package jp.aquafactory.apprenticecodex.spell.magelight;

import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.spells.*;
import io.redspace.ironsspellbooks.api.util.AnimationHolder;
import io.redspace.ironsspellbooks.api.util.Utils;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.config.ApprenticeCodexServerConfig;
import jp.aquafactory.apprenticecodex.registry.BlockRegistry;
import jp.aquafactory.apprenticecodex.registry.SoundRegistry;
import jp.aquafactory.apprenticecodex.spell.IChargecastStaffbowIncompatibleSpell;
import jp.aquafactory.apprenticecodex.utility.AudioTools;
import jp.aquafactory.apprenticecodex.utility.BlockTargetingHelper;
import jp.aquafactory.apprenticecodex.utility.BlockTools;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.common.ForgeMod;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

public class MageLight extends AbstractSpell implements jp.aquafactory.apprenticecodex.spell.IClientBlockTargetingSpell,
        IChargecastStaffbowIncompatibleSpell {
    private final ResourceLocation spellId = ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "mage_light");

    private final DefaultConfig config = new DefaultConfig()
            .setMinRarity(SpellRarity.UNCOMMON)
            .setSchoolResource(SchoolRegistry.HOLY_RESOURCE)
            .setMaxLevel(1)
            .setCooldownSeconds(0.5)
            .build();

    public MageLight() {
        baseSpellPower = 100;
        spellPowerPerLevel = 25;
        baseManaCost = 20;
        manaCostPerLevel = 0;
        castTime = 0;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.irons_spellbooks.distance", Utils.stringTruncation(getNormalRange(spellLevel, caster), 1))
        );
    }

    public double getNormalRange(int spellLevel, @Nullable LivingEntity entity) {
        var configuredMaxRange = entity != null && entity.level().isClientSide
                ? MageLightConfigState.maxRange()
                : ApprenticeCodexServerConfig.mageLightMaxRange();
        return Math.max(0.0D, Math.min(8 * getSpellPower(spellLevel, entity) / 100.0, configuredMaxRange));
    }

    public MageLightCastProfile createCastProfile(int spellLevel, LivingEntity entity, double requestedExtendedRange) {
        var normalRange = getNormalRange(spellLevel, entity);
        return new MageLightCastProfile(
                normalRange,
                Math.max(normalRange, requestedExtendedRange),
                getManaCost(spellLevel)
        );
    }

    @Override
    public double getClientBlockTargetingRange(int spellLevel, LivingEntity entity) {
        return getNormalRange(spellLevel, entity);
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
        return Optional.of(SoundRegistry.SET_MAGE_LIGHT_TORCH.get());
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

    @Override
    public final ICastDataSerializable getEmptyCastData() {
        return new MageLightCastData();
    }

    @Override
    public final boolean checkPreCastConditions(Level level, int spellLevel, LivingEntity entity, MagicData playerMagicData) {
        var contextTarget = MageLightCastContext.targetFor(entity);
        var target = contextTarget == null
                ? resolveCastTarget(level, entity, getNormalRange(spellLevel, entity))
                : Optional.of(contextTarget);
        if (target.isEmpty()) {
            if (entity instanceof ServerPlayer serverPlayer) {
                serverPlayer.connection.send(new ClientboundSetActionBarTextPacket(Component.translatable("ui.apprenticecodex.cant_place", this.getDisplayName(serverPlayer)).withStyle(ChatFormatting.RED)));
            }
            return false;
        }

        var castData = new MageLightCastData();
        castData.position = target.get().placePos;
        playerMagicData.setAdditionalCastData(castData);
        return true;
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        BlockPos placePos = null;
        if (playerMagicData.getAdditionalCastData() instanceof MageLightCastData castData) {
            placePos = castData.position;
        }
        if (placePos == null) {
            placePos = resolveCastTarget(level, entity, getNormalRange(spellLevel, entity))
                    .map(CastTarget::placePos)
                    .orElse(null);
        }

        if (placePos != null && BlockTools.tryPlaceBlockByEntity(
                level,
                entity,
                placePos,
                BlockRegistry.MAGE_LIGHT_TORCH.get().defaultBlockState(),
                Direction.UP
        )) {
            AudioTools.playSoundFromPosition(level, placePos.getCenter(), SoundEvents.WOOD_PLACE, SoundSource.BLOCKS);

            if (level instanceof ServerLevel server) {
                var position = placePos.getCenter();
                server.sendParticles(ParticleTypes.END_ROD, position.x, position.y, position.z, 4, 0.1, 0.1, 0.1, 0.01);
                server.sendParticles(ParticleTypes.FIREWORK, position.x, position.y, position.z, 2, 0.1, 0.1, 0.1, 0.02);
            }
        }

        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }

    public Optional<CastTarget> resolveCastTarget(Level level, LivingEntity entity, double range) {
        var pendingTarget = BlockTargetingHelper.getPendingTargetForCustomValidation(
                level,
                entity,
                getSpellResource()
        );
        if (pendingTarget.isPresent()) {
            // アウトラインの面選択を尊重するため再レイキャストせず、通常ブロックリーチ付近の入力だけを受理する。
            var clientTargetRange = Math.min(
                    range,
                    entity.getAttributeValue(ForgeMod.BLOCK_REACH.get()) + 1.0D
            );
            var validated = BlockTargetingHelper.validateTarget(
                    level,
                    entity,
                    clientTargetRange,
                    pendingTarget.get()
            );
            if (validated.isPresent()) {
                var target = validated.get();
                return validatePlacement(
                        level,
                        target.getPlacePos(),
                        entity.getEyePosition(1.0F).distanceTo(target.getHitLocation())
                );
            }
        }

        var start = entity.getEyePosition(1.0F);
        var end = start.add(entity.getViewVector(1.0F).scale(range));
        var hit = level.clip(new ClipContext(
                start,
                end,
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
                entity
        ));
        if (hit.getType() == HitResult.Type.MISS) {
            return Optional.empty();
        }

        var hitPos = hit.getBlockPos();
        var placePos = level.getBlockState(hitPos).canBeReplaced()
                ? hitPos
                : hitPos.relative(hit.getDirection());
        if (!level.getBlockState(placePos).canBeReplaced()) {
            return Optional.empty();
        }
        return validatePlacement(level, placePos, start.distanceTo(hit.getLocation()));
    }

    private Optional<CastTarget> validatePlacement(Level level, BlockPos placePos, double distance) {
        var torchState = BlockRegistry.MAGE_LIGHT_TORCH.get().defaultBlockState();
        if (!torchState.canSurvive(level, placePos)) {
            return Optional.empty();
        }

        return Optional.of(new CastTarget(placePos, distance));
    }

    public record CastTarget(BlockPos placePos, double distance) {
    }

    public static class MageLightCastData implements ICastDataSerializable {
        private BlockPos position;

        @Override
        public void writeToBuffer(FriendlyByteBuf friendlyByteBuf) {
            friendlyByteBuf.writeBoolean(position != null);
            if (position != null) {
                friendlyByteBuf.writeBlockPos(position);
            }
        }

        @Override
        public void readFromBuffer(FriendlyByteBuf friendlyByteBuf) {
            position = friendlyByteBuf.readBoolean() ? friendlyByteBuf.readBlockPos() : null;
        }

        @Override
        public void reset() {
            position = null;
        }

        @Override
        public CompoundTag serializeNBT() {
            var tag = new CompoundTag();
            if (position != null) {
                tag.putInt("PositionX", position.getX());
                tag.putInt("PositionY", position.getY());
                tag.putInt("PositionZ", position.getZ());
            }
            return tag;
        }

        @Override
        public void deserializeNBT(CompoundTag nbt) {
            if (nbt.contains("PositionX")) {
                position = new BlockPos(nbt.getInt("PositionX"), nbt.getInt("PositionY"), nbt.getInt("PositionZ"));
            } else {
                position = null;
            }
        }
    }
}
