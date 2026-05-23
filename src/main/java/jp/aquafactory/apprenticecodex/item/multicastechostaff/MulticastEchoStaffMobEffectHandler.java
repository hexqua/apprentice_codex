package jp.aquafactory.apprenticecodex.item.multicastechostaff;

import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.config.ApprenticeCodexServerConfig;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

@EventBusSubscriber(modid = ApprenticeCodex.MODID)
public final class MulticastEchoStaffMobEffectHandler {
    private static final ThreadLocal<CastContext> ACTIVE_CAST = new ThreadLocal<>();
    private static final ThreadLocal<Boolean> SUPPRESS_CAPTURE = ThreadLocal.withInitial(() -> false);

    private MulticastEchoStaffMobEffectHandler() {
    }

    public static void runRepeatedCast(ServerPlayer caster, AbstractSpell spell, Runnable castAction) {
        var profile = resolveActiveProfile(spell);
        if (profile == null) {
            castAction.run();
            return;
        }

        var context = new CastContext(profile, new ArrayList<>());
        var previousContext = ACTIVE_CAST.get();
        ACTIVE_CAST.set(context);
        try {
            castAction.run();
        } finally {
            if (previousContext == null) {
                ACTIVE_CAST.remove();
            } else {
                ACTIVE_CAST.set(previousContext);
            }
        }

        applyCapturedEffects(context);
    }

    private static @Nullable MulticastEchoStaffMobEffectProfile resolveActiveProfile(AbstractSpell spell) {
        if (!ApprenticeCodexServerConfig.multicastEchoStaffMobEffectProfilesEnabled()) {
            return null;
        }

        return MulticastEchoStaffMobEffectProfileManager.getProfile(spell).orElse(null);
    }

    @SubscribeEvent
    public static void onMobEffectAdded(MobEffectEvent.Added event) {
        var context = ACTIVE_CAST.get();
        if (context == null || SUPPRESS_CAPTURE.get()) {
            return;
        }

        var target = event.getEntity();
        if (target.level().isClientSide
                || event.getOldEffectInstance() == null
                || !target.isAlive()) {
            return;
        }

        var attempted = event.getEffectInstance();
        var effect = attempted.getEffect();
        if (effect.value().isInstantenous() || !isCategoryEnabled(effect.value().getCategory())) {
            return;
        }

        // Forge 1.20.1 の Added は既存効果との merge 前に同期発火するため、ここでは記録だけ行う。
        // 実際の上書きは castSpell 後に行い、Iron's 側や他 MOD の addEffect 結果を先に確定させる。
        context.captures().add(new EffectCapture(
                target,
                effect,
                attempted.getDuration(),
                attempted.getAmplifier(),
                event.getEffectSource()
        ));
    }

    private static boolean isCategoryEnabled(MobEffectCategory category) {
        return switch (category) {
            case BENEFICIAL -> ApprenticeCodexServerConfig.multicastEchoStaffBeneficialMobEffectsEnabled();
            case HARMFUL -> ApprenticeCodexServerConfig.multicastEchoStaffHarmfulMobEffectsEnabled();
            case NEUTRAL -> ApprenticeCodexServerConfig.multicastEchoStaffNeutralMobEffectsEnabled();
        };
    }

    private static void applyCapturedEffects(CastContext context) {
        if (context.captures().isEmpty()) {
            return;
        }

        var previousSuppress = SUPPRESS_CAPTURE.get();
        SUPPRESS_CAPTURE.set(true);
        try {
            for (var capture : context.captures()) {
                applyCapturedEffect(context.profile(), capture);
            }
        } finally {
            SUPPRESS_CAPTURE.set(previousSuppress);
        }
    }

    private static void applyCapturedEffect(MulticastEchoStaffMobEffectProfile profile, EffectCapture capture) {
        var target = capture.target();
        if (!target.isAlive()) {
            return;
        }

        var current = target.getEffect(capture.effect());
        if (current == null) {
            return;
        }

        var desiredDuration = resolveDesiredDuration(profile, current.getDuration(), capture.attemptedDuration());
        var desiredAmplifier = resolveDesiredAmplifier(profile, current.getAmplifier(), capture.attemptedAmplifier());
        if (desiredDuration <= current.getDuration() && desiredAmplifier <= current.getAmplifier()) {
            return;
        }

        target.addEffect(new MobEffectInstance(
                capture.effect(),
                desiredDuration,
                desiredAmplifier,
                current.isAmbient(),
                current.isVisible(),
                current.showIcon()
        ), capture.source());
    }

    private static int resolveDesiredDuration(
            MulticastEchoStaffMobEffectProfile profile,
            int currentDuration,
            int attemptedDuration
    ) {
        var bonus = (int) Math.floor(attemptedDuration * Math.max(0.0D, profile.durationExtendRate()))
                + Math.max(0, profile.durationExtendFlat());
        if (bonus <= 0) {
            return currentDuration;
        }

        var desiredDuration = addClamped(currentDuration, bonus);
        desiredDuration = applyUpperLimitWithoutShortening(currentDuration, desiredDuration, Math.max(0, profile.durationExtendLimit()));
        var serverCapTicks = Math.max(0, ApprenticeCodexServerConfig.multicastEchoStaffDurationServerCapTicks());
        if (ApprenticeCodexServerConfig.multicastEchoStaffDurationServerCapEnabled() && serverCapTicks > 0) {
            desiredDuration = applyUpperLimitWithoutShortening(
                    currentDuration,
                    desiredDuration,
                    serverCapTicks
            );
        }
        return desiredDuration;
    }

    private static int resolveDesiredAmplifier(
            MulticastEchoStaffMobEffectProfile profile,
            int currentAmplifier,
            int attemptedAmplifier
    ) {
        var attemptedLevel = attemptedAmplifier + 1;
        var bonus = (int) Math.floor(attemptedLevel * Math.max(0.0D, profile.amplifierStackRate()))
                + Math.max(0, profile.amplifierStackFlat());
        if (bonus <= 0) {
            return currentAmplifier;
        }

        var desiredAmplifier = addClamped(currentAmplifier, bonus);
        desiredAmplifier = applyUpperLimitWithoutShortening(
                currentAmplifier,
                desiredAmplifier,
                Math.max(0, profile.amplifierStackLimit())
        );
        var serverCap = Math.max(0, ApprenticeCodexServerConfig.multicastEchoStaffAmplifierServerCap());
        if (ApprenticeCodexServerConfig.multicastEchoStaffAmplifierServerCapEnabled() && serverCap > 0) {
            desiredAmplifier = applyUpperLimitWithoutShortening(
                    currentAmplifier,
                    desiredAmplifier,
                    serverCap
            );
        }
        return desiredAmplifier;
    }

    private static int addClamped(int base, int bonus) {
        if (Integer.MAX_VALUE - base < bonus) {
            return Integer.MAX_VALUE;
        }
        return base + bonus;
    }

    private static int applyUpperLimitWithoutShortening(int currentValue, int desiredValue, int limit) {
        if (limit <= 0 || currentValue >= limit) {
            return currentValue;
        }
        return Math.min(desiredValue, limit);
    }

    private record CastContext(MulticastEchoStaffMobEffectProfile profile, List<EffectCapture> captures) {
    }

    private record EffectCapture(
            LivingEntity target,
            Holder<MobEffect> effect,
            int attemptedDuration,
            int attemptedAmplifier,
            @Nullable Entity source
    ) {
    }
}
