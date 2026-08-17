package jp.aquafactory.apprenticecodex.entity.broom.client;

import io.redspace.ironsspellbooks.player.ClientMagicData;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.entity.broom.AbstractBroomEntity;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Objects;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = ApprenticeCodex.MODID, value = Dist.CLIENT)
public final class ClientBroomSpellSelectionEvents {
    private static UUID lastPlayerId;
    private static UUID lastBroomId;

    private ClientBroomSpellSelectionEvents() {
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        var player = event.player;
        if (!event.side.isClient() || event.phase != TickEvent.Phase.END
                || player != Minecraft.getInstance().player) {
            return;
        }

        var playerId = player.getUUID();
        var broomId = player.getVehicle() instanceof AbstractBroomEntity broom ? broom.getUUID() : null;
        if (Objects.equals(lastPlayerId, playerId) && Objects.equals(lastBroomId, broomId)) {
            return;
        }

        lastPlayerId = playerId;
        lastBroomId = broomId;
        ClientMagicData.updateSpellSelectionManager();
    }
}
