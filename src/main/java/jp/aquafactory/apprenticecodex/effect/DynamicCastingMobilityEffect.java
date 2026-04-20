package jp.aquafactory.apprenticecodex.effect;

import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeMap;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Set;

public abstract class DynamicCastingMobilityEffect extends MobEffect {
    private static final Set<ResourceLocation> MANAGED_MODIFIER_IDS = new LinkedHashSet<>();

    private final ResourceLocation castingMoveSpeedModifierId;

    protected DynamicCastingMobilityEffect(int color, String castingMoveSpeedModifierId) {
        super(MobEffectCategory.BENEFICIAL, color);
        this.castingMoveSpeedModifierId = ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, castingMoveSpeedModifierId);
        MANAGED_MODIFIER_IDS.add(this.castingMoveSpeedModifierId);
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return true;
    }

    @Override
    public boolean applyEffectTick(LivingEntity entity, int amplifier) {
        syncCastingMoveSpeedModifiers(entity, entity.getAttributes(), null, null);
        return true;
    }

    @Override
    public void onEffectStarted(LivingEntity livingEntity, int amplifier) {
        syncCastingMoveSpeedModifiers(livingEntity, livingEntity.getAttributes(), new ActiveEffect(this, amplifier), null);
    }

    @Override
    public void onEffectAdded(LivingEntity livingEntity, int amplifier) {
        super.onEffectAdded(livingEntity, amplifier);
        syncCastingMoveSpeedModifiers(livingEntity, livingEntity.getAttributes(), new ActiveEffect(this, amplifier), null);
    }

    @Override
    public void addAttributeModifiers(AttributeMap attributeMap, int amplifier) {
        super.addAttributeModifiers(attributeMap, amplifier);
    }

    @Override
    public void removeAttributeModifiers(AttributeMap attributeMap) {
        super.removeAttributeModifiers(attributeMap);

        var attributeInstance = attributeMap.getInstance(AttributeRegistry.CASTING_MOVESPEED);
        if (attributeInstance != null) {
            removeManagedModifiers(attributeInstance);
        }
    }

    protected boolean isCastingMoveSpeedContributionEnabled(int amplifier) {
        return true;
    }

    protected double getTargetCastingMoveSpeedBonus(int amplifier) {
        return CastingMoveSpeedAdjustment.MAX_CASTING_MOVE_SPEED_BONUS;
    }

    protected final ResourceLocation getCastingMoveSpeedModifierId() {
        return castingMoveSpeedModifierId;
    }

    private AttributeModifier createCastingMoveSpeedModifier(double amount) {
        return new AttributeModifier(
                castingMoveSpeedModifierId,
                amount,
                AttributeModifier.Operation.ADD_VALUE
        );
    }

    private static void syncCastingMoveSpeedModifiers(
            LivingEntity livingEntity,
            AttributeMap attributeMap,
            @Nullable ActiveEffect forcedEffect,
            @Nullable ResourceLocation excludedModifierId
    ) {
        var attributeInstance = attributeMap.getInstance(AttributeRegistry.CASTING_MOVESPEED);
        if (attributeInstance == null) {
            return;
        }

        removeManagedModifiers(attributeInstance);

        var activeEffects = collectActiveEffects(livingEntity, forcedEffect, excludedModifierId);
        if (activeEffects.isEmpty()) {
            return;
        }

        // 毎 tick 現在値から積み直し、後付け/解除された外部 modifier に次 tick で追従する。
        var externalBonus = computeExternalCastingMoveSpeedBonus(attributeInstance);
        var totalTargetBonus = activeEffects.values().stream()
                .mapToDouble(ActiveEffect::targetBonus)
                .sum();

        for (var activeEffect : activeEffects.values()) {
            var adjustedBonus = CastingMoveSpeedAdjustment.computeSharedBonus(
                    externalBonus,
                    activeEffect.targetBonus(),
                    totalTargetBonus
            );
            if (adjustedBonus <= 0.0D) {
                continue;
            }

            attributeInstance.addTransientModifier(activeEffect.effect().createCastingMoveSpeedModifier(adjustedBonus));
        }
    }

    private static void removeManagedModifiers(AttributeInstance attributeInstance) {
        for (var modifierId : MANAGED_MODIFIER_IDS) {
            attributeInstance.removeModifier(modifierId);
        }
    }

    private static double computeExternalCastingMoveSpeedBonus(AttributeInstance attributeInstance) {
        return Math.max(
                0.0D,
                attributeInstance.getValue() - attributeInstance.getAttribute().value().getDefaultValue()
        );
    }

    private static LinkedHashMap<ResourceLocation, ActiveEffect> collectActiveEffects(
            LivingEntity livingEntity,
            @Nullable ActiveEffect forcedEffect,
            @Nullable ResourceLocation excludedModifierId
    ) {
        var activeEffects = new LinkedHashMap<ResourceLocation, ActiveEffect>();

        for (MobEffectInstance activeInstance : livingEntity.getActiveEffects()) {
            if (!(activeInstance.getEffect().value() instanceof DynamicCastingMobilityEffect dynamicEffect)) {
                continue;
            }

            if (!dynamicEffect.isCastingMoveSpeedContributionEnabled(activeInstance.getAmplifier())) {
                continue;
            }

            var modifierId = dynamicEffect.getCastingMoveSpeedModifierId();
            if (modifierId.equals(excludedModifierId)) {
                continue;
            }

            activeEffects.put(modifierId, new ActiveEffect(dynamicEffect, activeInstance.getAmplifier()));
        }

        if (forcedEffect != null && forcedEffect.effect().isCastingMoveSpeedContributionEnabled(forcedEffect.amplifier())) {
            var modifierId = forcedEffect.effect().getCastingMoveSpeedModifierId();
            if (!modifierId.equals(excludedModifierId)) {
                activeEffects.put(modifierId, forcedEffect);
            }
        }

        return activeEffects;
    }

    private record ActiveEffect(
            DynamicCastingMobilityEffect effect,
            int amplifier
    ) {
        private double targetBonus() {
            return effect.getTargetCastingMoveSpeedBonus(amplifier);
        }
    }
}
