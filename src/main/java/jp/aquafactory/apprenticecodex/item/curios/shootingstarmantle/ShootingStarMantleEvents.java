package jp.aquafactory.apprenticecodex.item.curios.shootingstarmantle;

import io.redspace.ironsspellbooks.api.magic.SpellSelectionManager;
import io.redspace.ironsspellbooks.api.spells.SpellData;
import io.redspace.ironsspellbooks.network.EquipmentChangedPacket;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.registry.SpellRegistry;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import top.theillusivec4.curios.api.event.CurioChangeEvent;

@EventBusSubscriber(modid = ApprenticeCodex.MODID)
public final class ShootingStarMantleEvents {
    private ShootingStarMantleEvents() { }

    @SubscribeEvent
    public static void selection(SpellSelectionManager.SpellSelectionEvent event) {
        if (!ShootingStarMantleRuntime.findEquipped(event.getEntity()).isEmpty()) {
            event.addSelectionOption(new SpellData(SpellRegistry.WAVERING_STAR.get(), 1), ShootingStarMantleRuntime.SPELL_SLOT, 0);
        }
    }

    @SubscribeEvent
    public static void tick(PlayerTickEvent.Pre event) {
        if (event.getEntity() instanceof ServerPlayer player) ShootingStarMantleRuntime.tick(player);
    }

    @SubscribeEvent
    public static void equipped(CurioChangeEvent event) {
        if (event.getEntity() instanceof ServerPlayer player
                && (event.getFrom().getItem() instanceof ShootingStarMantle || event.getTo().getItem() instanceof ShootingStarMantle)) {
            // CuriosはCustom Data更新でもこのeventを発火する。残量更新で浮遊を解除しない。
            ShootingStarMantleRuntime.refreshEquipment(player);
            if (event.getFrom().getItem() != event.getTo().getItem()) {
                PacketDistributor.sendToPlayer(player, new EquipmentChangedPacket());
            }
        }
    }

    @SubscribeEvent
    public static void login(PlayerEvent.PlayerLoggedInEvent event) { reset(event); }
    @SubscribeEvent
    public static void dimension(PlayerEvent.PlayerChangedDimensionEvent event) { reset(event); }
    @SubscribeEvent
    public static void respawn(PlayerEvent.PlayerRespawnEvent event) { reset(event); }
    @SubscribeEvent
    public static void logout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) ShootingStarMantleRuntime.clear(player);
    }
    @SubscribeEvent
    public static void death(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) ShootingStarMantleRuntime.clear(player);
    }
    @SubscribeEvent
    public static void stopped(ServerStoppedEvent event) { ShootingStarMantleRuntime.clearServer(); }
    @SubscribeEvent
    public static void tracking(PlayerEvent.StartTracking event) {
        if (event.getEntity() instanceof ServerPlayer observer && event.getTarget() instanceof ServerPlayer target) {
            PacketDistributor.sendToPlayer(observer, ShootingStarMantleRuntime.packet(target, false, -1, false));
        }
    }

    private static void reset(PlayerEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ShootingStarMantleRuntime.clear(player);
            PacketDistributor.sendToPlayer(player, new EquipmentChangedPacket());
        }
    }
}
