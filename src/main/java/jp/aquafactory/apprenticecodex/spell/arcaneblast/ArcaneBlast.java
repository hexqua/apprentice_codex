package jp.aquafactory.apprenticecodex.spell.arcaneblast;

import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.spells.*;
import io.redspace.ironsspellbooks.api.util.AnimationHolder;
import io.redspace.ironsspellbooks.api.util.Utils;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.damage.DamageTypes;
import jp.aquafactory.apprenticecodex.registry.EffectRegistry;
import jp.aquafactory.apprenticecodex.registry.SoundRegistry;
import jp.aquafactory.apprenticecodex.spell.ICastHighlightSpell;
import jp.aquafactory.apprenticecodex.utility.AudioTools;
import jp.aquafactory.apprenticecodex.utility.CombatTools;
import jp.aquafactory.apprenticecodex.utility.RaycastTools;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

public class ArcaneBlast extends AbstractSpell implements ICastHighlightSpell {
    private final ResourceLocation spellId = ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "arcane_blast");

    private final DefaultConfig config = new DefaultConfig()
            .setMinRarity(SpellRarity.COMMON)
            .setSchoolResource(SchoolRegistry.ENDER_RESOURCE)
            .setMaxLevel(10)
            .setCooldownSeconds(1)
            .build();

    public ArcaneBlast() {
        baseSpellPower = 40;
        spellPowerPerLevel = 5;
        baseManaCost = 20;
        manaCostPerLevel = 4;
        castTime = 30;
    }
    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.irons_spellbooks.damage", Utils.stringTruncation(getDamage(spellLevel, caster), 2)),
                Component.translatable("ui.irons_spellbooks.distance", Utils.stringTruncation(getRange(), 1))
        );
    }

    private float getDamage(int spellLevel, LivingEntity entity) {
        return getSpellPower(spellLevel, entity) /10.0f;
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
    @Nullable
    public Entity getHighlightEntity(@NotNull Player player, int skillLevel) {
        return RaycastTools.raycastFromEye(player, getRange(), getHighlightWidth(), e -> CombatTools.isValidCombatTarget(e, player)).hitEntity();
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
            var source = CombatTools.getDamageSource(level, entity, DamageTypes.ARCANE_BLAST);
            CombatTools.applyDamage(target, getDamage(spellLevel, entity), source, getSchoolType(), CombatTools.KnockbackTypes.DEFAULT);
            AudioTools.playSoundFromEntity(level, target, SoundRegistry.ARCANE_BLAST.get(), SoundSource.PLAYERS);

            var currentCharge = entity.getEffect(EffectRegistry.ARCANE_CHARGE.get());
            if (currentCharge != null){
                var chargeLevel = Math.min(currentCharge.getAmplifier() + 1, 2);
                entity.addEffect(new MobEffectInstance(EffectRegistry.ARCANE_CHARGE.get(), 200, chargeLevel, false, false, true));
            } else {
                entity.addEffect(new MobEffectInstance(EffectRegistry.ARCANE_CHARGE.get(), 200, 0, false, false, true));
            }

            if (level instanceof ServerLevel server) {
                var position = result.hitEntity().getBoundingBox().getCenter();
                server.sendParticles(ParticleTypes.END_ROD, position.x, position.y, position.z, 32, 0.5, 0.5, 0.5, 0.05);
                server.sendParticles(ParticleTypes.FIREWORK, position.x, position.y, position.z, 24, 0.1, 0.1, 0.1, 0.1);
            }
        }
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }
}
