package jp.aquafactory.apprenticecodex.item.curios.spellcasterquiver;

import jp.aquafactory.apprenticecodex.item.FocusStaffbow;
import jp.aquafactory.apprenticecodex.item.curios.CuriosSlotConstants;
import jp.aquafactory.apprenticecodex.item.curios.spellcasterammopouch.SpellcasterAmmoPouchTooltip;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import jp.aquafactory.apprenticecodex.registry.TagRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ArrowItem;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.SlotResult;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Predicate;

public class SpellcasterQuiver extends Item implements ICurioItem {
    private static final int MAX_STORED_ITEMS = 512;
    private static final int BAR_COLOR = 0xA8792A;
    private static final String STORAGE_TAG = "SpellcasterQuiver";
    private static final String CONTENTS_TAG = "Contents";
    private static final String STACK_TAG = "Stack";
    private static final String COUNT_TAG = "Count";

    private final String slotIdentifier;

    public SpellcasterQuiver() {
        super(new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON));
        slotIdentifier = CuriosSlotConstants.BACK;
    }

    @Override
    public List<Component> getSlotsTooltip(List<Component> tooltips, ItemStack stack) {
        if (slotIdentifier != null) {
            tooltips.add(Component.empty());
            tooltips.add(Component.translatable("curios.modifiers." + slotIdentifier).withStyle(ChatFormatting.GOLD));
            tooltips.add(Component.literal(" ")
                    .append(Component.translatable(getDescriptionId() + ".desc_1"))
                    .withStyle(Style.EMPTY.withColor(ChatFormatting.YELLOW)));
            tooltips.add(Component.literal(" ")
                    .append(Component.translatable(getDescriptionId() + ".desc_2"))
                    .withStyle(Style.EMPTY.withColor(ChatFormatting.YELLOW)));
            tooltips.add(Component.literal(" ")
                    .append(Component.translatable(getDescriptionId() + ".desc_3"))
                    .withStyle(Style.EMPTY.withColor(ChatFormatting.YELLOW)));
        }

        return tooltips;
    }

    @Override
    public boolean canEquipFromUse(SlotContext slotContext, ItemStack stack) {
        return true;
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @Nullable Level level, @NotNull List<Component> lines, @NotNull TooltipFlag flag) {
        super.appendHoverText(stack, level, lines, flag);

        var storedItemCount = getStoredItemCount(stack);
        lines.add(Component.translatable(
                getDescriptionId() + ".stored_amount",
                storedItemCount,
                MAX_STORED_ITEMS
        ).withStyle(ChatFormatting.GRAY));
    }

    @Override
    public @NotNull Optional<TooltipComponent> getTooltipImage(@NotNull ItemStack stack) {
        var contents = readContents(stack);
        if (contents.isEmpty()) {
            return Optional.empty();
        }

        var displayStacks = NonNullList.<ItemStack>create();
        for (var entry : contents) {
            displayStacks.add(entry.displayStack.copyWithCount(entry.count));
        }
        return Optional.of(new SpellcasterAmmoPouchTooltip(
                displayStacks,
                findRemovalCandidateDisplayIndex(contents),
                getStoredItemCount(stack) >= MAX_STORED_ITEMS
        ));
    }

    @Override
    public boolean overrideStackedOnOther(@NotNull ItemStack quiverStack, @NotNull Slot slot, @NotNull ClickAction action, @NotNull Player player) {
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

            var inserted = addToQuiver(quiverStack, extracted);
            var leftoverCount = extracted.getCount() - inserted;
            if (leftoverCount > 0) {
                slot.safeInsert(extracted.copyWithCount(leftoverCount));
            }

            return inserted > 0;
        }

        var removedStack = removeStackFromQuiver(quiverStack);
        if (removedStack.isEmpty()) {
            return false;
        }

        var leftover = slot.safeInsert(removedStack);
        if (!leftover.isEmpty()) {
            addToQuiver(quiverStack, leftover);
        }
        return leftover.getCount() < removedStack.getCount();
    }

    @Override
    public boolean overrideOtherStackedOnMe(
            @NotNull ItemStack quiverStack,
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
            var removedStack = removeStackFromQuiver(quiverStack);
            if (removedStack.isEmpty()) {
                return false;
            }

            if (!slotAccess.set(removedStack)) {
                addToQuiver(quiverStack, removedStack);
                return false;
            }

            slot.setChanged();
            return true;
        }

        var inserted = addToQuiver(quiverStack, otherStack);
        if (inserted <= 0) {
            return false;
        }

        otherStack.shrink(inserted);
        slotAccess.set(otherStack);
        slot.setChanged();
        return true;
    }

    @Override
    public boolean isBarVisible(@NotNull ItemStack stack) {
        return getStoredItemCount(stack) > 0;
    }

    @Override
    public int getBarWidth(@NotNull ItemStack stack) {
        var storedItemCount = getStoredItemCount(stack);
        return Math.max(1, Math.round(13.0F * storedItemCount / (float) MAX_STORED_ITEMS));
    }

    @Override
    public int getBarColor(@NotNull ItemStack stack) {
        return BAR_COLOR;
    }

    public static boolean isEquippedBy(@Nullable LivingEntity entity) {
        if (entity == null) {
            return false;
        }

        return CuriosApi.getCuriosInventory(entity)
                .map(inventory -> inventory.isEquipped(ItemRegistry.SPELLCASTER_QUIVER.get()))
                .orElse(false);
    }

    public static boolean shouldIgnoreBowSlowdown(@Nullable LivingEntity entity) {
        if (entity == null || !isEquippedBy(entity) || !entity.isUsingItem()) {
            return false;
        }

        // FocusStaffbow は右クリック保持中の移動を弓の引き絞りとして扱い、Quiver の恩恵を共有する。
        return entity.getUseItem().getItem() instanceof BowItem || FocusStaffbow.isBowDrawUse(entity);
    }

    public static int store(ItemStack quiverStack, ItemStack stack) {
        if (stack.isEmpty()) {
            return 0;
        }

        return addToQuiver(quiverStack, stack.copy());
    }

    public static ItemStack removeOneStack(ItemStack quiverStack) {
        return removeStackFromQuiver(quiverStack);
    }

    public static int storeInEquippedQuivers(Player player, ItemStack stack) {
        if (stack.isEmpty() || !accepts(stack)) {
            return 0;
        }

        return storeInQuivers(getEquippedQuivers(player), stack);
    }

    @Nullable
    public static ItemStack findAccessibleArrow(Player player, Predicate<ItemStack> predicate) {
        for (var quiverStack : getEquippedQuivers(player)) {
            var match = findMatchingEntryCopy(quiverStack, predicate);
            if (!match.isEmpty()) {
                return match;
            }
        }
        for (var quiverStack : getInventoryQuivers(player)) {
            var match = findMatchingEntryCopy(quiverStack, predicate);
            if (!match.isEmpty()) {
                return match;
            }
        }
        return null;
    }

    public static boolean consumeAccessibleArrow(Player player, Predicate<ItemStack> predicate) {
        for (var quiverStack : getEquippedQuivers(player)) {
            if (consumeOneMatching(quiverStack, predicate)) {
                return true;
            }
        }
        for (var quiverStack : getInventoryQuivers(player)) {
            if (consumeOneMatching(quiverStack, predicate)) {
                return true;
            }
        }
        return false;
    }

    public static void forEachAccessibleArrow(Player player, BiConsumer<ItemStack, Integer> consumer) {
        forEachStoredArrow(getEquippedQuivers(player), consumer);
        forEachStoredArrow(getInventoryQuivers(player), consumer);
    }

    public static int getStoredItemCount(ItemStack quiverStack) {
        var total = 0;
        for (var entry : readContents(quiverStack)) {
            total += entry.count;
        }
        return total;
    }

    public static boolean accepts(ItemStack stack) {
        return !stack.isEmpty() && (stack.getItem() instanceof ArrowItem || stack.is(TagRegistry.Items.SPELLCASTER_QUIVER_STORABLE));
    }

    private static void forEachStoredArrow(List<ItemStack> quiverStacks, BiConsumer<ItemStack, Integer> consumer) {
        for (var quiverStack : quiverStacks) {
            for (var entry : readContents(quiverStack)) {
                if (entry.displayStack.isEmpty() || entry.count <= 0) {
                    continue;
                }
                consumer.accept(entry.displayStack.copy(), entry.count);
            }
        }
    }

    private static ItemStack findMatchingEntryCopy(ItemStack quiverStack, Predicate<ItemStack> predicate) {
        var contents = readContents(quiverStack);
        var candidate = findRemovalCandidateEntry(contents, predicate);
        if (candidate == null) {
            return ItemStack.EMPTY;
        }
        return candidate.displayStack.copyWithCount(1);
    }

    private static boolean consumeOneMatching(ItemStack quiverStack, Predicate<ItemStack> predicate) {
        var contents = readContents(quiverStack);
        var candidateIndex = findRemovalCandidateSourceIndex(contents, predicate);
        if (candidateIndex < 0) {
            return false;
        }

        var entry = contents.get(candidateIndex);
        if (entry.count == 1) {
            contents.remove(candidateIndex);
        } else {
            entry.count -= 1;
        }
        writeContents(quiverStack, contents);
        return true;
    }

    private static int addToQuiver(ItemStack quiverStack, ItemStack stack) {
        if (!accepts(stack)) {
            return 0;
        }

        var remainingSpace = MAX_STORED_ITEMS - getStoredItemCount(quiverStack);
        var inserted = Math.min(remainingSpace, stack.getCount());
        if (inserted <= 0) {
            return 0;
        }

        var contents = readContents(quiverStack);
        for (var entry : contents) {
            if (!ItemStack.isSameItemSameTags(entry.displayStack, stack)) {
                continue;
            }

            entry.count += inserted;
            writeContents(quiverStack, contents);
            return inserted;
        }

        // 効能付きの矢も保持したいため、種別判定は NBT 込みの ItemStack 単位で分ける。
        contents.add(new StoredEntry(stack.copyWithCount(1), inserted));
        writeContents(quiverStack, contents);
        return inserted;
    }

    private static ItemStack removeStackFromQuiver(ItemStack quiverStack) {
        var contents = readContents(quiverStack);
        var candidateIndex = findRemovalCandidateSourceIndex(contents, stack -> true);
        if (candidateIndex < 0) {
            return ItemStack.EMPTY;
        }

        var entry = contents.get(candidateIndex);
        var removedCount = Math.min(entry.count, entry.displayStack.getMaxStackSize());
        var removedStack = entry.displayStack.copyWithCount(removedCount);
        entry.count -= removedCount;
        if (entry.count <= 0) {
            contents.remove(candidateIndex);
        }
        writeContents(quiverStack, contents);
        return removedStack;
    }

    private static List<ItemStack> getEquippedQuivers(Player player) {
        return CuriosApi.getCuriosInventory(player)
                .map(inventory -> inventory.findCurios(ItemRegistry.SPELLCASTER_QUIVER.get()).stream()
                        .map(SlotResult::stack)
                        .toList())
                .orElse(List.of());
    }

    private static List<ItemStack> getInventoryQuivers(Player player) {
        var quivers = new ArrayList<ItemStack>();
        appendInventoryQuivers(quivers, player.getInventory().items);
        appendInventoryQuivers(quivers, player.getInventory().offhand);
        return quivers;
    }

    private static void appendInventoryQuivers(List<ItemStack> destination, List<ItemStack> source) {
        for (var stack : source) {
            if (stack.is(ItemRegistry.SPELLCASTER_QUIVER.get())) {
                destination.add(stack);
            }
        }
    }

    private static int storeInQuivers(List<ItemStack> quiverStacks, ItemStack stack) {
        var beforeCount = stack.getCount();
        for (var quiverStack : quiverStacks) {
            stack.shrink(addToQuiver(quiverStack, stack));
            if (stack.isEmpty()) {
                break;
            }
        }
        return beforeCount - stack.getCount();
    }

    private static List<StoredEntry> readContents(ItemStack quiverStack) {
        var storageTag = quiverStack.getTagElement(STORAGE_TAG);
        if (storageTag == null || !storageTag.contains(CONTENTS_TAG, Tag.TAG_LIST)) {
            return new ArrayList<>();
        }

        var contentsTag = storageTag.getList(CONTENTS_TAG, Tag.TAG_COMPOUND);
        var contents = new ArrayList<StoredEntry>(contentsTag.size());
        for (var entryTag : contentsTag) {
            if (!(entryTag instanceof CompoundTag compoundTag)) {
                continue;
            }

            var stackTag = compoundTag.getCompound(STACK_TAG);
            var storedStack = ItemStack.of(stackTag);
            var storedCount = compoundTag.getInt(COUNT_TAG);
            if (storedStack.isEmpty() || storedCount <= 0) {
                continue;
            }

            contents.add(new StoredEntry(storedStack, storedCount));
        }
        return contents;
    }

    private static void writeContents(ItemStack quiverStack, List<StoredEntry> contents) {
        if (contents.isEmpty()) {
            quiverStack.removeTagKey(STORAGE_TAG);
            return;
        }

        var contentsTag = new ListTag();
        for (var entry : contents) {
            if (entry.displayStack.isEmpty() || entry.count <= 0) {
                continue;
            }

            var compoundTag = new CompoundTag();
            compoundTag.put(STACK_TAG, entry.displayStack.copyWithCount(1).save(new CompoundTag()));
            compoundTag.putInt(COUNT_TAG, entry.count);
            contentsTag.add(compoundTag);
        }

        if (contentsTag.isEmpty()) {
            quiverStack.removeTagKey(STORAGE_TAG);
            return;
        }

        quiverStack.getOrCreateTagElement(STORAGE_TAG).put(CONTENTS_TAG, contentsTag);
    }

    private static int findRemovalCandidateDisplayIndex(List<StoredEntry> displayEntries) {
        var highlightedEntry = displayEntries.isEmpty() ? null : findRemovalCandidateEntry(displayEntries, stack -> true);
        if (highlightedEntry == null) {
            return -1;
        }

        for (int i = 0; i < displayEntries.size(); ++i) {
            if (displayEntries.get(i) == highlightedEntry) {
                return i;
            }
        }
        return -1;
    }

    private static int findRemovalCandidateSourceIndex(List<StoredEntry> contents, Predicate<ItemStack> predicate) {
        var candidate = findRemovalCandidateEntry(contents, predicate);
        if (candidate == null) {
            return -1;
        }

        for (int i = 0; i < contents.size(); ++i) {
            if (contents.get(i) == candidate) {
                return i;
            }
        }
        return -1;
    }

    @Nullable
    private static StoredEntry findRemovalCandidateEntry(List<StoredEntry> contents, Predicate<ItemStack> predicate) {
        StoredEntry bestEntry = null;
        for (var entry : contents) {
            if (entry.displayStack.isEmpty() || entry.count <= 0 || !predicate.test(entry.displayStack)) {
                continue;
            }

            if (bestEntry == null || entry.count < bestEntry.count) {
                bestEntry = entry;
            }
        }
        return bestEntry;
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
