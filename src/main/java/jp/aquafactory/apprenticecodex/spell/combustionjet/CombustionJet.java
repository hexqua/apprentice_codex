package jp.aquafactory.apprenticecodex.spell.combustionjet;

import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.spells.*;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.registries.SoundRegistry;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.config.ApprenticeCodexServerConfig;
import jp.aquafactory.apprenticecodex.config.DamageMultiplierKey;
import jp.aquafactory.apprenticecodex.registry.EntityRegistry;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.Optional;

public class CombustionJet extends AbstractSpell {
    private final ResourceLocation spellId = ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "combustion_jet");

    private final DefaultConfig config = new DefaultConfig()
            .setMinRarity(SpellRarity.UNCOMMON)
            .setSchoolResource(SchoolRegistry.FIRE_RESOURCE)
            .setMaxLevel(5)
            .setCooldownSeconds(8)
            .build();

    public CombustionJet() {
        baseSpellPower = 400;
        spellPowerPerLevel = 200;
        baseManaCost = 30;
        manaCostPerLevel = 10;
        castTime = 0;
    }
    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.irons_spellbooks.damage", Utils.stringTruncation(getDamage(spellLevel, caster), 2)),
                Component.translatable("ui.irons_spellbooks.distance", Utils.stringTruncation(getRange(), 0)),
                Component.translatable("ui.apprenticecodex.combustion_jet.burn_time", Utils.timeFromTicks(getBurnDuration(spellLevel), 1))
        );
    }

    float getDamage(int spellLevel, LivingEntity entity) {
        var rawDamage = 1 + getSpellPower(spellLevel, entity) / 100.0f;
        return rawDamage * ApprenticeCodexServerConfig.damageMultiplier(DamageMultiplierKey.COMBUSTION_JET);
    }

    int getBurnDuration(int spellLevel) {
        // Lv1は見た目だけの炎上(1秒おきのダメージのため)、Lv2以上で炎上ダメージを期待させる.
        return Math.max((spellLevel - 1) * 40, 10);
    }

    static float getRange(){
        return 10f;
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
        return Optional.of(SoundRegistry.FIRE_CAST.get());
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        var wave = new CombustionJetWaveEntity(EntityRegistry.COMBUSTION_JET_WAVE.get(), level, entity);
        var direction = entity.getLookAngle();
        wave.setPos(entity.getEyePosition().add(direction));
        wave.setDamage(getDamage(spellLevel, entity));
        wave.setBurnDuration(getBurnDuration(spellLevel));
        wave.setMaxTravelDistance(getRange());
        wave.shoot(direction);
        level.addFreshEntity(wave);
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }
}
