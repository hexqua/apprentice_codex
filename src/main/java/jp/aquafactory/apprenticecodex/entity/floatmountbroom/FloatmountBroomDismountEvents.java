package jp.aquafactory.apprenticecodex.entity.floatmountbroom;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityMountEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@EventBusSubscriber(modid = ApprenticeCodex.MODID)
public final class FloatmountBroomDismountEvents {
    private static final Map<UUID, Confirmation> CONFIRMATIONS = new HashMap<>();
    private static final Map<UUID, SneakInput> SNEAK_INPUTS = new HashMap<>();
    private static final Set<UUID> AUTHORIZED_DISMOUNTS = new HashSet<>();

    private FloatmountBroomDismountEvents() {
    }

    @SubscribeEvent
    public static void onDismount(EntityMountEvent event) {
        if (!event.isDismounting() || event.getLevel().isClientSide
                || !(event.getEntityMounting() instanceof Player player)
                || !(event.getEntityBeingMounted() instanceof FloatmountBroomEntity broom)
                || broom.isBreaking() || !player.isAlive()) {
            return;
        }

        var playerId = player.getUUID();
        if (AUTHORIZED_DISMOUNTS.contains(playerId)) {
            clear(playerId);
            return;
        }

        if (!broom.isDangerousDismount()) {
            clear(playerId);
            return;
        }

        var input = SNEAK_INPUTS.get(playerId);
        if (input == null || !input.pressed || !input.broomId.equals(broom.getUUID())) {
            return;
        }

        event.setCanceled(true);
        var now = player.level().getGameTime();
        var confirmation = CONFIRMATIONS.get(playerId);
        if (!isActive(confirmation, broom, now)) {
            beginConfirmation(player, broom, now);
        }
    }

    public static void handleSneakInput(Player player, FloatmountBroomEntity broom, boolean pressed) {
        if (player.level().isClientSide || player.getVehicle() != broom || broom.getControllingPassenger() != player) {
            return;
        }

        var playerId = player.getUUID();
        var previous = SNEAK_INPUTS.put(playerId, new SneakInput(broom.getUUID(), pressed));
        if (previous != null && previous.broomId.equals(broom.getUUID()) && previous.pressed == pressed) {
            return;
        }

        var now = player.level().getGameTime();
        var confirmation = CONFIRMATIONS.get(playerId);
        if (!pressed) {
            if (isActive(confirmation, broom, now) && !confirmation.released) {
                CONFIRMATIONS.put(playerId, new Confirmation(broom.getUUID(), confirmation.warningTick, true));
            }
            return;
        }

        if (!broom.isDangerousDismount()) {
            CONFIRMATIONS.remove(playerId);
            return;
        }

        if (isActive(confirmation, broom, now) && confirmation.released) {
            CONFIRMATIONS.remove(playerId);
            AUTHORIZED_DISMOUNTS.add(playerId);
            try {
                player.stopRiding();
            } finally {
                AUTHORIZED_DISMOUNTS.remove(playerId);
                clear(playerId);
            }
        } else if (!isActive(confirmation, broom, now)) {
            beginConfirmation(player, broom, now);
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Pre event) {
        if (!(event.getEntity() instanceof Player player) || player.level().isClientSide) {
            return;
        }
        var playerId = player.getUUID();
        var confirmation = CONFIRMATIONS.get(playerId);
        if (confirmation == null && !SNEAK_INPUTS.containsKey(playerId)) {
            return;
        }
        var now = player.level().getGameTime();
        if (!(player.getVehicle() instanceof FloatmountBroomEntity)
                || !player.isAlive()) {
            clear(playerId);
        } else if (confirmation != null
                && now - confirmation.warningTick > FloatmountBroomEntity.DISMOUNT_CONFIRM_TICKS) {
            CONFIRMATIONS.remove(playerId);
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        clear(event.getEntity().getUUID());
    }

    private static boolean isActive(Confirmation confirmation, FloatmountBroomEntity broom, long now) {
        return confirmation != null
                && confirmation.broomId.equals(broom.getUUID())
                && now - confirmation.warningTick <= FloatmountBroomEntity.DISMOUNT_CONFIRM_TICKS;
    }

    private static void beginConfirmation(Player player, FloatmountBroomEntity broom, long now) {
        CONFIRMATIONS.put(player.getUUID(), new Confirmation(broom.getUUID(), now, false));
        player.displayClientMessage(Component.translatable(
                "ui.apprenticecodex.floatmount_broom.warning_dismount",
                Component.keybind("key.sneak")
        ).withStyle(ChatFormatting.YELLOW), true);
    }

    private static void clear(UUID playerId) {
        CONFIRMATIONS.remove(playerId);
        SNEAK_INPUTS.remove(playerId);
        AUTHORIZED_DISMOUNTS.remove(playerId);
    }

    private record Confirmation(UUID broomId, long warningTick, boolean released) {
    }

    private record SneakInput(UUID broomId, boolean pressed) {
    }
}
