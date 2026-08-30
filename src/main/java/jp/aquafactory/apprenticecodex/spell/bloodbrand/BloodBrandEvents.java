package jp.aquafactory.apprenticecodex.spell.bloodbrand;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.damage.DamageTypes;
import jp.aquafactory.apprenticecodex.registry.EffectRegistry;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

@Mod.EventBusSubscriber(modid = ApprenticeCodex.MODID)
public final class BloodBrandEvents {
    private BloodBrandEvents() {
    }

    @SubscribeEvent
    public static void onEntityTick(LivingEvent.LivingTickEvent event) {
        var living = event.getEntity();
        if (living.level().isClientSide) {
            return;
        }

        var state = BloodBrandState.get(living);
        if (state != null && !living.hasEffect(EffectRegistry.BLOOD_ENGRAVED.get())) {
            BloodBrandState.remove(living);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onLivingDeath(LivingDeathEvent event) {
        var target = event.getEntity();
        if (event.isCanceled() || !(target.level() instanceof ServerLevel level)) {
            return;
        }

        BloodBrandState state;
        if (!(target instanceof Player) && event.getSource().is(DamageTypes.BLOOD_BRAND)
                && event.getSource().getDirectEntity() instanceof BloodBrandKunai kunai) {
            // 直撃が致死なら刻印処理へ戻る前に死亡イベントが完了するため、苦無自身の起爆情報を使う。
            state = kunai.createBurstState();
        } else if (target.hasEffect(EffectRegistry.BLOOD_ENGRAVED.get())) {
            state = BloodBrandState.get(target);
        } else {
            return;
        }
        if (state == null || state.isEmpty()) {
            return;
        }

        // 連鎖起爆中も同じ対象を再処理しないよう、周囲へダメージを与える前に消費する。
        BloodBrandState.remove(target);
        var caster = resolveCaster(level, state.casterUuid());
        if (caster == null) {
            return;
        }

        BloodBrandBurst.burst(level, target, caster, state, event.getSource().is(DamageTypes.HIGANBANA));
    }

    private static @Nullable LivingEntity resolveCaster(ServerLevel originLevel, UUID casterUuid) {
        var player = originLevel.getServer().getPlayerList().getPlayer(casterUuid);
        if (player != null) {
            return player;
        }

        for (var level : originLevel.getServer().getAllLevels()) {
            if (level.getEntity(casterUuid) instanceof LivingEntity caster) {
                return caster;
            }
        }
        return null;
    }
}
