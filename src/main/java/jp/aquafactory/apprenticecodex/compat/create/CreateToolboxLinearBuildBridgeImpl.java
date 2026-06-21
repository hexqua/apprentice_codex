package jp.aquafactory.apprenticecodex.compat.create;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.equipment.toolbox.ToolboxBlockEntity;
import com.simibubi.create.content.equipment.toolbox.ToolboxHandler;
import com.simibubi.create.content.equipment.toolbox.ToolboxInventory;
import jp.aquafactory.apprenticecodex.spell.linearbuild.LinearBuildItemSource;
import jp.aquafactory.apprenticecodex.spell.linearbuild.LinearBuildItemSources;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

final class CreateToolboxLinearBuildBridgeImpl {
    private static final String INVENTORY_TAG = "Inventory";

    private CreateToolboxLinearBuildBridgeImpl() {
    }

    static List<LinearBuildItemSource> collectSources(ServerPlayer player) {
        var sources = new ArrayList<LinearBuildItemSource>();
        addPlacedToolboxSources(player, sources);
        addInventoryToolboxSources(player, sources);
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
        addScannedPlacedToolboxSources(player, seenPositions, sources);
    }

    private static void addScannedPlacedToolboxSources(
            ServerPlayer player,
            Set<BlockPos> seenPositions,
            List<LinearBuildItemSource> sources
    ) {
        var maxRange = ToolboxHandler.getMaxRange(player);
        if (maxRange <= 0) {
            return;
        }

        var level = player.level();
        var origin = player.blockPosition();
        var scanRange = (int) Math.ceil(maxRange);
        var candidates = new ArrayList<PlacedToolbox>();
        for (var pos : BlockPos.betweenClosed(origin.offset(-scanRange, -scanRange, -scanRange), origin.offset(scanRange, scanRange, scanRange))) {
            var immutablePos = pos.immutable();
            if (seenPositions.contains(immutablePos) || !level.isLoaded(immutablePos)
                    || !AllBlocks.TOOLBOXES.contains(level.getBlockState(immutablePos).getBlock())
                    || ToolboxHandler.distance(player.position(), immutablePos) >= maxRange * maxRange) {
                continue;
            }
            if (level.getBlockEntity(immutablePos) instanceof ToolboxBlockEntity toolbox) {
                candidates.add(new PlacedToolbox(immutablePos, toolbox));
            }
        }

        candidates.sort(Comparator.comparingDouble(toolbox -> ToolboxHandler.distance(player.position(), toolbox.pos())));
        for (var toolbox : candidates) {
            if (seenPositions.add(toolbox.pos())) {
                addPlacedToolboxSource(toolbox.blockEntity(), sources);
            }
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

    private static void addInventoryToolboxSources(ServerPlayer player, List<LinearBuildItemSource> sources) {
        var inventory = player.getInventory();
        for (var slot = 0; slot < inventory.items.size(); ++slot) {
            var stack = inventory.items.get(slot);
            if (isToolboxStack(stack)) {
                sources.add(new InventoryToolboxSource(stack, stack.getHoverName(), inventory));
            }
        }
    }

    private static boolean isToolboxStack(ItemStack stack) {
        return stack.getItem() instanceof BlockItem blockItem && AllBlocks.TOOLBOXES.contains(blockItem.getBlock());
    }

    private static final class InventoryToolboxSource implements LinearBuildItemSource {
        private final ItemStack toolboxStack;
        private final Component label;
        private final Inventory inventory;

        private InventoryToolboxSource(ItemStack toolboxStack, Component label, Inventory inventory) {
            this.toolboxStack = toolboxStack;
            this.label = label;
            this.inventory = inventory;
        }

        @Override
        public Component label() {
            return label;
        }

        @Override
        public boolean shouldNotifyRetrieved() {
            return true;
        }

        @Override
        public boolean hasMatchingItem(ItemStack template) {
            var toolboxInventory = readInventory();
            return findSlot(toolboxInventory, template) >= 0;
        }

        @Override
        public boolean consumeOne(ItemStack template) {
            var toolboxInventory = readInventory();
            var slot = findSlot(toolboxInventory, template);
            if (slot < 0 || toolboxInventory.extractItem(slot, 1, false).isEmpty()) {
                return false;
            }

            toolboxInventory.settle(slot / ToolboxInventory.STACKS_PER_COMPARTMENT);
            saveInventory(toolboxInventory);
            inventory.setChanged();
            return true;
        }

        private ToolboxInventory readInventory() {
            var toolboxInventory = new ToolboxInventory(null);
            var tag = toolboxStack.getTag();
            if (tag != null && tag.contains(INVENTORY_TAG, Tag.TAG_COMPOUND)) {
                toolboxInventory.deserializeNBT(tag.getCompound(INVENTORY_TAG));
            }
            return toolboxInventory;
        }

        private void saveInventory(ToolboxInventory toolboxInventory) {
            toolboxStack.getOrCreateTag().put(INVENTORY_TAG, toolboxInventory.serializeNBT());
        }

        private static int findSlot(ToolboxInventory toolboxInventory, ItemStack template) {
            for (var slot = 0; slot < toolboxInventory.getSlots(); ++slot) {
                var stack = toolboxInventory.getStackInSlot(slot);
                if (LinearBuildItemSources.isSameItemIgnoringEmptyTag(stack, template)
                        && !toolboxInventory.extractItem(slot, 1, true).isEmpty()) {
                    return slot;
                }
            }
            return -1;
        }
    }

    private record PlacedToolbox(BlockPos pos, ToolboxBlockEntity blockEntity) {
    }
}
