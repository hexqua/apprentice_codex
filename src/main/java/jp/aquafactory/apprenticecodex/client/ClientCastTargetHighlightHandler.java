package jp.aquafactory.apprenticecodex.client;

import io.redspace.ironsspellbooks.player.ClientMagicData;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.common.registry.SpellsRegistry;
import jp.aquafactory.apprenticecodex.common.spells.ICastHighlightSpell;
import jp.aquafactory.apprenticecodex.common.utility.RaycastTools;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.Objects;

@EventBusSubscriber(modid = ApprenticeCodex.MODID, value = Dist.CLIENT)
public class ClientCastTargetHighlightHandler {
    private static int highlightColor = 0xFFFFFF;
    private static int highlightEntityId = -1;

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        var player = event.getEntity();
        if (player.level().isClientSide && player == Minecraft.getInstance().player) {
            highlightEntityId = -1;
            highlightColor = 0xFFFFFF;

            var level = Minecraft.getInstance().level;
            if (level != null) {
                var spellData = ClientMagicData.getSyncedSpellData(player);
                if (spellData.isCasting()) {
                    for(var spellEntry : SpellsRegistry.SPELLS.getEntries()){
                        var spell = spellEntry.get();
                        if (!Objects.equals(spellData.getCastingSpellId(), spell.getSpellId())){
                            continue;
                        }
                        if (!(spell instanceof ICastHighlightSpell hs)){
                            continue;
                        }

                        var result = hs.getHighlightEntity(player, spellData.getCastingSpellLevel());
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
