package jp.aquafactory.apprenticecodex.item.magicitem;

import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.magic.SpellSelectionManager;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.IPresetSpellContainer;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import io.redspace.ironsspellbooks.api.spells.SpellData;
import io.redspace.ironsspellbooks.config.ServerConfigs;
import io.redspace.ironsspellbooks.item.UniqueItem;
import io.redspace.ironsspellbooks.network.SyncManaPacket;
import jp.aquafactory.apprenticecodex.item.ImmediateSneakSelectionUiItem;
import jp.aquafactory.apprenticecodex.item.SneakSelectionView;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.PacketDistributor;
import java.util.ArrayList;
import java.util.List;

public final class StorageStabilizer extends Item implements IPresetSpellContainer, UniqueItem, ImmediateSneakSelectionUiItem {
    private static final String SELECTED_SPELL_INDEX_TAG = "SelectedStorageSpellIndex";
    private static final int DEFAULT_SPELL_INDEX = 0;
    private static final int ENDER_CHEST_SPELL_LEVEL = 1;
    private static final Component ENDER_CHEST_TITLE = Component.translatable("container.enderchest");

    public StorageStabilizer() {
        super(new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON));
    }

    @Override
    public boolean isEnchantable(@NotNull ItemStack stack) {
        return false;
    }

    @Override
    public int getEnchantmentValue(ItemStack stack) {
        return 0;
    }

    @Override
    public boolean supportsEnchantment(@NotNull ItemStack stack, @NotNull Holder<Enchantment> enchantment) {
        return false;
    }

    @Override
    public boolean isBookEnchantable(ItemStack stack, ItemStack book) {
        return false;
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(@NotNull Level level, Player player, @NotNull InteractionHand usedHand) {
        var stack = player.getItemInHand(usedHand);
        refreshSelectedSpellContainer(stack);
        var spellData = getSelectedSpellData(stack);
        if (spellData == SpellData.EMPTY || spellData.getSpell() == null || spellData.getSpell() == SpellRegistry.none()) {
            return InteractionResultHolder.pass(stack);
        }

        var spell = spellData.getSpell();
        var spellLevel = spell.getLevelFor(spellData.getLevel(), player);
        var casted = spell.attemptInitiateCast(
                stack,
                spellLevel,
                level,
                player,
                CastSource.SWORD,
                true,
                usedHand == InteractionHand.OFF_HAND ? SpellSelectionManager.OFFHAND : SpellSelectionManager.MAINHAND
        );

        return casted
                ? InteractionResultHolder.sidedSuccess(stack, level.isClientSide)
                : InteractionResultHolder.fail(stack);
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

        if (player instanceof ServerPlayer serverPlayer) {
            tryOpenEnderChest(serverPlayer);
        }
        return true;
    }

    @Override
    public void initializeSpellContainer(ItemStack stack) {
        refreshSelectedSpellContainer(stack);
    }

    @Override
    public void inventoryTick(@NotNull ItemStack stack, @NotNull Level level, @NotNull Entity entity, int slotId, boolean isSelected) {
        super.inventoryTick(stack, level, entity, slotId, isSelected);
        if (!isCurrentSelectedSpellContainer(stack)) {
            refreshSelectedSpellContainer(stack);
        }
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, Item.@NotNull TooltipContext context, @NotNull List<Component> lines,
                                @NotNull TooltipFlag flag) {
        refreshSelectedSpellContainer(stack);
        lines.add(Component.translatable("item.apprenticecodex.storage_stabilizer.desc")
                .withStyle(ChatFormatting.GRAY));
        lines.add(Component.translatable(
                "item.apprenticecodex.storage_stabilizer.ender_help",
                Component.literal(Integer.toString(getEnderChestManaCost())).withStyle(ChatFormatting.AQUA)
        ).withStyle(ChatFormatting.GRAY));
        super.appendHoverText(stack, context, lines, flag);
    }

    @Override
    public @NotNull Component getName(@NotNull ItemStack stack) {
        var spellData = getSelectedSpellData(stack);
        if (spellData == SpellData.EMPTY || spellData.getSpell() == null) {
            return super.getName(stack);
        }

        return Component.translatable(
                "item.apprenticecodex.storage_stabilizer.with_spell",
                super.getName(stack),
                spellData.getSpell().getDisplayName(null)
        );
    }

    public static @NotNull SpellData getSelectedSpellData(@NotNull ItemStack stack) {
        return getSpellDataAt(normalizeSelectedSpellIndex(stack));
    }

    public static int getSelectedSpellIndex(@NotNull ItemStack stack) {
        if (!isValidStorageStabilizer(stack)) {
            return DEFAULT_SPELL_INDEX;
        }

        var tag = getCustomDataTag(stack);
        if (tag == null || !tag.contains(SELECTED_SPELL_INDEX_TAG, Tag.TAG_INT)) {
            return DEFAULT_SPELL_INDEX;
        }

        var index = tag.getInt(SELECTED_SPELL_INDEX_TAG);
        return isSelectableSpellIndex(index) ? index : DEFAULT_SPELL_INDEX;
    }

    public static boolean isSelectableSpellIndex(int selectedIndex) {
        return selectedIndex >= 0 && selectedIndex < getSpellCount();
    }

    public static void setSelectedSpellIndex(@NotNull ItemStack stack, int selectedIndex) {
        if (!isValidStorageStabilizer(stack)) {
            return;
        }

        if (!isSelectableSpellIndex(selectedIndex)) {
            selectedIndex = DEFAULT_SPELL_INDEX;
        }

        var normalizedIndex = selectedIndex;
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.putInt(SELECTED_SPELL_INDEX_TAG, normalizedIndex));
        refreshSelectedSpellContainer(stack);
    }

    @Override
    public List<SneakSelectionView> getSneakSelectionViews(ItemStack stack) {
        return getSelectionViews(stack);
    }

    @Override
    public int getSneakSelectionIndex(ItemStack stack) {
        return getSelectedSpellIndex(stack);
    }

    @Override
    public boolean isSneakSelectionIndexSelectable(ItemStack stack, int selectionIndex) {
        return isSelectableSpellIndex(selectionIndex);
    }

    @Override
    public void setSneakSelectionIndex(ItemStack stack, int selectionIndex) {
        setSelectedSpellIndex(stack, selectionIndex);
    }

    public static @NotNull List<SneakSelectionView> getSelectionViews(@NotNull ItemStack stack) {
        if (!isValidStorageStabilizer(stack)) {
            return List.of();
        }

        normalizeSelectedSpellIndex(stack);
        var views = new ArrayList<SneakSelectionView>();
        for (var index = 0; index < getSpellCount(); ++index) {
            var spellData = getSpellDataAt(index);
            views.add(SneakSelectionView.forSpell(
                    index,
                    spellData,
                    isSelectableSpellIndex(index)
            ));
        }
        return List.copyOf(views);
    }

    public static int getEnderChestManaCost() {
        return SpellRegistry.SUMMON_ENDER_CHEST_SPELL.get().getManaCost(ENDER_CHEST_SPELL_LEVEL);
    }

    public static void openEnderChestFromInventorySlot(@NotNull ServerPlayer player, int sourceSlot) {
        if (!isPlayerInventorySlot(sourceSlot)) {
            return;
        }

        var stack = player.getInventory().getItem(sourceSlot);
        if (stack.getItem() instanceof StorageStabilizer) {
            tryOpenEnderChest(player);
        }
    }

    public static void refreshSelectedSpellContainer(@NotNull ItemStack stack) {
        if (!isValidStorageStabilizer(stack)) {
            return;
        }

        var spellData = getSelectedSpellData(stack);
        if (spellData == SpellData.EMPTY || spellData.getSpell() == null) {
            ISpellContainer.remove(stack);
            return;
        }

        if (isCurrentSelectedSpellContainer(stack, spellData)) {
            return;
        }

        // Iron's 側の tooltip とクールダウン表示だけに載せるため、選択中の固定魔法を locked container として投影する。
        var spellContainer = ISpellContainer.create(1, false, false).mutableCopy();
        spellContainer.addSpellAtIndex(spellData.getSpell(), spellData.getLevel(), 0, true);
        ISpellContainer.set(stack, spellContainer.toImmutable());
    }

    private static int normalizeSelectedSpellIndex(@NotNull ItemStack stack) {
        var selectedIndex = getSelectedSpellIndex(stack);
        if (isSelectableSpellIndex(selectedIndex)) {
            return selectedIndex;
        }

        setSelectedSpellIndex(stack, DEFAULT_SPELL_INDEX);
        return DEFAULT_SPELL_INDEX;
    }

    private static @NotNull SpellData getSpellDataAt(int index) {
        var spell = switch (index) {
            case 0 -> io.redspace.ironsspellbooks.api.registry.SpellRegistry.SUMMON_ENDER_CHEST_SPELL.get();
            case 1 -> jp.aquafactory.apprenticecodex.registry.SpellRegistry.PERSONAL_SHELF.get();
            case 2 -> jp.aquafactory.apprenticecodex.registry.SpellRegistry.COMPANION_TRUNK.get();
            default -> null;
        };
        return spell == null ? SpellData.EMPTY : new SpellData(spell, 1);
    }

    private static int getSpellCount() {
        return 3;
    }

    private static void tryOpenEnderChest(ServerPlayer player) {
        var magicData = MagicData.getPlayerMagicData(player);
        var manaCost = getEnderChestManaCost();
        var consumesMana = !(player.isCreative() && !ServerConfigs.CREATIVE_MANA_COST.get());
        if (consumesMana && magicData.getMana() < manaCost) {
            player.displayClientMessage(
                    Component.translatable(
                            "ui.irons_spellbooks.cast_error_mana",
                            SpellRegistry.SUMMON_ENDER_CHEST_SPELL.get().getDisplayName(player)
                    ).withStyle(ChatFormatting.RED),
                    false
            );
            return;
        }

        if (consumesMana) {
            magicData.setMana(Math.max(0.0F, magicData.getMana() - manaCost));
            PacketDistributor.sendToPlayer(player, new SyncManaPacket(magicData));
        }

        var enderChestInventory = player.getEnderChestInventory();
        player.openMenu(new SimpleMenuProvider(
                (containerId, inventory, targetPlayer) -> ChestMenu.threeRows(containerId, inventory, enderChestInventory),
                ENDER_CHEST_TITLE
        ));
        player.level().playSound(
                null,
                player.getX(),
                player.getY(),
                player.getZ(),
                SoundEvents.ENDER_CHEST_OPEN,
                SoundSource.BLOCKS,
                0.5F,
                player.getRandom().nextFloat() * 0.1F + 0.9F
        );
    }

    private static boolean isPlayerInventorySlot(int slot) {
        return slot >= 0 && slot < Inventory.INVENTORY_SIZE || slot == Inventory.SLOT_OFFHAND;
    }

    private static boolean isCurrentSelectedSpellContainer(@NotNull ItemStack stack) {
        var spellData = getSelectedSpellData(stack);
        return spellData != SpellData.EMPTY
                && spellData.getSpell() != null
                && isCurrentSelectedSpellContainer(stack, spellData);
    }

    private static boolean isCurrentSelectedSpellContainer(@NotNull ItemStack stack, @NotNull SpellData spellData) {
        var current = ISpellContainer.get(stack);
        if (current == null) {
            return false;
        }

        var currentSpell = current.getSpellAtIndex(0);
        return current.getMaxSpellCount() == 1
                && !current.isSpellWheel()
                && !current.mustEquip()
                && currentSpell != SpellData.EMPTY
                && currentSpell.getSpell() == spellData.getSpell()
                && currentSpell.getLevel() == spellData.getLevel()
                && currentSpell.isLocked();
    }

    private static boolean isValidStorageStabilizer(@NotNull ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() instanceof StorageStabilizer;
    }

    private static @Nullable CompoundTag getCustomDataTag(ItemStack stack) {
        var customData = stack.get(DataComponents.CUSTOM_DATA);
        return customData == null ? null : customData.copyTag();
    }
}
