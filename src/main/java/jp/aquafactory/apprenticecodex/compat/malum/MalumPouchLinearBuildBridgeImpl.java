package jp.aquafactory.apprenticecodex.compat.malum;

import com.sammy.malum.common.data.component.pouch.MalumPouchContentsComponent;
import com.sammy.malum.common.item.curiosities.pouch.MalumPouchItem;
import jp.aquafactory.apprenticecodex.spell.linearbuild.LinearBuildItemSource;
import jp.aquafactory.apprenticecodex.spell.linearbuild.LinearBuildItemSources;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

final class MalumPouchLinearBuildBridgeImpl {
    private MalumPouchLinearBuildBridgeImpl() {
    }

    static List<LinearBuildItemSource> collectSources(ServerPlayer player) {
        var sources = new ArrayList<LinearBuildItemSource>();
        var inventory = player.getInventory();
        inventory.items.forEach(stack -> addSource(stack, inventory, sources));
        inventory.offhand.forEach(stack -> addSource(stack, inventory, sources));
        return sources;
    }

    private static void addSource(
            ItemStack stack,
            Inventory inventory,
            List<LinearBuildItemSource> sources
    ) {
        if (stack.getItem() instanceof MalumPouchItem pouchItem) {
            sources.add(new MalumPouchSource(stack, pouchItem, inventory));
        }
    }

    private record MalumPouchSource(
            ItemStack pouchStack,
            MalumPouchItem pouchItem,
            Inventory inventory
    ) implements LinearBuildItemSource {
        @Override
        public Component label() {
            return pouchStack.getHoverName();
        }

        @Override
        public boolean shouldNotifyRetrieved() {
            return true;
        }

        @Override
        public boolean hasMatchingItem(ItemStack template) {
            return findMatchingIndex(contents(), template) >= 0;
        }

        @Override
        public boolean consumeOne(ItemStack template) {
            var contents = contents();
            // Malum の getItemsCopy() は元リストに結び付いた遅延変換 view のため、更新前に実体化する。
            var items = new ArrayList<>(contents.getItemsCopy());
            var index = findMatchingIndex(items, template);
            if (index < 0) {
                return false;
            }

            var consumedStack = items.get(index);
            consumedStack.shrink(1);
            if (consumedStack.isEmpty()) {
                items.remove(index);
            }

            var mutable = contents.mutable();
            mutable.clearItems();
            for (var itemIndex = items.size() - 1; itemIndex >= 0; --itemIndex) {
                var remainingStack = items.get(itemIndex).copy();
                var expectedCount = remainingStack.getCount();
                if (mutable.tryInsert(remainingStack) != expectedCount || !remainingStack.isEmpty()) {
                    return false;
                }
            }

            pouchItem.setContents(pouchStack, mutable.immutable());
            inventory.setChanged();
            return true;
        }

        @Override
        public long countMatchingItems(ItemStack template) {
            var total = 0L;
            for (var stack : contents().getItems()) {
                if (LinearBuildItemSources.isSameItemIgnoringEmptyTag(stack, template)) {
                    total += stack.getCount();
                }
            }
            return total;
        }

        private MalumPouchContentsComponent contents() {
            return pouchItem.getContents(pouchStack);
        }

        private static int findMatchingIndex(MalumPouchContentsComponent contents, ItemStack template) {
            return findMatchingIndex(contents.getItems(), template);
        }

        private static int findMatchingIndex(List<ItemStack> items, ItemStack template) {
            for (var index = 0; index < items.size(); ++index) {
                if (LinearBuildItemSources.isSameItemIgnoringEmptyTag(items.get(index), template)) {
                    return index;
                }
            }
            return -1;
        }
    }
}
