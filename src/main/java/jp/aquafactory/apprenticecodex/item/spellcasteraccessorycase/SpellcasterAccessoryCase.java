package jp.aquafactory.apprenticecodex.item.spellcasteraccessorycase;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.registry.BlockRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
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
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.NotNull;
import top.theillusivec4.curios.api.CuriosApi;

import java.util.List;

public final class SpellcasterAccessoryCase extends BlockItem {
    public static final int ROW_COUNT = 3;
    public static final int COLUMN_COUNT = 9;
    public static final int SLOT_COUNT = ROW_COUNT * COLUMN_COUNT;

    private static final String INVENTORY_TAG = ApprenticeCodex.MODID + ":spellcaster_accessory_case_inventory";
    private static final String CONTAINER_KEY = "container.apprenticecodex.spellcaster_accessory_case";

    public SpellcasterAccessoryCase() {
        super(BlockRegistry.SPELLCASTER_ACCESSORY_CASE.get(), new Properties().stacksTo(1).fireResistant());
    }

    @Override
    public void appendHoverText(
            @NotNull ItemStack stack,
            Level context,
            @NotNull List<Component> lines,
            @NotNull TooltipFlag flag
    ) {
        super.appendHoverText(stack, context, lines, flag);
        lines.add(Component.translatable("item.apprenticecodex.spellcaster_accessory_case.desc_1").withStyle(ChatFormatting.GRAY));
        lines.add(Component.translatable("item.apprenticecodex.spellcaster_accessory_case.desc_2").withStyle(ChatFormatting.GRAY));
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(
            @NotNull Level level,
            @NotNull Player player,
            @NotNull InteractionHand usedHand
    ) {
        var stack = player.getItemInHand(usedHand);
        if (player.isSecondaryUseActive()) {
            return InteractionResultHolder.fail(stack);
        }

        var sourceSlot = usedHand == InteractionHand.MAIN_HAND ? player.getInventory().selected : Inventory.SLOT_OFFHAND;
        openMenu(player, sourceSlot, stack);
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public @NotNull net.minecraft.world.InteractionResult useOn(@NotNull UseOnContext context) {
        var player = context.getPlayer();
        if (player == null || player.isSecondaryUseActive()) {
            return super.useOn(context);
        }

        var sourceSlot = context.getHand() == InteractionHand.MAIN_HAND
                ? player.getInventory().selected
                : Inventory.SLOT_OFFHAND;
        openMenu(player, sourceSlot, context.getItemInHand());
        return net.minecraft.world.InteractionResult.sidedSuccess(context.getLevel().isClientSide);
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

        NetworkHooks.openScreen(serverPlayer, new MenuProvider() {
            @Override
            public @NotNull Component getDisplayName() {
                return stack.hasCustomHoverName()
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
        }, buffer -> SpellcasterAccessoryCaseMenu.writeInventorySource(buffer, sourceSlot));
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

    public static void loadInventory(
            ItemStack caseStack,
            ItemStackHandler inventory
    ) {
        var root = caseStack.getTag();
        if (root == null) {
            return;
        }

        if (!root.contains(INVENTORY_TAG, Tag.TAG_COMPOUND)) {
            return;
        }

        var inventoryTag = root.getCompound(INVENTORY_TAG).copy();
        inventoryTag.putInt("Size", SLOT_COUNT);
        inventory.deserializeNBT(inventoryTag);
    }

    public static void saveInventory(
            ItemStack caseStack,
            IItemHandler inventory
    ) {
        var inventoryTag = new CompoundTag();
        inventoryTag.putInt("Size", SLOT_COUNT);
        var items = new net.minecraft.nbt.ListTag();
        for (var slot = 0; slot < inventory.getSlots(); ++slot) {
            var stack = inventory.getStackInSlot(slot);
            if (stack.isEmpty()) {
                continue;
            }
            var itemTag = stack.save(new CompoundTag());
            itemTag.putInt("Slot", slot);
            items.add(itemTag);
        }
        inventoryTag.put("Items", items);
        caseStack.getOrCreateTag().put(INVENTORY_TAG, inventoryTag);
    }

    public static final class CaseInventory extends ItemStackHandler {
        private final ItemStack caseStack;
        private final LivingEntity wearer;
        private boolean loading;

        public CaseInventory(ItemStack caseStack, LivingEntity wearer) {
            super(SLOT_COUNT);
            this.caseStack = caseStack;
            this.wearer = wearer;
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
            loading = true;
            try {
                loadInventory(caseStack, this);
            } finally {
                loading = false;
            }
        }

        private void save() {
            saveInventory(caseStack, this);
        }
    }
}
