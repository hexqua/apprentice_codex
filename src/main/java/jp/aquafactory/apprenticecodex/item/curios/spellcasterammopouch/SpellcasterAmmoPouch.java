package jp.aquafactory.apprenticecodex.item.curios.spellcasterammopouch;

import jp.aquafactory.apprenticecodex.item.InventoryInsertTarget;
import jp.aquafactory.apprenticecodex.item.curios.CuriosSlotConstants;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import jp.aquafactory.apprenticecodex.registry.TagRegistry;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import org.jetbrains.annotations.Nullable;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class SpellcasterAmmoPouch extends Item implements ICurioItem, InventoryInsertTarget {
    private static final float EQUIPPED_EMPTY_CASING_RETURN_CHANCE = 0.9F;
    private static final int MAX_STORED_ITEMS = 1024;
    private static final int BAR_COLOR = 0xD79C37;
    private static final String STORAGE_TAG = "AmmoPouch";
    private static final String CONTENTS_TAG = "Contents";
    private static final String ITEM_ID_TAG = "ItemId";
    private static final String COUNT_TAG = "Count";

    private final String slotIdentifier;

    public SpellcasterAmmoPouch() {
        super(new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON));
        slotIdentifier = CuriosSlotConstants.BELT;
    }

    @Override
    public List<Component> getSlotsTooltip(List<Component> tooltips, Item.TooltipContext context, ItemStack stack) {
        var result = new ArrayList<>(tooltips);
        if (slotIdentifier != null) {
            result.add(Component.empty());
            result.add(Component.translatable("curios.modifiers." + slotIdentifier).withStyle(ChatFormatting.GOLD));
            result.add(Component.literal(" ")
                    .append(Component.translatable(getDescriptionId() + ".desc_1"))
                    .withStyle(Style.EMPTY.withColor(ChatFormatting.YELLOW)));
            result.add(Component.literal(" ")
                    .append(Component.translatable(getDescriptionId() + ".desc_2"))
                    .withStyle(Style.EMPTY.withColor(ChatFormatting.YELLOW)));
            result.add(Component.literal(" ")
                    .append(Component.translatable(getDescriptionId() + ".desc_3"))
                    .withStyle(Style.EMPTY.withColor(ChatFormatting.YELLOW)));
        }

        return result;
    }

    @Override
    public boolean canEquipFromUse(SlotContext slotContext, ItemStack stack) {
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> lines, TooltipFlag flag) {
        super.appendHoverText(stack, context, lines, flag);

        var storedItemCount = getStoredItemCount(stack);
        lines.add(Component.translatable(
                getDescriptionId() + ".stored_amount",
                storedItemCount,
                MAX_STORED_ITEMS
        ).withStyle(ChatFormatting.GRAY));
    }

    @Override
    public Optional<TooltipComponent> getTooltipImage(ItemStack stack) {
        var contents = readContents(stack);
        if (contents.isEmpty()) {
            return Optional.empty();
        }

        var displayStacks = NonNullList.<ItemStack>create();
        var displayEntries = getDisplayEntries(contents);
        for (var entry : displayEntries) {
            displayStacks.add(entry.displayStack.copyWithCount(entry.count));
        }
        return Optional.of(new SpellcasterAmmoPouchTooltip(
                displayStacks,
                findRemovalCandidateDisplayIndex(displayEntries),
                getStoredItemCount(stack) >= MAX_STORED_ITEMS
        ));
    }

    @Override
    public boolean overrideStackedOnOther(ItemStack pouchStack, Slot slot, ClickAction action, Player player) {
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

            var inserted = addToPouch(pouchStack, extracted);
            var leftoverCount = extracted.getCount() - inserted;
            if (leftoverCount > 0) {
                slot.safeInsert(extracted.copyWithCount(leftoverCount));
            }

            return inserted > 0;
        }

        var removedStack = removeStackFromPouch(pouchStack);
        if (removedStack.isEmpty()) {
            return false;
        }

        var leftover = slot.safeInsert(removedStack);
        if (!leftover.isEmpty()) {
            addToPouch(pouchStack, leftover);
        }
        return leftover.getCount() < removedStack.getCount();
    }

    @Override
    public boolean overrideOtherStackedOnMe(
            ItemStack pouchStack,
            ItemStack otherStack,
            Slot slot,
            ClickAction action,
            Player player,
            SlotAccess slotAccess
    ) {
        if (action != ClickAction.SECONDARY) {
            return false;
        }
        if (!InventoryInsertTarget.canModifyStorageSlot(pouchStack, slot, player)) {
            return false;
        }

        if (otherStack.isEmpty()) {
            var removedStack = removeStackFromPouch(pouchStack);
            if (removedStack.isEmpty()) {
                return false;
            }

            if (!slotAccess.set(removedStack)) {
                addToPouch(pouchStack, removedStack);
                return false;
            }

            slot.setChanged();
            return true;
        }

        var inserted = addToPouch(pouchStack, otherStack);
        if (inserted <= 0) {
            return false;
        }

        otherStack.shrink(inserted);
        slotAccess.set(otherStack);
        slot.setChanged();
        return true;
    }

    @Override
    public InsertHint getInventoryInsertHint(ItemStack storageStack, ItemStack incomingStack, Player player) {
        return storageStack.getItem() instanceof SpellcasterAmmoPouch
                && accepts(incomingStack)
                && getStoredItemCount(storageStack) < MAX_STORED_ITEMS
                ? InsertHint.ITEM
                : InsertHint.NONE;
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return getStoredItemCount(stack) > 0;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        var storedItemCount = getStoredItemCount(stack);
        return Math.max(1, Math.round(13.0F * storedItemCount / (float) MAX_STORED_ITEMS));
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return BAR_COLOR;
    }

    public static float applyEmptyCasingReturnChanceBonus(float baseChance, @Nullable LivingEntity entity) {
        if (!isEquippedBy(entity)) {
            return baseChance;
        }

        return Math.max(baseChance, EQUIPPED_EMPTY_CASING_RETURN_CHANCE);
    }

    public static boolean isEquippedBy(@Nullable LivingEntity entity) {
        if (entity == null) {
            return false;
        }

        return CuriosApi.getCuriosInventory(entity)
                .map(inventory -> inventory.findFirstCurio(stack -> stack.is(ItemRegistry.SPELLCASTER_AMMO_POUCH.get())).isPresent())
                .orElse(false);
    }

    public static boolean hasAmmoInAccessiblePouches(Player player, Item ammoItem) {
        return withAccessiblePouch(player, pouchStack -> containsItem(pouchStack, ammoItem));
    }

    public static int countAmmoInAccessiblePouches(Player player, Item ammoItem) {
        var total = 0;
        for (var pouchStack : getEquippedPouches(player)) {
            total += countItem(pouchStack, ammoItem);
        }
        for (var pouchStack : getInventoryPouches(player)) {
            total += countItem(pouchStack, ammoItem);
        }
        return total;
    }

    public static boolean consumeAmmoFromAccessiblePouches(Player player, Item ammoItem) {
        return withAccessiblePouch(player, pouchStack -> consumeOne(pouchStack, ammoItem));
    }

    public static boolean storeInAccessiblePouches(Player player, ItemStack stack) {
        if (stack.isEmpty() || !accepts(stack)) {
            return false;
        }

        var inserted = storeInPouches(getEquippedPouches(player), stack);
        if (!stack.isEmpty()) {
            inserted += storeInPouches(getInventoryPouches(player), stack);
        }
        return inserted > 0;
    }

    public static int storeInEquippedPouches(Player player, ItemStack stack) {
        if (stack.isEmpty() || !accepts(stack)) {
            return 0;
        }

        return storeInPouches(getEquippedPouches(player), stack);
    }

    public static int getStoredItemCount(ItemStack pouchStack) {
        var total = 0;
        for (var entry : readContents(pouchStack)) {
            total += entry.count;
        }
        return total;
    }

    public static boolean accepts(ItemStack stack) {
        return !stack.isEmpty() && stack.is(TagRegistry.Items.SPELLCASTER_AMMO_POUCH_STORABLE);
    }

    private static boolean containsItem(ItemStack pouchStack, Item item) {
        for (var entry : readContents(pouchStack)) {
            if (entry.count > 0 && entry.displayStack.is(item)) {
                return true;
            }
        }
        return false;
    }

    private static boolean consumeOne(ItemStack pouchStack, Item item) {
        var contents = readContents(pouchStack);
        for (int i = 0; i < contents.size(); ++i) {
            var entry = contents.get(i);
            if (!entry.displayStack.is(item) || entry.count <= 0) {
                continue;
            }

            if (entry.count == 1) {
                contents.remove(i);
            } else {
                entry.count -= 1;
            }
            writeContents(pouchStack, contents);
            return true;
        }
        return false;
    }

    private static int countItem(ItemStack pouchStack, Item item) {
        var total = 0;
        for (var entry : readContents(pouchStack)) {
            if (entry.count > 0 && entry.displayStack.is(item)) {
                total += entry.count;
            }
        }
        return total;
    }

    private static int addToPouch(ItemStack pouchStack, ItemStack stack) {
        if (!accepts(stack)) {
            return 0;
        }

        var remainingSpace = MAX_STORED_ITEMS - getStoredItemCount(pouchStack);
        var inserted = Math.min(remainingSpace, stack.getCount());
        if (inserted <= 0) {
            return 0;
        }

        var contents = readContents(pouchStack);
        for (var entry : contents) {
            if (!ItemStack.isSameItemSameComponents(entry.displayStack, stack)) {
                continue;
            }

            entry.count += inserted;
            writeContents(pouchStack, contents);
            return inserted;
        }

        // バニラの ItemStack Count では 64 超え表示が扱いづらいため、実データは個数を別管理する。
        contents.add(new StoredEntry(stack.copyWithCount(1), inserted));
        writeContents(pouchStack, contents);
        return inserted;
    }

    private static ItemStack removeStackFromPouch(ItemStack pouchStack) {
        var contents = readContents(pouchStack);
        var candidateIndex = findRemovalCandidateSourceIndex(contents);
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
        writeContents(pouchStack, contents);
        return removedStack;
    }

    private static List<ItemStack> getEquippedPouches(Player player) {
        return CuriosApi.getCuriosInventory(player)
                .map(inventory -> inventory.findCurios(ItemRegistry.SPELLCASTER_AMMO_POUCH.get()).stream()
                        .map(slotResult -> slotResult.stack())
                        .toList())
                .orElse(List.of());
    }

    private static List<ItemStack> getInventoryPouches(Player player) {
        var pouches = new ArrayList<ItemStack>();
        appendInventoryPouches(pouches, player.getInventory().items);
        appendInventoryPouches(pouches, player.getInventory().offhand);
        return pouches;
    }

    private static void appendInventoryPouches(List<ItemStack> destination, List<ItemStack> source) {
        for (var stack : source) {
            if (stack.is(ItemRegistry.SPELLCASTER_AMMO_POUCH.get())) {
                destination.add(stack);
            }
        }
    }

    private static boolean withAccessiblePouch(Player player, java.util.function.Predicate<ItemStack> action) {
        for (var pouchStack : getEquippedPouches(player)) {
            if (action.test(pouchStack)) {
                return true;
            }
        }
        for (var pouchStack : getInventoryPouches(player)) {
            if (action.test(pouchStack)) {
                return true;
            }
        }
        return false;
    }

    private static int storeInPouches(List<ItemStack> pouchStacks, ItemStack stack) {
        var beforeCount = stack.getCount();
        for (var pouchStack : pouchStacks) {
            stack.shrink(addToPouch(pouchStack, stack));
            if (stack.isEmpty()) {
                break;
            }
        }
        return beforeCount - stack.getCount();
    }

    private static List<StoredEntry> readContents(ItemStack pouchStack) {
        var customData = pouchStack.get(DataComponents.CUSTOM_DATA);
        if (customData == null) {
            return new ArrayList<>();
        }

        var rootTag = customData.copyTag();
        if (!rootTag.contains(STORAGE_TAG, Tag.TAG_COMPOUND)) {
            return new ArrayList<>();
        }

        var storageTag = rootTag.getCompound(STORAGE_TAG);
        if (storageTag == null || !storageTag.contains(CONTENTS_TAG, Tag.TAG_LIST)) {
            return new ArrayList<>();
        }

        var contentsTag = storageTag.getList(CONTENTS_TAG, Tag.TAG_COMPOUND);
        var contents = new ArrayList<StoredEntry>(contentsTag.size());
        for (var entryTag : contentsTag) {
            if (!(entryTag instanceof CompoundTag compoundTag)) {
                continue;
            }

            var itemId = compoundTag.getString(ITEM_ID_TAG);
            var itemKey = ResourceLocation.tryParse(itemId);
            if (itemKey == null) {
                continue;
            }

            var item = BuiltInRegistries.ITEM.getOptional(itemKey).orElse(null);
            var storedCount = compoundTag.getInt(COUNT_TAG);
            if (item == null || item == Items.AIR || storedCount <= 0) {
                continue;
            }

            contents.add(new StoredEntry(new ItemStack(item), storedCount));
        }
        return contents;
    }

    private static void writeContents(ItemStack pouchStack, List<StoredEntry> contents) {
        if (contents.isEmpty()) {
            removeStorageTag(pouchStack);
            return;
        }

        var contentsTag = new ListTag();
        for (var entry : contents) {
            if (entry.displayStack.isEmpty() || entry.count <= 0) {
                continue;
            }

            var itemKey = BuiltInRegistries.ITEM.getKey(entry.displayStack.getItem());
            if (itemKey == null || entry.displayStack.is(Items.AIR)) {
                continue;
            }

            var compoundTag = new CompoundTag();
            compoundTag.putString(ITEM_ID_TAG, itemKey.toString());
            compoundTag.putInt(COUNT_TAG, entry.count);
            contentsTag.add(compoundTag);
        }

        if (contentsTag.isEmpty()) {
            removeStorageTag(pouchStack);
            return;
        }

        CustomData.update(DataComponents.CUSTOM_DATA, pouchStack, tag -> {
            var storageTag = new CompoundTag();
            storageTag.put(CONTENTS_TAG, contentsTag);
            tag.put(STORAGE_TAG, storageTag);
        });
    }

    private static void removeStorageTag(ItemStack pouchStack) {
        CustomData.update(DataComponents.CUSTOM_DATA, pouchStack, tag -> tag.remove(STORAGE_TAG));
    }

    private static List<StoredEntry> getDisplayEntries(List<StoredEntry> contents) {
        var displayEntries = new ArrayList<StoredEntry>(contents.size());
        appendDisplayEntries(displayEntries, contents, false);
        appendDisplayEntries(displayEntries, contents, true);
        return displayEntries;
    }

    private static void appendDisplayEntries(List<StoredEntry> destination, List<StoredEntry> source, boolean emptyCasing) {
        for (var entry : source) {
            if (isEmptyCasing(entry.displayStack) == emptyCasing) {
                destination.add(entry);
            }
        }
    }

    private static int findRemovalCandidateDisplayIndex(List<StoredEntry> displayEntries) {
        var highlightedEntry = displayEntries.isEmpty() ? null : findRemovalCandidateEntry(displayEntries);
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

    private static int findRemovalCandidateSourceIndex(List<StoredEntry> contents) {
        var candidate = findRemovalCandidateEntry(contents);
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
    private static StoredEntry findRemovalCandidateEntry(List<StoredEntry> contents) {
        StoredEntry bestEntry = null;
        for (var entry : contents) {
            if (entry.displayStack.isEmpty() || entry.count <= 0) {
                continue;
            }

            if (bestEntry == null || isHigherRemovalPriority(entry, bestEntry)) {
                bestEntry = entry;
            }
        }
        return bestEntry;
    }

    private static boolean isHigherRemovalPriority(StoredEntry candidate, StoredEntry currentBest) {
        var candidateIsCasing = isEmptyCasing(candidate.displayStack);
        var currentIsCasing = isEmptyCasing(currentBest.displayStack);
        if (candidateIsCasing != currentIsCasing) {
            return candidateIsCasing;
        }

        if (candidate.count != currentBest.count) {
            return candidate.count < currentBest.count;
        }

        return false;
    }

    private static boolean isEmptyCasing(ItemStack stack) {
        return stack.is(TagRegistry.Items.SPELLCASTER_EMPTY_CASINGS);
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
