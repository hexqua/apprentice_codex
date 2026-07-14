package jp.aquafactory.apprenticecodex.item.smashcastscepter;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jetbrains.annotations.Nullable;

@Mod.EventBusSubscriber(modid = ApprenticeCodex.MODID, value = Dist.CLIENT)
public final class SmashcastScepterClientRenderState {
    private static boolean syncedReadyState;

    private SmashcastScepterClientRenderState() {
    }

    public static void setSyncedReadyState(boolean ready) {
        syncedReadyState = ready;
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        var minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null) {
            syncedReadyState = false;
        }
    }

    public static boolean shouldPlayReadyAnimation(@Nullable ItemStack renderingStack,
                                                   @Nullable ItemDisplayContext perspective) {
        if (renderingStack == null || renderingStack.isEmpty()
                || !(renderingStack.getItem() instanceof SmashcastScepter)) {
            return false;
        }

        var player = Minecraft.getInstance().player;
        return player != null
                && syncedReadyState
                && isRenderedMainHandStack(player, renderingStack, perspective)
                && matchesLocalReadyCondition(player);
    }

    private static boolean matchesLocalReadyCondition(Player player) {
        return !player.onGround()
                && !player.isFallFlying()
                && !player.isInWater()
                && !player.hasEffect(net.minecraft.world.effect.MobEffects.SLOW_FALLING);
    }

    private static boolean isRenderedMainHandStack(Player player, ItemStack renderingStack,
                                                   @Nullable ItemDisplayContext perspective) {
        var renderedHand = resolveRenderedHand(player, perspective);
        return renderedHand == InteractionHand.MAIN_HAND
                && ItemStack.isSameItemSameTags(player.getMainHandItem(), renderingStack);
    }

    @Nullable
    private static InteractionHand resolveRenderedHand(Player player, @Nullable ItemDisplayContext perspective) {
        if (perspective == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND
                || perspective == ItemDisplayContext.THIRD_PERSON_RIGHT_HAND) {
            return resolveHandByArm(player, HumanoidArm.RIGHT);
        }
        if (perspective == ItemDisplayContext.FIRST_PERSON_LEFT_HAND
                || perspective == ItemDisplayContext.THIRD_PERSON_LEFT_HAND) {
            return resolveHandByArm(player, HumanoidArm.LEFT);
        }
        return null;
    }

    private static InteractionHand resolveHandByArm(Player player, HumanoidArm arm) {
        return player.getMainArm() == arm ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
    }
}
