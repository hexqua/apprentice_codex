package jp.aquafactory.apprenticecodex.item.curios.quickcastscrollcartridge;

import io.redspace.ironsspellbooks.api.magic.SpellSelectionManager;
import io.redspace.ironsspellbooks.api.spells.SpellData;
import io.redspace.ironsspellbooks.network.EquipmentChangedPacket;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
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
            QuickcastCartridgeCasting.validate(player);
            PacketDistributor.sendToPlayer(player, new EquipmentChangedPacket());
        }
    }

    @SubscribeEvent
    public static void onTick(PlayerTickEvent.Pre event) {
        if (event.getEntity() instanceof ServerPlayer player) QuickcastCartridgeCasting.validate(player);
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) QuickcastCartridgeCasting.clear(player);
    }

    @SubscribeEvent
    public static void onDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) QuickcastCartridgeCasting.clear(player);
    }

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) QuickcastCartridgeCasting.clear(player);
    }
}
