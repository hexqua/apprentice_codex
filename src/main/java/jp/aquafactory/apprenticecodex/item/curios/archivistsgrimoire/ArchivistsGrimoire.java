package jp.aquafactory.apprenticecodex.item.curios.archivistsgrimoire;

import io.redspace.ironsspellbooks.api.item.ISpellbook;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import io.redspace.ironsspellbooks.api.spells.SpellData;
import io.redspace.ironsspellbooks.registries.SoundRegistry;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.Level;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.NotNull;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurio;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

public class ArchivistsGrimoire extends Item implements ICurioItem, ISpellbook {
    public static final int ROW_COUNT = 6;
    public static final int COLUMN_COUNT = 9;
    public static final int SLOT_COUNT = ROW_COUNT * COLUMN_COUNT;

    private static final String INVENTORY_TAG = ApprenticeCodex.MODID + ":archivists_grimoire_inventory";
    private static final String SELECTED_ROW_TAG = ApprenticeCodex.MODID + ":archivists_grimoire_selected_row";
    private static final String DEFAULT_CONTAINER_KEY = "container.apprenticecodex.archivists_grimoire.default";

    public ArchivistsGrimoire() {
        super(new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON));
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
            NetworkHooks.openScreen(serverPlayer, new MenuProvider() {
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
        return stack.hasCustomHoverName() ? stack.getHoverName() : Component.translatable(DEFAULT_CONTAINER_KEY);
    }

    public static int getSelectedRow(ItemStack stack) {
        var tag = stack.getTag();
        if (tag == null || !tag.contains(SELECTED_ROW_TAG)) {
            return 0;
        }
        return Math.floorMod(tag.getInt(SELECTED_ROW_TAG), ROW_COUNT);
    }

    public static int setSelectedRow(ItemStack stack, int row) {
        var selectedRow = Math.floorMod(row, ROW_COUNT);
        stack.getOrCreateTag().putInt(SELECTED_ROW_TAG, selectedRow);
        return selectedRow;
    }

    public static int changeSelectedRow(ItemStack stack, int delta) {
        return setSelectedRow(stack, getSelectedRow(stack) + delta);
    }

    public static SpellData getVisibleSpell(ItemStack grimoireStack, int visibleSlot) {
        if (visibleSlot < 0 || visibleSlot >= COLUMN_COUNT) {
            return SpellData.EMPTY;
        }

        var inventory = new ScrollInventory(grimoireStack);
        var scrollStack = inventory.getStackInSlot(getSelectedRow(grimoireStack) * COLUMN_COUNT + visibleSlot);
        if (scrollStack.isEmpty() || !isScroll(scrollStack)) {
            return SpellData.EMPTY;
        }

        var scrollContainer = ISpellContainer.get(scrollStack);
        return scrollContainer == null ? SpellData.EMPTY : scrollContainer.getSpellAtIndex(0);
    }

    static boolean isScroll(ItemStack stack) {
        return stack.is(io.redspace.ironsspellbooks.registries.ItemRegistry.SCROLL.get());
    }

    @NotNull
    @Override
    public ICurio.SoundInfo getEquipSound(SlotContext slotContext, ItemStack stack) {
        return new ICurio.SoundInfo(SoundRegistry.EQUIP_SPELL_BOOK.get(), 1.0f, 1.0f);
    }

    public static final class ScrollInventory extends ItemStackHandler {
        private final ItemStack grimoireStack;

        public ScrollInventory(ItemStack grimoireStack) {
            super(SLOT_COUNT);
            this.grimoireStack = grimoireStack;
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
            var tag = grimoireStack.getTag();
            if (tag == null || !tag.contains(INVENTORY_TAG)) {
                return;
            }

            var inventoryTag = tag.getCompound(INVENTORY_TAG).copy();
            inventoryTag.putInt("Size", SLOT_COUNT);
            deserializeNBT(inventoryTag);

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
            grimoireStack.getOrCreateTag().put(INVENTORY_TAG, serializeNBT());
        }
    }
}
