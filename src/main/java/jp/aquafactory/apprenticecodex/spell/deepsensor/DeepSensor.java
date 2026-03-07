package jp.aquafactory.apprenticecodex.spell.deepsensor;

import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.spells.*;
import io.redspace.ironsspellbooks.api.util.AnimationHolder;
import io.redspace.ironsspellbooks.api.util.Utils;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.registry.EffectRegistry;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.Optional;

public class DeepSensor extends AbstractSpell {
    private final ResourceLocation spellId = ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "deep_sensor");

    private final DefaultConfig config = new DefaultConfig()
            .setMinRarity(SpellRarity.LEGENDARY)
            .setSchoolResource(SchoolRegistry.ELDRITCH_RESOURCE)
            .setMaxLevel(3)
            .setCooldownSeconds(60)
            .build();

    public DeepSensor() {
        baseSpellPower = 100;
        spellPowerPerLevel = 100;
        baseManaCost = 100;
        manaCostPerLevel = 50;
        castTime = 0;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.irons_spellbooks.distance", Utils.stringTruncation(getRange(spellLevel, caster), 1)),
                Component.translatable("ui.irons_spellbooks.effect_length", Utils.timeFromTicks(getDuration(spellLevel, caster), 1))
        );
    }

    private int getRange(int spellLevel, LivingEntity caster) {
        // MobEffectのレベルで制御しているため.
        return 8 * (1 + getAmplify(spellLevel, caster));
    }

    private int getAmplify(int spellLevel, LivingEntity caster) {
        // 初期状態でAmp=0になるように調整.
        return Math.round(getSpellPower(spellLevel, caster) / 100.0f) - 1;
    }

    private int getDuration(int spellLevel, LivingEntity caster) {
        return 20 * 20 + Math.round(5 * 20 * getSpellPower(spellLevel, caster) / 100.0f);
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
        return Optional.of(SoundEvents.SCULK_SENSOR_PLACE);
    }

    @Override
    public AnimationHolder getCastStartAnimation() {
        return SpellAnimations.TOUCH_GROUND_ANIMATION;
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        entity.addEffect(new MobEffectInstance(
                EffectRegistry.SENSE_SENSOR.get(),
                getDuration(spellLevel, entity),
                getAmplify(spellLevel, entity),
                false,
                false,
                true
        ));
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }
}
