package jp.aquafactory.apprenticecodex.compat.malum;

import com.sammy.malum.common.item.IMalumEventResponder;
import com.sammy.malum.common.item.curiosities.weapons.scythe.MalumScytheItem;
import com.sammy.malum.registry.common.MalumDamageTypes;
import com.sammy.malum.registry.common.MalumParticleEffectTypes;
import com.sammy.malum.registry.common.MalumSoundEvents;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import team.lodestar.lodestone.handlers.ItemEventHandler;
import team.lodestar.lodestone.helpers.SoundHelper;

import java.util.List;

final class MalumSpellReaperScytheBridgeImpl {
    private static final ResourceLocation RESPONDER_SOURCE_ID = ResourceLocation.fromNamespaceAndPath(
            ApprenticeCodex.MODID,
            "spell_reaper_scythe"
    );
    private static final IMalumEventResponder RESPONDER = new SpellReaperScytheResponder();
    private static boolean registered;

    private MalumSpellReaperScytheBridgeImpl() {
    }

    static void register() {
        if (registered) {
            return;
        }
        registered = true;

        // Item本体をMalum型へ継承させず、本家大鎌と同じLodestoneの攻撃レスポンダー経路だけを追加する。
        ItemEventHandler.registerLookup(new ItemEventHandler.EventResponderSource(
                RESPONDER_SOURCE_ID,
                entity -> List.of(entity.getMainHandItem()),
                (entity, stack) -> stack.is(ItemRegistry.SPELL_REAPER_SCYTHE.get()) ? RESPONDER : null
        ));
    }

    static boolean shouldUseNoSweepCombo(LivingEntity attacker) {
        return !MalumScytheItem.canSweep(attacker);
    }

    private static final class SpellReaperScytheResponder implements IMalumEventResponder {
        @Override
        public void outgoingDamageEvent(
                LivingDamageEvent.Pre event,
                LivingEntity attacker,
                LivingEntity target,
                ItemStack stack
        ) {
            if (!(attacker.level() instanceof ServerLevel serverLevel)
                    || !event.getSource().is(MalumDamageTypes.SCYTHE_MELEE)) {
                return;
            }

            var particle = MalumParticleEffectTypes.SCYTHE_SLASH.createEffect()
                    .originatesFrom(attacker)
                    .targets(target)
                    .color(stack.getItem())
                    .upwardOffset(-0.4F)
                    .forwardOffset(0.8F);
            if (shouldUseNoSweepCombo(attacker)) {
                SoundHelper.playSound(attacker, MalumSoundEvents.SCYTHE_CUT.value(), 1.0F, 0.75F);
                particle.verticalSlashRotation().horizontalOffset(0.6F).spawn(serverLevel);
                return;
            }

            SoundHelper.playSound(attacker, MalumSoundEvents.SCYTHE_SWEEP.value(), 1.0F, 1.0F);
            particle.mirroredRandomly(attacker.getRandom()).spawn(serverLevel);
            MalumScytheItem.trySweep(attacker, target, event.getNewDamage());
        }
    }
}
