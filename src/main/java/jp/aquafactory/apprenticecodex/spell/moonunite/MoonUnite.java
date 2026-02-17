package jp.aquafactory.apprenticecodex.spell.moonunite;

import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.spells.CastType;
import io.redspace.ironsspellbooks.api.spells.SpellAnimations;
import io.redspace.ironsspellbooks.api.spells.SpellRarity;
import io.redspace.ironsspellbooks.api.util.AnimationHolder;
import io.redspace.ironsspellbooks.api.util.Utils;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.registry.EntityRegistry;
import jp.aquafactory.apprenticecodex.spell.AbstractSummonWeaponSpell;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;

public class MoonUnite extends AbstractSummonWeaponSpell<MoonUniteKatanaEntity> {

    private final ResourceLocation spellId = ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "moon_unite");

    private final DefaultConfig config = new DefaultConfig()
            .setMinRarity(SpellRarity.RARE)
            .setSchoolResource(SchoolRegistry.HOLY_RESOURCE)
            .setMaxLevel(3)
            .setCooldownSeconds(4)
            .build();

    public MoonUnite() {
        super(MoonUniteKatanaEntity.class);
        baseSpellPower = 100;
        spellPowerPerLevel = 50;
        baseManaCost = 10;
        manaCostPerLevel = 5;
        castTime = 0;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.irons_spellbooks.damage", Utils.stringTruncation(getDamage(spellLevel, caster), 2)),
                Component.literal(ApprenticeCodex.NAME)
        );
    }

    private float getDamage(int spellLevel, LivingEntity entity) {
        return 0.5f + 1.5f * getSpellPower(spellLevel, entity) / 100.0f;
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
    public Optional<SoundEvent> getCastStartSound() {
        return Optional.of(SoundEvents.ENDERMAN_TELEPORT);
    }

    @Override
    public Optional<SoundEvent> getCastFinishSound() {
        return Optional.empty();
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
    public MoonUniteKatanaEntity onCastNoWeapon(Level level, int spellLevel, LivingEntity entity, MagicData playerMagicData) {
        var summonWeapon = new MoonUniteKatanaEntity(EntityRegistry.MOON_UNITE_KATANA.get(), level, entity);
        level.addFreshEntity(summonWeapon);
        return summonWeapon;
    }

    @Override
    public void onCastTickWithWeapon(Level level, int spellLevel, LivingEntity entity, MagicData playerMagicData, @NotNull MoonUniteKatanaEntity weapon) {
        // todo:色々やる.
    }

    @Override
    public CompleteCastTypes onCastCompleteWithWeapon(Level level, int spellLevel, LivingEntity entity, MagicData playerMagicData, boolean cancelled, @NotNull MoonUniteKatanaEntity weapon) {
        return CompleteCastTypes.KEEP_WEAPON;
    }
}
