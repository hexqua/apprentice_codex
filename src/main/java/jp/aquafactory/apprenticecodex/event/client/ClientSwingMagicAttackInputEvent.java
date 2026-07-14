package jp.aquafactory.apprenticecodex.event.client;

import com.mojang.blaze3d.platform.InputConstants;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.compat.bettercombat.BetterCombatClientCompat;
import jp.aquafactory.apprenticecodex.compat.epicfight.EpicFightClientCompat;
import jp.aquafactory.apprenticecodex.item.crystalbladedstaff.CrystalBladedStaff;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.ModList;
import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(modid = ApprenticeCodex.MODID, value = Dist.CLIENT)
public final class ClientSwingMagicAttackInputEvent {
    private static final String BETTER_COMBAT_MOD_ID = "bettercombat";

    private ClientSwingMagicAttackInputEvent() {
    }

    @SubscribeEvent
    public static void onMouseButtonPre(InputEvent.MouseButton.Pre event) {
        if (event.getAction() != GLFW.GLFW_PRESS) {
            return;
        }

        var minecraft = Minecraft.getInstance();
        if (!matchesVanillaAttackInput(minecraft, InputConstants.Type.MOUSE, event.getButton())) {
            return;
        }

        trySendVanillaPreAttack(minecraft);
    }

    @SubscribeEvent
    public static void onKey(InputEvent.Key event) {
        if (event.getAction() != GLFW.GLFW_PRESS) {
            return;
        }

        var minecraft = Minecraft.getInstance();
        if (matchesVanillaAttackInput(minecraft, InputConstants.Type.KEYSYM, event.getKey())
                || matchesVanillaAttackInput(minecraft, InputConstants.Type.SCANCODE, event.getScanCode())) {
            trySendVanillaPreAttack(minecraft);
        }
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

    private static void trySendVanillaPreAttack(Minecraft minecraft) {
        var player = minecraft.player;
        if (minecraft.screen != null
                || player == null
                || player.isSpectator()
                || !CrystalBladedStaff.isCrystalBladedStaff(player.getMainHandItem())) {
            return;
        }

        if (ModList.get().isLoaded(BETTER_COMBAT_MOD_ID)
                && BetterCombatClientCompat.usesBetterCombatAttackTiming(player)) {
            return;
        }

        if (ModList.get().isLoaded(EpicFightClientCompat.MOD_ID)
                && EpicFightClientCompat.isBattleMode()) {
            return;
        }

        ClientSwingMagicAttackTrigger.trySendForVanillaPreAttack(minecraft);
    }

    private static boolean matchesVanillaAttackInput(Minecraft minecraft, InputConstants.Type type, int value) {
        var attackKey = minecraft.options.keyAttack.getKey();
        return attackKey.getType() == type && attackKey.getValue() == value;
    }
}
