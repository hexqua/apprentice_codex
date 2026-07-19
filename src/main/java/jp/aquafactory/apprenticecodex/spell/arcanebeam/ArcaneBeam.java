package jp.aquafactory.apprenticecodex.spell.arcanebeam;

import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.spells.*;
import io.redspace.ironsspellbooks.api.util.AnimationHolder;
import io.redspace.ironsspellbooks.api.util.Utils;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.config.ApprenticeCodexServerConfig;
import jp.aquafactory.apprenticecodex.config.DamageMultiplierKey;
import jp.aquafactory.apprenticecodex.registry.EffectRegistry;
import jp.aquafactory.apprenticecodex.registry.EntityRegistry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class ArcaneBeam extends AbstractSpell {
    private final ResourceLocation spellId = ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "arcane_beam");

    private final DefaultConfig config = new DefaultConfig()
            .setMinRarity(SpellRarity.RARE)
            .setSchoolResource(SchoolRegistry.ENDER_RESOURCE)
            .setMaxLevel(5)
            .setCooldownSeconds(12)
            .build();

    public ArcaneBeam() {
        baseSpellPower = 200;
        spellPowerPerLevel = 100;
        baseManaCost = 5;
        manaCostPerLevel = 5;
        castTime = 60;
    }
    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.irons_spellbooks.damage", Utils.stringTruncation(getDamage(spellLevel, caster), 2)),
                Component.translatable("ui.irons_spellbooks.distance", Utils.stringTruncation(getRange(), 0)),
                Component.translatable("ui.apprenticecodex.arcane_charge_damage_multiplier", getChargeDamageAmplifier(spellLevel, caster))
        );
    }

    private float getDamage(int spellLevel, LivingEntity entity) {
        var rawDamage = 1 + getSpellPower(spellLevel, entity) / 100.0f;
        return rawDamage * ApprenticeCodexServerConfig.damageMultiplier(DamageMultiplierKey.ARCANE_BEAM);
    }

    private int getChargeDamageAmplifier(int spellLevel, LivingEntity entity){
        return Math.round((getSpellPower(spellLevel, entity) - 100) / 30.0f);
    }

    static float getRange(){
        return 32f;
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
        return CastType.CONTINUOUS;
    }

    @Override
    public Optional<SoundEvent> getCastStartSound() {
        return Optional.of(io.redspace.ironsspellbooks.registries.SoundRegistry.BLACK_HOLE_CAST.get());
    }

    @Override
    public Optional<SoundEvent> getCastFinishSound() {
        return Optional.empty();
    }

    @Override
    public AnimationHolder getCastStartAnimation() {
        return SpellAnimations.ANIMATION_CONTINUOUS_CAST_ONE_HANDED;
    }

    @Override
    public AnimationHolder getCastFinishAnimation() {
        return SpellAnimations.FINISH_ANIMATION;
    }

    @Override
    public final ICastDataSerializable getEmptyCastData() {
        return new BeamCastData();
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        if (!(playerMagicData.getAdditionalCastData() instanceof BeamCastData)) {
            var castData = new BeamCastData();
            var beam = new ArcaneBeamEntity(EntityRegistry.ARCANE_BEAM.get(), level, entity);

            var beamPos = calculateBeamPosition(entity);
            beam.moveTo(beamPos.x, beamPos.y, beamPos.z, entity.getYRot(), entity.getXRot());
            beam.setup(0x30AA88FF, 0x80DDAAFF, getRange(), 0.25f);
            beam.updateLength(getRange(), level);

            var baseDamage = getDamage(spellLevel, entity);
            var currentCharge = entity.getEffect(EffectRegistry.ARCANE_CHARGE.get());
            if (currentCharge != null)
            {
                var chargeDamageAmplifier = getChargeDamageAmplifier(spellLevel, entity) * (currentCharge.getAmplifier() + 1);
                beam.setDamage(baseDamage * (1f + chargeDamageAmplifier / 100.0f));
            }
            else
            {
                beam.setDamage(baseDamage);
            }

            level.addFreshEntity(beam);

            castData.setEntity(beam);
            playerMagicData.setAdditionalCastData(castData);
        }

        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }

    @Override
    public void onServerCastTick(Level level, int spellLevel, LivingEntity entity, @Nullable MagicData playerMagicData) {
        if (playerMagicData instanceof MagicData && playerMagicData.getAdditionalCastData() instanceof BeamCastData beamData && level instanceof ServerLevel server) {
            var dataEntity = beamData.getEntity(server);
            if (dataEntity instanceof ArcaneBeamEntity beam) {
                var beamPos = calculateBeamPosition(entity);
                beam.moveTo(beamPos.x, beamPos.y, beamPos.z, entity.getYRot(), entity.getXRot());
                beam.updateLength(getRange(), level);
            }
        }

        super.onServerCastTick(level, spellLevel, entity, playerMagicData);
    }

    @Override
    public void onServerCastComplete(Level level, int spellLevel, LivingEntity entity, MagicData playerMagicData, boolean cancelled) {
        if (playerMagicData.getAdditionalCastData() instanceof BeamCastData castData && level instanceof ServerLevel serverLevel) {
            var beam = castData.getEntity(serverLevel);
            if (beam != null) {
                beam.discard();
            }
        }

        entity.removeEffect(EffectRegistry.ARCANE_CHARGE.get());
        super.onServerCastComplete(level, spellLevel, entity, playerMagicData, cancelled);
    }

    private static Vec3 calculateBeamPosition(LivingEntity entity){
        // ちょっと下にして見えやすくする.
        return entity.getEyePosition(1.0f).add(0, -0.7, 0).add(entity.getLookAngle().scale(0.75f));
    }

    public static class BeamCastData implements ICastDataSerializable {
        private UUID entityId;

        public void setEntity(Entity entity) {
            entityId = entity.getUUID();
        }

        public Entity getEntity(ServerLevel level) {
            return level.getEntity(entityId);
        }

        @Override
        public void writeToBuffer(FriendlyByteBuf friendlyByteBuf) {
            friendlyByteBuf.writeUUID(entityId);
        }

        @Override
        public void readFromBuffer(FriendlyByteBuf friendlyByteBuf) {
            entityId = friendlyByteBuf.readUUID();
        }

        @Override
        public void reset() {
            entityId = null;
        }

        @Override
        public CompoundTag serializeNBT() {
            var tag = new CompoundTag();
            tag.putUUID("Entity", entityId);
            return tag;
        }

        @Override
        public void deserializeNBT(CompoundTag nbt) {
            entityId = nbt.getUUID("Entity");
        }
    }
}
