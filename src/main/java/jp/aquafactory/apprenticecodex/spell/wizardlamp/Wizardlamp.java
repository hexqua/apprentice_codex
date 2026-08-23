package jp.aquafactory.apprenticecodex.spell.wizardlamp;

import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.spells.*;
import io.redspace.ironsspellbooks.api.util.AnimationHolder;
import io.redspace.ironsspellbooks.api.util.Utils;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.registry.BlockRegistry;
import jp.aquafactory.apprenticecodex.registry.SoundRegistry;
import jp.aquafactory.apprenticecodex.spell.IClientBlockTargetCaptureSpell;
import jp.aquafactory.apprenticecodex.spell.IClientBlockTargetingSpell;
import jp.aquafactory.apprenticecodex.spell.IChargecastStaffbowIncompatibleSpell;
import jp.aquafactory.apprenticecodex.utility.AudioTools;
import jp.aquafactory.apprenticecodex.utility.BlockTargetData;
import jp.aquafactory.apprenticecodex.utility.BlockTargetingHelper;
import jp.aquafactory.apprenticecodex.utility.BlockTools;
import jp.aquafactory.apprenticecodex.utility.ClientBlockTargetingHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
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
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Optional;

public class Wizardlamp extends AbstractSpell implements IClientBlockTargetingSpell, IClientBlockTargetCaptureSpell,
        IChargecastStaffbowIncompatibleSpell {
    private final ResourceLocation spellId = ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "wizardlamp");

    private final DefaultConfig config = new DefaultConfig()
            .setMinRarity(SpellRarity.RARE)
            .setSchoolResource(SchoolRegistry.HOLY_RESOURCE)
            .setMaxLevel(1)
            .setCooldownSeconds(0.5)
            .build();

    public Wizardlamp() {
        baseSpellPower = 0;
        spellPowerPerLevel = 0;
        baseManaCost = 50;
        manaCostPerLevel = 0;
        castTime = 0;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.irons_spellbooks.distance", Utils.stringTruncation(getRange(), 1))
        );
    }

    private double getRange(){
        return 6;
    }

    @Override
    public double getClientBlockTargetingRange(int spellLevel, LivingEntity entity) {
        return getRange();
    }

    @Override
    public BlockTargetData captureClientBlockTarget(Player player, int spellLevel) {
        return ClientBlockTargetingHelper.captureRaycastTarget(player, getRange());
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
    public Optional<SoundEvent> getCastFinishSound() {
        return Optional.of(SoundRegistry.SET_MAGE_LIGHT_TORCH.get());
    }

    @Override
    public AnimationHolder getCastStartAnimation() {
        return SpellAnimations.ANIMATION_INSTANT_CAST;
    }

    @Override
    public ICastDataSerializable getEmptyCastData() {
        return new WizardlampCastData();
    }

    @Override
    public boolean checkPreCastConditions(Level level, int spellLevel, LivingEntity entity, MagicData playerMagicData) {
        var placePos = findPlacePos(level, spellLevel, entity);
        if (placePos.isEmpty()) {
            if (entity instanceof ServerPlayer serverPlayer) {
                serverPlayer.connection.send(new ClientboundSetActionBarTextPacket(
                        Component.translatable("ui.apprenticecodex.cant_place", getDisplayName(serverPlayer))
                                .withStyle(ChatFormatting.RED)
                ));
            }
            return false;
        }

        var castData = new WizardlampCastData();
        castData.position = placePos.get();
        playerMagicData.setAdditionalCastData(castData);
        return true;
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        BlockPos placePos = null;
        if (playerMagicData.getAdditionalCastData() instanceof WizardlampCastData castData) {
            placePos = castData.position;
        }
        if (placePos == null) {
            placePos = findPlacePos(level, spellLevel, entity).orElse(null);
        }

        var lanternBlock = BlockRegistry.WIZARDLAMP_LANTERN.get();
        var lanternState = placePos == null
                ? lanternBlock.defaultBlockState()
                : lanternBlock.defaultBlockState().setValue(
                        WizardlampLanternBlock.WATERLOGGED,
                        level.getFluidState(placePos).getType() == Fluids.WATER
                );
        if (placePos != null && BlockTools.tryPlaceBlockByEntity(
                level,
                entity,
                placePos,
                lanternState,
                Direction.UP
        )) {
            AudioTools.playSoundFromPosition(level, placePos.getCenter(), SoundEvents.WOOD_PLACE, SoundSource.BLOCKS);
            if (level instanceof ServerLevel serverLevel) {
                var position = placePos.getCenter();
                serverLevel.sendParticles(ParticleTypes.END_ROD, position.x, position.y, position.z,
                        4, 0.1, 0.1, 0.1, 0.01);
                serverLevel.sendParticles(ParticleTypes.FIREWORK, position.x, position.y, position.z,
                        2, 0.1, 0.1, 0.1, 0.02);
            }
        }

        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }

    private Optional<BlockPos> findPlacePos(Level level, int spellLevel, LivingEntity entity) {
        if (level.isClientSide && entity instanceof Player player) {
            return resolveClientPlacePos(level, entity, captureClientBlockTarget(player, spellLevel), getRange());
        }

        var clientTarget = BlockTargetingHelper.getPendingTargetForCustomValidation(
                level,
                entity,
                getSpellResource()
        );
        if (clientTarget.isPresent()) {
            return resolveClientPlacePos(level, entity, clientTarget.get(), getRange());
        }

        return BlockTools.findPlacePos(level, entity, getRange())
                .map(BlockTools.PlaceData::pos)
                .filter(pos -> canPlaceAt(level, pos));
    }

    public static Optional<BlockPos> resolveClientPlacePos(Level level, LivingEntity entity,
                                                            BlockTargetData targetData, double range) {
        if (targetData == null || !targetData.hasTarget() || targetData.getPlacePos() == null || range <= 0.0) {
            return Optional.empty();
        }

        var eyePosition = entity.getEyePosition(1.0F);
        var requestedPosition = Vec3.atCenterOf(targetData.getPlacePos());
        var offset = requestedPosition.subtract(eyePosition);
        var resolvedPos = targetData.getPlacePos();
        if (offset.lengthSqr() > range * range) {
            if (offset.lengthSqr() < 1.0E-8) {
                return Optional.empty();
            }
            resolvedPos = BlockPos.containing(eyePosition.add(offset.normalize().scale(range)));
        }

        return canPlaceAt(level, resolvedPos) ? Optional.of(resolvedPos.immutable()) : Optional.empty();
    }

    private static boolean canPlaceAt(Level level, BlockPos pos) {
        if (!level.isInWorldBounds(pos) || !level.getBlockState(pos).canBeReplaced()) {
            return false;
        }
        return BlockRegistry.WIZARDLAMP_LANTERN.get().defaultBlockState().canSurvive(level, pos);
    }

    public static class WizardlampCastData implements ICastDataSerializable {
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
                tag.putLong("Position", position.asLong());
            }
            return tag;
        }

        @Override
        public void deserializeNBT(CompoundTag nbt) {
            position = nbt.contains("Position") ? BlockPos.of(nbt.getLong("Position")) : null;
        }
    }
}
