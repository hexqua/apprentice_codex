package jp.aquafactory.apprenticecodex.spell.bloodbrand;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.damage.DamageTypes;
import jp.aquafactory.apprenticecodex.registry.AttachmentRegistry;
import jp.aquafactory.apprenticecodex.registry.EffectRegistry;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

@EventBusSubscriber(modid = ApprenticeCodex.MODID)
public final class BloodBrandEvents {
    private BloodBrandEvents() {
    }

    @SubscribeEvent
    public static void onEntityTick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof LivingEntity living) || living.level().isClientSide) {
            return;
        }

        var state = living.getExistingDataOrNull(AttachmentRegistry.BLOOD_BRAND_STATE);
        if (state != null && !living.hasEffect(EffectRegistry.BLOOD_ENGRAVED)) {
            living.removeData(AttachmentRegistry.BLOOD_BRAND_STATE);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onLivingDeath(LivingDeathEvent event) {
        var target = event.getEntity();
        if (event.isCanceled() || !(target.level() instanceof ServerLevel level)
                || !target.hasEffect(EffectRegistry.BLOOD_ENGRAVED)) {
            return;
        }

        var state = target.getExistingDataOrNull(AttachmentRegistry.BLOOD_BRAND_STATE);
        if (state == null || state.isEmpty()) {
            return;
        }

        // 連鎖起爆中も同じ対象を再処理しないよう、周囲へダメージを与える前に消費する。
        target.removeData(AttachmentRegistry.BLOOD_BRAND_STATE);
        var caster = resolveCaster(level, state.casterUuid());
        if (caster == null) {
            return;
        }

        BloodBrandBurst.burst(level, target, caster, state, event.getSource().is(DamageTypes.HIGANBANA));
    }

    @SubscribeEvent
    public static void onLivingDamage(LivingDamageEvent.Post event) {
        if (event.getEntity().level().isClientSide) {
            return;
        }

        var source = event.getSource();
        float healRate;
        if (source.is(DamageTypes.BLOOD_BRAND_HIGANBANA_BURST)) {
            healRate = 1.0F;
        } else if (source.is(DamageTypes.BLOOD_BRAND_BURST)) {
            healRate = 0.5F;
        } else {
            return;
        }

        if (source.getEntity() instanceof LivingEntity caster && event.getNewDamage() > 0.0F) {
            caster.heal(event.getNewDamage() * healRate);
        }
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
