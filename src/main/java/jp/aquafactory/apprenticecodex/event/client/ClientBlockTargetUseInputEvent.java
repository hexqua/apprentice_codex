package jp.aquafactory.apprenticecodex.event.client;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.network.Networks;
import jp.aquafactory.apprenticecodex.network.packet.ClientBlockTargetCastPacket;
import jp.aquafactory.apprenticecodex.spell.IClientBlockTargetingSpell;
import jp.aquafactory.apprenticecodex.utility.BlockTargetData;
import jp.aquafactory.apprenticecodex.utility.ClientBlockTargetingHelper;
import jp.aquafactory.apprenticecodex.utility.RightClickSpellResolver;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;

@Mod.EventBusSubscriber(modid = ApprenticeCodex.MODID, value = Dist.CLIENT)
public final class ClientBlockTargetUseInputEvent {
    @Nullable
    private static TargetSyncSignature lastSentTargetSync;
    private static long lastSentTargetTick = Long.MIN_VALUE;

    private ClientBlockTargetUseInputEvent() {
    }

    @SubscribeEvent
    public static void onInteractionKeyMappingTriggered(InputEvent.InteractionKeyMappingTriggered event) {
        if (!event.isUseItem()) {
            return;
        }

        var minecraft = Minecraft.getInstance();
        if (minecraft.screen != null) {
            return;
        }

        var player = minecraft.player;
        if (player == null) {
            return;
        }

        var resolvedSpell = RightClickSpellResolver.resolve(player);
        if (resolvedSpell.isEmpty()) {
            return;
        }

        var spell = resolvedSpell.get().spellData().getSpell();
        if (!(spell instanceof IClientBlockTargetingSpell targetingSpell)) {
            return;
        }

        var targetData = ClientBlockTargetingHelper.captureOutlinedTarget(
                player,
                targetingSpell.getClientBlockTargetingRange(resolvedSpell.get().spellLevel(), player)
        );
        if (shouldSuppressDuplicate(player.level().getGameTime(), resolvedSpell.get().spellResource(), player.getMainHandItem(), player.getOffhandItem(), targetData)) {
            return;
        }

        Networks.sendToServer(new ClientBlockTargetCastPacket(-1, resolvedSpell.get().spellResource(), targetData, false));
    }

    private static boolean shouldSuppressDuplicate(long gameTime, ResourceLocation spellResource, ItemStack mainHand, ItemStack offHand, BlockTargetData targetData) {
        // 同一クリックで event が複数回来る環境があるため、同 tick 同内容の sync は 1 回に潰す。
        var signature = new TargetSyncSignature(
                spellResource,
                getItemId(mainHand),
                getItemId(offHand),
                targetData.hasTarget(),
                targetData.getHitBlockPos(),
                targetData.getHitFace(),
                targetData.getPlacePos(),
                targetData.getPlaceFacing()
        );

        if (gameTime == lastSentTargetTick && signature.equals(lastSentTargetSync)) {
            return true;
        }

        lastSentTargetTick = gameTime;
        lastSentTargetSync = signature;
        return false;
    }

    @Nullable
    private static ResourceLocation getItemId(ItemStack stack) {
        return stack.isEmpty() ? null : ForgeRegistries.ITEMS.getKey(stack.getItem());
    }

    private record TargetSyncSignature(
            ResourceLocation spellResource,
            @Nullable ResourceLocation mainHandItemId,
            @Nullable ResourceLocation offHandItemId,
            boolean hasTarget,
            @Nullable BlockPos hitBlockPos,
            @Nullable Direction hitFace,
            @Nullable BlockPos placePos,
            @Nullable Direction placeFacing
    ) {
    }
}
