package jp.aquafactory.apprenticecodex.spell.searchbeacon;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.capability.Capabilities;
import jp.aquafactory.apprenticecodex.capability.codexspelldata.CodexSpellStateTypeRegister;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
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
        var traversed = new LinkedHashSet<net.minecraft.resources.ResourceLocation>();

        // 現在地を含む start だけを踏破済み対象にする。
        // SearchBeacon 側も構造物種別単位で判定しているため、ここでも start 個別の履歴は持たない。
        for (var start : structureManager.startsForStructure(new ChunkPos(pos), structure -> true)) {
            if (!structureManager.structureHasPieceAt(pos, start)) {
                continue;
            }

            var structureId = structureRegistry.getKey(start.getStructure());
            if (structureId != null) {
                traversed.add(structureId);
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
