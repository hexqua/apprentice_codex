package jp.aquafactory.apprenticecodex.spell.phalanxcharge;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.registry.EffectRegistry;
import jp.aquafactory.apprenticecodex.registry.SoundRegistry;
import jp.aquafactory.apprenticecodex.spell.phalanxcharge.PhalanxWeaponryEntity;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.event.entity.living.LivingShieldBlockEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

import java.util.Map;
import java.util.WeakHashMap;

@EventBusSubscriber(modid = ApprenticeCodex.MODID)
public final class PhalanxGuardSuccessFlashEvent {
    private static final double SEARCH_RANGE = 8.0;
    private static final float FORCE_FIELD_VOLUME = 0.9f;
    private static final float FORCE_FIELD_PITCH = 1.0f;
    private static final int FLASH_INTERVAL_TICKS = 10;
    private static final Map<Player, Integer> LAST_GUARD_FLASH_TICK = new WeakHashMap<>();

    private PhalanxGuardSuccessFlashEvent() {
    }

    @SubscribeEvent
    public static void onShieldBlock(LivingShieldBlockEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        var level = player.level();
        if (level.isClientSide) {
            return;
        }

        if (!player.hasEffect(BuiltInRegistries.MOB_EFFECT.wrapAsHolder(EffectRegistry.PHALANX_STANCE.get()))) {
            return;
        }

        if (event.getBlockedDamage() <= 0.0f) {
            return;
        }

        var currentTick = player.tickCount;
        var lastTick = LAST_GUARD_FLASH_TICK.get(player);
        // バニラ盾のヒット間隔に合わせ、短時間連続ヒット時の演出連打を抑える.
        if (lastTick != null && currentTick - lastTick < FLASH_INTERVAL_TICKS) {
            return;
        }

        triggerGuardSuccess(player);
    }

    public static void triggerGuardSuccess(Player player) {
        var level = player.level();
        if (level.isClientSide) {
            return;
        }

        var box = player.getBoundingBox().inflate(SEARCH_RANGE);
        var entities = level.getEntitiesOfClass(
                PhalanxWeaponryEntity.class,
                box,
                entity -> {
                    var owner = entity.getOwner();
                    return owner != null && owner.getUUID().equals(player.getUUID());
                }
        );

        if (entities.isEmpty()) {
            return;
        }

        LAST_GUARD_FLASH_TICK.put(player, player.tickCount);

        level.playSound(
                null,
                player.getX(),
                player.getY(),
                player.getZ(),
                SoundRegistry.FORCE_FIELD.get(),
                SoundSource.PLAYERS,
                FORCE_FIELD_VOLUME,
                FORCE_FIELD_PITCH
        );

        for (var entity : entities) {
            entity.triggerGuardFlash(level);
        }
    }
}

