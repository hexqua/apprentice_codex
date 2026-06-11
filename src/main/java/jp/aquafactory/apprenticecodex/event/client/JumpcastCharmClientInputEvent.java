package jp.aquafactory.apprenticecodex.event.client;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.item.curios.jumpcastcharm.JumpcastCharm;
import jp.aquafactory.apprenticecodex.item.curios.jumpcastcharm.JumpcastCharmCastManager;
import jp.aquafactory.apprenticecodex.network.Networks;
import jp.aquafactory.apprenticecodex.network.packet.ClientJumpcastCharmCastPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import top.theillusivec4.curios.api.CuriosApi;

@Mod.EventBusSubscriber(modid = ApprenticeCodex.MODID, value = Dist.CLIENT)
public final class JumpcastCharmClientInputEvent {
    private static boolean previousJumpDown;
    private static boolean blockedUntilJumpRelease;

    private JumpcastCharmClientInputEvent() {
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.START) {
            return;
        }

        handleClientTick(Minecraft.getInstance());
    }

    private static void handleClientTick(Minecraft minecraft) {
        var player = minecraft.player;
        if (player == null || minecraft.level == null || minecraft.screen != null || !isJumpcastCharmEquipped(player)) {
            resetLocalState();
            return;
        }

        var jumpDown = minecraft.options.keyJump.isDown();
        if (!jumpDown) {
            previousJumpDown = false;
            blockedUntilJumpRelease = false;
            return;
        }

        if (JumpcastCharmCastManager.isMovementContextBlocked(player)) {
            previousJumpDown = true;
            blockedUntilJumpRelease = true;
            return;
        }

        if (!previousJumpDown && !blockedUntilJumpRelease) {
            Networks.sendToServer(new ClientJumpcastCharmCastPacket());
        }
        previousJumpDown = true;
    }

    private static void resetLocalState() {
        previousJumpDown = false;
        blockedUntilJumpRelease = false;
    }

    private static boolean isJumpcastCharmEquipped(Player player) {
        return CuriosApi.getCuriosInventory(player)
                .map(inventory -> inventory.isEquipped(stack -> stack.getItem() instanceof JumpcastCharm))
                .orElse(false);
    }
}
