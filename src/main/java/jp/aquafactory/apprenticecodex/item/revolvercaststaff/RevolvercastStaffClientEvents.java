package jp.aquafactory.apprenticecodex.item.revolvercaststaff;

import io.redspace.ironsspellbooks.player.ClientMagicData;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

@EventBusSubscriber(modid = ApprenticeCodex.MODID, value = Dist.CLIENT)
public final class RevolvercastStaffClientEvents {
    private static ItemStack main = ItemStack.EMPTY;
    private static ItemStack off = ItemStack.EMPTY;

    private RevolvercastStaffClientEvents() {}

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        var player = Minecraft.getInstance().player;
        if (player == null) {
            main = off = ItemStack.EMPTY;
            return;
        }
        var newMain = snapshot(player.getMainHandItem());
        var newOff = snapshot(player.getOffhandItem());
        // 装備変更通知がインベントリ同期より先に到着しても、新しい保存データで再構築する。
        if (!ItemStack.isSameItemSameComponents(main, newMain) || !ItemStack.isSameItemSameComponents(off, newOff)) {
            ClientMagicData.updateSpellSelectionManager();
        }
        main = newMain;
        off = newOff;
    }

    private static ItemStack snapshot(ItemStack stack) {
        return stack.getItem() instanceof RevolvercastStaff ? stack.copy() : ItemStack.EMPTY;
    }

}
