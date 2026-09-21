package jp.aquafactory.apprenticecodex.item.curios.manasoultransducer;

import io.redspace.ironsspellbooks.api.magic.MagicData;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.compat.malum.MalumStaffChargeBridge;
import jp.aquafactory.apprenticecodex.config.ApprenticeCodexServerConfig;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import top.theillusivec4.curios.api.CuriosApi;

@EventBusSubscriber(modid = ApprenticeCodex.MODID)
public final class ManaSoulTransducerEvents {
    private ManaSoulTransducerEvents() {}
    public static boolean isEquipped(LivingEntity entity) {
        // 同tick内の解除も反映するため、アイテム指定検索のキャッシュを使わない。
        return CuriosApi.getCuriosInventory(entity)
                .map(inventory -> !inventory.findCurios(stack -> stack.is(ItemRegistry.MANA_SOUL_TRANSDUCER.get())).isEmpty())
                .orElse(false);
    }
    public static boolean tryPay(ServerPlayer player) {
        if (!isEquipped(player)) return false;
        var data = MagicData.getPlayerMagicData(player);
        int cost = ApprenticeCodexServerConfig.manaSoulTransducerManaCost();
        if (!(data.getMana() >= cost)) return false;
        // 解除と発射の支払いを共有しない。
        data.setMana(data.getMana() - cost);
        io.redspace.ironsspellbooks.setup.PacketDistributor.sendToPlayer(player, new io.redspace.ironsspellbooks.network.SyncManaPacket(data));
        return true;
    }
    @SubscribeEvent
    public static void onTick(TickEvent.PlayerTickEvent event) {
        if (event.phase == TickEvent.Phase.END && event.player instanceof ServerPlayer player
                && isEquipped(player)) MalumStaffChargeBridge.clearHeldCooldowns(player);
    }
}
