package jp.aquafactory.apprenticecodex.common.spells.arcaneblast;

import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.spells.*;
import io.redspace.ironsspellbooks.api.util.AnimationHolder;
import io.redspace.ironsspellbooks.api.util.Utils;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.common.registry.SoundRegistry;
import jp.aquafactory.apprenticecodex.common.registry.SpellsRegistry;
import jp.aquafactory.apprenticecodex.common.spells.ICastHighlightSpell;
import jp.aquafactory.apprenticecodex.common.utility.AudioTools;
import jp.aquafactory.apprenticecodex.common.utility.CombatTools;
import jp.aquafactory.apprenticecodex.common.utility.RaycastTools;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.Optional;

public class ArcaneBlast extends AbstractSpell implements ICastHighlightSpell {
    private final ResourceLocation spellId = ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "arcane_blast");

    private final DefaultConfig config = new DefaultConfig()
            .setMinRarity(SpellRarity.COMMON)
            .setSchoolResource(SchoolRegistry.ENDER_RESOURCE)
            .setMaxLevel(10)
            .setCooldownSeconds(2)
            .build();

    public ArcaneBlast() {
        // todo:バランス調整.
        baseSpellPower = 100;
        spellPowerPerLevel = 15;
        manaCostPerLevel = 10;
        baseManaCost = 60;
        castTime = 30;
    }
    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.irons_spellbooks.damage", Utils.stringTruncation(getDamage(spellLevel, caster), 2)),
                Component.literal(ApprenticeCodex.NAME)
        );
    }

    private float getDamage(int spellLevel, LivingEntity entity) {
        // todo:バランス調整.
        return 4;
    }

    private double getRange(){
        return 16;
    }

    @Override
    public double getHighlightRange() {
        return getRange();
    }

    @Override
    public double getHighlightWidth(){
        return 0.1;
    }

    @Override
    public int getHighlightColor() {
        return 0x7733ff;
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
        return SpellAnimations.ANIMATION_INSTANT_CAST;
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        var result = RaycastTools.raycastFromEye(entity, getRange(), getHighlightWidth(), e -> CombatTools.isValidCombatTarget(e, entity));
        if (result.hitEntity() != null) {
            var target = CombatTools.resolutePartEntity(result.hitEntity());
            var source = CombatTools.getDamageSource(level, entity, "arcane_blast");
            CombatTools.applyDamage(target, getDamage(spellLevel, entity), source, getSchoolType(), CombatTools.KnockbackTypes.DEFAULT);
            AudioTools.playSoundFromEntity(level, target, SoundRegistry.ARCANE_BLAST.get(), SoundSource.PLAYERS);
        }
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }
}
