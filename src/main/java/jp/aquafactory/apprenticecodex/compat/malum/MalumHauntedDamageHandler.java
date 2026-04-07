package jp.aquafactory.apprenticecodex.compat.malum;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.damage.DamageTypes;
import jp.aquafactory.apprenticecodex.utility.CombatTools;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;

@EventBusSubscriber(modid = ApprenticeCodex.MODID)
public final class MalumHauntedDamageHandler {
    private static final ThreadLocal<Integer> RECURSION_DEPTH = ThreadLocal.withInitial(() -> 0);

    private MalumHauntedDamageHandler() {
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onLivingDamage(LivingDamageEvent.Post event) {
        if (!ModList.get().isLoaded(MalumCompatibility.MOD_ID) || event.getEntity().level().isClientSide) {
            return;
        }

        if (event.getNewDamage() <= 0f || event.getEntity().isDeadOrDying()) {
            return;
        }

        if (RECURSION_DEPTH.get() > 0 || event.getSource().is(DamageTypes.HAUNTED_BONUS)) {
            return;
        }

        var attacker = resolveAttacker(event.getSource());
        if (attacker == null) {
            return;
        }

        var mainHandStack = attacker.getMainHandItem();
        var hauntedLevel = MalumCompatibility.getHauntedLevel(mainHandStack);
        if (hauntedLevel <= 0) {
            return;
        }

        if (!shouldApplyHauntedBonus(event.getSource(), attacker)) {
            return;
        }

        RECURSION_DEPTH.set(RECURSION_DEPTH.get() + 1);
        try {
            event.getEntity().hurt(
                    CombatTools.getDamageSource(attacker.level(), attacker, attacker, DamageTypes.HAUNTED_BONUS),
                    hauntedLevel
            );
        } finally {
            RECURSION_DEPTH.set(Math.max(0, RECURSION_DEPTH.get() - 1));
        }
    }

    private static boolean shouldApplyHauntedBonus(DamageSource source, LivingEntity attacker) {
        // 1.20.1 / 1.21.1 ともに Iron's の詠唱魔法へは Haunted を乗せない前提に合わせる。
        return isDirectWeaponAttack(source, attacker);
    }

    private static boolean isDirectWeaponAttack(DamageSource source, LivingEntity attacker) {
        if (source.getEntity() != attacker || source.getDirectEntity() != attacker) {
            return false;
        }

        return source.is(net.minecraft.world.damagesource.DamageTypes.PLAYER_ATTACK)
                || source.is(net.minecraft.world.damagesource.DamageTypes.MOB_ATTACK)
                || "player".equals(source.getMsgId());
    }
    private static LivingEntity resolveAttacker(DamageSource source) {
        if (source.getEntity() instanceof LivingEntity livingEntity) {
            return livingEntity;
        }

        return source.getDirectEntity() instanceof LivingEntity livingEntity ? livingEntity : null;
    }
}
