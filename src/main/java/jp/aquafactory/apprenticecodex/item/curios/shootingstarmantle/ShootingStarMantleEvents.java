package jp.aquafactory.apprenticecodex.item.curios.shootingstarmantle;

import io.redspace.ironsspellbooks.api.magic.SpellSelectionManager;
import io.redspace.ironsspellbooks.api.spells.SpellData;
import io.redspace.ironsspellbooks.network.EquipmentChangedPacket;
import io.redspace.ironsspellbooks.setup.PacketDistributor;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.enchantment.TranscendenceHelper;
import jp.aquafactory.apprenticecodex.network.Networks;
import jp.aquafactory.apprenticecodex.registry.SpellRegistry;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.event.TickEvent;
import top.theillusivec4.curios.api.event.CurioChangeEvent;

@Mod.EventBusSubscriber(modid = ApprenticeCodex.MODID)
public final class ShootingStarMantleEvents {
    private ShootingStarMantleEvents() { }

    @SubscribeEvent
    public static void selection(SpellSelectionManager.SpellSelectionEvent event) {
        var stack = ShootingStarMantleRuntime.findEquipped(event.getEntity());
        if (!stack.isEmpty()) {
            event.addSelectionOption(new SpellData(SpellRegistry.WAVERING_STAR.get(), 1), ShootingStarMantleRuntime.SPELL_SLOT, 0);
            var lookup = event.getEntity().level().registryAccess();
            int selectionIndex = 0;
            for (int slot = 0; slot < MantleCalibration.enabledSlots(stack, lookup); slot++) {
                var spell = MantleCalibration.readSpell(stack, slot, lookup);
                if (spell == SpellData.EMPTY) continue;
                // 固有魔法と分離し、Iron'sが扱うindexは空枠を除いた連番にする。
                event.addSelectionOption(TranscendenceHelper.resolveScrollSpellData(stack, spell),
                        ShootingStarMantleRuntime.SCROLL_SPELL_SLOT, selectionIndex++);
            }
        }
    }

    @SubscribeEvent
    public static void tick(TickEvent.PlayerTickEvent event) {
        if (event.phase == TickEvent.Phase.START && event.player instanceof ServerPlayer player)
            ShootingStarMantleRuntime.tick(player);
    }

    @SubscribeEvent
    public static void equipped(CurioChangeEvent event) {
        if (event.getEntity() instanceof ServerPlayer player
                && (event.getFrom().getItem() instanceof ShootingStarMantle || event.getTo().getItem() instanceof ShootingStarMantle)) {
            // CuriosはCustom Data更新でもこのeventを発火する。残量更新で浮遊を解除しない。
            ShootingStarMantleRuntime.refreshEquipment(player);
            if (!MantleCalibration.hasSameScrollSelection(event.getFrom(), event.getTo(), player.level().registryAccess())) {
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
            Networks.sendToPlayer(observer, ShootingStarMantleRuntime.packet(target, false, -1, false));
            Networks.sendToPlayer(observer, ShootingStarMantleRuntime.state(target).elemental.packet(target));
        }
    }

    private static void reset(PlayerEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ShootingStarMantleRuntime.clear(player);
            PacketDistributor.sendToPlayer(player, new EquipmentChangedPacket());
        }
    }
}
