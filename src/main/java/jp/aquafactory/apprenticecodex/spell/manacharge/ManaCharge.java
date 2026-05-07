package jp.aquafactory.apprenticecodex.spell.manacharge;

import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.spells.*;
import io.redspace.ironsspellbooks.api.util.AnimationHolder;
import io.redspace.ironsspellbooks.api.util.Utils;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.config.ApprenticeCodexServerConfig;
import jp.aquafactory.apprenticecodex.config.DamageMultiplierKey;
import jp.aquafactory.apprenticecodex.registry.SoundRegistry;
import jp.aquafactory.apprenticecodex.utility.MagicTools;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.List;
import java.util.Optional;

public class ManaCharge extends AbstractSpell{
    private static final DustParticleOptions MANA_CHARGE_PARTICLE =
            new DustParticleOptions(new Vector3f(0.35f, 0.85f, 1.0f), 1.0f);
    private static final int MANA_CHARGE_PARTICLE_COUNT = 10;
    private static final double MANA_CHARGE_PARTICLE_SPEED = 0.01D;

    private final ResourceLocation spellId = ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "mana_charge");

    private final DefaultConfig config = new DefaultConfig()
            .setMinRarity(SpellRarity.LEGENDARY)
            .setSchoolResource(SchoolRegistry.HOLY_RESOURCE)
            .setMaxLevel(3)
            .setCooldownSeconds(120)
            .build();

    public ManaCharge() {
        baseSpellPower = 50;
        spellPowerPerLevel = 25;
        baseManaCost = 0;
        manaCostPerLevel = 0;
        castTime = 100;
    }
    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.apprenticecodex.mana_recover_speed", Utils.stringTruncation(getManaRechargePerSecond(spellLevel, caster), 2))
        );
    }

    private float getManaRechargePerSecond(int spellLevel, LivingEntity caster){
        var rawRecover = getSpellPower(spellLevel, caster);
        return rawRecover  * ApprenticeCodexServerConfig.damageMultiplier(DamageMultiplierKey.MANA_CHARGE);
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
        return Optional.of(SoundRegistry.VANILLA_POWER_ACTIVATE.get());
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
        return AnimationHolder.none();
    }

    @Override
    public void onServerCastTick(Level level, int spellLevel, LivingEntity entity, @Nullable MagicData playerMagicData) {
        if (playerMagicData != null){
            var tick = playerMagicData.getCastDurationRemaining();
            if (tick % 10 == 0){
                var rechargeMana = getManaRechargePerSecond(spellLevel, entity) / 2f;
                MagicTools.recoverManaSafely(entity, playerMagicData, rechargeMana);
            }
        }

        spawnManaChargeParticles(level, entity);
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }

    private void spawnManaChargeParticles(Level level, LivingEntity entity) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }

        if (entity.tickCount % 3 != 0) {
            return;
        }

        var horizontalSpread = Math.max(0.2D, entity.getBbWidth() * 0.45D);
        var verticalSpread = Math.max(0.35D, entity.getBbHeight() * 0.45D);
        var centerY = entity.getY() + entity.getBbHeight() * 0.5D;
        serverLevel.sendParticles(
                MANA_CHARGE_PARTICLE,
                entity.getX(),
                centerY,
                entity.getZ(),
                MANA_CHARGE_PARTICLE_COUNT,
                horizontalSpread,
                verticalSpread,
                horizontalSpread,
                MANA_CHARGE_PARTICLE_SPEED
        );
    }
}
