package jp.aquafactory.apprenticecodex.spell.phalanxcharge;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.mixin.LivingEntityAccessor;
import jp.aquafactory.apprenticecodex.registry.EffectRegistry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Items;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

@EventBusSubscriber(modid = ApprenticeCodex.MODID)
public final class PhalanxStanceGuardEvent {
    private static final String VIRTUAL_SHIELD_TAG = "ApprenticeCodexVirtualPhalanxShield";
    private static final int USING_ITEM_FLAG = 1;
    private static final int OFF_HAND_FLAG = 2;
    private static final int VIRTUAL_SHIELD_USE_TICKS = 72_000;
    private static final int BLOCK_READY_TICKS = 5;

    private PhalanxStanceGuardEvent() {
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Pre event) {
        var player = event.getEntity();
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
                player.hasEffect(BuiltInRegistries.MOB_EFFECT.wrapAsHolder(EffectRegistry.PHALANX_STANCE.get())) &&
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

        var customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData == null) {
            return false;
        }
        return customData.copyTag().getBoolean(VIRTUAL_SHIELD_TAG);
    }

    private static ItemStack createVirtualShield() {
        var stack = new ItemStack(Items.SHIELD);
        net.minecraft.world.item.component.CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
            tag.putBoolean(VIRTUAL_SHIELD_TAG, true);
            tag.putBoolean("Unbreakable", true);
        });
        return stack;
    }
}

