package jp.aquafactory.apprenticecodex.item.curios.satellitefollowcastamulet;

import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastType;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import io.redspace.ironsspellbooks.api.spells.SpellData;
import io.redspace.ironsspellbooks.compat.Curios;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.compat.jei.IJeiInfoItem;
import jp.aquafactory.apprenticecodex.item.ArcaneAnvilImbueBlockItem;
import jp.aquafactory.apprenticecodex.item.ImbueTooltipHelper;
import jp.aquafactory.apprenticecodex.remoteownercast.RemoteOwnerCastOrigin;
import jp.aquafactory.apprenticecodex.remoteownercast.RemoteOwnerCastRules;
import jp.aquafactory.apprenticecodex.registry.TagRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

import java.util.ArrayList;
import java.util.List;

public class SatelliteFollowcastAmulet extends Item implements ICurioItem, IJeiInfoItem, ArcaneAnvilImbueBlockItem {
    public static final int MIN_SPELL_SLOTS = 1;
    public static final int CALIBRATION_ADJUSTMENT_SLOT_COUNT = 3;
    public static final int MAX_SPELL_SLOTS = MIN_SPELL_SLOTS + CALIBRATION_ADJUSTMENT_SLOT_COUNT;
    public static final double CRYSTAL_ORBIT_RADIUS = 1.35D;
    public static final double CRYSTAL_ORBIT_HEIGHT = 1.05D;
    public static final double CRYSTAL_ORBIT_SPEED = Math.PI / 60.0D;
    public static final double CRYSTAL_FLOAT_SPEED = Math.PI / 24.0D;
    public static final double CRYSTAL_FLOAT_RANGE = 0.12D;

    private static final String JEI_INFO_KEY_PREFIX = "jei.apprenticecodex.satellite_followcast_amulet.desc_";
    private static final String CALIBRATION_TAG = "SpellCalibration";
    private static final String ADJUSTMENTS_TAG = "Adjustments";
    private static final String SCROLLS_TAG = "Scrolls";
    private static final String SLOT_TAG = "Slot";
    private static final String ITEM_TAG = "Item";
    private static final String ITEM_ID_TAG = "ItemId";
    private static final String SPELL_ID_TAG = "Spell";
    private static final String SPELL_LEVEL_TAG = "Level";
    private static final String NEXT_SEARCH_INDEX_TAG = ApprenticeCodex.MODID + ":satellite_followcast_next_search_index";

    private final String slotIdentifier;

    public SatelliteFollowcastAmulet() {
        super(new Properties().stacksTo(1).rarity(Rarity.UNCOMMON));
        this.slotIdentifier = Curios.NECKLACE_SLOT;
    }

    @Override
    public @NotNull ItemStack getDefaultInstance() {
        var stack = super.getDefaultInstance();
        initializeSpellContainer(stack);
        return stack;
    }

    @Override
    public void onCraftedBy(@NotNull ItemStack stack, @NotNull Level level, @NotNull Player player) {
        super.onCraftedBy(stack, level, player);
        initializeSpellContainer(stack);
    }

    @Override
    public void inventoryTick(@NotNull ItemStack stack, @NotNull Level level, @NotNull Entity entity, int slotId, boolean isSelected) {
        super.inventoryTick(stack, level, entity, slotId, isSelected);
        if (!level.isClientSide) {
            initializeSpellContainer(stack);
        }
    }

    @Override
    public boolean canEquipFromUse(SlotContext slotContext, ItemStack stack) {
        return true;
    }

    @Override
    public List<Component> getSlotsTooltip(List<Component> tooltips, Item.@NotNull TooltipContext context, ItemStack stack) {
        var result = new ArrayList<>(tooltips);
        result.add(Component.empty());
        result.add(Component.translatable("curios.modifiers." + slotIdentifier).withStyle(ChatFormatting.GOLD));
        result.add(Component.literal(" ")
                .append(Component.translatable(getDescriptionId() + ".desc_1"))
                .withStyle(Style.EMPTY.withColor(ChatFormatting.YELLOW)));
        result.add(Component.literal(" ")
                .append(Component.translatable(getDescriptionId() + ".desc_2"))
                .withStyle(Style.EMPTY.withColor(ChatFormatting.YELLOW)));
        appendFollowcastTooltip(stack, result);
        return result;
    }

    @Override
    public String getJeiInfoTranslationKeyPrefix() {
        return JEI_INFO_KEY_PREFIX;
    }

    public boolean canImbueSpell(SpellData spellData) {
        return spellData != SpellData.EMPTY && canImbueSpell(spellData.getSpell(), spellData.getLevel());
    }

    public boolean canImbueSpell(@Nullable AbstractSpell spell, int spellLevel) {
        return RemoteOwnerCastRules.checkImbue(spell, spellLevel, RemoteOwnerCastOrigin.SATELLITE_FOLLOWCAST)
                .isAllowed();
    }

    public List<Component> getImbueRestrictionTooltipLines() {
        return getImbueRestrictionTooltipLines(ItemStack.EMPTY);
    }

    public List<Component> getImbueRestrictionTooltipLines(ItemStack stack) {
        return collectFollowcastRestrictTooltipSection(stack);
    }

    public void initializeSpellContainer(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return;
        }
        convertLegacySpellContainer(stack);
    }

    public static int clampSpellSlotCount(int spellSlotCount) {
        return Math.max(MIN_SPELL_SLOTS, Math.min(MAX_SPELL_SLOTS, spellSlotCount));
    }

    public static int getStoredSpellSlotCount() {
        return MIN_SPELL_SLOTS + CALIBRATION_ADJUSTMENT_SLOT_COUNT;
    }

    public static @NotNull ItemStack getCalibrationAdjustment(@NotNull ItemStack amuletStack, int slot) {
        return getCalibrationItem(amuletStack, ADJUSTMENTS_TAG, slot, CALIBRATION_ADJUSTMENT_SLOT_COUNT);
    }

    public static void setCalibrationAdjustment(@NotNull ItemStack amuletStack, int slot, @NotNull ItemStack stack) {
        setCalibrationItem(amuletStack, ADJUSTMENTS_TAG, slot, CALIBRATION_ADJUSTMENT_SLOT_COUNT, stack);
    }

    public static @NotNull ItemStack getCalibrationScroll(@NotNull ItemStack amuletStack, int slot) {
        return getCalibrationItem(amuletStack, SCROLLS_TAG, slot, getStoredSpellSlotCount());
    }

    public static void setCalibrationScroll(@NotNull ItemStack amuletStack, int slot, @NotNull ItemStack stack) {
        setCalibrationItem(amuletStack, SCROLLS_TAG, slot, getStoredSpellSlotCount(), stack);
    }

    public static int getEnabledSpellSlotCount(@NotNull ItemStack amuletStack) {
        if (!isValidCalibrationAccess(amuletStack, 0)) {
            return 0;
        }

        var upgradeCount = 0;
        for (var slot = 0; slot < CALIBRATION_ADJUSTMENT_SLOT_COUNT; ++slot) {
            if (isCalibrationSlotUpgrade(getCalibrationAdjustment(amuletStack, slot))) {
                ++upgradeCount;
            }
        }
        return clampSpellSlotCount(MIN_SPELL_SLOTS + upgradeCount);
    }

    public static boolean isCalibrationAdjustmentItem(@NotNull ItemStack stack) {
        return isCalibrationSlotUpgrade(stack) || isSilverRing(stack);
    }

    public static boolean isCalibrationSlotUpgrade(@NotNull ItemStack stack) {
        return !stack.isEmpty() && stack.is(TagRegistry.Items.SCROLLCASTER_GAUNTLET_SLOT_UPGRADES);
    }

    public static boolean isSilverRing(@NotNull ItemStack stack) {
        return !stack.isEmpty()
                && stack.getItem() == io.redspace.ironsspellbooks.registries.ItemRegistry.SILVER_RING.get();
    }

    public static boolean hasSilverRingAdjustment(@NotNull ItemStack amuletStack) {
        if (!isValidCalibrationAccess(amuletStack, 0)) {
            return false;
        }

        for (var slot = 0; slot < CALIBRATION_ADJUSTMENT_SLOT_COUNT; ++slot) {
            if (isSilverRing(getCalibrationAdjustment(amuletStack, slot))) {
                return true;
            }
        }
        return false;
    }

    public static boolean isCastableConfiguredSpell(@NotNull ItemStack amuletStack, @NotNull SpellData spellData) {
        if (spellData == SpellData.EMPTY || spellData.getSpell() == null) {
            return false;
        }
        return ((SatelliteFollowcastAmulet) amuletStack.getItem()).canFollowcastSpell(amuletStack, spellData);
    }

    public boolean canFollowcastSpell(@NotNull ItemStack stack, @NotNull SpellData spellData) {
        return spellData != SpellData.EMPTY
                && spellData.getSpell() != null
                && canImbueSpell(spellData)
                && getSupportedCastTypes(stack).contains(spellData.getSpell().getCastType());
    }

    public static boolean isEnabledSpellSlot(@NotNull ItemStack amuletStack, int slot) {
        return isValidStoredSpellAccess(amuletStack, slot) && slot < getEnabledSpellSlotCount(amuletStack);
    }

    public static boolean isMismatchedCastConditionAt(@NotNull ItemStack amuletStack, int slot) {
        if (!isValidStoredSpellAccess(amuletStack, slot)) {
            return false;
        }

        var spellData = getSpellDataAt(amuletStack, slot);
        return spellData != SpellData.EMPTY
                && spellData.getSpell() != null
                && !isCastableConfiguredSpell(amuletStack, spellData);
    }

    public static List<SpellData> getImbuedSpells(ItemStack stack) {
        var spells = new ArrayList<SpellData>();
        if (!isValidStoredSpellAccess(stack, 0)) {
            return spells;
        }

        for (var index = 0; index < getStoredSpellSlotCount(); ++index) {
            var spellData = getSpellDataAt(stack, index);
            if (spellData == SpellData.EMPTY || spellData.getSpell() == null) {
                continue;
            }

            spells.add(spellData);
        }
        return spells;
    }

    public static SpellData getSpellAtIndex(ItemStack stack, int slotIndex) {
        return getSpellDataAt(stack, slotIndex);
    }

    public static @NotNull SpellData getSpellDataAt(@NotNull ItemStack amuletStack, int slot) {
        if (!isValidStoredSpellAccess(amuletStack, slot)) {
            return SpellData.EMPTY;
        }

        return getScrollSpellData(getCalibrationScroll(amuletStack, slot));
    }

    public static int getMaxSpellSlots(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return MIN_SPELL_SLOTS;
        }

        return getEnabledSpellSlotCount(stack);
    }

    public static int advanceAndGetSearchStartIndex(ItemStack stack, int maxSpellSlots) {
        var clampedSlots = clampSpellSlotCount(maxSpellSlots);
        var customData = stack.get(DataComponents.CUSTOM_DATA);
        var tag = customData == null ? null : customData.copyTag();
        var current = tag != null && tag.contains(NEXT_SEARCH_INDEX_TAG) ? tag.getInt(NEXT_SEARCH_INDEX_TAG) : -1;
        var next = Math.floorMod(current + 1, clampedSlots);
        CustomData.update(DataComponents.CUSTOM_DATA, stack, nextTag -> nextTag.putInt(NEXT_SEARCH_INDEX_TAG, next));
        return next;
    }

    public static Vec3 getCrystalOffset(LivingEntity owner, int slotIndex, int maxSpellSlots, float partialTick) {
        var clampedSlots = Math.max(1, clampSpellSlotCount(maxSpellSlots));
        var time = owner.tickCount + partialTick;
        var angle = (Math.PI * 2.0D * slotIndex / clampedSlots) + time * CRYSTAL_ORBIT_SPEED;
        var floatOffset = Math.sin(time * CRYSTAL_FLOAT_SPEED + slotIndex * 0.7D) * CRYSTAL_FLOAT_RANGE;
        var height = owner.getBbHeight() * 0.6D + CRYSTAL_ORBIT_HEIGHT + floatOffset;
        return new Vec3(Math.cos(angle) * CRYSTAL_ORBIT_RADIUS, height, Math.sin(angle) * CRYSTAL_ORBIT_RADIUS);
    }

    public static Vec3 getCrystalPosition(LivingEntity owner, int slotIndex, int maxSpellSlots, float partialTick) {
        return owner.position().add(getCrystalOffset(owner, slotIndex, maxSpellSlots, partialTick));
    }

    private static List<CastType> getSupportedCastTypes(@NotNull ItemStack stack) {
        return hasSilverRingAdjustment(stack)
                ? List.of(CastType.INSTANT, CastType.LONG, CastType.CONTINUOUS)
                : List.of(CastType.INSTANT);
    }

    private static void convertLegacySpellContainer(@NotNull ItemStack amuletStack) {
        var spellContainer = ISpellContainer.get(amuletStack);
        if (spellContainer == null) {
            return;
        }

        var legacyExtraSlots = clampSpellSlotCount(spellContainer.getMaxSpellCount()) - MIN_SPELL_SLOTS;
        repairLegacySlotUpgradeAdjustments(amuletStack, legacyExtraSlots);
        for (var slot = 0; slot < spellContainer.getMaxSpellCount() && slot < getStoredSpellSlotCount(); ++slot) {
            var spellData = spellContainer.getSpellAtIndex(slot);
            if (spellData == SpellData.EMPTY
                    || spellData.getSpell() == null
                    || !((SatelliteFollowcastAmulet) amuletStack.getItem()).canImbueSpell(spellData)) {
                continue;
            }
            setCalibrationScroll(amuletStack, slot, createScroll(spellData));
        }
        ISpellContainer.remove(amuletStack);
    }

    private static void repairLegacySlotUpgradeAdjustments(@NotNull ItemStack amuletStack, int legacyExtraSlots) {
        if (legacyExtraSlots <= 0) {
            return;
        }

        var existingUpgradeCount = 0;
        for (var slot = 0; slot < CALIBRATION_ADJUSTMENT_SLOT_COUNT; ++slot) {
            if (isCalibrationSlotUpgrade(getCalibrationAdjustment(amuletStack, slot))) {
                ++existingUpgradeCount;
            }
        }

        var missingUpgradeCount = Math.min(CALIBRATION_ADJUSTMENT_SLOT_COUNT, legacyExtraSlots) - existingUpgradeCount;
        for (var slot = 0; slot < CALIBRATION_ADJUSTMENT_SLOT_COUNT && missingUpgradeCount > 0; ++slot) {
            if (!getCalibrationAdjustment(amuletStack, slot).isEmpty()) {
                continue;
            }
            setCalibrationItem(
                    amuletStack,
                    ADJUSTMENTS_TAG,
                    slot,
                    CALIBRATION_ADJUSTMENT_SLOT_COUNT,
                    new ItemStack(io.redspace.ironsspellbooks.registries.ItemRegistry.LESSER_SPELL_SLOT_UPGRADE.get())
            );
            --missingUpgradeCount;
        }
    }

    private static @Nullable CompoundTag getCustomDataTag(ItemStack stack) {
        var customData = stack.get(DataComponents.CUSTOM_DATA);
        return customData == null ? null : customData.copyTag();
    }

    private static @NotNull ItemStack getCalibrationItem(@NotNull ItemStack amuletStack, String listName, int slot, int slotCount) {
        if (!isValidCalibrationAccess(amuletStack, slot, slotCount)) {
            return ItemStack.EMPTY;
        }

        var rootTag = getCustomDataTag(amuletStack);
        if (rootTag == null || !rootTag.contains(CALIBRATION_TAG, Tag.TAG_COMPOUND)) {
            return ItemStack.EMPTY;
        }

        var calibrationTag = rootTag.getCompound(CALIBRATION_TAG);
        if (!calibrationTag.contains(listName, Tag.TAG_LIST)) {
            return ItemStack.EMPTY;
        }

        var list = calibrationTag.getList(listName, Tag.TAG_COMPOUND);
        for (var index = 0; index < list.size(); ++index) {
            var entry = list.getCompound(index);
            if (entry.getInt(SLOT_TAG) != slot || !entry.contains(ITEM_TAG, Tag.TAG_COMPOUND)) {
                continue;
            }
            return decodeStoredCalibrationItem(entry.getCompound(ITEM_TAG));
        }
        return ItemStack.EMPTY;
    }

    private static void setCalibrationItem(@NotNull ItemStack amuletStack, String listName, int slot, int slotCount,
                                           @NotNull ItemStack stack) {
        if (!isValidCalibrationAccess(amuletStack, slot, slotCount)) {
            return;
        }

        CustomData.update(DataComponents.CUSTOM_DATA, amuletStack, rootTag -> {
            var calibrationTag = rootTag.contains(CALIBRATION_TAG, Tag.TAG_COMPOUND)
                    ? rootTag.getCompound(CALIBRATION_TAG)
                    : new CompoundTag();
            var list = calibrationTag.contains(listName, Tag.TAG_LIST)
                    ? calibrationTag.getList(listName, Tag.TAG_COMPOUND)
                    : new ListTag();
            removeCalibrationItem(list, slot);

            if (!stack.isEmpty()) {
                var storedTag = encodeCalibrationItem(stack);
                if (!storedTag.isEmpty()) {
                    var entry = new CompoundTag();
                    entry.putInt(SLOT_TAG, slot);
                    entry.put(ITEM_TAG, storedTag);
                    list.add(entry);
                }
            }

            if (list.isEmpty()) {
                calibrationTag.remove(listName);
            } else {
                calibrationTag.put(listName, list);
            }

            if (calibrationTag.isEmpty()) {
                rootTag.remove(CALIBRATION_TAG);
            } else {
                rootTag.put(CALIBRATION_TAG, calibrationTag);
            }
        });
    }

    private static @NotNull CompoundTag encodeCalibrationItem(@NotNull ItemStack stack) {
        var tag = new CompoundTag();
        var spellData = getScrollSpellData(stack);
        if (spellData != SpellData.EMPTY && spellData.getSpell() != null) {
            tag.putString(SPELL_ID_TAG, spellData.getSpell().getSpellResource().toString());
            tag.putInt(SPELL_LEVEL_TAG, spellData.getLevel());
            return tag;
        }

        var itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (itemId != null) {
            tag.putString(ITEM_ID_TAG, itemId.toString());
        }
        return tag;
    }

    private static @NotNull ItemStack decodeStoredCalibrationItem(@NotNull CompoundTag tag) {
        if (tag.contains(SPELL_ID_TAG, Tag.TAG_STRING)) {
            var spell = io.redspace.ironsspellbooks.api.registry.SpellRegistry.getSpell(
                    ResourceLocation.parse(tag.getString(SPELL_ID_TAG))
            );
            if (spell == null || spell == io.redspace.ironsspellbooks.api.registry.SpellRegistry.none()) {
                return ItemStack.EMPTY;
            }
            return createScroll(new SpellData(spell, Math.max(1, tag.getInt(SPELL_LEVEL_TAG))));
        }

        if (tag.contains(ITEM_ID_TAG, Tag.TAG_STRING)) {
            var item = BuiltInRegistries.ITEM.get(ResourceLocation.parse(tag.getString(ITEM_ID_TAG)));
            if (item == null) {
                return ItemStack.EMPTY;
            }
            return new ItemStack(item);
        }
        return ItemStack.EMPTY;
    }

    private static void removeCalibrationItem(ListTag list, int slot) {
        for (var index = list.size() - 1; index >= 0; --index) {
            if (list.getCompound(index).getInt(SLOT_TAG) == slot) {
                list.remove(index);
            }
        }
    }

    private static boolean isValidCalibrationAccess(@NotNull ItemStack amuletStack, int slot) {
        return isValidCalibrationAccess(amuletStack, slot, CALIBRATION_ADJUSTMENT_SLOT_COUNT);
    }

    private static boolean isValidCalibrationAccess(@NotNull ItemStack amuletStack, int slot, int slotCount) {
        return !amuletStack.isEmpty()
                && amuletStack.getItem() instanceof SatelliteFollowcastAmulet
                && slot >= 0
                && slot < slotCount;
    }

    private static boolean isValidStoredSpellAccess(@NotNull ItemStack amuletStack, int slot) {
        return !amuletStack.isEmpty()
                && amuletStack.getItem() instanceof SatelliteFollowcastAmulet
                && slot >= 0
                && slot < getStoredSpellSlotCount();
    }

    private static @NotNull ItemStack createScroll(@NotNull SpellData spellData) {
        if (spellData == SpellData.EMPTY || spellData.getSpell() == null) {
            return ItemStack.EMPTY;
        }

        var scrollStack = new ItemStack(io.redspace.ironsspellbooks.registries.ItemRegistry.SCROLL.get());
        ISpellContainer.createScrollContainer(spellData.getSpell(), spellData.getLevel(), scrollStack);
        return scrollStack;
    }

    private static @NotNull SpellData getScrollSpellData(@NotNull ItemStack scrollStack) {
        if (scrollStack.isEmpty()) {
            return SpellData.EMPTY;
        }

        var scrollContainer = ISpellContainer.get(scrollStack);
        if (scrollContainer == null || scrollContainer.getActiveSpellCount() != 1) {
            return SpellData.EMPTY;
        }

        var spellData = scrollContainer.getSpellAtIndex(0);
        //noinspection ConstantValue
        return spellData == null ? SpellData.EMPTY : spellData;
    }

    private static void appendFollowcastTooltip(ItemStack stack, List<Component> lines) {
        ImbueTooltipHelper.appendBlankLineIfNeeded(lines);
        if (ImbueTooltipHelper.appendHintIfDetailsHidden(lines)) {
            ImbueTooltipHelper.appendBlankLineIfNeeded(lines);
            appendFollowcastSpellStatusTooltip(stack, lines);
            return;
        }

        ImbueTooltipHelper.appendTooltipSection(
                lines,
                collectFollowcastAbilityTooltipSection(stack),
                "item." + ApprenticeCodex.MODID + ".spellgun.tooltip.ability_title",
                "item." + ApprenticeCodex.MODID + ".spellgun.tooltip.ability_none"
        );
        ImbueTooltipHelper.appendTooltipSection(
                lines,
                collectFollowcastRestrictTooltipSection(stack),
                "item." + ApprenticeCodex.MODID + ".spellgun.tooltip.restrict_title",
                "item." + ApprenticeCodex.MODID + ".spellgun.tooltip.restrict_none"
        );

        ImbueTooltipHelper.appendBlankLineIfNeeded(lines);
        appendFollowcastSpellStatusTooltip(stack, lines);
    }

    private static List<Component> collectFollowcastAbilityTooltipSection(ItemStack stack) {
        var translatedLines = new ArrayList<Component>();
        if (!hasSilverRingAdjustment(stack)) {
            return translatedLines;
        }

        translatedLines.add(ImbueTooltipHelper.translatableGray(
                "item." + ApprenticeCodex.MODID + ".spellgun.tooltip.ability_long_to_instant"
        ));
        translatedLines.add(ImbueTooltipHelper.translatableGray(
                "item." + ApprenticeCodex.MODID + ".spellgun.tooltip.ability_extend_cooldown"
        ));
        return translatedLines;
    }

    private static List<Component> collectFollowcastRestrictTooltipSection(ItemStack stack) {
        var translatedLines = new ArrayList<Component>();
        if (stack.isEmpty() || !hasSilverRingAdjustment(stack)) {
            translatedLines.add(ImbueTooltipHelper.translatableGray(
                    "item." + ApprenticeCodex.MODID + ".spellgun.tooltip.restrict_restrict_instant_only"
            ));
        }
        translatedLines.add(ImbueTooltipHelper.translatableGray(
                "item." + ApprenticeCodex.MODID + ".spellgun.tooltip.restrict_restrict_no_recast"
        ));
        translatedLines.add(ImbueTooltipHelper.translatableGray(
                "item." + ApprenticeCodex.MODID + ".spellgun.tooltip.restrict_restrict_by_profile"
        ));
        return translatedLines;
    }

    private static void appendFollowcastSpellStatusTooltip(ItemStack stack, List<Component> lines) {
        for (var index = 0; index < getStoredSpellSlotCount(); ++index) {
            if (!isEnabledSpellSlot(stack, index)) {
                continue;
            }

            var spellData = getSpellDataAt(stack, index);
            if (spellData == SpellData.EMPTY || spellData.getSpell() == null) {
                continue;
            }

            var spell = spellData.getSpell();
            var spellName = spell.getDisplayName(null);
            var status = isCastableConfiguredSpell(stack, spellData)
                    ? Component.translatable("item.apprenticecodex.common.imbue_tooltip.ready").withStyle(ChatFormatting.AQUA)
                    : Component.translatable("item.apprenticecodex.common.imbue_tooltip.invalid").withStyle(ChatFormatting.RED);

            lines.add(Component.translatable(
                    "item.apprenticecodex.common.imbue_tooltip.spell_line",
                    spellName,
                    status
            ));
        }
    }
}
