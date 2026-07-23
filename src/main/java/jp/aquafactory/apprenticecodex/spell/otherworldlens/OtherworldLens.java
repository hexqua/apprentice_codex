package jp.aquafactory.apprenticecodex.spell.otherworldlens;

import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.spells.*;
import io.redspace.ironsspellbooks.api.util.AnimationHolder;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.registries.SoundRegistry;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.Optional;

public class OtherworldLens extends AbstractSpell {
    private final ResourceLocation spellId = ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "otherworld_lens");

    private final DefaultConfig config = new DefaultConfig()
            .setMinRarity(SpellRarity.LEGENDARY)
            .setSchoolResource(SchoolRegistry.ELDRITCH_RESOURCE)
            .setMaxLevel(1)
            .setCooldownSeconds(5)
            .build();

    public OtherworldLens() {
        baseSpellPower = 0;
        spellPowerPerLevel = 0;
        baseManaCost = 35;
        manaCostPerLevel = 0;
        castTime = 300;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.irons_spellbooks.distance", Utils.stringTruncation(getRange(), 0))
        );
    }

    static float getRange(){
        // レンズを設置できる最大距離.
        return 8;
    }

    static float getKeepRange(){
        // これ以上離れると効果中断.
        return 12;
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
        // todo:それっぽい音を作る.
        return Optional.of(SoundRegistry.ABYSSAL_TELEPORT.get());
    }

    @Override
    public Optional<SoundEvent> getCastFinishSound() {
        return Optional.of(jp.aquafactory.apprenticecodex.registry.SoundRegistry.VANILLA_SUMMON_MAGICAL_ENTITY.get());
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
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }
}
