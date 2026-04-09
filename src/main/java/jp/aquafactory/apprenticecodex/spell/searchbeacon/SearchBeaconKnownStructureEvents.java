package jp.aquafactory.apprenticecodex.spell.searchbeacon;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.capability.Capabilities;
import jp.aquafactory.apprenticecodex.capability.codexspelldata.CodexSpellStateTypeRegister;
import jp.aquafactory.apprenticecodex.capability.codexspelldata.spellstates.SearchBeaconState;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.LinkedHashSet;

@EventBusSubscriber(modid = ApprenticeCodex.MODID)
public final class SearchBeaconKnownStructureEvents {
    private static final int CHECK_INTERVAL_TICKS = 20;

    private SearchBeaconKnownStructureEvents() {
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
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
        var start = structureManager.getStructureWithPieceAt(pos, structure -> true);
        if (start.isValid()) {
            var structureId = structureRegistry.getKey(start.getStructure());
            if (structureId != null) {
                traversed.add(new SearchBeaconState.StructureMarker(
                        player.serverLevel().dimension().location(),
                        structureId,
                        start.getChunkPos().toLong()
                ));
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
