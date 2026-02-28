package jp.aquafactory.apprenticecodex.event;

import io.redspace.ironsspellbooks.player.ClientMagicData;
import jp.aquafactory.apprenticecodex.registry.SpellRegistry;
import jp.aquafactory.apprenticecodex.spell.ICastHighlightSpell;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Objects;

// クライアントイベントだがゲームロジックに関わるためclient配下ではない.
@Mod.EventBusSubscriber(Dist.CLIENT)
public final class ClientCastTargetHighlightEvent {
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
                    for(var spellEntry : SpellRegistry.SPELLS.getEntries()){
                        var spell = spellEntry.get();
                        if (!Objects.equals(spellData.getCastingSpellId(), spell.getSpellId())){
                            continue;
                        }
                        if (!(spell instanceof ICastHighlightSpell hs)){
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
