package jp.aquafactory.apprenticecodex.client;

import io.redspace.ironsspellbooks.player.ClientMagicData;
import jp.aquafactory.apprenticecodex.common.registry.SpellsRegistry;
import jp.aquafactory.apprenticecodex.common.spells.ICastHighlightSpell;
import jp.aquafactory.apprenticecodex.common.utility.RaycastTools;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(Dist.CLIENT)
public class ClientCastTargetHighlightHandler {
    private static int highlightColor = 0xFFFFFF;
    private static int highlightEntityId = -1;

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.side.isClient() && event.phase == TickEvent.Phase.END && event.player == Minecraft.getInstance().player) {
            highlightEntityId = -1;
            highlightColor = 0xFFFFFF;

            var level = Minecraft.getInstance().level;
            if (level != null) {
                var spellData = ClientMagicData.getSyncedSpellData(event.player);
                if (spellData.isCasting()) {
                    for(var spell : SpellsRegistry.SPELLS.getEntries()){
                        if (!(spell.get() instanceof ICastHighlightSpell hs)){
                            continue;
                        }

                        var result = hs.getHighlightEntity(event.player, spellData.getCastingSpellLevel());
                        if (result != null){
                            highlightColor = hs.getHighlightColor();
                            highlightEntityId = result.getId();
                        }
                        break;
                    }
                }
            }
        }
    }

    public static int getHighlightColor() {
        return highlightColor;
    }

    public static int getHighlightEntityId() {
        return highlightEntityId;
    }
}
