package jp.aquafactory.apprenticecodex.event.client;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.item.AbstractRightClickMagicWeaponItem;
import jp.aquafactory.apprenticecodex.item.AbstractSwingMagicItem;
import jp.aquafactory.apprenticecodex.network.Networks;
import jp.aquafactory.apprenticecodex.network.packet.ClientSwingMagicAttackPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = ApprenticeCodex.MODID, value = Dist.CLIENT)
public final class ClientSwingMagicAttackInputEvent {
    private static long lastSentTick = Long.MIN_VALUE;

    private ClientSwingMagicAttackInputEvent() {
    }

    @SubscribeEvent
    public static void onInteractionKeyMappingTriggered(InputEvent.InteractionKeyMappingTriggered event) {
        if (!event.isAttack()) {
            return;
        }

        var minecraft = Minecraft.getInstance();
        if (minecraft.screen != null) {
            return;
        }

        var player = minecraft.player;
        if (player == null || player.isSpectator() || !(player.getMainHandItem().getItem() instanceof AbstractSwingMagicItem)) {
            return;
        }

        if (minecraft.hitResult != null && minecraft.hitResult.getType() == HitResult.Type.BLOCK) {
            return;
        }

        if (!AbstractRightClickMagicWeaponItem.isFullyChargedAttack(player)) {
            return;
        }

        var gameTime = player.level().getGameTime();
        if (gameTime == lastSentTick) {
            return;
        }

        lastSentTick = gameTime;
        Networks.sendToServer(new ClientSwingMagicAttackPacket());
    }
}
