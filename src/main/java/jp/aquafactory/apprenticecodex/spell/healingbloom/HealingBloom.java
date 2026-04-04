package jp.aquafactory.apprenticecodex.spell.healingbloom;

import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.CastType;
import io.redspace.ironsspellbooks.api.spells.ICastDataSerializable;
import io.redspace.ironsspellbooks.api.spells.SpellAnimations;
import io.redspace.ironsspellbooks.api.spells.SpellRarity;
import io.redspace.ironsspellbooks.api.util.AnimationHolder;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.registries.SoundRegistry;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.registry.EntityRegistry;
import jp.aquafactory.apprenticecodex.spell.ClientPlacementPreviewData;
import jp.aquafactory.apprenticecodex.spell.IClientBlockTargetCaptureSpell;
import jp.aquafactory.apprenticecodex.spell.IClientBlockTargetingSpell;
import jp.aquafactory.apprenticecodex.spell.IClientPlacementPreviewSpell;
import jp.aquafactory.apprenticecodex.utility.BlockTargetData;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.Optional;

public class HealingBloom extends AbstractSpell implements IClientBlockTargetingSpell, IClientBlockTargetCaptureSpell, IClientPlacementPreviewSpell {
    private final ResourceLocation spellId = ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "healing_bloom");

    private final DefaultConfig config = new DefaultConfig()
            .setMinRarity(SpellRarity.RARE)
            .setSchoolResource(SchoolRegistry.NATURE_RESOURCE)
            .setMaxLevel(1)
            .setCooldownSeconds(180)
            .build();

    public HealingBloom() {
        baseSpellPower = 100;
        spellPowerPerLevel = 100;
        baseManaCost = 100;
        manaCostPerLevel = 100;
        castTime = 60;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.irons_spellbooks.hp", getBloomHealth(spellLevel, caster)),
                Component.translatable("ui.irons_spellbooks.distance", Utils.stringTruncation(getRange(spellLevel, caster), 1)),
                Component.translatable("ui.apprenticecodex.bloom_wither_time", Utils.timeFromTicks(getDuration(), 1))
        );
    }

    private int getBloomHealth(int spellLevel, LivingEntity entity) {
        return Math.round(20 * getSpellPower(spellLevel, entity) / 100.0f);
    }

    private int getRange(int spellLevel, LivingEntity caster) {
        return Math.round(4 * getSpellPower(spellLevel, caster) / 100.0f);
    }

    private int getDuration() {
        return 20 * 60 * 3;
    }

    private double getTargetingRange() {
        return 8.0;
    }

    @Override
    public double getClientBlockTargetingRange(int spellLevel, LivingEntity entity) {
        return getTargetingRange();
    }

    @Override
    public BlockTargetData captureClientBlockTarget(Player player, int spellLevel) {
        return HealingBloomPlacementHelper.captureClientTarget(player, getClientBlockTargetingRange(spellLevel, player));
    }

    @Override
    public Optional<ClientPlacementPreviewData> getClientPlacementPreview(Level level, LivingEntity entity, int spellLevel, BlockTargetData targetData) {
        var placement = targetData != null
                ? HealingBloomPlacementHelper.resolve(level, targetData)
                : HealingBloomPlacementHelper.resolveClientPreview(level, entity, getTargetingRange());
        return placement.map(result -> ClientPlacementPreviewData.singleBlockColumn(result.center()));
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
        return CastType.LONG;
    }

    @Override
    public Optional<SoundEvent> getCastStartSound() {
        return Optional.of(SoundRegistry.CLOUD_OF_REGEN_LOOP.get());
    }

    @Override
    public Optional<SoundEvent> getCastFinishSound() {
        return Optional.of(SoundRegistry.OAKSKIN_CAST.get());
    }

    @Override
    public AnimationHolder getCastStartAnimation() {
        return SpellAnimations.ANIMATION_CONTINUOUS_CAST_ONE_HANDED;
    }

    @Override
    public AnimationHolder getCastFinishAnimation() {
        return AnimationHolder.none();
    }

    @Override
    public ICastDataSerializable getEmptyCastData() {
        return new HealingBloomCastData();
    }

    @Override
    public boolean checkPreCastConditions(Level level, int spellLevel, LivingEntity entity, MagicData playerMagicData) {
        var placement = HealingBloomPlacementHelper.resolveServer(level, entity, getSpellResource(), getTargetingRange());
        if (placement.isEmpty()) {
            sendCantPlaceMessage(entity);
            return false;
        }

        var castData = new HealingBloomCastData();
        castData.position = placement.get().blockPos();
        playerMagicData.setAdditionalCastData(castData);
        return true;
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        if (level instanceof ServerLevel serverLevel) {
            var placement = restorePlacement(level, playerMagicData)
                    .or(() -> HealingBloomPlacementHelper.resolveServer(level, entity, getSpellResource(), getTargetingRange()));
            if (placement.isEmpty()) {
                sendCantPlaceMessage(entity);
            } else {
                var bloom = new HealingBloomEntity(EntityRegistry.HEALING_BLOOM.get(), serverLevel);
                bloom.setOwner(entity);
                bloom.setAnchorPos(placement.get().blockPos());
                bloom.setEffectRange(getRange(spellLevel, entity));
                bloom.setBloomMaxHealth(getBloomHealth(spellLevel, entity));
                bloom.setWitherTime(getDuration());
                bloom.moveTo(placement.get().center().x, placement.get().center().y, placement.get().center().z, entity.getYRot(), 0.0f);
                serverLevel.addFreshEntity(bloom);
            }
        }

        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }

    private Optional<HealingBloomPlacementHelper.PlacementResult> restorePlacement(Level level, MagicData playerMagicData) {
        if (!(playerMagicData.getAdditionalCastData() instanceof HealingBloomCastData castData) || castData.position == null) {
            return Optional.empty();
        }

        return HealingBloomPlacementHelper.resolve(level, castData.position);
    }

    private void sendCantPlaceMessage(LivingEntity entity) {
        if (entity instanceof ServerPlayer serverPlayer) {
            serverPlayer.connection.send(new ClientboundSetActionBarTextPacket(
                    Component.translatable("ui.apprenticecodex.cant_place", this.getDisplayName(serverPlayer))
                            .withStyle(ChatFormatting.RED)
            ));
        }
    }

    public static class HealingBloomCastData implements ICastDataSerializable {
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
        public CompoundTag serializeNBT(HolderLookup.Provider provider) {
            var tag = new CompoundTag();
            if (position != null) {
                tag.putInt("PositionX", position.getX());
                tag.putInt("PositionY", position.getY());
                tag.putInt("PositionZ", position.getZ());
            }
            return tag;
        }

        @Override
        public void deserializeNBT(HolderLookup.Provider provider, CompoundTag nbt) {
            if (nbt.contains("PositionX")) {
                position = new BlockPos(nbt.getInt("PositionX"), nbt.getInt("PositionY"), nbt.getInt("PositionZ"));
            } else {
                position = null;
            }
        }
    }
}
