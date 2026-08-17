package jp.aquafactory.apprenticecodex.entity.broom.client;

import io.redspace.ironsspellbooks.player.ClientMagicData;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.entity.broom.AbstractBroomEntity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.Objects;
import java.util.UUID;

@EventBusSubscriber(modid = ApprenticeCodex.MODID, value = Dist.CLIENT)
public final class ClientBroomSpellSelectionEvents {
    private static UUID lastPlayerId;
    private static UUID lastBroomId;

    private ClientBroomSpellSelectionEvents() {
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        var player = event.getEntity();
        if (!player.isLocalPlayer()) {
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
