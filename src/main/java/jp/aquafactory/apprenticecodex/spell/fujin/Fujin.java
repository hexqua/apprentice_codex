package jp.aquafactory.apprenticecodex.spell.fujin;

import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.spells.CastType;
import io.redspace.ironsspellbooks.api.spells.SpellAnimations;
import io.redspace.ironsspellbooks.api.spells.SpellRarity;
import io.redspace.ironsspellbooks.api.util.AnimationHolder;
import io.redspace.ironsspellbooks.api.util.Utils;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.config.ApprenticeCodexServerConfig;
import jp.aquafactory.apprenticecodex.config.DamageMultiplierKey;
import jp.aquafactory.apprenticecodex.registry.EntityRegistry;
import jp.aquafactory.apprenticecodex.registry.SoundRegistry;
import jp.aquafactory.apprenticecodex.spell.AbstractSummonWeaponSpell;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;

public class Fujin extends AbstractSummonWeaponSpell<FujinKatanaEntity> {
    private final ResourceLocation spellId = ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "fujin");

    private final DefaultConfig config = new DefaultConfig()
            .setMinRarity(SpellRarity.UNCOMMON)
            .setSchoolResource(SchoolRegistry.EVOCATION_RESOURCE)
            .setMaxLevel(10)
            .setCooldownSeconds(12)
            .build();

    public Fujin() {
        super(FujinKatanaEntity.class);
        baseSpellPower = 100;
        spellPowerPerLevel = 75;
        baseManaCost = 5;
        manaCostPerLevel = 1;
        castTime = 100;
    }
    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.irons_spellbooks.damage", Utils.stringTruncation(getDamage(spellLevel, caster), 2)),
                Component.translatable("ui.irons_spellbooks.distance", Utils.stringTruncation(getRange(), 0))
        );
    }

    private float getDamage(int spellLevel, LivingEntity entity) {
        var rawDamage = 1 + getSpellPower(spellLevel, entity) / 200.0f;
        return rawDamage * ApprenticeCodexServerConfig.damageMultiplier(DamageMultiplierKey.FUJIN);
    }

    static float getRange() {
        return 10;
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
        return Optional.of(SoundRegistry.VANILLA_SUMMON_WEAPON.get());
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
    public FujinKatanaEntity onCastNoWeapon(Level level, int spellLevel, LivingEntity entity, MagicData playerMagicData) {
        var katana = new FujinKatanaEntity(EntityRegistry.FUJIN_KATANA.get(), level, entity);
        katana.setDamage(getDamage(spellLevel, entity));
        katana.setProjectileRange(getRange());
        level.addFreshEntity(katana);
        return katana;
    }

    @Override
    public void onCastTickWithWeapon(Level level, int spellLevel, LivingEntity entity,
                                     MagicData playerMagicData, @NotNull FujinKatanaEntity weapon) {
        // 発射周期は刀側で管理し、詠唱速度補正によって10tick間隔が変わらないようにする。
    }

    @Override
    public CompleteCastTypes onCastCompleteWithWeapon(Level level, int spellLevel, LivingEntity entity,
                                                       MagicData playerMagicData, boolean cancelled,
                                                       @NotNull FujinKatanaEntity weapon) {
        return CompleteCastTypes.RELEASE_WEAPON;
    }
}
