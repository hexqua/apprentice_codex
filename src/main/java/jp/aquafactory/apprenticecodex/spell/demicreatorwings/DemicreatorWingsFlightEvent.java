package jp.aquafactory.apprenticecodex.spell.demicreatorwings;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.capability.Capabilities;
import jp.aquafactory.apprenticecodex.capability.codexspelldata.CodexSpellStateTypeRegister;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber(modid = ApprenticeCodex.MODID)
public final class DemicreatorWingsFlightEvent {
    private DemicreatorWingsFlightEvent() {
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Pre event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        var spellData = Capabilities.getSpellDataOrNull(player);
        if (spellData == null) {
            return;
        }

        var state = spellData.get(CodexSpellStateTypeRegister.DEMICREATOR_WINGS_STATE);
        if (!state.active) {
            return;
        }

        var core = DemicreatorWingsManager.getManagedCore(player);
        if (core == null || !DemicreatorWingsManager.isInsideCoreArea(player, core)) {
            DemicreatorWingsManager.deactivate(player, true);
            return;
        }

        DemicreatorWingsManager.ensureFlightGranted(player);
        DemicreatorWingsManager.ensureWing(player);
        player.fallDistance = 0.0f;

        var remainingTicks = core.getRemainingTicks();
        var alertInterval = remainingTicks <= DemicreatorWingsManager.ALERT_FAST_THRESHOLD_TICKS
                ? DemicreatorWingsManager.ALERT_FAST_INTERVAL_TICKS
                : DemicreatorWingsManager.ALERT_INTERVAL_TICKS;
        if (remainingTicks <= DemicreatorWingsManager.ALERT_THRESHOLD_TICKS
                && player.level().getGameTime() % alertInterval == 0) {
            player.playNotifySound(SoundEvents.AMETHYST_BLOCK_BREAK, SoundSource.PLAYERS, 0.75f, 1.0f);
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            DemicreatorWingsManager.deactivate(player, true);
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            DemicreatorWingsManager.deactivate(player, true);
        }
    }

    @SubscribeEvent
    public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            DemicreatorWingsManager.deactivate(player, true);
        }
    }

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            DemicreatorWingsManager.deactivate(player, true);
        }
    }
}
