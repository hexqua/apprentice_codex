package jp.aquafactory.apprenticecodex.item.luminousdevice;

import jp.aquafactory.apprenticecodex.item.SneakSelectionUiItem;
import jp.aquafactory.apprenticecodex.registry.TagRegistry;
import jp.aquafactory.apprenticecodex.utility.BlockTools;
import jp.aquafactory.apprenticecodex.utility.CompactCountFormatter;
import net.minecraft.ChatFormatting;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class LuminousDevice extends Item implements SneakSelectionUiItem {
    public static final int MAX_STORED_ITEMS = 1024;

    private static final String STORAGE_TAG = "LuminousDevice";
    private static final String CONTENTS_TAG = "Contents";
    private static final String SELECTED_STACK_TAG = "SelectedStack";
    private static final String STACK_TAG = "Stack";
    private static final String COUNT_TAG = "Count";
    private static final int SELECTION_COUNT_COLOR = 0xFFFFFF;
    private static final int EMPTY_SELECTION_COUNT_COLOR = 0xFF5555;

    public LuminousDevice() {
        super(new Properties().stacksTo(1).fireResistant());
    }

    @Override
    public @NotNull Component getName(@NotNull ItemStack stack) {
        var selectedStack = getSelectedStack(stack);
        if (selectedStack.isEmpty()) {
            return super.getName(stack);
        }

        return Component.translatable(
                "item.apprenticecodex.luminous_device.with_select",
                super.getName(stack),
                selectedStack.getHoverName()
        );
    }

    @Override
    public @NotNull Optional<TooltipComponent> getTooltipImage(@NotNull ItemStack stack) {
        var displayStacks = NonNullList.<ItemStack>create();
        var contents = readContents(stack);
        for (var entry : contents) {
            displayStacks.add(entry.displayStack.copyWithCount(entry.count));
        }
        return Optional.of(new LuminousDeviceTooltip(
                displayStacks,
                findRemovalCandidateIndex(contents, getSelectedStack(stack)),
                getStoredItemCount(stack) >= MAX_STORED_ITEMS
        ));
    }

    @Override
    public boolean overrideStackedOnOther(@NotNull ItemStack deviceStack, @NotNull Slot slot, @NotNull ClickAction action, @NotNull Player player) {
        if (action != ClickAction.SECONDARY) {
            return false;
        }

        if (slot.hasItem()) {
            var slotStack = slot.getItem();
            if (!accepts(slotStack)) {
                return false;
            }

            var extracted = slot.safeTake(slotStack.getCount(), slotStack.getCount(), player);
            if (extracted.isEmpty()) {
                return false;
            }

            var inserted = addToDevice(deviceStack, extracted);
            var leftoverCount = extracted.getCount() - inserted;
            if (leftoverCount > 0) {
                slot.safeInsert(extracted.copyWithCount(leftoverCount));
            }
            return inserted > 0;
        }

        var previousTag = deviceStack.getTag() == null ? null : deviceStack.getTag().copy();
        var previousSelection = getSelectedStack(deviceStack);
        var removedStack = removeStackForInventory(deviceStack);
        if (removedStack.isEmpty()) {
            return false;
        }

        // safeInsert は渡したスタック自体を減らすため、挿入前の個数を退避して差分を求める。
        var removedCount = removedStack.getCount();
        var leftover = slot.safeInsert(removedStack);
        var insertedCount = removedCount - leftover.getCount();
        if (insertedCount <= 0) {
            deviceStack.setTag(previousTag);
            return false;
        }
        if (!leftover.isEmpty()) {
            addToDeviceWithoutAutoSelection(deviceStack, leftover);
            if (!previousSelection.isEmpty() && getStoredCount(deviceStack, previousSelection) > 0) {
                setSelectedStackInternal(deviceStack, previousSelection);
            }
        }
        return true;
    }

    @Override
    public boolean overrideOtherStackedOnMe(
            @NotNull ItemStack deviceStack,
            @NotNull ItemStack otherStack,
            @NotNull Slot slot,
            @NotNull ClickAction action,
            @NotNull Player player,
            @NotNull SlotAccess slotAccess
    ) {
        if (action != ClickAction.SECONDARY) {
            return false;
        }

        if (otherStack.isEmpty()) {
            var previousTag = deviceStack.getTag() == null ? null : deviceStack.getTag().copy();
            var removedStack = removeStackForInventory(deviceStack);
            if (removedStack.isEmpty()) {
                return false;
            }

            if (!slotAccess.set(removedStack)) {
                deviceStack.setTag(previousTag);
                return false;
            }

            slot.setChanged();
            return true;
        }

        var inserted = addToDevice(deviceStack, otherStack);
        if (inserted <= 0) {
            return false;
        }

        otherStack.shrink(inserted);
        slotAccess.set(otherStack);
        slot.setChanged();
        return true;
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(
            @NotNull Level level,
            @NotNull Player player,
            @NotNull InteractionHand usedHand
    ) {
        var deviceStack = player.getItemInHand(usedHand);
        var selectedStack = getSelectedStack(deviceStack);
        if (selectedStack.isEmpty()) {
            return InteractionResultHolder.pass(deviceStack);
        }
        if (getStoredCount(deviceStack, selectedStack) <= 0) {
            displayOutOfItem(player, selectedStack);
            return InteractionResultHolder.fail(deviceStack);
        }

        var interactionStack = BlockTools.copyForTemporaryUse(selectedStack);
        InteractionResultHolder<ItemStack> delegatedResult;
        try {
            player.setItemInHand(usedHand, interactionStack);
            delegatedResult = interactionStack.getItem().use(level, player, usedHand);
        } finally {
            player.setItemInHand(usedHand, deviceStack);
        }

        if (!level.isClientSide
                && delegatedResult.getResult().consumesAction()) {
            consumeSelectedForUse(player, deviceStack);
        }
        return new InteractionResultHolder<>(delegatedResult.getResult(), deviceStack);
    }

    @Override
    public @NotNull InteractionResult useOn(@NotNull UseOnContext context) {
        var player = context.getPlayer();
        if (player == null) {
            return InteractionResult.PASS;
        }

        var usedHand = context.getHand();
        var deviceStack = player.getItemInHand(usedHand);
        var selectedStack = getSelectedStack(deviceStack);
        if (selectedStack.isEmpty()) {
            return InteractionResult.PASS;
        }
        if (getStoredCount(deviceStack, selectedStack) <= 0) {
            displayOutOfItem(player, selectedStack);
            return InteractionResult.FAIL;
        }

        var interactionStack = BlockTools.copyForTemporaryUse(selectedStack);
        InteractionResult delegatedResult;
        try {
            // UseOnContext は生成時の手持ちを保持するため、差し替え後に作り直して元アイテムの設置処理へ渡す。
            player.setItemInHand(usedHand, interactionStack);
            var delegatedHit = new BlockHitResult(
                    context.getClickLocation(),
                    context.getClickedFace(),
                    context.getClickedPos(),
                    context.isInside()
            );
            var delegatedContext = new UseOnContext(player, usedHand, delegatedHit);
            delegatedResult = interactionStack.getItem().useOn(delegatedContext);
        } finally {
            player.setItemInHand(usedHand, deviceStack);
        }

        if (!context.getLevel().isClientSide
                && delegatedResult.consumesAction()) {
            consumeSelectedForUse(player, deviceStack);
        }
        return delegatedResult;
    }

    public static boolean accepts(ItemStack stack) {
        return !stack.isEmpty() && stack.is(TagRegistry.Items.LUMINOUS_DEVICE_STORABLE);
    }

    public static int addToDevice(ItemStack deviceStack, ItemStack stack) {
        var inserted = addToDeviceWithoutAutoSelection(deviceStack, stack);
        if (inserted > 0 && getSelectedStack(deviceStack).isEmpty()) {
            var contents = readContents(deviceStack);
            if (!contents.isEmpty()) {
                setSelectedStackInternal(deviceStack, contents.get(0).displayStack);
            }
        }
        return inserted;
    }

    public static boolean canStorePickedUpStack(ItemStack deviceStack, ItemStack stack) {
        if (!(deviceStack.getItem() instanceof LuminousDevice) || !accepts(stack)) {
            return false;
        }

        return getStoredCount(deviceStack, stack) > 0
                || sameStoredItem(getSelectedStack(deviceStack), stack);
    }

    public static int storePickedUpStackInInventoryDevices(Player player, ItemStack stack) {
        if (stack.isEmpty() || !accepts(stack)) {
            return 0;
        }

        var beforeCount = stack.getCount();
        storePickedUpStackInDevices(player.getInventory().items, stack);
        storePickedUpStackInDevices(player.getInventory().offhand, stack);
        return beforeCount - stack.getCount();
    }

    public static ItemStack removeStackForInventory(ItemStack deviceStack) {
        var contents = readContents(deviceStack);
        var selectedStack = getSelectedStack(deviceStack);
        var candidateIndex = findRemovalCandidateIndex(contents, selectedStack);
        if (candidateIndex < 0) {
            return ItemStack.EMPTY;
        }

        var entry = contents.get(candidateIndex);
        var removedSelectedEntry = sameStoredItem(entry.displayStack, selectedStack);
        var removedCount = Math.min(entry.count, entry.displayStack.getMaxStackSize());
        var removedStack = entry.displayStack.copyWithCount(removedCount);
        entry.count -= removedCount;
        if (entry.count <= 0) {
            contents.remove(candidateIndex);
        }
        writeContents(deviceStack, contents);

        if (contents.isEmpty()) {
            clearSelectedStack(deviceStack);
        } else if (removedSelectedEntry && countStoredItem(contents, selectedStack) <= 0) {
            setSelectedStackInternal(deviceStack, contents.get(0).displayStack);
        }
        return removedStack;
    }

    public static int getStoredItemCount(ItemStack deviceStack) {
        var total = 0;
        for (var entry : readContents(deviceStack)) {
            total += entry.count;
        }
        return total;
    }

    public static int getStoredCount(ItemStack deviceStack, ItemStack targetStack) {
        return countStoredItem(readContents(deviceStack), targetStack);
    }

    public static boolean consumeOneStored(ItemStack deviceStack, ItemStack targetStack) {
        var contents = readContents(deviceStack);
        for (int i = 0; i < contents.size(); ++i) {
            var entry = contents.get(i);
            if (!sameStoredItem(entry.displayStack, targetStack) || entry.count <= 0) {
                continue;
            }

            entry.count -= 1;
            if (entry.count <= 0) {
                contents.remove(i);
            }
            writeContents(deviceStack, contents);
            // LinearBuild から消費した場合も、補充対象とテンプレートを維持するため選択状態は残す。
            return true;
        }
        return false;
    }

    public static ItemStack getSelectedStack(ItemStack deviceStack) {
        if (!(deviceStack.getItem() instanceof LuminousDevice)) {
            return ItemStack.EMPTY;
        }

        var storageTag = deviceStack.getTagElement(STORAGE_TAG);
        if (storageTag == null || !storageTag.contains(SELECTED_STACK_TAG, Tag.TAG_COMPOUND)) {
            return ItemStack.EMPTY;
        }
        var selectedStack = ItemStack.of(storageTag.getCompound(SELECTED_STACK_TAG));
        return selectedStack.isEmpty() ? ItemStack.EMPTY : selectedStack.copyWithCount(1);
    }

    public static boolean setSelectedStack(ItemStack deviceStack, ItemStack requestedStack) {
        if (!(deviceStack.getItem() instanceof LuminousDevice) || requestedStack.isEmpty()) {
            return false;
        }

        var currentSelection = getSelectedStack(deviceStack);
        if (!sameStoredItem(currentSelection, requestedStack)
                && getStoredCount(deviceStack, requestedStack) <= 0) {
            return false;
        }
        setSelectedStackInternal(deviceStack, requestedStack);
        return true;
    }

    public static List<SelectionView> getSelectionViews(ItemStack deviceStack) {
        if (!(deviceStack.getItem() instanceof LuminousDevice)) {
            return List.of();
        }

        var selectedStack = getSelectedStack(deviceStack);
        var views = new ArrayList<SelectionView>();
        var selectedPresent = false;
        for (var entry : readContents(deviceStack)) {
            var currentSelection = sameStoredItem(entry.displayStack, selectedStack);
            selectedPresent |= currentSelection;
            views.add(createSelectionView(entry.displayStack, entry.count, currentSelection));
        }
        if (!selectedStack.isEmpty() && !selectedPresent) {
            views.add(createSelectionView(selectedStack, 0, true));
        }
        return List.copyOf(views);
    }

    private static SelectionView createSelectionView(ItemStack stack, int count, boolean currentSelection) {
        return new SelectionView(
                stack.copyWithCount(1),
                stack.getHoverName(),
                CompactCountFormatter.format(count).toLowerCase(java.util.Locale.ROOT),
                count > 0 ? SELECTION_COUNT_COLOR : EMPTY_SELECTION_COUNT_COLOR,
                currentSelection
        );
    }

    private static int addToDeviceWithoutAutoSelection(ItemStack deviceStack, ItemStack stack) {
        if (!(deviceStack.getItem() instanceof LuminousDevice) || !accepts(stack)) {
            return 0;
        }

        var remainingSpace = MAX_STORED_ITEMS - getStoredItemCount(deviceStack);
        var inserted = Math.min(remainingSpace, stack.getCount());
        if (inserted <= 0) {
            return 0;
        }

        var contents = readContents(deviceStack);
        for (var entry : contents) {
            if (!sameStoredItem(entry.displayStack, stack)) {
                continue;
            }
            entry.count += inserted;
            writeContents(deviceStack, contents);
            return inserted;
        }

        // ItemStack の Count を1024まで拡張せず、表示用スタックと実数を分けて保持する。
        contents.add(new StoredEntry(stack.copyWithCount(1), inserted));
        writeContents(deviceStack, contents);
        return inserted;
    }

    private static void storePickedUpStackInDevices(List<ItemStack> inventoryStacks, ItemStack pickedUpStack) {
        for (var deviceStack : inventoryStacks) {
            if (!canStorePickedUpStack(deviceStack, pickedUpStack)) {
                continue;
            }

            pickedUpStack.shrink(addToDeviceWithoutAutoSelection(deviceStack, pickedUpStack));
            if (pickedUpStack.isEmpty()) {
                return;
            }
        }
    }

    private static boolean consumeSelectedForUse(Player player, ItemStack deviceStack) {
        // クリエイティブの非消費は設置・使用成功後のこの経路だけに限定する。
        if (player.getAbilities().instabuild) {
            return false;
        }

        var selectedStack = getSelectedStack(deviceStack);
        if (selectedStack.isEmpty()) {
            return false;
        }

        var contents = readContents(deviceStack);
        for (int i = 0; i < contents.size(); ++i) {
            var entry = contents.get(i);
            if (!sameStoredItem(entry.displayStack, selectedStack) || entry.count <= 0) {
                continue;
            }

            entry.count -= 1;
            if (entry.count <= 0) {
                contents.remove(i);
            }
            writeContents(deviceStack, contents);
            // 使用消費では、空になっても選択中の表示用スタックを意図的に残す。
            return true;
        }
        return false;
    }

    private static void displayOutOfItem(Player player, ItemStack selectedStack) {
        if (player.level().isClientSide) {
            return;
        }
        player.displayClientMessage(
                Component.translatable(
                        "ui.apprenticecodex.luminous_device.out_of_item",
                        selectedStack.getHoverName()
                ).withStyle(ChatFormatting.RED),
                true
        );
    }

    private static List<StoredEntry> readContents(ItemStack deviceStack) {
        var storageTag = deviceStack.getTagElement(STORAGE_TAG);
        if (storageTag == null || !storageTag.contains(CONTENTS_TAG, Tag.TAG_LIST)) {
            return new ArrayList<>();
        }

        var contentsTag = storageTag.getList(CONTENTS_TAG, Tag.TAG_COMPOUND);
        var contents = new ArrayList<StoredEntry>(contentsTag.size());
        for (var entryTag : contentsTag) {
            if (!(entryTag instanceof CompoundTag compoundTag)) {
                continue;
            }

            var storedStack = ItemStack.of(compoundTag.getCompound(STACK_TAG));
            var storedCount = compoundTag.getInt(COUNT_TAG);
            if (!storedStack.isEmpty() && storedCount > 0) {
                contents.add(new StoredEntry(storedStack.copyWithCount(1), storedCount));
            }
        }
        return contents;
    }

    private static void writeContents(ItemStack deviceStack, List<StoredEntry> contents) {
        var storageTag = deviceStack.getOrCreateTagElement(STORAGE_TAG);
        var contentsTag = new ListTag();
        for (var entry : contents) {
            if (entry.displayStack.isEmpty() || entry.count <= 0) {
                continue;
            }
            var entryTag = new CompoundTag();
            entryTag.put(STACK_TAG, entry.displayStack.copyWithCount(1).save(new CompoundTag()));
            entryTag.putInt(COUNT_TAG, entry.count);
            contentsTag.add(entryTag);
        }

        if (contentsTag.isEmpty()) {
            storageTag.remove(CONTENTS_TAG);
        } else {
            storageTag.put(CONTENTS_TAG, contentsTag);
        }
        removeEmptyStorageTag(deviceStack);
    }

    private static void setSelectedStackInternal(ItemStack deviceStack, ItemStack selectedStack) {
        deviceStack.getOrCreateTagElement(STORAGE_TAG).put(
                SELECTED_STACK_TAG,
                selectedStack.copyWithCount(1).save(new CompoundTag())
        );
    }

    private static void clearSelectedStack(ItemStack deviceStack) {
        var storageTag = deviceStack.getTagElement(STORAGE_TAG);
        if (storageTag == null) {
            return;
        }
        storageTag.remove(SELECTED_STACK_TAG);
        removeEmptyStorageTag(deviceStack);
    }

    private static void removeEmptyStorageTag(ItemStack deviceStack) {
        var storageTag = deviceStack.getTagElement(STORAGE_TAG);
        if (storageTag != null && storageTag.isEmpty()) {
            deviceStack.removeTagKey(STORAGE_TAG);
        }
    }

    private static int countStoredItem(List<StoredEntry> contents, ItemStack targetStack) {
        if (targetStack.isEmpty()) {
            return 0;
        }

        var total = 0;
        for (var entry : contents) {
            if (sameStoredItem(entry.displayStack, targetStack)) {
                total += entry.count;
            }
        }
        return total;
    }

    private static int findRemovalCandidateIndex(List<StoredEntry> contents, ItemStack selectedStack) {
        for (int i = 0; i < contents.size(); ++i) {
            var entry = contents.get(i);
            if (entry.count > 0 && sameStoredItem(entry.displayStack, selectedStack)) {
                return i;
            }
        }

        // 将来、魔法などアイテム以外が選択されている場合も取り出せるよう、従来の最少数規則を残す。
        var candidateIndex = -1;
        var candidateCount = Integer.MAX_VALUE;
        for (int i = 0; i < contents.size(); ++i) {
            var entry = contents.get(i);
            if (entry.displayStack.isEmpty() || entry.count <= 0) {
                continue;
            }
            if (entry.count < candidateCount) {
                candidateIndex = i;
                candidateCount = entry.count;
            }
        }
        return candidateIndex;
    }

    private static boolean sameStoredItem(ItemStack first, ItemStack second) {
        return !first.isEmpty() && !second.isEmpty() && ItemStack.isSameItemSameTags(first, second);
    }

    public record SelectionView(
            ItemStack iconStack,
            Component displayName,
            String badgeText,
            int badgeColor,
            boolean currentSelection
    ) {
        public SelectionView {
            iconStack = iconStack.copyWithCount(1);
        }
    }

    private static final class StoredEntry {
        private final ItemStack displayStack;
        private int count;

        private StoredEntry(ItemStack displayStack, int count) {
            this.displayStack = displayStack;
            this.count = count;
        }
    }
}
