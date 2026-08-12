package jp.aquafactory.apprenticecodex.spell.longstride;

import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.CastType;
import io.redspace.ironsspellbooks.api.spells.SpellRarity;
import io.redspace.ironsspellbooks.api.util.AnimationHolder;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.registry.EffectRegistry;
import jp.aquafactory.apprenticecodex.registry.SoundRegistry;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

public class LongStride extends AbstractSpell {
    private static final int EFFECT_REFRESH_TICKS = 5;

    private final ResourceLocation spellId = ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "long_stride");

    private final DefaultConfig config = new DefaultConfig()
            .setMinRarity(SpellRarity.RARE)
            .setSchoolResource(SchoolRegistry.ENDER_RESOURCE)
            .setMaxLevel(1)
            .setCooldownSeconds(12)
            .setAllowCrafting(false)
            .build();

    public LongStride() {
        baseSpellPower = 0;
        spellPowerPerLevel = 0;
        baseManaCost = 12;
        manaCostPerLevel = -1;
        castTime = 20 * 60;
    }

    @Override
    public boolean allowLooting(){
        // アイテム専用化.
        return false;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.apprenticecodex.move_speed_bonus", getMoveSpeedBonusPercent(spellLevel))
        );
    }

    @Override
    public int getEffectiveCastTime(int spellLevel, LivingEntity entity) {
        // 詠唱時間短縮による延長は乗らない.
        return getCastTime(spellLevel);
    }

    private int getMobEffectAmplifier(int spellLevel) {
        return spellLevel - 1;
    }

    private int getMoveSpeedBonusPercent(int spellLevel) {
        var amplifier = getMobEffectAmplifier(spellLevel);
        return 10 + (int) Math.round(100.0 * 0.05 * amplifier);
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
        return Optional.of(SoundRegistry.VANILLA_LONG_STRIDE.get());
    }

    @Override
    public Optional<SoundEvent> getCastFinishSound() {
        return Optional.empty();
    }

    @Override
    public AnimationHolder getCastStartAnimation() {
        // 手元のコンパスを見せるため、意図的にモーションオフ.
        return AnimationHolder.none();
    }

    @Override
    public AnimationHolder getCastFinishAnimation() {
        return AnimationHolder.none();
    }

    @Override
    public void onServerCastTick(Level level, int spellLevel, LivingEntity entity, @Nullable MagicData playerMagicData) {
        entity.addEffect(new MobEffectInstance(
                EffectRegistry.LONG_STRIDE_MOBILITY,
                EFFECT_REFRESH_TICKS,
                getMobEffectAmplifier(spellLevel),
                false,
                true,
                true
        ));
        super.onServerCastTick(level, spellLevel, entity, playerMagicData);
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }
}
