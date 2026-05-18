package jp.aquafactory.apprenticecodex.spell.demicreatorwings;

import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.spells.*;
import io.redspace.ironsspellbooks.api.util.AnimationHolder;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.capabilities.magic.RecastInstance;
import io.redspace.ironsspellbooks.capabilities.magic.RecastResult;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.config.ApprenticeCodexServerConfig;
import jp.aquafactory.apprenticecodex.registry.SoundRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.Optional;

public class DemicreatorWings extends AbstractSpell {
    private final ResourceLocation spellId = ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "demicreator_wings");

    private final DefaultConfig config = new DefaultConfig()
            .setMinRarity(SpellRarity.EPIC)
            .setSchoolResource(SchoolRegistry.ENDER_RESOURCE)
            .setMaxLevel(3)
            .setCooldownSeconds(120)
            .build();

    public DemicreatorWings() {
        baseSpellPower = 800;
        spellPowerPerLevel = 250;
        baseManaCost = 300;
        manaCostPerLevel = 100;
        castTime = 40;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.irons_spellbooks.distance", Utils.stringTruncation(getActivateArea(spellLevel, caster), 0)),
                Component.translatable("ui.irons_spellbooks.duration", Utils.timeFromTicks(getDuration(spellLevel, caster), 1))
        );
    }

    @Override
    public ResourceLocation getSpellResource() {
        return spellId;
    }

    @Override
    public DefaultConfig getDefaultConfig() {
        return config;
    }

    int getActivateArea(int skillLevel, LivingEntity entity) {
        return 1 + Math.round(getSpellPower(skillLevel, entity) / 100.0f);
    }

    int getDuration(int skillLevel, LivingEntity entity) {
        return 20 * Math.round(30 * getSpellPower(skillLevel, entity) / 800.0f);
    }

    @Override
    public ICastDataSerializable getEmptyCastData() {
        return new DemicreatorWingsCastData();
    }

    @Override
    public int getRecastCount(int spellLevel, LivingEntity entity) {
        return 2;
    }

    @Override
    public CastType getCastType() {
        return CastType.LONG;
    }

    @Override
    public Optional<SoundEvent> getCastStartSound() {
        return Optional.of(SoundRegistry.VANILLA_POWER_TUNING.get());
    }

    @Override
    public Optional<SoundEvent> getCastFinishSound() {
        return Optional.of(SoundRegistry.VANILLA_POWER_ACTIVATE.get());
    }

    @Override
    public AnimationHolder getCastStartAnimation() {
        return SpellAnimations.ANIMATION_LONG_CAST;
    }

    @Override
    public AnimationHolder getCastFinishAnimation() {
        return AnimationHolder.none();
    }

    @Override
    public boolean checkPreCastConditions(Level level, int spellLevel, LivingEntity entity, MagicData playerMagicData) {
        if (!(entity instanceof Player player)) {
            return false;
        }

        var recasts = playerMagicData.getPlayerRecasts();
        if (recasts.hasRecastForSpell(this)) {
            if (player instanceof ServerPlayer serverPlayer
                    && !DemicreatorWingsManager.hasActiveCore(serverPlayer)) {
                var staleRecast = recasts.getRecastInstance(getSpellId());
                if (staleRecast != null) {
                    recasts.removeRecast(staleRecast, RecastResult.USED_ALL_RECASTS);
                }
            } else {
                playerMagicData.setAdditionalCastData(DemicreatorWingsCastData.closeCast());
                return true;
            }
        }

        if (!level.isClientSide && !ApprenticeCodexServerConfig.isDemicreatorWingsDimensionAllowed(level.dimension().location())) {
            sendDimensionNotAllowedMessage(player);
            return false;
        }

        playerMagicData.setAdditionalCastData(DemicreatorWingsCastData.openCast());
        return true;
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        if (entity instanceof ServerPlayer serverPlayer) {
            var castData = playerMagicData.getAdditionalCastData() instanceof DemicreatorWingsCastData data ? data : null;
            if (castData != null && castData.isCloseCast()) {
                DemicreatorWingsManager.deactivate(serverPlayer, true);
            } else {
                DemicreatorWingsManager.activate(serverPlayer, spellLevel, castSource, playerMagicData, this);
            }
        }

        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }

    @Override
    public void onRecastFinished(net.minecraft.server.level.ServerPlayer serverPlayer, RecastInstance recastInstance,
                                 RecastResult recastResult, ICastDataSerializable castDataSerializable) {
        DemicreatorWingsManager.deactivate(serverPlayer, false);
        super.onRecastFinished(serverPlayer, recastInstance, recastResult, castDataSerializable);
    }

    @Override
    public void onServerCastComplete(Level level, int spellLevel, LivingEntity entity, MagicData playerMagicData, boolean cancelled) {
        if (playerMagicData != null) {
            if (playerMagicData.getAdditionalCastData() instanceof DemicreatorWingsCastData castData) {
                castData.reset();
            }
            playerMagicData.setAdditionalCastData(null);
        }

        super.onServerCastComplete(level, spellLevel, entity, playerMagicData, cancelled);
    }

    private void sendDimensionNotAllowedMessage(Player player) {
        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.connection.send(new ClientboundSetActionBarTextPacket(
                    Component.translatable("ui.apprenticecodex.spell.dimension_not_allowed").withStyle(ChatFormatting.RED)
            ));
        }
    }

    private enum DemicreatorWingsMode {
        NONE,
        OPEN,
        CLOSE
    }

    public static class DemicreatorWingsCastData implements ICastDataSerializable {
        private DemicreatorWingsMode mode = DemicreatorWingsMode.NONE;

        public static DemicreatorWingsCastData openCast() {
            var castData = new DemicreatorWingsCastData();
            castData.mode = DemicreatorWingsMode.OPEN;
            return castData;
        }

        public static DemicreatorWingsCastData closeCast() {
            var castData = new DemicreatorWingsCastData();
            castData.mode = DemicreatorWingsMode.CLOSE;
            return castData;
        }

        public boolean isCloseCast() {
            return mode == DemicreatorWingsMode.CLOSE;
        }

        @Override
        public void writeToBuffer(FriendlyByteBuf friendlyByteBuf) {
            friendlyByteBuf.writeEnum(mode);
        }

        @Override
        public void readFromBuffer(FriendlyByteBuf friendlyByteBuf) {
            mode = friendlyByteBuf.readEnum(DemicreatorWingsMode.class);
        }

        @Override
        public void reset() {
            mode = DemicreatorWingsMode.NONE;
        }

        @Override
        public CompoundTag serializeNBT(HolderLookup.Provider provider) {
            var tag = new CompoundTag();
            tag.putString("Mode", mode.name());
            return tag;
        }

        @Override
        public void deserializeNBT(HolderLookup.Provider provider, CompoundTag nbt) {
            mode = nbt.contains("Mode") ? DemicreatorWingsMode.valueOf(nbt.getString("Mode")) : DemicreatorWingsMode.NONE;
        }
    }
}
