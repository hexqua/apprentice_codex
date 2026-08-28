package jp.aquafactory.apprenticecodex.spell.servantgaze;

import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.spells.*;
import io.redspace.ironsspellbooks.api.util.AnimationHolder;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.capabilities.magic.RecastInstance;
import io.redspace.ironsspellbooks.capabilities.magic.RecastResult;
import io.redspace.ironsspellbooks.registries.ItemRegistry;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.config.ApprenticeCodexServerConfig;
import jp.aquafactory.apprenticecodex.config.DamageMultiplierKey;
import jp.aquafactory.apprenticecodex.registry.EntityRegistry;
import jp.aquafactory.apprenticecodex.registry.SoundRegistry;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class ServantGaze extends AbstractSpell {
    private final ResourceLocation spellId = ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "servant_gaze");

    private final DefaultConfig config = new DefaultConfig()
            .setMinRarity(SpellRarity.UNCOMMON)
            .setSchoolResource(SchoolRegistry.ENDER_RESOURCE)
            .setMaxLevel(5)
            .setCooldownSeconds(150)
            .build();

    public ServantGaze() {
        baseSpellPower = 500;
        spellPowerPerLevel = 200;
        baseManaCost = 75;
        manaCostPerLevel = 0;
        castTime = 50;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.irons_spellbooks.damage", Utils.stringTruncation(getDamage(spellLevel, caster), 2)),
                Component.translatable("ui.irons_spellbooks.radius", Utils.stringTruncation(getRadius(), 0)),
                Component.translatable("ui.apprenticecodex.staff_turret_spell.shot_mana_cost", getConsumeManaPerAttack(spellLevel)),
                Component.translatable("ui.irons_spellbooks.duration", Utils.timeFromTicks(getDuration(), 1))
        );
    }

    private float getDamage(int spellLevel, LivingEntity entity) {
        var rawDamage = getSpellPower(spellLevel, entity) / 100.0f;
        return rawDamage * ApprenticeCodexServerConfig.damageMultiplier(DamageMultiplierKey.SERVANT_GAZE);
    }

    private double getRadius() {
        // 強化はされない前提
        return 20;
    }

    public int getConsumeManaPerAttack(int spellLevel) {
        return 15 + (spellLevel - 1) * 5;
    }

    public int getDuration() {
        return 20 * 60 * 10;
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
    public int getRecastCount(int spellLevel, @Nullable LivingEntity entity) {
        return 2;
    }

    @Override
    public ICastDataSerializable getEmptyCastData() {
        return new ServantGazeCastData();
    }

    @Override
    public void onRecastFinished(ServerPlayer player, RecastInstance recast, RecastResult result,
                                 ICastDataSerializable rawCastData) {
        ServantGazeManager.finishRecast(player, rawCastData);
        // 10分を自然完走した場合だけ、Greater Conjurer's Talismanの召喚cooldown無効化を適用する。
        if (result != RecastResult.TIMEOUT || !ItemRegistry.GREATER_CONJURERS_TALISMAN.get().isEquippedBy(player)) {
            super.onRecastFinished(player, recast, result, rawCastData);
        }
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource,
                       MagicData playerMagicData) {
        var recasts = playerMagicData.getPlayerRecasts();
        if (!recasts.hasRecastForSpell(this) && level instanceof ServerLevel serverLevel
                && entity instanceof ServerPlayer player) {
            var castData = new ServantGazeCastData(spellLevel, getDamage(spellLevel, entity), getRadius(),
                    getConsumeManaPerAttack(spellLevel));
            var staff = new ServantGazeStaffEntity(EntityRegistry.SERVANT_GAZE_STAFF.get(), serverLevel, player);
            ServantGazeManager.initialize(player, staff, getDuration(), castData);
            if (serverLevel.addFreshEntity(staff)) {
                var recast = new RecastInstance(getSpellId(), spellLevel, getRecastCount(spellLevel, entity),
                        getDuration(), castSource, castData);
                recasts.addRecast(recast, playerMagicData);
            }
        }
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }

    public static class ServantGazeCastData implements ICastDataSerializable {
        private UUID staffUuid;
        private ResourceLocation dimension;
        private int spellLevel;
        private float damage;
        private double radius;
        private int attackManaCost;

        public ServantGazeCastData() {
        }

        public ServantGazeCastData(int spellLevel, float damage, double radius, int attackManaCost) {
            this.spellLevel = spellLevel;
            this.damage = damage;
            this.radius = radius;
            this.attackManaCost = attackManaCost;
        }

        void bindStaff(ServantGazeStaffEntity staff) {
            staffUuid = staff.getUUID();
            dimension = staff.level().dimension().location();
        }

        boolean matches(ServantGazeStaffEntity staff) {
            return staffUuid != null && staffUuid.equals(staff.getUUID())
                    && dimension != null && dimension.equals(staff.level().dimension().location());
        }

        public UUID getStaffUuid() {
            return staffUuid;
        }

        public ResourceLocation getDimension() {
            return dimension;
        }

        int getSpellLevel() {
            return spellLevel;
        }

        float getDamage() {
            return damage;
        }

        double getRadius() {
            return radius;
        }

        int getAttackManaCost() {
            return attackManaCost;
        }

        @Override
        public void writeToBuffer(FriendlyByteBuf buffer) {
            buffer.writeBoolean(staffUuid != null);
            if (staffUuid != null) buffer.writeUUID(staffUuid);
            buffer.writeBoolean(dimension != null);
            if (dimension != null) buffer.writeResourceLocation(dimension);
            buffer.writeVarInt(spellLevel);
            buffer.writeFloat(damage);
            buffer.writeDouble(radius);
            buffer.writeVarInt(attackManaCost);
        }

        @Override
        public void readFromBuffer(FriendlyByteBuf buffer) {
            staffUuid = buffer.readBoolean() ? buffer.readUUID() : null;
            dimension = buffer.readBoolean() ? buffer.readResourceLocation() : null;
            spellLevel = buffer.readVarInt();
            damage = buffer.readFloat();
            radius = buffer.readDouble();
            attackManaCost = buffer.readVarInt();
        }

        @Override
        public void reset() {
            staffUuid = null;
            dimension = null;
            spellLevel = 0;
            damage = 0.0F;
            radius = 0.0;
            attackManaCost = 0;
        }

        @Override
        public CompoundTag serializeNBT(HolderLookup.@NotNull Provider provider) {
            var tag = new CompoundTag();
            if (staffUuid != null) tag.putUUID("StaffUuid", staffUuid);
            if (dimension != null) tag.putString("Dimension", dimension.toString());
            tag.putInt("SpellLevel", spellLevel);
            tag.putFloat("Damage", damage);
            tag.putDouble("Radius", radius);
            tag.putInt("AttackManaCost", attackManaCost);
            return tag;
        }

        @Override
        public void deserializeNBT(HolderLookup.@NotNull Provider provider, CompoundTag tag) {
            staffUuid = tag.hasUUID("StaffUuid") ? tag.getUUID("StaffUuid") : null;
            dimension = tag.contains("Dimension") ? ResourceLocation.tryParse(tag.getString("Dimension")) : null;
            spellLevel = tag.getInt("SpellLevel");
            damage = tag.getFloat("Damage");
            radius = tag.getDouble("Radius");
            attackManaCost = tag.getInt("AttackManaCost");
        }
    }
}
