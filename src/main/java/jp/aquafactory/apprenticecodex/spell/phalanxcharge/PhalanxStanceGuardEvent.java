package jp.aquafactory.apprenticecodex.spell.phalanxcharge;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.mixin.LivingEntityAccessor;
import jp.aquafactory.apprenticecodex.registry.EffectRegistry;
import net.minecraft.world.item.Items;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = ApprenticeCodex.MODID)
public final class PhalanxStanceGuardEvent {
    private static final String VIRTUAL_SHIELD_TAG = "ApprenticeCodexVirtualPhalanxShield";
    private static final int USING_ITEM_FLAG = 1;
    private static final int OFF_HAND_FLAG = 2;
    private static final int VIRTUAL_SHIELD_USE_TICKS = 72_000;
    private static final int BLOCK_READY_TICKS = 5;

    private PhalanxStanceGuardEvent() {
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.START) {
            return;
        }

        var player = event.player;
        if (player.level().isClientSide) {
            return;
        }

        if (!isGuardActive(player)) {
            clearVirtualShield(player);
            return;
        }

        if (player.getCooldowns().isOnCooldown(Items.SHIELD)) {
            clearVirtualShield(player);
            return;
        }

        if (player.isUsingItem() && !isVirtualShield(player.getUseItem())) {
            return;
        }

        applyVirtualShield(player);
    }

    private static boolean isGuardActive(Player player) {
        return player.isAlive() &&
                player.hasEffect(EffectRegistry.PHALANX_STANCE.get()) &&
                !player.isPassenger();
    }

    private static void applyVirtualShield(Player player) {
        var accessor = (LivingEntityAccessor) player;
        if (!isVirtualShield(player.getUseItem())) {
            accessor.apprenticecodex$setUseItem(createVirtualShield());
            accessor.apprenticecodex$setUseItemRemaining(VIRTUAL_SHIELD_USE_TICKS - BLOCK_READY_TICKS);
        } else if (player.getUseItemRemainingTicks() <= 0) {
            accessor.apprenticecodex$setUseItemRemaining(VIRTUAL_SHIELD_USE_TICKS - BLOCK_READY_TICKS);
        }

        accessor.apprenticecodex$setLivingEntityFlag(OFF_HAND_FLAG, true);
        accessor.apprenticecodex$setLivingEntityFlag(USING_ITEM_FLAG, true);
    }

    private static void clearVirtualShield(Player player) {
        if (!isVirtualShield(player.getUseItem())) {
            return;
        }

        player.stopUsingItem();
    }

    private static boolean isVirtualShield(ItemStack stack) {
        if (stack.isEmpty() || stack.getItem() != Items.SHIELD) {
            return false;
        }

        var tag = stack.getTag();
        return tag != null && tag.getBoolean(VIRTUAL_SHIELD_TAG);
    }

    private static ItemStack createVirtualShield() {
        var stack = new ItemStack(Items.SHIELD);
        var tag = stack.getOrCreateTag();
        tag.putBoolean(VIRTUAL_SHIELD_TAG, true);
        tag.putBoolean("Unbreakable", true);
        return stack;
    }
}
