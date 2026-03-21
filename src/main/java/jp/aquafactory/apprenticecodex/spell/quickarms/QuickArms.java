package jp.aquafactory.apprenticecodex.spell.quickarms;

import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.CastType;
import io.redspace.ironsspellbooks.api.spells.SpellAnimations;
import io.redspace.ironsspellbooks.api.spells.SpellRarity;
import io.redspace.ironsspellbooks.api.util.AnimationHolder;
import io.redspace.ironsspellbooks.api.util.Utils;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.config.ApprenticeCodexServerConfig;
import jp.aquafactory.apprenticecodex.config.DamageMultiplierKey;
import jp.aquafactory.apprenticecodex.registry.EntityRegistry;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.Optional;

public class QuickArms extends AbstractSpell {
    private static final int LIFETIME_TICKS = 20;

    private final ResourceLocation spellId = ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "quick_arms");

    private final DefaultConfig config = new DefaultConfig()
            .setMinRarity(SpellRarity.RARE)
            .setSchoolResource(SchoolRegistry.EVOCATION_RESOURCE)
            .setMaxLevel(5)
            .setCooldownSeconds(8)
            .build();

    public QuickArms() {
        baseSpellPower = 100;
        spellPowerPerLevel = 30;
        manaCostPerLevel = 15;
        baseManaCost = 30;
        castTime = 0;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.irons_spellbooks.damage", Utils.stringTruncation(getDamage(spellLevel, caster), 2)),
                Component.translatable("ui.apprenticecodex.sneak_damage_multiplier", getSneakPercent(spellLevel, caster))
        );
    }

    private float getDamage(int spellLevel, LivingEntity entity) {
        var rawDamage = 4 * getSpellPower(spellLevel, entity) / 100.0f;
        return rawDamage * ApprenticeCodexServerConfig.damageMultiplier(DamageMultiplierKey.QUICK_ARMS);
    }

    private float getOverSpellPower(int spellLevel, LivingEntity entity) {
        return getSpellPower(spellLevel, entity) - baseSpellPower;
    }

    private int getSneakPercent(int spellLevel, LivingEntity entity) {
        return Math.min(300, 150 + Math.round(25 * (getOverSpellPower(spellLevel, entity) / 100.0f)));
    }

    @Override
    public Optional<SoundEvent> getCastFinishSound() {
        return Optional.of(SoundEvents.SHULKER_TELEPORT);
    }

    private int getRange() {
        // ハンドガンイメージなので近距離(2チャンク程度)
        return 16 * 2;
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
    public AnimationHolder getCastStartAnimation() {
        return SpellAnimations.ANIMATION_INSTANT_CAST;
    }

    @Override
    public AnimationHolder getCastFinishAnimation() {
        return AnimationHolder.pass();
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        var summonWeapon = new QuickArmsHandgunEntity(EntityRegistry.QUICK_ARMS_HANDGUN.get(), level, entity);
        summonWeapon.setDamage(getDamage(spellLevel, entity), getSneakPercent(spellLevel, entity));
        summonWeapon.setRange(getRange());
        summonWeapon.setLifetimeTicks(LIFETIME_TICKS);
        level.addFreshEntity(summonWeapon);
        summonWeapon.fire(level);
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }
}
