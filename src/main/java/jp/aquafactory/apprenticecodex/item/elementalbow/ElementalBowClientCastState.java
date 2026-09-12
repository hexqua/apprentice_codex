package jp.aquafactory.apprenticecodex.item.elementalbow;

import io.redspace.ironsspellbooks.api.spells.CastType;
import io.redspace.ironsspellbooks.player.ClientMagicData;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.network.Networks;
import jp.aquafactory.apprenticecodex.network.packet.ClientElementalBowCancelPacket;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@EventBusSubscriber(modid = ApprenticeCodex.MODID, value = Dist.CLIENT)
public final class ElementalBowClientCastState {
    private static final Map<UUID, String> ACTIVE = new HashMap<>();

    private ElementalBowClientCastState() {}

    public static void sync(UUID playerId, String spellId, boolean active) {
        if (active) ACTIVE.put(playerId, spellId);
        else {
            ACTIVE.remove(playerId, spellId);
            var player = Minecraft.getInstance().player;
            if (player != null && player.getUUID().equals(playerId)
                    && spellId.equals(ClientMagicData.getCastingSpellId())) {
                ClientMagicData.resetClientCastState(playerId);
            }
            if (player != null && player.getUUID().equals(playerId)
                    && spellId.equals(ClientMagicData.getTargetingData().spellId)) ClientMagicData.resetTargetingData();
        }
    }

    public static boolean matches(UUID playerId, String spellId) {
        return spellId.equals(ACTIVE.get(playerId));
    }

    public static boolean isLocalActive() {
        var player = Minecraft.getInstance().player;
        return player != null && matches(player.getUUID(), ClientMagicData.getCastingSpellId());
    }

    @SubscribeEvent(priority = net.neoforged.bus.api.EventPriority.HIGHEST)
    public static void openScreen(ScreenEvent.Opening event) {
        var player = Minecraft.getInstance().player;
        if (player == null || event.getNewScreen() == null) return;
        var profile = player.getUseItem().getItem() instanceof ElementalBow
                ? ElementalBow.getDisplayedSpellProfile(player.getUseItem()) : null;
        // 開始応答がまだ届かない場合も、同じ接続上で use より後・release より先に中断要求を送る。
        if (!ACTIVE.containsKey(player.getUUID()) && !(player.isUsingItem()
                && player.getUseItem().getItem() instanceof ElementalBow
                && profile != null && profile.spell().getCastType() == CastType.LONG)) return;
        Networks.sendToServer(new ClientElementalBowCancelPacket());
        player.stopUsingItem();
        ClientMagicData.resetClientCastState(player.getUUID());
    }

    @SubscribeEvent
    public static void logout(ClientPlayerNetworkEvent.LoggingOut event) { ACTIVE.clear(); }
}
