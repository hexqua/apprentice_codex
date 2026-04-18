package jp.aquafactory.apprenticecodex.event.client;

import io.redspace.ironsspellbooks.player.ClientMagicData;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.compat.bettercombat.BetterCombatClientCompat;
import jp.aquafactory.apprenticecodex.compat.bettercombat.BetterCombatOffhandAttributeRescueCompat;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

@Mod.EventBusSubscriber(modid = ApprenticeCodex.MODID, value = Dist.CLIENT)
public final class BetterCombatOffhandSpellSelectionRefreshEvent {
    @Nullable
    private static OffhandSnapshot lastOffhandSnapshot;
    private static boolean wasRescueActive;

    private BetterCombatOffhandSpellSelectionRefreshEvent() {
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        if (!ModList.get().isLoaded(BetterCombatClientCompat.MOD_ID)) {
            clearState();
            return;
        }

        var minecraft = Minecraft.getInstance();
        var player = minecraft.player;
        var level = minecraft.level;
        if (player == null || level == null || !player.isAlive() || player.isSpectator()) {
            clearState();
            return;
        }

        var rescueActive = BetterCombatOffhandAttributeRescueCompat.isRescueActive(player);
        if (!rescueActive && !wasRescueActive) {
            lastOffhandSnapshot = null;
            return;
        }

        // Better Combat 1.20.1 は両手武器中の getOffhandItem() を空へ差し替えるため、
        // spell wheel の再構築判定だけは物理 offhand スロットを直接監視する。
        var currentOffhandSnapshot = rescueActive
                ? OffhandSnapshot.capture(BetterCombatOffhandAttributeRescueCompat.getPhysicalOffhandStack(player))
                : null;
        if (rescueActive != wasRescueActive || !Objects.equals(lastOffhandSnapshot, currentOffhandSnapshot)) {
            ClientMagicData.updateSpellSelectionManager();
        }

        wasRescueActive = rescueActive;
        lastOffhandSnapshot = currentOffhandSnapshot;
    }

    private static void clearState() {
        wasRescueActive = false;
        lastOffhandSnapshot = null;
    }

    private record OffhandSnapshot(
            Item item,
            int count,
            @Nullable CompoundTag tag
    ) {
        private static OffhandSnapshot capture(ItemStack stack) {
            return new OffhandSnapshot(
                    stack.getItem(),
                    stack.getCount(),
                    stack.hasTag() ? stack.getTag() != null ? stack.getTag().copy() : null : null
            );
        }
    }
}
