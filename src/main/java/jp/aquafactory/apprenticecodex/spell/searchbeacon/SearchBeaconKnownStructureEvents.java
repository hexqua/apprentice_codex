package jp.aquafactory.apprenticecodex.spell.searchbeacon;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.capability.Capabilities;
import jp.aquafactory.apprenticecodex.capability.codexspelldata.CodexSpellStateTypeRegister;
import jp.aquafactory.apprenticecodex.capability.codexspelldata.spellstates.SearchBeaconState;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.LinkedHashSet;

@Mod.EventBusSubscriber(modid = ApprenticeCodex.MODID)
public final class SearchBeaconKnownStructureEvents {
    private static final int CHECK_INTERVAL_TICKS = 20;

    private SearchBeaconKnownStructureEvents() {
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player)) {
            return;
        }
        if (player.tickCount % CHECK_INTERVAL_TICKS != 0) {
            return;
        }

        var structureManager = player.serverLevel().structureManager();
        var pos = player.blockPosition();
        var structureRegistry = player.serverLevel().registryAccess().registryOrThrow(Registries.STRUCTURE);
        var traversed = new LinkedHashSet<SearchBeaconState.StructureMarker>();

        // 村のような広い structure は現在 chunk の参照だけだと取りこぼすことがあるため、
        // 「現在地を含む piece があるか」を直接見る API で判定する。
        for (var entry : structureManager.getAllStructuresAt(pos).entrySet()) {
            var structureId = structureRegistry.getKey(entry.getKey());
            if (structureId != null) {
                for (var startChunkPos : entry.getValue()) {
                    traversed.add(new SearchBeaconState.StructureMarker(
                            player.serverLevel().dimension().location(),
                            structureId,
                            startChunkPos.longValue()
                    ));
                }
            }
        }

        if (traversed.isEmpty()) {
            return;
        }

        Capabilities.withSpellData(player, data -> {
            var state = data.get(CodexSpellStateTypeRegister.SEARCH_BEACON_STATE);
            if (state.markTraversed(traversed)) {
                data.markDirty(CodexSpellStateTypeRegister.SEARCH_BEACON_STATE.id());
            }
        });
    }
}
