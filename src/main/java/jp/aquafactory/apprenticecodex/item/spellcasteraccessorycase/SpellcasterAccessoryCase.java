package jp.aquafactory.apprenticecodex.item.spellcasteraccessorycase;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import top.theillusivec4.curios.api.CuriosApi;

public final class SpellcasterAccessoryCase extends Item {
    public static final int ROW_COUNT = 3;
    public static final int COLUMN_COUNT = 9;
    public static final int SLOT_COUNT = ROW_COUNT * COLUMN_COUNT;

    private static final String INVENTORY_TAG = ApprenticeCodex.MODID + ":spellcaster_accessory_case_inventory";
    private static final String CONTAINER_KEY = "container.apprenticecodex.spellcaster_accessory_case";

    public SpellcasterAccessoryCase() {
        super(new Item.Properties().stacksTo(1));
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(
            @NotNull Level level,
            @NotNull Player player,
            @NotNull InteractionHand usedHand
    ) {
        var sourceSlot = usedHand == InteractionHand.MAIN_HAND ? player.getInventory().selected : Inventory.SLOT_OFFHAND;
        var stack = player.getItemInHand(usedHand);
        openMenu(player, sourceSlot, stack);
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public boolean overrideOtherStackedOnMe(
            @NotNull ItemStack stack,
            @NotNull ItemStack carriedStack,
            @NotNull Slot slot,
            @NotNull ClickAction action,
            @NotNull Player player,
            @NotNull SlotAccess access
    ) {
        if (action != ClickAction.SECONDARY
                || !carriedStack.isEmpty()
                || slot.container != player.getInventory()
                || stack.getItem() != this) {
            return false;
        }

        openMenu(player, slot.getSlotIndex(), stack);
        return true;
    }

    public static void openFromInventorySlot(ServerPlayer player, int sourceSlot) {
        if (!isPlayerInventorySlot(sourceSlot)) {
            return;
        }

        var stack = player.getInventory().getItem(sourceSlot);
        if (stack.getItem() instanceof SpellcasterAccessoryCase) {
            openMenu(player, sourceSlot, stack);
        }
    }

    private static void openMenu(Player player, int sourceSlot, ItemStack stack) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }

        serverPlayer.openMenu(new MenuProvider() {
            @Override
            public @NotNull Component getDisplayName() {
                return stack.has(DataComponents.CUSTOM_NAME)
                        ? stack.getHoverName()
                        : Component.translatable(CONTAINER_KEY);
            }

            @Override
            public @NotNull AbstractContainerMenu createMenu(
                    int containerId,
                    @NotNull Inventory inventory,
                    @NotNull Player targetPlayer
            ) {
                return new SpellcasterAccessoryCaseMenu(containerId, inventory, sourceSlot);
            }
        }, buffer -> buffer.writeVarInt(sourceSlot));
    }

    public static boolean accepts(ItemStack stack, LivingEntity wearer) {
        if (stack.isEmpty() || stack.getItem() instanceof SpellcasterAccessoryCase) {
            return false;
        }

        // ICurioの実装有無ではなくvalidatorを通る装備先で判定し、表示専用品や未調整の箒を除外する。
        return !CuriosApi.getItemStackSlots(stack, wearer).isEmpty();
    }

    private static boolean isPlayerInventorySlot(int slot) {
        return slot >= 0 && slot < Inventory.INVENTORY_SIZE || slot == Inventory.SLOT_OFFHAND;
    }

    public static final class CaseInventory extends ItemStackHandler {
        private final ItemStack caseStack;
        private final LivingEntity wearer;
        private final HolderLookup.Provider registries;
        private boolean loading;

        public CaseInventory(ItemStack caseStack, LivingEntity wearer) {
            super(SLOT_COUNT);
            this.caseStack = caseStack;
            this.wearer = wearer;
            this.registries = wearer.registryAccess();
            load();
        }

        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            return accepts(stack, wearer);
        }

        @Override
        protected void onContentsChanged(int slot) {
            if (!loading) {
                save();
            }
        }

        private void load() {
            var customData = caseStack.get(DataComponents.CUSTOM_DATA);
            if (customData == null) {
                return;
            }

            var root = customData.copyTag();
            if (!root.contains(INVENTORY_TAG, Tag.TAG_COMPOUND)) {
                return;
            }

            loading = true;
            try {
                var inventoryTag = root.getCompound(INVENTORY_TAG).copy();
                inventoryTag.putInt("Size", SLOT_COUNT);
                deserializeNBT(registries, inventoryTag);
            } finally {
                loading = false;
            }
        }

        private void save() {
            var inventoryTag = serializeNBT(registries);
            CustomData.update(DataComponents.CUSTOM_DATA, caseStack, root -> root.put(INVENTORY_TAG, inventoryTag));
        }
    }
}
