package jp.aquafactory.apprenticecodex.item.curios.quickcastscrollcartridge;

import io.redspace.ironsspellbooks.api.magic.SpellSelectionManager;
import io.redspace.ironsspellbooks.api.spells.SpellData;
import io.redspace.ironsspellbooks.network.EquipmentChangedPacket;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.TickEvent;
import io.redspace.ironsspellbooks.setup.PacketDistributor;
import top.theillusivec4.curios.api.event.CurioChangeEvent;

@EventBusSubscriber(modid = ApprenticeCodex.MODID)
public final class QuickcastCartridgeEvents {
    private QuickcastCartridgeEvents() {}

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onSelection(SpellSelectionManager.SpellSelectionEvent event) {
        var stack = QuickcastCartridgeCasting.findEquipped(event.getEntity());
        if (stack.isEmpty()) return;
        var data = QuickcastScrollCartridge.getSelectedSpellData(stack);
        if (data != SpellData.EMPTY) event.addSelectionOption(data, QuickcastCartridgeCasting.SLOT, 0);
    }

    @SubscribeEvent
    public static void onEquipment(CurioChangeEvent event) {
        if (event.getEntity() instanceof ServerPlayer player
                && (event.getFrom().getItem() instanceof QuickcastScrollCartridge
                || event.getTo().getItem() instanceof QuickcastScrollCartridge)) {
            QuickcastCartridgeCharge.equipmentChanged(player);
            PacketDistributor.sendToPlayer(player, new EquipmentChangedPacket());
        }
    }

    @SubscribeEvent
    public static void onTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.START) return;
        if (event.player instanceof ServerPlayer player) {
            QuickcastCartridgeCasting.validate(player);
            QuickcastCartridgeCharge.tick(player);
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) QuickcastCartridgeCharge.forget(player);
    }

    @SubscribeEvent
    public static void onDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            QuickcastCartridgeCharge.forget(player);
            QuickcastCartridgeCharge.tick(player);
        }
    }

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) QuickcastCartridgeCharge.forget(player);
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) QuickcastCartridgeCharge.tick(player);
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) QuickcastCartridgeCharge.tick(player);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onDamage(LivingDamageEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && event.getAmount() > 0) {
            QuickcastCartridgeCharge.interruptReload(player);
        }
    }

    @SubscribeEvent
    public static void onAttack(AttackEntityEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) QuickcastCartridgeCharge.interruptReload(player);
    }

    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        interruptInteraction(event);
    }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        interruptInteraction(event);
    }

    @SubscribeEvent
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        interruptInteraction(event);
    }

    @SubscribeEvent
    public static void onEntityInteractSpecific(PlayerInteractEvent.EntityInteractSpecific event) {
        interruptInteraction(event);
    }

    @SubscribeEvent
    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        interruptInteraction(event);
    }

    private static void interruptInteraction(PlayerInteractEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) QuickcastCartridgeCharge.interruptReload(player);
    }

    @SubscribeEvent
    public static void onUse(LivingEntityUseItemEvent.Start event) {
        if (event.getEntity() instanceof ServerPlayer player) QuickcastCartridgeCharge.interruptReload(player);
    }
}
