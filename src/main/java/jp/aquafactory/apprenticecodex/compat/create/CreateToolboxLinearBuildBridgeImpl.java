package jp.aquafactory.apprenticecodex.compat.create;

import com.simibubi.create.content.equipment.toolbox.ToolboxBlockEntity;
import com.simibubi.create.content.equipment.toolbox.ToolboxHandler;
import jp.aquafactory.apprenticecodex.spell.linearbuild.LinearBuildItemSource;
import jp.aquafactory.apprenticecodex.spell.linearbuild.LinearBuildItemSources;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.items.IItemHandler;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;

final class CreateToolboxLinearBuildBridgeImpl {
    private static final int FALLBACK_SCAN_RANGE = 4;

    private CreateToolboxLinearBuildBridgeImpl() {
    }

    static List<LinearBuildItemSource> collectSources(ServerPlayer player) {
        var sources = new ArrayList<LinearBuildItemSource>();
        addPlacedToolboxSources(player, sources);
        return sources;
    }

    private static void addPlacedToolboxSources(ServerPlayer player, List<LinearBuildItemSource> sources) {
        var seenPositions = new HashSet<BlockPos>();
        // Create の範囲設定と追跡済み Toolbox 一覧は ToolboxHandler が持つため、1.20.1 固有接着コードとして隔離する。
        for (var toolbox : ToolboxHandler.getNearest(player.level(), player, Integer.MAX_VALUE)) {
            var toolboxPos = resolveToolboxPos(toolbox).orElse(null);
            if (toolbox == null || toolboxPos == null || seenPositions.contains(toolboxPos)) {
                continue;
            }
            if (addPlacedToolboxSource(toolbox, sources)) {
                seenPositions.add(toolboxPos);
            }
        }
        addPlacedToolboxSourcesByBlockEntityScan(player, sources, seenPositions);
    }

    private static void addPlacedToolboxSourcesByBlockEntityScan(
            ServerPlayer player,
            List<LinearBuildItemSource> sources,
            HashSet<BlockPos> seenPositions
    ) {
        var level = player.level();
        var origin = player.blockPosition();
        var from = origin.offset(-FALLBACK_SCAN_RANGE, -FALLBACK_SCAN_RANGE, -FALLBACK_SCAN_RANGE);
        var to = origin.offset(FALLBACK_SCAN_RANGE, FALLBACK_SCAN_RANGE, FALLBACK_SCAN_RANGE);
        for (var pos : BlockPos.betweenClosed(from, to)) {
            var immutablePos = pos.immutable();
            if (seenPositions.contains(immutablePos)) {
                continue;
            }
            if (level.getBlockEntity(pos) instanceof ToolboxBlockEntity toolbox
                    && addPlacedToolboxSource(toolbox, sources)) {
                seenPositions.add(immutablePos);
            }
        }
    }

    private static boolean addPlacedToolboxSource(ToolboxBlockEntity toolbox, List<LinearBuildItemSource> sources) {
        var handler = resolveToolboxItemHandler(toolbox);
        if (handler != null) {
            sources.add(LinearBuildItemSources.itemHandler(handler, resolveToolboxDisplayName(toolbox), true));
            return true;
        }
        return false;
    }

    private static Component resolveToolboxDisplayName(ToolboxBlockEntity toolbox) {
        try {
            var result = ((Object) toolbox).getClass().getMethod("getDisplayName").invoke(toolbox);
            if (result instanceof Component component) {
                return component;
            }
        } catch (ReflectiveOperationException ignored) {
            // 表示名だけ取れない場合でも取り寄せ自体は続ける。
        }
        return Component.translatable("block.create.toolbox");
    }

    private static Optional<BlockPos> resolveToolboxPos(ToolboxBlockEntity toolbox) {
        if (toolbox == null) {
            return Optional.empty();
        }

        try {
            var result = ((Object) toolbox).getClass().getMethod("getBlockPos").invoke(toolbox);
            return result instanceof BlockPos pos ? Optional.of(pos.immutable()) : Optional.empty();
        } catch (ReflectiveOperationException ignored) {
            return Optional.empty();
        }
    }

    private static IItemHandler resolveToolboxItemHandler(ToolboxBlockEntity toolbox) {
        var level = resolveToolboxLevel(toolbox).orElse(null);
        var pos = resolveToolboxPos(toolbox).orElse(null);
        if (level == null || pos == null) {
            return null;
        }
        return level.getCapability(
                net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.BLOCK,
                pos,
                null
        );
    }

    private static Optional<net.minecraft.world.level.Level> resolveToolboxLevel(ToolboxBlockEntity toolbox) {
        if (toolbox == null) {
            return Optional.empty();
        }

        try {
            var result = ((Object) toolbox).getClass().getMethod("getLevel").invoke(toolbox);
            return result instanceof net.minecraft.world.level.Level level ? Optional.of(level) : Optional.empty();
        } catch (ReflectiveOperationException ignored) {
            return Optional.empty();
        }
    }
}
