package jp.aquafactory.apprenticecodex.item.curios.archivistsgrimoire;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import io.redspace.ironsspellbooks.api.item.ISpellbook;
import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import io.redspace.ironsspellbooks.api.spells.SpellData;
import io.redspace.ironsspellbooks.compat.Curios;
import io.redspace.ironsspellbooks.item.weapons.AttributeContainer;
import io.redspace.ironsspellbooks.registries.SoundRegistry;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.compat.jei.IJeiInfoItem;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurio;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

import java.util.ArrayList;
import java.util.List;

public class ArchivistsGrimoire extends Item implements ICurioItem, ISpellbook, IJeiInfoItem {
    public static final int ROW_COUNT = 6;
    public static final int COLUMN_COUNT = 9;
    public static final int SLOT_COUNT = ROW_COUNT * COLUMN_COUNT;

    private static final String JEI_INFO_KEY_PREFIX = "jei.apprenticecodex.archivists_grimoire.desc_";
    private static final String INVENTORY_TAG = ApprenticeCodex.MODID + ":archivists_grimoire_inventory";
    private static final String SELECTED_ROW_TAG = ApprenticeCodex.MODID + ":archivists_grimoire_selected_row";
    private static final String DEFAULT_CONTAINER_KEY = "container.apprenticecodex.archivists_grimoire.default";
    private static final AttributeContainer[] SPELLBOOK_ATTRIBUTES = {
            new AttributeContainer(
                    AttributeRegistry.MAX_MANA,
                    200,
                    AttributeModifier.Operation.ADD_VALUE
            )
    };

    public ArchivistsGrimoire() {
        super(new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON));
    }

    @Override
    public String getJeiInfoTranslationKeyPrefix() {
        return JEI_INFO_KEY_PREFIX;
    }

    @Override
    public boolean canEquipFromUse(SlotContext slotContext, ItemStack stack) {
        // 右クリックは内部インベントリ編集に使うため、Curiosの右クリック装備はさせない。
        return false;
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(@NotNull Level level, Player player, @NotNull InteractionHand usedHand) {
        var stack = player.getItemInHand(usedHand);
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            serverPlayer.openMenu(new MenuProvider() {
                @Override
                public @NotNull Component getDisplayName() {
                    return getMenuTitle(stack);
                }

                @Override
                public @NotNull AbstractContainerMenu createMenu(int containerId, @NotNull Inventory inventory, @NotNull Player targetPlayer) {
                    return new ArchivistsGrimoireMenu(containerId, inventory, usedHand);
                }
            }, buffer -> buffer.writeEnum(usedHand));
        }

        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    public static Component getMenuTitle(ItemStack stack) {
        return stack.get(DataComponents.CUSTOM_NAME) != null ? stack.getHoverName() : Component.translatable(DEFAULT_CONTAINER_KEY);
    }

    public static int getSelectedRow(ItemStack stack) {
        var customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData == null) {
            return 0;
        }

        var tag = customData.copyTag();
        if (!tag.contains(SELECTED_ROW_TAG, Tag.TAG_INT)) {
            return 0;
        }
        return Math.floorMod(tag.getInt(SELECTED_ROW_TAG), ROW_COUNT);
    }

    public static int setSelectedRow(ItemStack stack, int row) {
        var selectedRow = Math.floorMod(row, ROW_COUNT);
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.putInt(SELECTED_ROW_TAG, selectedRow));
        return selectedRow;
    }

    public static int changeSelectedRow(ItemStack stack, int delta) {
        return setSelectedRow(stack, getSelectedRow(stack) + delta);
    }

    public static boolean ensureSelectedRowHasScroll(ItemStack stack, HolderLookup.Provider registries) {
        var inventory = new ScrollInventory(stack, registries);
        var selectedRow = getSelectedRow(stack);
        if (hasScrollInRow(inventory, selectedRow)) {
            return true;
        }

        for (var row = 0; row < ROW_COUNT; ++row) {
            if (hasScrollInRow(inventory, row)) {
                setSelectedRow(stack, row);
                return true;
            }
        }
        return false;
    }

    public static boolean changeSelectedRowToPopulatedRow(ItemStack stack, int delta, HolderLookup.Provider registries) {
        if (delta == 0) {
            return false;
        }

        var inventory = new ScrollInventory(stack, registries);
        for (var offset = 1; offset <= ROW_COUNT; ++offset) {
            var row = Math.floorMod(getSelectedRow(stack) + delta * offset, ROW_COUNT);
            if (hasScrollInRow(inventory, row)) {
                setSelectedRow(stack, row);
                return true;
            }
        }
        return false;
    }

    public static SpellData getVisibleSpell(ItemStack grimoireStack, int visibleSlot, HolderLookup.Provider registries) {
        if (visibleSlot < 0 || visibleSlot >= COLUMN_COUNT) {
            return SpellData.EMPTY;
        }

        if (!ensureSelectedRowHasScroll(grimoireStack, registries)) {
            return SpellData.EMPTY;
        }

        var inventory = new ScrollInventory(grimoireStack, registries);
        var scrollStack = inventory.getStackInSlot(getSelectedRow(grimoireStack) * COLUMN_COUNT + visibleSlot);
        if (scrollStack.isEmpty() || !isScroll(scrollStack)) {
            return SpellData.EMPTY;
        }

        var scrollContainer = ISpellContainer.get(scrollStack);
        return scrollContainer == null ? SpellData.EMPTY : scrollContainer.getSpellAtIndex(0);
    }

    @Override
    public Multimap<Holder<Attribute>, AttributeModifier> getAttributeModifiers(SlotContext slotContext, ResourceLocation id, ItemStack stack) {
        if (!Curios.SPELLBOOK_SLOT.equals(slotContext.identifier())) {
            return ICurioItem.super.getAttributeModifiers(slotContext, id, stack);
        }

        var builder = ImmutableMultimap.<Holder<Attribute>, AttributeModifier>builder();
        for (var attributeContainer : SPELLBOOK_ATTRIBUTES) {
            var modifierName = String.format("%s_%s", Curios.SPELLBOOK_SLOT, slotContext.index());
            builder.put(attributeContainer.attribute(), attributeContainer.createModifier(modifierName));
        }

        return builder.build();
    }

    @Override
    public void appendHoverText(@NotNull ItemStack itemStack, Item.TooltipContext context, @NotNull List<Component> lines, @NotNull TooltipFlag flag) {
        var registries = context.registries();
        var selectedRow = getSelectedRow(itemStack);
        var visibleSpells = getVisibleSpells(itemStack, selectedRow, registries);
        var otherScrollCount = countScrollsOutsideRow(itemStack, selectedRow, registries);

        lines.add(Component.translatable("tooltip.irons_spellbooks.spellbook_spell_count", SLOT_COUNT).withStyle(ChatFormatting.GRAY));
        lines.add(Component.translatable(
                        "item.apprenticecodex.archivists_grimoire.tooltip.current_page",
                        selectedRow + 1,
                        ROW_COUNT)
                .withStyle(ChatFormatting.GRAY));
        if (!hasStoredSpell(itemStack)) {
            lines.add(Component.translatable("item.apprenticecodex.special_spellbook.inscribe_hint").withStyle(ChatFormatting.GRAY));
        }

        if (!visibleSpells.isEmpty() && FMLEnvironment.dist == Dist.CLIENT) {
            ArchivistsGrimoireClientTooltip.append(itemStack, lines, visibleSpells);
        }

        if (otherScrollCount > 0) {
            lines.add(Component.empty());
            lines.add(Component.translatable(
                            "item.apprenticecodex.archivists_grimoire.tooltip.other_scroll_count",
                            otherScrollCount)
                    .withStyle(ChatFormatting.GRAY));
        }

        super.appendHoverText(itemStack, context, lines, flag);
    }

    private static List<VisibleSpell> getVisibleSpells(ItemStack grimoireStack, int selectedRow, HolderLookup.Provider registries) {
        var visibleSpells = new ArrayList<VisibleSpell>();
        var inventory = new ScrollInventory(grimoireStack, registries);
        var startSlot = Math.floorMod(selectedRow, ROW_COUNT) * COLUMN_COUNT;
        for (var visibleSlot = 0; visibleSlot < COLUMN_COUNT; ++visibleSlot) {
            var spellData = getSpellData(inventory.getStackInSlot(startSlot + visibleSlot));
            if (spellData != SpellData.EMPTY) {
                visibleSpells.add(new VisibleSpell(visibleSlot, spellData));
            }
        }
        return visibleSpells;
    }

    private static SpellData getSpellData(ItemStack scrollStack) {
        if (scrollStack.isEmpty() || !isScroll(scrollStack)) {
            return SpellData.EMPTY;
        }

        var scrollContainer = ISpellContainer.get(scrollStack);
        return scrollContainer == null ? SpellData.EMPTY : scrollContainer.getSpellAtIndex(0);
    }

    private static int countScrollsOutsideRow(ItemStack grimoireStack, int excludedRow, HolderLookup.Provider registries) {
        var inventory = new ScrollInventory(grimoireStack, registries);
        var normalizedExcludedRow = Math.floorMod(excludedRow, ROW_COUNT);
        var count = 0;
        for (var row = 0; row < ROW_COUNT; ++row) {
            if (row == normalizedExcludedRow) {
                continue;
            }

            var startSlot = row * COLUMN_COUNT;
            for (var slot = startSlot; slot < startSlot + COLUMN_COUNT; ++slot) {
                if (isScroll(inventory.getStackInSlot(slot))) {
                    ++count;
                }
            }
        }
        return count;
    }

    private static boolean hasStoredSpell(ItemStack grimoireStack) {
        var inventory = new ScrollInventory(grimoireStack);
        for (var slot = 0; slot < SLOT_COUNT; ++slot) {
            if (getSpellData(inventory.getStackInSlot(slot)) != SpellData.EMPTY) {
                return true;
            }
        }
        return false;
    }

    static boolean isScroll(ItemStack stack) {
        return stack.is(io.redspace.ironsspellbooks.registries.ItemRegistry.SCROLL.get());
    }

    private static boolean hasScrollInRow(ScrollInventory inventory, int row) {
        var normalizedRow = Math.floorMod(row, ROW_COUNT);
        var startSlot = normalizedRow * COLUMN_COUNT;
        for (var slot = startSlot; slot < startSlot + COLUMN_COUNT; ++slot) {
            if (isScroll(inventory.getStackInSlot(slot))) {
                return true;
            }
        }
        return false;
    }

    record VisibleSpell(int visibleSlot, SpellData spellData) {
    }

    @NotNull
    @Override
    public ICurio.SoundInfo getEquipSound(SlotContext slotContext, ItemStack stack) {
        return new ICurio.SoundInfo(SoundRegistry.EQUIP_SPELL_BOOK.get(), 1.0f, 1.0f);
    }

    public static final class ScrollInventory extends ItemStackHandler {
        private final ItemStack grimoireStack;
        private final HolderLookup.Provider registries;

        public ScrollInventory(ItemStack grimoireStack, HolderLookup.Provider registries) {
            super(SLOT_COUNT);
            this.grimoireStack = grimoireStack;
            this.registries = registries;
            load();
        }

        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            return isScroll(stack);
        }

        @Override
        protected void onContentsChanged(int slot) {
            save();
        }

        private void load() {
            var customData = grimoireStack.get(DataComponents.CUSTOM_DATA);
            if (customData == null) {
                return;
            }

            var tag = customData.copyTag();
            if (!tag.contains(INVENTORY_TAG, Tag.TAG_COMPOUND)) {
                return;
            }

            var inventoryTag = tag.getCompound(INVENTORY_TAG).copy();
            inventoryTag.putInt("Size", SLOT_COUNT);
            deserializeNBT(registries, inventoryTag);

            var changed = false;
            for (var slot = 0; slot < getSlots(); ++slot) {
                if (!getStackInSlot(slot).isEmpty() && !isScroll(getStackInSlot(slot))) {
                    stacks.set(slot, ItemStack.EMPTY);
                    changed = true;
                }
            }
            if (changed) {
                save();
            }
        }

        private void save() {
            CustomData.update(DataComponents.CUSTOM_DATA, grimoireStack, tag -> tag.put(INVENTORY_TAG, serializeNBT(registries)));
        }
    }
}
