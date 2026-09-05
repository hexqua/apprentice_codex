package jp.aquafactory.apprenticecodex.item.spellreaperscythe;

import io.redspace.ironsspellbooks.player.ClientMagicData;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.config.ApprenticeCodexServerConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;

@EventBusSubscriber(modid = ApprenticeCodex.MODID, value = Dist.CLIENT)
public final class ScytheThrowClient {
    private static boolean requireRelease;
    private static boolean wasThrown;
    private ScytheThrowClient() {}

    public static InteractionResultHolder<ItemStack> use(Player player, ItemStack stack) {
        if (requireRelease) return InteractionResultHolder.fail(stack);
        if (ScytheThrowManager.isThrown(stack)) {
            requireRelease = true;
            return InteractionResultHolder.consume(stack);
        }
        int cost = ApprenticeCodexServerConfig.spellReaperScytheConfig().throwManaCost();
        if (!player.getAbilities().instabuild && ClientMagicData.getPlayerMana() < cost) {
            ScytheThrowManager.insufficientMana(player);
            return InteractionResultHolder.fail(stack);
        }
        player.startUsingItem(InteractionHand.MAIN_HAND);
        return InteractionResultHolder.consume(stack);
    }

    @SubscribeEvent public static void input(InputEvent.InteractionKeyMappingTriggered event) {
        // use()内の拒否だけではバニラのC2S使用packet送信を止められない。
        if (event.isUseItem() && requireRelease) {
            event.setCanceled(true);
            event.setSwingHand(false);
        }
    }

    @SubscribeEvent public static void tick(ClientTickEvent.Pre event) {
        var mc = Minecraft.getInstance();
        if (mc.player == null) { requireRelease = false; wasThrown = false; return; }
        boolean thrown = ScytheThrowManager.isThrown(mc.player.getMainHandItem());
        if (thrown && !wasThrown && net.neoforged.fml.ModList.get().isLoaded("bettercombat")) {
            jp.aquafactory.apprenticecodex.compat.bettercombat.BetterCombatScytheThrowClientCompat.stopSwing();
        }
        if (wasThrown && !thrown && mc.options.keyUse.isDown()) requireRelease = true;
        if (!mc.options.keyUse.isDown()) requireRelease = false;
        wasThrown = thrown;
    }
}
