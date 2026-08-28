package jp.aquafactory.apprenticecodex.spell.totemofpermafrost;

import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.CastType;
import io.redspace.ironsspellbooks.api.spells.ICastDataSerializable;
import io.redspace.ironsspellbooks.api.spells.SpellRarity;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.capabilities.magic.RecastInstance;
import io.redspace.ironsspellbooks.capabilities.magic.RecastResult;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.config.ApprenticeCodexServerConfig;
import jp.aquafactory.apprenticecodex.config.DamageMultiplierKey;
import jp.aquafactory.apprenticecodex.registry.EntityRegistry;
import jp.aquafactory.apprenticecodex.spell.ClientPlacementPreviewData;
import jp.aquafactory.apprenticecodex.spell.IClientBlockTargetCaptureSpell;
import jp.aquafactory.apprenticecodex.spell.IClientBlockTargetingSpell;
import jp.aquafactory.apprenticecodex.spell.IClientPlacementPreviewSpell;
import jp.aquafactory.apprenticecodex.spell.PlacementHelper;
import jp.aquafactory.apprenticecodex.utility.BlockTargetData;
import jp.aquafactory.apprenticecodex.utility.AudioTools;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class TotemOfPermafrost extends AbstractSpell implements IClientBlockTargetingSpell, IClientBlockTargetCaptureSpell, IClientPlacementPreviewSpell {
    private final ResourceLocation spellId = ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "totem_of_permafrost");

    private final DefaultConfig config = new DefaultConfig()
            .setMinRarity(SpellRarity.UNCOMMON)
            .setSchoolResource(SchoolRegistry.ICE_RESOURCE)
            .setMaxLevel(5)
            .setCooldownSeconds(12)
            .build();

    public TotemOfPermafrost() {
        baseSpellPower = 200;
        spellPowerPerLevel = 150;
        baseManaCost = 30;
        manaCostPerLevel = 15;
        castTime = 0;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.irons_spellbooks.aoe_damage", Utils.stringTruncation(getDamage(spellLevel, caster), 2)),
                Component.translatable("ui.irons_spellbooks.radius", Utils.stringTruncation(getRadius(), 0)),
                Component.translatable("ui.irons_spellbooks.slowness_effect", getSlownessAmplifier(spellLevel, caster) + 1),
                Component.translatable("ui.irons_spellbooks.duration", Utils.timeFromTicks(getDuration(), 1))
        );
    }

    private float getDamage(int spellLevel, LivingEntity entity) {
        var rawDamage = getSpellPower(spellLevel, entity) / 100.0f;
        return rawDamage * ApprenticeCodexServerConfig.damageMultiplier(DamageMultiplierKey.TOTEM_OF_PERMAFROST);
    }

    private int getSlownessAmplifier(int spellLevel, LivingEntity caster) {
        return Math.min(2, Math.round(getSpellPower(spellLevel, caster) / 500.0f));
    }

    private double getRadius() {
        // ブレスの置き換えのように使う想定のため、範囲強化は想定されていない.
        return 3;
    }

    private double getTargetingRange() {
        return 8.0;
    }

    private int getDuration() {
        // 二重に総火力が伸びて分かりづらくなるため、時間は固定化.
        return 20 * 6;
    }

    @Override
    public double getClientBlockTargetingRange(int spellLevel, LivingEntity entity) {
        return getTargetingRange();
    }

    @Override
    public BlockTargetData captureClientBlockTarget(Player player, int spellLevel) {
        return PlacementHelper.captureClientTarget(player, getClientBlockTargetingRange(spellLevel, player));
    }

    @Override
    public Optional<ClientPlacementPreviewData> getClientPlacementPreview(Level level, LivingEntity entity, int spellLevel, BlockTargetData targetData) {
        var placement = targetData != null
                ? PlacementHelper.resolve(level, targetData, TotemOfPermafrostTotemEntity::makePlacementAabb)
                : PlacementHelper.resolveClientPreview(level, entity, getTargetingRange(), TotemOfPermafrostTotemEntity::makePlacementAabb);
        if (placement.isEmpty() || hasNearbyTotem(level, placement.get())) {
            return Optional.empty();
        }

        return Optional.of(ClientPlacementPreviewData.singleBlockColumn(placement.get().center()));
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
        return Optional.empty();
    }

    @Override
    public int getRecastCount(int spellLevel, LivingEntity entity) {
        return 2;
    }

    @Override
    public ICastDataSerializable getEmptyCastData() {
        return new TotemOfPermafrostCastData();
    }

    @Override
    public boolean checkPreCastConditions(Level level, int spellLevel, LivingEntity entity, MagicData playerMagicData) {
        if (playerMagicData.getPlayerRecasts().hasRecastForSpell(this)) {
            return true;
        }

        var placement = PlacementHelper.resolveServer(level, entity, getSpellResource(), getTargetingRange(), TotemOfPermafrostTotemEntity::makePlacementAabb);
        if (placement.isEmpty() || hasNearbyTotem(level, placement.get())) {
            sendCantPlaceMessage(entity);
            return false;
        }

        var castData = new TotemOfPermafrostCastData();
        castData.position = placement.get().blockPos();
        playerMagicData.setAdditionalCastData(castData);
        return true;
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        if (level instanceof ServerLevel serverLevel) {
            var recasts = playerMagicData.getPlayerRecasts();
            if (recasts.hasRecastForSpell(this)) {
                removeStoredTotem(serverLevel, recasts.getRecastInstance(getSpellId()).getCastData());
            } else {
                var placement = restorePlacement(level, playerMagicData)
                        .or(() -> PlacementHelper.resolveServer(level, entity, getSpellResource(), getTargetingRange(), TotemOfPermafrostTotemEntity::makePlacementAabb));
                if (placement.isEmpty() || hasNearbyTotem(level, placement.get())) {
                    sendCantPlaceMessage(entity);
                } else {
                    var totem = new TotemOfPermafrostTotemEntity(EntityRegistry.TOTEM_OF_PERMAFROST_TOTEM.get(), serverLevel);
                    totem.setOwner(entity);
                    totem.setAnchorPos(placement.get().blockPos());
                    totem.setDamage(getDamage(spellLevel, entity));
                    totem.setSlownessAmplifier(getSlownessAmplifier(spellLevel, entity));
                    totem.setRadius(getRadius());
                    var totemYaw = entity.getYRot() + 180.0f;
                    totem.moveTo(placement.get().center().x, placement.get().center().y, placement.get().center().z, totemYaw, 0.0f);
                    totem.setYRot(totemYaw);
                    totem.setYHeadRot(totemYaw);
                    totem.setYBodyRot(totemYaw);
                    serverLevel.addFreshEntity(totem);
                    AudioTools.playSoundFromEntity(
                            serverLevel,
                            totem,
                            jp.aquafactory.apprenticecodex.registry.SoundRegistry.VANILLA_INSCRIBE_MANA.get(),
                            SoundSource.PLAYERS,
                            0.9f,
                            1.0f,
                            0.04f
                    );

                    var castData = new TotemOfPermafrostCastData();
                    castData.position = placement.get().blockPos();
                    castData.totemUuid = totem.getUUID();
                    var recastInstance = new RecastInstance(
                            getSpellId(),
                            spellLevel,
                            getRecastCount(spellLevel, entity),
                            getDuration(),
                            castSource,
                            castData
                    );
                    recasts.addRecast(recastInstance, playerMagicData);
                }
            }
        }

        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }

    @Override
    public void onRecastFinished(ServerPlayer serverPlayer, RecastInstance recastInstance, RecastResult recastResult,
                                 ICastDataSerializable castDataSerializable) {
        removeStoredTotem(serverPlayer.serverLevel(), castDataSerializable);
        super.onRecastFinished(serverPlayer, recastInstance, recastResult, castDataSerializable);
    }

    private Optional<PlacementHelper.PlacementResult> restorePlacement(Level level, MagicData playerMagicData) {
        if (!(playerMagicData.getAdditionalCastData() instanceof TotemOfPermafrostCastData castData) || castData.position == null) {
            return Optional.empty();
        }

        var targetData = new BlockTargetData();
        var hitPos = castData.position.below();
        targetData.setTarget(hitPos, net.minecraft.core.Direction.UP, castData.position.getCenter(), castData.position, net.minecraft.core.Direction.DOWN);
        return PlacementHelper.resolve(level, targetData, TotemOfPermafrostTotemEntity::makePlacementAabb);
    }

    private boolean hasNearbyTotem(Level level, PlacementHelper.PlacementResult placement) {
        return !level.getEntitiesOfClass(TotemOfPermafrostTotemEntity.class, placement.placementBox().inflate(0.1)).isEmpty();
    }

    private void removeStoredTotem(ServerLevel level, ICastDataSerializable castDataSerializable) {
        if (!(castDataSerializable instanceof TotemOfPermafrostCastData castData) || castData.totemUuid == null) {
            return;
        }

        var entity = level.getEntity(castData.totemUuid);
        if (entity instanceof TotemOfPermafrostTotemEntity totem) {
            totem.discard();
        }
    }

    private void sendCantPlaceMessage(LivingEntity entity) {
        if (entity instanceof ServerPlayer serverPlayer) {
            serverPlayer.connection.send(new ClientboundSetActionBarTextPacket(
                    Component.translatable("ui.apprenticecodex.cant_place", this.getDisplayName(serverPlayer))
                            .withStyle(ChatFormatting.RED)
            ));
        }
    }

    public static class TotemOfPermafrostCastData implements ICastDataSerializable {
        private BlockPos position;
        private UUID totemUuid;

        @Override
        public void writeToBuffer(FriendlyByteBuf friendlyByteBuf) {
            friendlyByteBuf.writeBoolean(position != null);
            if (position != null) {
                friendlyByteBuf.writeBlockPos(position);
            }
            friendlyByteBuf.writeBoolean(totemUuid != null);
            if (totemUuid != null) {
                friendlyByteBuf.writeUUID(totemUuid);
            }
        }

        @Override
        public void readFromBuffer(FriendlyByteBuf friendlyByteBuf) {
            position = friendlyByteBuf.readBoolean() ? friendlyByteBuf.readBlockPos() : null;
            totemUuid = friendlyByteBuf.readBoolean() ? friendlyByteBuf.readUUID() : null;
        }

        @Override
        public void reset() {
            position = null;
            totemUuid = null;
        }

        @Override
        public CompoundTag serializeNBT() {
            var tag = new CompoundTag();
            if (position != null) {
                tag.putInt("PositionX", position.getX());
                tag.putInt("PositionY", position.getY());
                tag.putInt("PositionZ", position.getZ());
            }
            if (totemUuid != null) {
                tag.putUUID("TotemUUID", totemUuid);
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
            totemUuid = nbt.hasUUID("TotemUUID") ? nbt.getUUID("TotemUUID") : null;
        }
    }
}
