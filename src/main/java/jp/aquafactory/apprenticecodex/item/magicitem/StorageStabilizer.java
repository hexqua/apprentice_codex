package jp.aquafactory.apprenticecodex.item.magicitem;

import io.redspace.ironsspellbooks.api.magic.SpellSelectionManager;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.IPresetSpellContainer;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import io.redspace.ironsspellbooks.api.spells.SpellData;
import io.redspace.ironsspellbooks.item.UniqueItem;
import jp.aquafactory.apprenticecodex.item.SneakSelectionUiItem;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;

public final class StorageStabilizer extends Item implements IPresetSpellContainer, UniqueItem, SneakSelectionUiItem {
    private static final String SELECTED_SPELL_INDEX_TAG = "SelectedStorageSpellIndex";
    private static final int DEFAULT_SPELL_INDEX = 0;

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
    public boolean canApplyAtEnchantingTable(ItemStack stack, Enchantment enchantment) {
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
    public void appendHoverText(@NotNull ItemStack stack, @Nullable Level level, @NotNull List<Component> lines,
                                @NotNull TooltipFlag flag) {
        refreshSelectedSpellContainer(stack);
        lines.add(Component.translatable("item.apprenticecodex.storage_stabilizer.desc")
                .withStyle(ChatFormatting.GRAY));
        super.appendHoverText(stack, level, lines, flag);
    }

    public static @NotNull SpellData getSelectedSpellData(@NotNull ItemStack stack) {
        return getSpellDataAt(normalizeSelectedSpellIndex(stack));
    }

    public static int getSelectedSpellIndex(@NotNull ItemStack stack) {
        if (!isValidStorageStabilizer(stack)) {
            return DEFAULT_SPELL_INDEX;
        }

        var tag = stack.getTag();
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

        stack.getOrCreateTag().putInt(SELECTED_SPELL_INDEX_TAG, selectedIndex);
        refreshSelectedSpellContainer(stack);
    }

    public static @NotNull List<SpellSelectionView> getSelectionViews(@NotNull ItemStack stack) {
        if (!isValidStorageStabilizer(stack)) {
            return List.of();
        }

        var selectedIndex = normalizeSelectedSpellIndex(stack);
        var views = new ArrayList<SpellSelectionView>();
        for (var index = 0; index < getSpellCount(); ++index) {
            var spellData = getSpellDataAt(index);
            views.add(new SpellSelectionView(
                    index,
                    spellData,
                    createSelectionDisplayName(spellData),
                    spellData == SpellData.EMPTY || spellData.getSpell() == null
                            ? null
                            : spellData.getSpell().getSpellIconResource(),
                    index == selectedIndex
            ));
        }
        return List.copyOf(views);
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
            default -> null;
        };
        return spell == null ? SpellData.EMPTY : new SpellData(spell, 1);
    }

    private static int getSpellCount() {
        return 2;
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

    private static @NotNull Component createSelectionDisplayName(@NotNull SpellData spellData) {
        if (spellData == SpellData.EMPTY || spellData.getSpell() == null) {
            return Component.empty();
        }

        var spell = spellData.getSpell();
        return spell.getDisplayName(null)
                .copy()
                .append(" ")
                .append(Integer.toString(spellData.getLevel()))
                .withStyle(spell.getSchoolType().getDisplayName().getStyle());
    }

    private static boolean isValidStorageStabilizer(@NotNull ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() instanceof StorageStabilizer;
    }

    public record SpellSelectionView(
            int spellIndex,
            SpellData spellData,
            Component displayName,
            @Nullable ResourceLocation spellIcon,
            boolean currentSelection
    ) {
        public boolean hasSpell() {
            return spellData != SpellData.EMPTY && spellData.getSpell() != null;
        }

        public AbstractSpell spell() {
            return spellData.getSpell();
        }
    }
}
