package jp.aquafactory.apprenticecodex.item.psychicdrivegauntlets;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.utility.CombatTools;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Forgeのhurt戻り値だけでは分からない、物理命中の拒否を追撃処理へ伝える。 */
@Mod.EventBusSubscriber(modid = ApprenticeCodex.MODID)
public final class PsychicDriveGauntletsDamage {
    private static final ThreadLocal<Attempt> CURRENT = new ThreadLocal<>();

    private static final class Attempt {
        private final Entity target;
        private final DamageSource source;
        private LivingHurtEvent event;

        private Attempt(Entity target, DamageSource source) {
            this.target = target;
            this.source = source;
        }
    }

    private PsychicDriveGauntletsDamage() {}

    public static boolean applyPhysical(Entity target, float amount, DamageSource source,
                                        CombatTools.KnockbackTypes knockback) {
        var previous = CURRENT.get();
        var attempt = new Attempt(CombatTools.resolutePartEntity(target), source);
        CURRENT.set(attempt);
        try {
            boolean applied = CombatTools.applyUnscaledDamage(target, amount, source, knockback);
            // Forge 1.20.1はLivingHurtEventをキャンセルしてもhurtがtrueを返す。
            // event参照を保持し、後続listenerも終了した最終状態で追撃とドリル継続を判断する。
            return applied && (attempt.event == null
                    || !attempt.event.isCanceled() && attempt.event.getAmount() > 0);
        } finally {
            if (previous == null) CURRENT.remove();
            else CURRENT.set(previous);
        }
    }

    @SubscribeEvent(receiveCanceled = true)
    public static void observePhysicalHit(LivingHurtEvent event) {
        var attempt = CURRENT.get();
        // 同期的な再入で別のダメージが発生しても、最初の物理イベントだけを観測する。
        if (attempt != null && attempt.event == null && event.getEntity() == attempt.target
                && event.getSource() == attempt.source) attempt.event = event;
    }
}
