package jp.aquafactory.apprenticecodex.item.revolvercaststaff;

import io.redspace.ironsspellbooks.player.ClientMagicData;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.event.TickEvent;

@Mod.EventBusSubscriber(modid = ApprenticeCodex.MODID, value = Dist.CLIENT)
public final class RevolvercastStaffClientEvents {
    private static ItemStack main = ItemStack.EMPTY;
    private static ItemStack off = ItemStack.EMPTY;

    private RevolvercastStaffClientEvents() {}

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        var player = Minecraft.getInstance().player;
        if (player == null) {
            main = off = ItemStack.EMPTY;
            return;
        }
        var newMain = snapshot(player.getMainHandItem());
        var newOff = snapshot(player.getOffhandItem());
        // 装備変更通知がインベントリ同期より先に到着しても、新しい保存データで再構築する。
        if (!ItemStack.isSameItemSameTags(main, newMain) || !ItemStack.isSameItemSameTags(off, newOff)) {
            ClientMagicData.updateSpellSelectionManager();
        }
        main = newMain;
        off = newOff;
    }

    private static ItemStack snapshot(ItemStack stack) {
        return stack.getItem() instanceof RevolvercastStaff ? stack.copy() : ItemStack.EMPTY;
    }

}
