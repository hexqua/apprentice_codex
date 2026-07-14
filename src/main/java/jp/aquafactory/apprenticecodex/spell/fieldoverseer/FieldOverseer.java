package jp.aquafactory.apprenticecodex.spell.fieldoverseer;

import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.CastType;
import io.redspace.ironsspellbooks.api.spells.ICastDataSerializable;
import io.redspace.ironsspellbooks.api.spells.SpellAnimations;
import io.redspace.ironsspellbooks.api.spells.SpellRarity;
import io.redspace.ironsspellbooks.api.util.AnimationHolder;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.capabilities.magic.RecastInstance;
import io.redspace.ironsspellbooks.capabilities.magic.RecastResult;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.config.ApprenticeCodexServerConfig;
import jp.aquafactory.apprenticecodex.config.DamageMultiplierKey;
import jp.aquafactory.apprenticecodex.registry.EntityRegistry;
import jp.aquafactory.apprenticecodex.registry.SoundRegistry;
import jp.aquafactory.apprenticecodex.spell.ClientPlacementPreviewData;
import jp.aquafactory.apprenticecodex.spell.IClientBlockTargetCaptureSpell;
import jp.aquafactory.apprenticecodex.spell.IClientBlockTargetingSpell;
import jp.aquafactory.apprenticecodex.spell.IClientPlacementPreviewSpell;
import jp.aquafactory.apprenticecodex.spell.PlacementHelper;
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
import java.util.UUID;

public class FieldOverseer extends AbstractSpell implements IClientBlockTargetingSpell,
        IClientBlockTargetCaptureSpell, IClientPlacementPreviewSpell {
    private final ResourceLocation spellId = ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "field_overseer");

    private final DefaultConfig config = new DefaultConfig()
            .setMinRarity(SpellRarity.UNCOMMON)
            .setSchoolResource(SchoolRegistry.LIGHTNING_RESOURCE)
            .setMaxLevel(5)
            .setCooldownSeconds(30)
            .build();

    public FieldOverseer() {
        baseSpellPower = 1200;
        spellPowerPerLevel = 200;
        baseManaCost = 50;
        manaCostPerLevel = 20;
        castTime = 40;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.irons_spellbooks.damage", Utils.stringTruncation(getDamage(spellLevel, caster), 2)),
                Component.translatable("ui.irons_spellbooks.radius", Utils.stringTruncation(getRadius(), 0)),
                Component.translatable("ui.irons_spellbooks.hp", getStaffHealth(spellLevel)),
                Component.translatable("ui.apprenticecodex.staff_turret_spell.shot_mana_cost", getConsumeManaPerAttack(spellLevel)),
                Component.translatable("ui.irons_spellbooks.duration", Utils.timeFromTicks(getDuration(), 1))
        );
    }

    public float getDamage(int spellLevel, LivingEntity entity) {
        var rawDamage = getSpellPower(spellLevel, entity) / 100.0f;
        return rawDamage * ApprenticeCodexServerConfig.damageMultiplier(DamageMultiplierKey.FIELD_OVERSEER);
    }

    public double getRadius() {
        return 24.0;
    }

    private double getTargetingRange() {
        return 12.0;
    }

    public int getStaffHealth(int spellLevel) {
        return 10 + (spellLevel - 1) * 5;
    }

    public int getConsumeManaPerAttack(int spellLevel) {
        return 40 + (spellLevel - 1) * 20;
    }

    public int getDuration() {
        return 20 * 60 * 5;
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
    public Optional<ClientPlacementPreviewData> getClientPlacementPreview(Level level, LivingEntity entity,
                                                                           int spellLevel, BlockTargetData targetData) {
        var placement = targetData != null
                ? PlacementHelper.resolve(level, targetData, FieldOverseerStaffEntity::makePlacementAabb)
                : PlacementHelper.resolveClientPreview(level, entity, getTargetingRange(), FieldOverseerStaffEntity::makePlacementAabb);
        if (placement.isEmpty() || hasNearbyStaff(level, placement.get())) {
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
        return CastType.LONG;
    }

    @Override
    public Optional<SoundEvent> getCastStartSound() {
        return Optional.of(io.redspace.ironsspellbooks.registries.SoundRegistry.CLOUD_OF_REGEN_LOOP.get());
    }

    @Override
    public Optional<SoundEvent> getCastFinishSound() {
        return Optional.of(SoundRegistry.VANILLA_SUMMON_MAGICAL_ENTITY.get());
    }

    @Override
    public AnimationHolder getCastStartAnimation() {
        return SpellAnimations.ANIMATION_CONTINUOUS_CAST_ONE_HANDED;
    }

    @Override
    public AnimationHolder getCastFinishAnimation() {
        return SpellAnimations.ANIMATION_INSTANT_CAST;
    }

    @Override
    public int getRecastCount(int spellLevel, LivingEntity entity) {
        return 2;
    }

    @Override
    public ICastDataSerializable getEmptyCastData() {
        return new FieldOverseerCastData();
    }

    @Override
    public boolean checkPreCastConditions(Level level, int spellLevel, LivingEntity entity, MagicData playerMagicData) {
        if (playerMagicData.getPlayerRecasts().hasRecastForSpell(this)) {
            return true;
        }
        var placement = PlacementHelper.resolveServer(
                level, entity, getSpellResource(), getTargetingRange(), FieldOverseerStaffEntity::makePlacementAabb);
        if (placement.isEmpty() || hasNearbyStaff(level, placement.get())) {
            sendCantPlaceMessage(entity);
            return false;
        }
        var castData = new FieldOverseerCastData();
        castData.position = placement.get().blockPos();
        playerMagicData.setAdditionalCastData(castData);
        return true;
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource,
                       MagicData playerMagicData) {
        if (level instanceof ServerLevel serverLevel) {
            var recasts = playerMagicData.getPlayerRecasts();
            if (!recasts.hasRecastForSpell(this)) {
                var placement = restorePlacement(level, playerMagicData)
                        .or(() -> PlacementHelper.resolveServer(
                                level, entity, getSpellResource(), getTargetingRange(), FieldOverseerStaffEntity::makePlacementAabb));
                if (placement.isEmpty() || hasNearbyStaff(level, placement.get())) {
                    sendCantPlaceMessage(entity);
                } else {
                    var staff = new FieldOverseerStaffEntity(EntityRegistry.FIELD_OVERSEER_STAFF.get(), serverLevel);
                    staff.setOwner(entity);
                    staff.configure(
                            placement.get().blockPos(),
                            getDamage(spellLevel, entity),
                            getRadius(),
                            getConsumeManaPerAttack(spellLevel),
                            (float) entity.getAttributeValue(AttributeRegistry.MAX_MANA),
                            getStaffHealth(spellLevel)
                    );
                    staff.moveTo(placement.get().center().x, placement.get().center().y, placement.get().center().z,
                            entity.getYRot(), 0.0F);
                    var castData = new FieldOverseerCastData();
                    FieldOverseerManager.initialize(
                            entity, staff, placement.get().blockPos(), getDuration(), castData);
                    serverLevel.addFreshEntity(staff);
                    recasts.addRecast(new RecastInstance(
                            getSpellId(), spellLevel, getRecastCount(spellLevel, entity), getDuration(), castSource, castData),
                            playerMagicData);
                }
            }
        }
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }

    @Override
    public void onRecastFinished(ServerPlayer serverPlayer, RecastInstance recastInstance, RecastResult recastResult,
                                 ICastDataSerializable castDataSerializable) {
        if (FieldOverseerManager.finishRecast(serverPlayer, recastInstance, recastResult, castDataSerializable)) {
            super.onRecastFinished(serverPlayer, recastInstance, recastResult, castDataSerializable);
        }
    }

    private boolean hasNearbyStaff(Level level, PlacementHelper.PlacementResult placement) {
        return !level.getEntitiesOfClass(
                FieldOverseerStaffEntity.class, placement.placementBox().inflate(0.1D)).isEmpty();
    }

    private Optional<PlacementHelper.PlacementResult> restorePlacement(Level level, MagicData playerMagicData) {
        if (!(playerMagicData.getAdditionalCastData() instanceof FieldOverseerCastData castData)
                || castData.position == null) {
            return Optional.empty();
        }
        var targetData = new BlockTargetData();
        var hitPos = castData.position.below();
        targetData.setTarget(hitPos, net.minecraft.core.Direction.UP, castData.position.getCenter(),
                castData.position, net.minecraft.core.Direction.DOWN);
        return PlacementHelper.resolve(level, targetData, FieldOverseerStaffEntity::makePlacementAabb);
    }

    private void sendCantPlaceMessage(LivingEntity entity) {
        if (entity instanceof ServerPlayer serverPlayer) {
            serverPlayer.connection.send(new ClientboundSetActionBarTextPacket(
                    Component.translatable("ui.apprenticecodex.cant_place", getDisplayName(serverPlayer))
                            .withStyle(ChatFormatting.RED)
            ));
        }
    }

    public static class FieldOverseerCastData implements ICastDataSerializable {
        private BlockPos position;
        private UUID staffUuid;
        private ResourceLocation dimension;

        void bindStaff(FieldOverseerStaffEntity staff, BlockPos position) {
            this.position = position.immutable();
            staffUuid = staff.getUUID();
            dimension = staff.level().dimension().location();
        }

        boolean matches(FieldOverseerStaffEntity staff) {
            return staffUuid != null
                    && staffUuid.equals(staff.getUUID())
                    && dimension != null
                    && dimension.equals(staff.level().dimension().location());
        }

        UUID getStaffUuid() {
            return staffUuid;
        }

        ResourceLocation getDimension() {
            return dimension;
        }

        @Override
        public void writeToBuffer(FriendlyByteBuf buffer) {
            buffer.writeBoolean(position != null);
            if (position != null) buffer.writeBlockPos(position);
            buffer.writeBoolean(staffUuid != null);
            if (staffUuid != null) buffer.writeUUID(staffUuid);
            buffer.writeBoolean(dimension != null);
            if (dimension != null) buffer.writeResourceLocation(dimension);
        }

        @Override
        public void readFromBuffer(FriendlyByteBuf buffer) {
            position = buffer.readBoolean() ? buffer.readBlockPos() : null;
            staffUuid = buffer.readBoolean() ? buffer.readUUID() : null;
            dimension = buffer.readBoolean() ? buffer.readResourceLocation() : null;
        }

        @Override
        public void reset() {
            position = null;
            staffUuid = null;
            dimension = null;
        }

        @Override
        public CompoundTag serializeNBT(HolderLookup.Provider provider) {
            var tag = super.serializeNBT(provider);
            if (position != null) tag.putLong("Position", position.asLong());
            if (staffUuid != null) tag.putUUID("Staff", staffUuid);
            if (dimension != null) tag.putString("Dimension", dimension.toString());
            return tag;
        }

        @Override
        public void deserializeNBT(HolderLookup.Provider provider, CompoundTag tag) {
            super.deserializeNBT(provider, tag);
            position = tag.contains("Position") ? BlockPos.of(tag.getLong("Position")) : null;
            staffUuid = tag.hasUUID("Staff") ? tag.getUUID("Staff") : null;
            dimension = tag.contains("Dimension") ? ResourceLocation.tryParse(tag.getString("Dimension")) : null;
        }
    }
}
