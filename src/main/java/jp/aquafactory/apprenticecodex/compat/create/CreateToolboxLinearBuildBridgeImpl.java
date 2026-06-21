package jp.aquafactory.apprenticecodex.compat.create;

import com.simibubi.create.content.equipment.toolbox.ToolboxBlockEntity;
import com.simibubi.create.content.equipment.toolbox.ToolboxHandler;
import jp.aquafactory.apprenticecodex.spell.linearbuild.LinearBuildItemSource;
import jp.aquafactory.apprenticecodex.spell.linearbuild.LinearBuildItemSources;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;

final class CreateToolboxLinearBuildBridgeImpl {
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
            if (toolbox == null || toolboxPos == null || !seenPositions.add(toolboxPos)) {
                continue;
            }
            addPlacedToolboxSource(toolbox, sources);
        }
    }

    private static void addPlacedToolboxSource(ToolboxBlockEntity toolbox, List<LinearBuildItemSource> sources) {
        resolveToolboxItemHandler(toolbox).ifPresent(handler ->
                sources.add(LinearBuildItemSources.itemHandler(handler, resolveToolboxDisplayName(toolbox), true)));
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

    private static LazyOptional<IItemHandler> resolveToolboxItemHandler(ToolboxBlockEntity toolbox) {
        try {
            // Ponder など Create の追加依存型を javac に露出しないため、capability 取得だけ反射で呼ぶ。
            var result = ((Object) toolbox).getClass()
                    .getMethod("getCapability", Capability.class, Direction.class)
                    .invoke(toolbox, ForgeCapabilities.ITEM_HANDLER, null);
            if (result instanceof LazyOptional<?> lazyOptional) {
                return lazyOptional.cast();
            }
        } catch (ReflectiveOperationException ignored) {
            // Toolbox の capability が読めない場合は、その Toolbox だけ取り寄せ対象から外す。
        }
        return LazyOptional.empty();
    }
}
