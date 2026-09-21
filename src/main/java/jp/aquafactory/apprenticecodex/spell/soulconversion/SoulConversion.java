package jp.aquafactory.apprenticecodex.spell.soulconversion;

import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.spells.*;
import io.redspace.ironsspellbooks.api.util.AnimationHolder;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.registry.SoundRegistry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

import java.util.Optional;

public class SoulConversion extends AbstractSpell {
    private final ResourceLocation spellId = ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "soul_conversion");

    private final DefaultConfig config = new DefaultConfig()
            .setMinRarity(SpellRarity.LEGENDARY)
            .setSchoolResource(SchoolRegistry.ELDRITCH_RESOURCE)
            .setMaxLevel(1)
            .setCooldownSeconds(30)
            .setAllowCrafting(false)
            .build();

    public SoulConversion() {
        // 1.6.7は発射ごとのマナ代替を使うため、専用魔法は設定でも有効化させない。
        config.enabled = false;
        baseSpellPower = 0;
        spellPowerPerLevel = 0;
        baseManaCost = 500;
        manaCostPerLevel = 0;
        castTime = 30;
    }

    @Override
    public boolean isEnabled() {
        return false;
    }

    @Override
    public boolean requiresLearning() {
        // エルドリッチだけどアイテム専用なので初期解禁.
        return false;
    }

    @Override
    public boolean allowLooting() {
        // エルドリッチはデフォルトfalseだが他の処理でtrueにされる可能性を考慮して明示的にfalseにする.
        return false;
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
        return Optional.of(SoundRegistry.VANILLA_POWER_TUNING.get());
    }

    @Override
    public Optional<SoundEvent> getCastFinishSound() {
        return Optional.of(SoundRegistry.VANILLA_POWER_ACTIVATE.get());
    }

    @Override
    public AnimationHolder getCastStartAnimation() {
        return SpellAnimations.ANIMATION_CONTINUOUS_CAST_ONE_HANDED;
    }

    @Override
    public final boolean checkPreCastConditions(Level level, int spellLevel, LivingEntity entity, MagicData playerMagicData) {
        return false;
    }
}
