package jp.aquafactory.apprenticecodex.item.curios.quickcastscrollcartridge;

import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import jp.aquafactory.apprenticecodex.item.*;
import io.redspace.ironsspellbooks.api.spells.*;
import io.redspace.ironsspellbooks.item.Scroll;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.*;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.jetbrains.annotations.NotNull;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.*;
import software.bernie.geckolib.util.GeckoLibUtil;
import java.util.Optional;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.*;
import org.jetbrains.annotations.Nullable;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;
import top.theillusivec4.curios.api.type.capability.ICuriosItemHandler;

import java.util.ArrayList;
import java.util.List;

public class QuickcastScrollCartridge extends Item implements ICurioItem, GeoItem,
        StoredSpellCalibrationImbueTarget, SpellCalibrationAdjustmentTarget,
        ImmediateSneakSelectionUiItem, ArcaneAnvilScrollImbueBlockItem {
    private static final String CALIBRATION_TAG = "QuickcastCartridgeCalibration";
    private static final String SCROLLS_TAG = "Scrolls";
    private static final String SLOT_TAG = "Slot";
    private static final String ITEM_TAG = "Item";
    private static final String SELECTED_SCROLL_INDEX_TAG = "SelectedScrollIndex";
    private static final HolderLookup.Provider FALLBACK_SERIALIZATION_LOOKUP =
            RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
    private static final CalibrationAdjustmentProfile PROFILE = CalibrationAdjustmentProfile.of(
            CalibrationAdjustmentRule.repeatable("scroll_slot",
                    stack -> stack.is(ItemRegistry.SCROLLWOVEN_PARCHMENT.get()),
                    CalibrationAdjustmentHint.specificItem(ItemRegistry.SCROLLWOVEN_PARCHMENT))
                    .withEffectLines(CalibrationAdjustmentEffects.addScrollSlot(1)));
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    public QuickcastScrollCartridge() {
        super(new Properties().stacksTo(1).rarity(Rarity.RARE).fireResistant());
        GeoItem.registerSyncedAnimatable(this);
    }

    @Override
    public List<Component> getSlotsTooltip(List<Component> tooltips, TooltipContext context, ItemStack stack) {
        var result = new ArrayList<>(tooltips);
        result.add(Component.empty());
        // item.modifiers.anyは1.20.1にはないため、1.21.1でもオリジナルのキーを定義して使う.
        result.add(Component.translatable("curios.apprenticecodex.modifier.for_quiver").withStyle(ChatFormatting.GOLD));

        // キー設定はクライアントだけで参照し、専用サーバーでは未割り当ての説明を使う。
        var keyDescription = net.neoforged.fml.loading.FMLEnvironment.dist == net.neoforged.api.distmarker.Dist.CLIENT
                ? jp.aquafactory.apprenticecodex.event.client.QuickcastCartridgeClientEvents.getCastKeyDescription()
                : Component.translatable(getDescriptionId() + ".no_assign");
        result.add(keyDescription.copy().withStyle(ChatFormatting.YELLOW));
        for (int i = 2; i <= 3; i++) {
            result.add(Component.translatable(getDescriptionId() + ".desc_" + i).withStyle(ChatFormatting.YELLOW));
        }
        if (getEnabledCalibrationScrollSlotCount(stack) >= 2) {
            result.add(Component.translatable(getDescriptionId() + ".additive_slot").withStyle(ChatFormatting.YELLOW));
        }

        return result;
    }

    @Override
    public boolean canEquip(SlotContext slotContext, ItemStack stack) {
        if (slotContext.entity() == null) {
            return true;
        }

        return CuriosApi.getCuriosInventory(slotContext.entity())
                .map(inventory -> hasNoOtherEquippedCartridge(inventory, slotContext))
                .orElse(true);
    }

    @Override
    public boolean canEquipFromUse(SlotContext slotContext, ItemStack stack) {
        return true;
    }

    private static boolean hasNoOtherEquippedCartridge(ICuriosItemHandler inventory, SlotContext targetSlot) {
        // 枠の種類・数・有効状態に依存せず、Curiosが保持する実装備領域全体で一つだけに制限する.
        // コスメティックスロットは考慮しない.
        for (var entry : inventory.getCurios().entrySet()) {
            var stacks = entry.getValue().getStacks();
            for (var index = 0; index < stacks.getSlots(); index++) {
                if (entry.getKey().equals(targetSlot.identifier()) && index == targetSlot.index()) {
                    continue;
                }
                if (stacks.getStackInSlot(index).is(ItemRegistry.QUICKCAST_SCROLL_CARTRIDGE.get())) {
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public int getCalibrationAdjustmentSlotCount(@NotNull ItemStack stack) { return 3; }

    @Override
    public @NotNull CalibrationAdjustmentProfile getCalibrationAdjustmentProfile(@NotNull ItemStack stack) {
        return PROFILE;
    }

    @Override
    public void onCalibrationAdjustmentsChanged(@NotNull ItemStack stack, @NotNull HolderLookup.Provider lookup) {
        normalizeSelection(stack);
    }

    public static int getEnabledCalibrationScrollSlotCount(ItemStack stack) {
        int count = 1;
        for (int i = 0; i < 3; i++) {
            if (CalibrationAdjustmentStorage.get(stack, i, 3, serializationLookup())
                    .is(ItemRegistry.SCROLLWOVEN_PARCHMENT.get())) count++;
        }
        return count;
    }

    public static ItemStack getCalibrationScroll(ItemStack stack, int slot) {
        return getCalibrationItem(stack, SCROLLS_TAG, slot, 4, serializationLookup());
    }

    public static void setCalibrationScroll(ItemStack stack, int slot, ItemStack scroll) {
        setCalibrationItem(stack, SCROLLS_TAG, slot, 4, scroll, serializationLookup());
        normalizeSelection(stack);
    }

    private static SpellData readSpell(ItemStack stack, int slot) {
        var scroll = getCalibrationScroll(stack, slot);
        if (!(scroll.getItem() instanceof Scroll)) return SpellData.EMPTY;
        var container = ISpellContainer.get(scroll);
        if (container == null) return SpellData.EMPTY;
        var data = container.getSpellAtIndex(0);
        //noinspection ConstantValue
        return data == null ? SpellData.EMPTY : data;
    }

    public static int getSelectedScrollIndex(ItemStack stack) {
        var tag = getCalibrationTag(stack);
        int selected = tag != null && tag.contains(SELECTED_SCROLL_INDEX_TAG)
                ? tag.getInt(SELECTED_SCROLL_INDEX_TAG) : -1;
        if (isSelectable(stack, selected)) return selected;
        for (int i = 0; i < getEnabledCalibrationScrollSlotCount(stack); i++) {
            if (isSelectable(stack, i)) return i;
        }
        return -1;
    }

    public static SpellData getSelectedSpellData(ItemStack stack) {
        int selected = getSelectedScrollIndex(stack);
        return selected < 0 ? SpellData.EMPTY : readSpell(stack, selected);
    }

    private static boolean isSelectable(ItemStack stack, int index) {
        return index >= 0 && index < getEnabledCalibrationScrollSlotCount(stack)
                && readSpell(stack, index) != SpellData.EMPTY;
    }

    public static void normalizeSelection(ItemStack stack) {
        int selected = getSelectedScrollIndex(stack);
        updateCalibrationTag(stack, tag -> tag.putInt(SELECTED_SCROLL_INDEX_TAG, selected));
    }

    @Override
    public boolean hasAnyStoredCalibrationScroll(@NotNull ItemStack stack) {
        for (int i = 0; i < 4; i++) if (!getCalibrationScroll(stack, i).isEmpty()) return true;
        return false;
    }

    @Override
    public @NotNull SpellCalibrationImbueState evaluateCalibrationImbue(
            @NotNull ItemStack stack, int slot, @NotNull SpellData data) {
        return slot >= 0 && slot < getEnabledCalibrationScrollSlotCount(stack) && data != SpellData.EMPTY
                ? SpellCalibrationImbueState.ACCEPTED_USABLE : SpellCalibrationImbueState.REJECTED;
    }

    @Override
    public boolean isSneakSelectionUiEnabled(ItemStack stack) { return getEnabledCalibrationScrollSlotCount(stack) > 1; }

    @Override
    public List<SneakSelectionView> getSneakSelectionViews(ItemStack stack) {
        var result = new ArrayList<SneakSelectionView>();
        for (int i = 0; i < getEnabledCalibrationScrollSlotCount(stack); i++) {
            result.add(SneakSelectionView.forSpell(i, readSpell(stack, i), isSelectable(stack, i)));
        }
        return result;
    }

    @Override
    public int getSneakSelectionIndex(ItemStack stack) { return getSelectedScrollIndex(stack); }

    @Override
    public boolean isSneakSelectionIndexSelectable(ItemStack stack, int index) { return isSelectable(stack, index); }

    @Override
    public void setSneakSelectionIndex(ItemStack stack, int index) {
        if (isSelectable(stack, index)) updateCalibrationTag(stack, tag -> tag.putInt(SELECTED_SCROLL_INDEX_TAG, index));
    }

    @Override
    public @NotNull Optional<TooltipComponent> getTooltipImage(@NotNull ItemStack stack) {
        return createCalibrationAdjustmentTooltip(stack);
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @NotNull TooltipContext context,
                                @NotNull List<Component> lines, @NotNull TooltipFlag flag) {
        super.appendHoverText(stack, context, lines, flag);
        var spell = getSelectedSpellData(stack);
        if (spell != SpellData.EMPTY) lines.add(spell.getSpell().getDisplayName(null).copy().withStyle(ChatFormatting.AQUA));
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "cartridge", 0,
                state -> state.setAndContinue(RawAnimation.begin().thenLoop("idle")))
                .triggerableAnim("cast", RawAnimation.begin().thenPlay("cast")));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() { return cache; }

    public static boolean isEquippedBy(@Nullable LivingEntity entity) {
        if (entity == null) {
            return false;
        }

        return CuriosApi.getCuriosInventory(entity)
                .map(inventory -> inventory.isEquipped(ItemRegistry.QUICKCAST_SCROLL_CARTRIDGE.get()))
                .orElse(false);
    }
    private static @NotNull ItemStack getCalibrationItem(
            ItemStack owner, String listName, int slot, int slotCount,
            @NotNull HolderLookup.Provider lookupProvider
    ) {
        if (!isValidCalibrationAccess(owner, slot, slotCount)) {
            return ItemStack.EMPTY;
        }
        var calibration = getCalibrationTag(owner);
        if (calibration == null || !calibration.contains(listName, Tag.TAG_LIST)) {
            return ItemStack.EMPTY;
        }
        var list = calibration.getList(listName, Tag.TAG_COMPOUND);
        for (var index = 0; index < list.size(); ++index) {
            var entry = list.getCompound(index);
            if (entry.getInt(SLOT_TAG) == slot && entry.contains(ITEM_TAG, Tag.TAG_COMPOUND)) {
                return ItemStack.parseOptional(lookupProvider, entry.getCompound(ITEM_TAG));
            }
        }
        return ItemStack.EMPTY;
    }

    private static void setCalibrationItem(
            ItemStack owner, String listName, int slot, int slotCount, ItemStack item,
            @NotNull HolderLookup.Provider lookupProvider
    ) {
        if (!isValidCalibrationAccess(owner, slot, slotCount)) {
            return;
        }
        updateCalibrationTag(owner, calibration -> {
            var list = calibration.contains(listName, Tag.TAG_LIST)
                    ? calibration.getList(listName, Tag.TAG_COMPOUND) : new ListTag();
            for (var index = list.size() - 1; index >= 0; --index) {
                if (list.getCompound(index).getInt(SLOT_TAG) == slot) {
                    list.remove(index);
                }
            }
            if (!item.isEmpty()) {
                var stored = item.copyWithCount(1);
                var entry = new CompoundTag();
                entry.putInt(SLOT_TAG, slot);
                entry.put(ITEM_TAG, stored.saveOptional(lookupProvider));
                list.add(entry);
            }
            if (list.isEmpty()) {
                calibration.remove(listName);
            } else {
                calibration.put(listName, list);
            }
        });
    }

    private static @Nullable CompoundTag getCalibrationTag(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return null;
        }
        var customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData == null) {
            return null;
        }
        var root = customData.copyTag();
        return root.contains(CALIBRATION_TAG, Tag.TAG_COMPOUND) ? root.getCompound(CALIBRATION_TAG) : null;
    }

    private static void updateCalibrationTag(ItemStack stack, Consumer<CompoundTag> updater) {
        if (stack == null || stack.isEmpty()) {
            return;
        }
        CustomData.update(DataComponents.CUSTOM_DATA, stack, root -> {
            var calibration = root.contains(CALIBRATION_TAG, Tag.TAG_COMPOUND)
                    ? root.getCompound(CALIBRATION_TAG) : new CompoundTag();
            updater.accept(calibration);
            if (calibration.isEmpty()) {
                root.remove(CALIBRATION_TAG);
            } else {
                root.put(CALIBRATION_TAG, calibration);
            }
        });
    }

    private static HolderLookup.Provider serializationLookup() {
        var server = ServerLifecycleHooks.getCurrentServer();
        return server == null ? FALLBACK_SERIALIZATION_LOOKUP : server.registryAccess();
    }

    private static boolean isValidCalibrationAccess(ItemStack stack, int slot, int slotCount) {
        return !stack.isEmpty() && stack.getItem() instanceof QuickcastScrollCartridge
                && slot >= 0 && slot < slotCount;
    }

}
