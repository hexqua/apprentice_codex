package jp.aquafactory.apprenticecodex.event.client;

import io.redspace.ironsspellbooks.api.magic.MagicData;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.item.curios.manathruster.ManaThruster;
import jp.aquafactory.apprenticecodex.item.curios.manathruster.ManaThrusterConfigState;
import jp.aquafactory.apprenticecodex.item.curios.manathruster.ManaThrusterContext;
import jp.aquafactory.apprenticecodex.item.curios.manathruster.ManaThrusterMovement;
import jp.aquafactory.apprenticecodex.network.Networks;
import jp.aquafactory.apprenticecodex.network.packet.ClientManaThrusterInputPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import top.theillusivec4.curios.api.CuriosApi;

@EventBusSubscriber(modid = ApprenticeCodex.MODID, value = Dist.CLIENT)
public final class ManaThrusterClientInputEvent {
    private static boolean previousJumpDown;
    private static boolean activeSent;
    private static boolean pendingAirbornePress;
    private static boolean blockedUntilJumpRelease;
    private static float lastSentStrafeInput;
    private static float lastSentForwardInput;

    private ManaThrusterClientInputEvent() {
    }

    @SubscribeEvent
    public static void onClientTickPre(ClientTickEvent.Pre event) {
        captureJumpPressStart(Minecraft.getInstance());
    }

    @SubscribeEvent
    public static void onClientTickPost(ClientTickEvent.Post event) {
        handleClientTickEnd(Minecraft.getInstance());
    }

    private static void captureJumpPressStart(Minecraft minecraft) {
        var player = minecraft.player;
        if (player == null || minecraft.level == null) {
            resetLocalStateWithoutPacket();
            return;
        }
        if (minecraft.screen != null || !isManaThrusterEquipped(player)) {
            resetLocalStateWithPacket();
            return;
        }
        if (ManaThrusterContext.isDisabled(player)) {
            resetBlockedContext(minecraft.options.keyJump.isDown());
            return;
        }

        var jumpDown = minecraft.options.keyJump.isDown();
        if (!jumpDown) {
            sendInactiveIfNeeded();
            previousJumpDown = false;
            pendingAirbornePress = false;
            blockedUntilJumpRelease = false;
            return;
        }

        if (player.onGround()) {
            sendInactiveIfNeeded();
            previousJumpDown = true;
            pendingAirbornePress = false;
            blockedUntilJumpRelease = true;
            return;
        }

        if (!previousJumpDown && !blockedUntilJumpRelease) {
            pendingAirbornePress = true;
        }
        previousJumpDown = true;
    }

    private static void handleClientTickEnd(Minecraft minecraft) {
        var player = minecraft.player;
        if (player == null || minecraft.level == null) {
            resetLocalStateWithoutPacket();
            return;
        }
        if (minecraft.screen != null || !isManaThrusterEquipped(player)) {
            resetLocalStateWithPacket();
            return;
        }
        if (ManaThrusterContext.isDisabled(player)) {
            resetBlockedContext(minecraft.options.keyJump.isDown());
            return;
        }

        var jumpDown = minecraft.options.keyJump.isDown();
        if (!jumpDown) {
            sendInactiveIfNeeded();
            previousJumpDown = false;
            pendingAirbornePress = false;
            blockedUntilJumpRelease = false;
            return;
        }

        if (player.onGround()) {
            sendInactiveIfNeeded();
            pendingAirbornePress = false;
            blockedUntilJumpRelease = true;
            return;
        }

        if (!previousJumpDown && !pendingAirbornePress) {
            previousJumpDown = true;
            blockedUntilJumpRelease = true;
            return;
        }

        if (pendingAirbornePress) {
            sendActiveInput(player);
            activeSent = true;
            pendingAirbornePress = false;
        } else if (activeSent) {
            syncActiveInputIfChanged(player);
        }

        if (activeSent && canPredictSuccessfulThrust(player)) {
            ManaThrusterMovement.applyThrust(player);
            player.fallDistance = 0.0F;
        }
    }

    private static boolean isManaThrusterEquipped(Player player) {
        return CuriosApi.getCuriosInventory(player)
                .map(inventory -> inventory.isEquipped(stack -> stack.getItem() instanceof ManaThruster))
                .orElse(false);
    }

    private static void resetBlockedContext(boolean jumpDown) {
        sendInactiveIfNeeded();
        previousJumpDown = jumpDown;
        pendingAirbornePress = false;
        blockedUntilJumpRelease = jumpDown;
    }

    private static void resetLocalStateWithPacket() {
        if (activeSent) {
            Networks.sendToServer(ClientManaThrusterInputPacket.inactive());
            lastSentStrafeInput = 0.0F;
            lastSentForwardInput = 0.0F;
        }
        resetLocalStateWithoutPacket();
    }

    private static void resetLocalStateWithoutPacket() {
        previousJumpDown = false;
        activeSent = false;
        pendingAirbornePress = false;
        blockedUntilJumpRelease = false;
        lastSentStrafeInput = 0.0F;
        lastSentForwardInput = 0.0F;
    }

    private static void sendInactiveIfNeeded() {
        if (activeSent) {
            Networks.sendToServer(ClientManaThrusterInputPacket.inactive());
            activeSent = false;
            lastSentStrafeInput = 0.0F;
            lastSentForwardInput = 0.0F;
        }
    }

    public static void deactivateFromServer() {
        activeSent = false;
        pendingAirbornePress = false;
        blockedUntilJumpRelease = true;
        lastSentStrafeInput = 0.0F;
        lastSentForwardInput = 0.0F;
    }

    private static void syncActiveInputIfChanged(Player player) {
        if (Math.abs(player.xxa - lastSentStrafeInput) > 1.0e-4F
                || Math.abs(player.zza - lastSentForwardInput) > 1.0e-4F) {
            sendActiveInput(player);
        }
    }

    private static void sendActiveInput(Player player) {
        lastSentStrafeInput = player.xxa;
        lastSentForwardInput = player.zza;
        Networks.sendToServer(new ClientManaThrusterInputPacket(true, lastSentStrafeInput, lastSentForwardInput));
    }

    private static boolean canPredictSuccessfulThrust(Player player) {
        var manaCost = Math.max(0.0F, ManaThrusterConfigState.manaCostPerTick());
        if (manaCost <= 0.0F) {
            return true;
        }

        var magicData = MagicData.getPlayerMagicData(player);
        return magicData == null || magicData.getMana() + 1.0e-4F >= manaCost;
    }
}
