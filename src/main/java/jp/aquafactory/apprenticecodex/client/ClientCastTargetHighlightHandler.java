package jp.aquafactory.apprenticecodex.client;

import io.redspace.ironsspellbooks.player.ClientMagicData;
import jp.aquafactory.apprenticecodex.common.registry.SpellsRegistry;
import jp.aquafactory.apprenticecodex.common.utility.RaycastTools;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;
import java.util.Objects;

@Mod.EventBusSubscriber(Dist.CLIENT)
public class ClientCastTargetHighlightHandler {
    private static int highlightEntityId = -1;

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.side.isClient() && event.phase == TickEvent.Phase.END && event.player == Minecraft.getInstance().player) {
            highlightEntityId = -1;

            var level = Minecraft.getInstance().level;
            if (level != null) {
                var spellData = ClientMagicData.getSyncedSpellData(event.player);
                if (spellData.isCasting()) {
                    // todo:インタフェースを参照して数値や色を持ってこれるようにする.
                    if (spellData.getCastingSpellId().equals(SpellsRegistry.ARCANE_BLAST.get().getSpellId())){
                        var result = RaycastTools.raycastFromEye(event.player, 10, 0.1, e -> e.getId() != event.player.getId());
                        if (result.hitEntity() != null){
                            highlightEntityId = result.hitEntity().getId();
                        }
                    }
                }
            }
        }
    }

    public static int getHighlightEntityId() {
        return highlightEntityId;
    }
}
