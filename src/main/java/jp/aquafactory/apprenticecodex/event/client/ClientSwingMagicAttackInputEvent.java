package jp.aquafactory.apprenticecodex.event.client;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.compat.bettercombat.BetterCombatClientCompat;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.ModList;

@Mod.EventBusSubscriber(modid = ApprenticeCodex.MODID, value = Dist.CLIENT)
public final class ClientSwingMagicAttackInputEvent {
    private static final String BETTER_COMBAT_MOD_ID = "bettercombat";

    private ClientSwingMagicAttackInputEvent() {
    }

    @SubscribeEvent
    public static void onInteractionKeyMappingTriggered(InputEvent.InteractionKeyMappingTriggered event) {
        if (!event.isAttack()) {
            return;
        }

        var minecraft = Minecraft.getInstance();
        if (ModList.get().isLoaded(BETTER_COMBAT_MOD_ID)
                && BetterCombatClientCompat.usesBetterCombatAttackTiming(minecraft.player)) {
            return;
        }

        ClientSwingMagicAttackTrigger.trySend(minecraft);
    }
}
