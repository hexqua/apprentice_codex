package jp.aquafactory.apprenticecodex.event.client;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.item.curios.manamaneuvergear.ManaManeuverGear;
import jp.aquafactory.apprenticecodex.network.Networks;
import jp.aquafactory.apprenticecodex.network.packet.ClientManaManeuverGearJumpPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.event.TickEvent;
import top.theillusivec4.curios.api.CuriosApi;

@Mod.EventBusSubscriber(modid = ApprenticeCodex.MODID, value = Dist.CLIENT)
public final class ManaManeuverGearClientInputEvent {
    private static boolean previousJumpDown;
    private static boolean blockedUntilJumpRelease;

    private ManaManeuverGearClientInputEvent() {
    }

    @SubscribeEvent
    public static void onClientTickPre(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.START) {
            handleClientTick(Minecraft.getInstance());
        }
    }

    private static void handleClientTick(Minecraft minecraft) {
        var player = minecraft.player;
        if (player == null || minecraft.level == null || minecraft.screen != null || !isEquipped(player)) {
            resetLocalState();
            return;
        }

        var jumpDown = minecraft.options.keyJump.isDown();
        if (!jumpDown) {
            previousJumpDown = false;
            blockedUntilJumpRelease = false;
            return;
        }

        if (player.onGround()) {
            previousJumpDown = true;
            blockedUntilJumpRelease = true;
            return;
        }

        if (!previousJumpDown && !blockedUntilJumpRelease) {
            Networks.sendToServer(new ClientManaManeuverGearJumpPacket());
        }
        previousJumpDown = true;
    }

    private static void resetLocalState() {
        previousJumpDown = false;
        blockedUntilJumpRelease = false;
    }

    private static boolean isEquipped(Player player) {
        return CuriosApi.getCuriosInventory(player)
                .map(inventory -> inventory.isEquipped(stack -> stack.getItem() instanceof ManaManeuverGear))
                .orElse(false);
    }
}
