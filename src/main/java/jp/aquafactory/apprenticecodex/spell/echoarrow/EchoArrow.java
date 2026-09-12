package jp.aquafactory.apprenticecodex.spell.echoarrow;

import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.spells.*;
import io.redspace.ironsspellbooks.api.util.AnimationHolder;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.registries.SoundRegistry;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.config.ApprenticeCodexServerConfig;
import jp.aquafactory.apprenticecodex.config.DamageMultiplierKey;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.Optional;

public class EchoArrow extends AbstractSpell {
    private final ResourceLocation spellId = ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "echo_arrow");

    private final DefaultConfig config = new DefaultConfig()
            .setMinRarity(SpellRarity.LEGENDARY)
            .setSchoolResource(SchoolRegistry.ELDRITCH_RESOURCE)
            .setMaxLevel(4)
            .setCooldownSeconds(8)
            .build();

    public EchoArrow() {
        baseSpellPower = 1500;
        spellPowerPerLevel = 500;
        baseManaCost = 90;
        manaCostPerLevel = 20;
        castTime = 30;
    }
    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.irons_spellbooks.damage", Utils.stringTruncation(getDamage(), 2)),
                Component.translatable("ui.irons_spellbooks.projectile_count", getArrowCount(spellLevel, caster)),
                Component.translatable("ui.irons_spellbooks.duration", Utils.timeFromTicks(getDuration(spellLevel, caster), 1))
        );
    }

    private float getDamage() {
        // 威力は本数ベースで決めるため、1発単位は変化しない.
        return 3 * ApprenticeCodexServerConfig.damageMultiplier(DamageMultiplierKey.ECHO_ARROW);
    }

    private int getArrowCount(int spellLevel, LivingEntity entity) {
        return Math.round(getSpellPower(spellLevel, entity) / 100.0f);
    }

    private int getDuration(int spellLevel, LivingEntity entity) {
        // 発射間隔は3tickにつき3発固定のため、本数から時間を逆算する(1～2発分に関しても1セット分の時間を使う)
        return Mth.ceil(getArrowCount(spellLevel, entity) / 3f) * 3;
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
        // todo:字幕を差し替える.
        return Optional.of(net.minecraft.sounds.SoundEvents.WARDEN_SONIC_CHARGE);
    }

    @Override
    public Optional<SoundEvent> getCastFinishSound() {
        return Optional.of(SoundRegistry.ELDRITCH_BLAST.get());
    }

    @Override
    public AnimationHolder getCastStartAnimation() {
        return SpellAnimations.BOW_CHARGE_ANIMATION;
    }

    @Override
    public AnimationHolder getCastFinishAnimation() {
        return AnimationHolder.none();
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }
}
