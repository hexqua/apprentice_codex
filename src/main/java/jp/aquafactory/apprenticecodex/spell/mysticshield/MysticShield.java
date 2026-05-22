package jp.aquafactory.apprenticecodex.spell.mysticshield;

import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.spells.*;
import io.redspace.ironsspellbooks.api.util.AnimationHolder;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.config.ApprenticeCodexServerConfig;
import jp.aquafactory.apprenticecodex.config.DamageMultiplierKey;
import jp.aquafactory.apprenticecodex.item.curios.protectionspellsupporter.ProtectionSpellSupporter;
import jp.aquafactory.apprenticecodex.registry.SoundRegistry;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.Optional;

public class MysticShield extends AbstractSpell {
    private final ResourceLocation spellId = ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "mystic_shield");

    private final DefaultConfig config = new DefaultConfig()
            .setMinRarity(SpellRarity.RARE)
            .setSchoolResource(SchoolRegistry.HOLY_RESOURCE)
            .setMaxLevel(3)
            .setCooldownSeconds(30)
            .build();

    public MysticShield() {
        baseSpellPower = 50;
        spellPowerPerLevel = 25;
        baseManaCost = 15;
        manaCostPerLevel = 3;
        castTime = 200;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.apprenticecodex.reflect_damage_multiplier", getReflectPercent(spellLevel, caster))
        );
    }

    private int getReflectPercent(int spellLevel, LivingEntity entity) {
        return Math.round(getReflectDamageMultiplier(spellLevel, entity) * 100.0f);
    }

    public float getReflectDamageMultiplier(int spellLevel, LivingEntity entity) {
        var multiplier = getSpellPower(spellLevel, entity) / 100.0f
                * ApprenticeCodexServerConfig.damageMultiplier(DamageMultiplierKey.MYSTIC_SHIELD);
        if (ProtectionSpellSupporter.isEquippedBy(entity)) {
            return multiplier * 2.0f;
        }
        return multiplier;
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
        return Optional.of(SoundRegistry.MYSTIC_SHIELD_DEPLOY.get());
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
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        if (!level.isClientSide) {
            if (MysticShieldDefenseEvent.spawnShieldEntity(level, entity)) {
                MysticShieldDefenseEvent.resetStoredDamage(entity);
            }
        }
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }

    @Override
    public void onServerCastComplete(Level level, int spellLevel, LivingEntity entity, MagicData playerMagicData, boolean cancelled) {
        MysticShieldDefenseEvent.releaseStoredDamage(level, spellLevel, entity, cancelled);
        super.onServerCastComplete(level, spellLevel, entity, playerMagicData, cancelled);
    }
}
