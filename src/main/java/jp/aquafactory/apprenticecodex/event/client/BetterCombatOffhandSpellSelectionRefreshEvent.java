package jp.aquafactory.apprenticecodex.event.client;

import io.redspace.ironsspellbooks.player.ClientMagicData;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.compat.bettercombat.BetterCombatClientCompat;
import jp.aquafactory.apprenticecodex.compat.bettercombat.BetterCombatOffhandAttributeRescueCompat;
import jp.aquafactory.apprenticecodex.compat.bettercombat.BetterCombatScrollcasterGauntletCompat;
import net.minecraft.client.Minecraft;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

@EventBusSubscriber(modid = ApprenticeCodex.MODID, value = Dist.CLIENT)
public final class BetterCombatOffhandSpellSelectionRefreshEvent {
    @Nullable
    private static OffhandSnapshot lastOffhandSnapshot;
    private static boolean wasRescueActive;

    private BetterCombatOffhandSpellSelectionRefreshEvent() {
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
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

        var rescueActive = BetterCombatOffhandAttributeRescueCompat.isRescueActive(player)
                || BetterCombatScrollcasterGauntletCompat.isRescueActive(player);
        if (!rescueActive && !wasRescueActive) {
            lastOffhandSnapshot = null;
            return;
        }

        // Better Combat 1.20.1 は両手武器中の getOffhandItem() を空へ差し替えるため、
        // spell wheel の再構築判定だけは物理 offhand スロットを直接監視する。
        var currentOffhandSnapshot = rescueActive
                ? OffhandSnapshot.capture(BetterCombatScrollcasterGauntletCompat.getPhysicalOffhandStack(player))
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
            var customData = stack.get(DataComponents.CUSTOM_DATA);
            return new OffhandSnapshot(
                    stack.getItem(),
                    stack.getCount(),
                    customData == null ? null : customData.copyTag()
            );
        }
    }
}
