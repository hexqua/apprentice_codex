package jp.aquafactory.apprenticecodex.spell.autoturret;

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
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.config.ApprenticeCodexServerConfig;
import jp.aquafactory.apprenticecodex.config.DamageMultiplierKey;
import jp.aquafactory.apprenticecodex.registry.EntityRegistry;
import jp.aquafactory.apprenticecodex.spell.ClientPlacementPreviewData;
import jp.aquafactory.apprenticecodex.spell.IClientBlockTargetCaptureSpell;
import jp.aquafactory.apprenticecodex.spell.IClientPlacementPreviewSpell;
import jp.aquafactory.apprenticecodex.spell.IClientBlockTargetingSpell;
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
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.Optional;

public class AutoTurret extends AbstractSpell implements IClientBlockTargetingSpell, IClientBlockTargetCaptureSpell, IClientPlacementPreviewSpell {
    private final ResourceLocation spellId = ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "auto_turret");

    private final DefaultConfig config = new DefaultConfig()
            .setMinRarity(SpellRarity.RARE)
            .setSchoolResource(SchoolRegistry.EVOCATION_RESOURCE)
            .setMaxLevel(3)
            .setCooldownSeconds(0)
            .build();

    public AutoTurret() {
        baseSpellPower = 100;
        spellPowerPerLevel = 40;
        baseManaCost = 70;
        manaCostPerLevel = 20;
        castTime = 40;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.irons_spellbooks.damage", Utils.stringTruncation(getDamage(spellLevel, caster), 2)),
                Component.translatable("ui.irons_spellbooks.projectile_count", getProjectileCount(spellLevel, caster)),
                Component.translatable("ui.irons_spellbooks.hp", getTurretHealth(spellLevel, caster))
        );
    }

    private float getDamage(int spellLevel, LivingEntity entity) {
        var rawDamage = 4 * getSpellPower(spellLevel, entity) / 100.0f;
        return rawDamage * ApprenticeCodexServerConfig.damageMultiplier(DamageMultiplierKey.AUTO_TURRET);
    }

    private int getProjectileCount(int spellLevel, LivingEntity entity) {
        return Math.round(12 * getSpellPower(spellLevel, entity) / 100.0f);
    }

    private int getTurretHealth(int spellLevel, LivingEntity entity) {
        return 10 + Math.round(5 * getSpellPower(spellLevel, entity) / 100.0f);
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
        return AutoTurretPlacementHelper.captureClientTarget(player, getClientBlockTargetingRange(spellLevel, player));
    }

    @Override
    public Optional<ClientPlacementPreviewData> getClientPlacementPreview(Level level, LivingEntity entity, int spellLevel, BlockTargetData targetData) {
        var placement = targetData != null
                ? AutoTurretPlacementHelper.resolve(level, targetData)
                : AutoTurretPlacementHelper.resolveClientPreview(level, entity, getTargetingRange());
        if (placement.isEmpty() || hasNearbyTurret(level, placement.get())) {
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
        return Optional.of(SoundEvents.ANVIL_USE);
    }

    @Override
    public Optional<SoundEvent> getCastFinishSound() {
        return Optional.of(SoundEvents.ANVIL_PLACE);
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
        return new AutoTurretCastData();
    }

    @Override
    public boolean checkPreCastConditions(Level level, int spellLevel, LivingEntity entity, MagicData playerMagicData) {
        var placement = AutoTurretPlacementHelper.resolveServer(level, entity, getSpellResource(), getTargetingRange());
        if (placement.isEmpty() || hasNearbyTurret(level, placement.get())) {
            sendCantPlaceMessage(entity);
            return false;
        }

        var castData = new AutoTurretCastData();
        castData.position = placement.get().blockPos();
        playerMagicData.setAdditionalCastData(castData);
        return true;
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        if (level instanceof ServerLevel serverLevel) {
            var placement = restorePlacement(level, playerMagicData)
                    .or(() -> AutoTurretPlacementHelper.resolveServer(level, entity, getSpellResource(), getTargetingRange()));
            if (placement.isEmpty() || hasNearbyTurret(level, placement.get())) {
                sendCantPlaceMessage(entity);
            } else {
                var turret = new AutoTurretEntity(EntityRegistry.AUTO_TURRET.get(), serverLevel);
                turret.setOwner(entity);
                turret.setAnchorPos(placement.get().blockPos());
                turret.setDamage(getDamage(spellLevel, entity));
                turret.setRestBulletCount(getProjectileCount(spellLevel, entity));
                turret.setTurretMaxHealth(getTurretHealth(spellLevel, entity));
                turret.moveTo(placement.get().center().x, placement.get().center().y, placement.get().center().z, entity.getYRot(), 0.0f);
                turret.setYRot(entity.getYRot());
                turret.setYHeadRot(entity.getYRot());
                turret.setYBodyRot(entity.getYRot());
                serverLevel.addFreshEntity(turret);
            }
        }

        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }

    private Optional<AutoTurretPlacementHelper.PlacementResult> restorePlacement(Level level, MagicData playerMagicData) {
        if (!(playerMagicData.getAdditionalCastData() instanceof AutoTurretCastData castData) || castData.position == null) {
            return Optional.empty();
        }

        var targetData = new BlockTargetData();
        var hitPos = castData.position.below();
        targetData.setTarget(hitPos, net.minecraft.core.Direction.UP, castData.position.getCenter(), castData.position, net.minecraft.core.Direction.DOWN);
        return AutoTurretPlacementHelper.resolve(level, targetData);
    }

    private boolean hasNearbyTurret(Level level, AutoTurretPlacementHelper.PlacementResult placement) {
        return !level.getEntitiesOfClass(AutoTurretEntity.class, placement.placementBox().inflate(0.1)).isEmpty();
    }

    private void sendCantPlaceMessage(LivingEntity entity) {
        if (entity instanceof ServerPlayer serverPlayer) {
            serverPlayer.connection.send(new ClientboundSetActionBarTextPacket(
                    Component.translatable("ui.apprenticecodex.cant_place", this.getDisplayName(serverPlayer))
                            .withStyle(ChatFormatting.RED)
            ));
        }
    }

    public static class AutoTurretCastData implements ICastDataSerializable {
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
