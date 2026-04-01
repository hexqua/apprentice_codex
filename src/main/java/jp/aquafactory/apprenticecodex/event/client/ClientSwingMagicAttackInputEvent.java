package jp.aquafactory.apprenticecodex.event.client;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.compat.bettercombat.BetterCombatClientCompat;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;

@EventBusSubscriber(modid = ApprenticeCodex.MODID, value = Dist.CLIENT)
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
        if (minecraft.player == null) {
            return;
        }

        if (ModList.get().isLoaded(BETTER_COMBAT_MOD_ID)
                && BetterCombatClientCompat.usesBetterCombatAttackTiming(minecraft.player)) {
            return;
        }

        ClientSwingMagicAttackTrigger.trySend(minecraft);
    }
}
