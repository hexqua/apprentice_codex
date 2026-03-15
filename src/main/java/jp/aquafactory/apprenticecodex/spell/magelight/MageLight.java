package jp.aquafactory.apprenticecodex.spell.magelight;

import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.spells.*;
import io.redspace.ironsspellbooks.api.util.AnimationHolder;
import io.redspace.ironsspellbooks.api.util.Utils;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.registry.BlockRegistry;
import jp.aquafactory.apprenticecodex.registry.SoundRegistry;
import jp.aquafactory.apprenticecodex.utility.AudioTools;
import jp.aquafactory.apprenticecodex.utility.BlockTargetingHelper;
import jp.aquafactory.apprenticecodex.utility.BlockTools;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
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

import java.util.List;
import java.util.Optional;

public class MageLight extends AbstractSpell implements jp.aquafactory.apprenticecodex.spell.IClientBlockTargetingSpell {
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
        manaCostPerLevel = 4;
        castTime = 0;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.irons_spellbooks.distance", Utils.stringTruncation(getRange(spellLevel, caster), 1))
        );
    }

    private double getRange(int spellLevel, LivingEntity entity){
        return 8 * getSpellPower(spellLevel, entity) / 100.0;
    }

    @Override
    public double getClientBlockTargetingRange(int spellLevel, LivingEntity entity) {
        return getRange(spellLevel, entity);
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
        var placePos = findPlacePos(level, spellLevel, entity);
        if (placePos.isEmpty()) {
            if (entity instanceof ServerPlayer serverPlayer) {
                serverPlayer.connection.send(new ClientboundSetActionBarTextPacket(Component.translatable("ui.apprenticecodex.cant_place", this.getDisplayName(serverPlayer)).withStyle(ChatFormatting.RED)));
            }
            return false;
        }

        var castData = new MageLightCastData();
        castData.position = placePos.get();
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
            placePos = findPlacePos(level, spellLevel, entity).orElse(null);
        }

        if (placePos != null) {
            level.setBlockAndUpdate(placePos, BlockRegistry.MAGE_LIGHT_TORCH.get().defaultBlockState());
            AudioTools.playSoundFromPosition(level, placePos.getCenter(), SoundEvents.WOOD_PLACE, SoundSource.BLOCKS);

            if (level instanceof ServerLevel server) {
                var position = placePos.getCenter();
                server.sendParticles(ParticleTypes.END_ROD, position.x, position.y, position.z, 4, 0.1, 0.1, 0.1, 0.01);
                server.sendParticles(ParticleTypes.FIREWORK, position.x, position.y, position.z, 2, 0.1, 0.1, 0.1, 0.02);
            }
        }

        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }

    private Optional<BlockPos> findPlacePos(Level level, int spellLevel, LivingEntity entity) {
        var range = getRange(spellLevel, entity);
        var result = BlockTargetingHelper.findClientPlacePos(level, entity, getSpellResource(), range);
        if (result.isEmpty()) {
            result = BlockTools.findPlacePos(level, entity, range);
        }
        if (result.isEmpty()) {
            return Optional.empty();
        }

        var placePos = result.get().pos();
        var torchState = BlockRegistry.MAGE_LIGHT_TORCH.get().defaultBlockState();
        if (!torchState.canSurvive(level, placePos)) {
            return Optional.empty();
        }

        return Optional.of(placePos);
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
