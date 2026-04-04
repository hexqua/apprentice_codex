package jp.aquafactory.apprenticecodex.effect;

import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
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
import java.util.UUID;

public abstract class DynamicCastingMobilityEffect extends MobEffect {
    private static final Set<UUID> MANAGED_MODIFIER_UUIDS = new LinkedHashSet<>();

    private final String castingMoveSpeedModifierId;
    private final UUID castingMoveSpeedModifierUuid;

    protected DynamicCastingMobilityEffect(int color, String castingMoveSpeedModifierId) {
        super(MobEffectCategory.BENEFICIAL, color);
        this.castingMoveSpeedModifierId = castingMoveSpeedModifierId;
        this.castingMoveSpeedModifierUuid = UUID.fromString(castingMoveSpeedModifierId);
        MANAGED_MODIFIER_UUIDS.add(this.castingMoveSpeedModifierUuid);
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        return true;
    }

    @Override
    public void applyEffectTick(LivingEntity entity, int amplifier) {
        syncCastingMoveSpeedModifiers(entity, entity.getAttributes(), null, null);
    }

    @Override
    public void addAttributeModifiers(LivingEntity livingEntity, AttributeMap attributeMap, int amplifier) {
        super.addAttributeModifiers(livingEntity, attributeMap, amplifier);
        syncCastingMoveSpeedModifiers(livingEntity, attributeMap, new ActiveEffect(this, amplifier), null);
    }

    @Override
    public void removeAttributeModifiers(LivingEntity livingEntity, AttributeMap attributeMap, int amplifier) {
        super.removeAttributeModifiers(livingEntity, attributeMap, amplifier);
        syncCastingMoveSpeedModifiers(livingEntity, attributeMap, null, castingMoveSpeedModifierUuid);
    }

    protected boolean isCastingMoveSpeedContributionEnabled(int amplifier) {
        return true;
    }

    protected double getTargetCastingMoveSpeedBonus(int amplifier) {
        return CastingMoveSpeedAdjustment.MAX_CASTING_MOVE_SPEED_BONUS;
    }

    protected final UUID getCastingMoveSpeedModifierUuid() {
        return castingMoveSpeedModifierUuid;
    }

    private AttributeModifier createCastingMoveSpeedModifier(double amount) {
        return new AttributeModifier(
                castingMoveSpeedModifierUuid,
                castingMoveSpeedModifierId,
                amount,
                AttributeModifier.Operation.ADDITION
        );
    }

    private static void syncCastingMoveSpeedModifiers(
            LivingEntity livingEntity,
            AttributeMap attributeMap,
            @Nullable ActiveEffect forcedEffect,
            @Nullable UUID excludedModifierUuid
    ) {
        var attributeInstance = attributeMap.getInstance(AttributeRegistry.CASTING_MOVESPEED.get());
        if (attributeInstance == null) {
            return;
        }

        removeManagedModifiers(attributeInstance);

        var activeEffects = collectActiveEffects(livingEntity, forcedEffect, excludedModifierUuid);
        if (activeEffects.isEmpty()) {
            return;
        }

        // Iron's Spellbooks 本体の最終式は維持し、この mod 側は外部加算で残る headroom だけを使う。
        var externalBonus = Math.max(
                0.0D,
                attributeInstance.getValue() - attributeInstance.getAttribute().getDefaultValue()
        );
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
        for (var modifierUuid : MANAGED_MODIFIER_UUIDS) {
            attributeInstance.removeModifier(modifierUuid);
        }
    }

    private static LinkedHashMap<UUID, ActiveEffect> collectActiveEffects(
            LivingEntity livingEntity,
            @Nullable ActiveEffect forcedEffect,
            @Nullable UUID excludedModifierUuid
    ) {
        var activeEffects = new LinkedHashMap<UUID, ActiveEffect>();

        for (MobEffectInstance activeInstance : livingEntity.getActiveEffects()) {
            if (!(activeInstance.getEffect() instanceof DynamicCastingMobilityEffect dynamicEffect)) {
                continue;
            }

            if (!dynamicEffect.isCastingMoveSpeedContributionEnabled(activeInstance.getAmplifier())) {
                continue;
            }

            var modifierUuid = dynamicEffect.getCastingMoveSpeedModifierUuid();
            if (modifierUuid.equals(excludedModifierUuid)) {
                continue;
            }

            activeEffects.put(modifierUuid, new ActiveEffect(dynamicEffect, activeInstance.getAmplifier()));
        }

        if (forcedEffect != null && forcedEffect.effect().isCastingMoveSpeedContributionEnabled(forcedEffect.amplifier())) {
            var modifierUuid = forcedEffect.effect().getCastingMoveSpeedModifierUuid();
            if (!modifierUuid.equals(excludedModifierUuid)) {
                activeEffects.put(modifierUuid, forcedEffect);
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
