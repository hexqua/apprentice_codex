package jp.aquafactory.apprenticecodex.block.apprenticedesk;

import com.google.common.collect.Lists;
import io.redspace.ironsspellbooks.api.config.SpellConfigManager;
import io.redspace.ironsspellbooks.api.config.SpellConfigParameter;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import io.redspace.ironsspellbooks.api.spells.SpellData;
import io.redspace.ironsspellbooks.api.spells.SpellRarity;
import io.redspace.ironsspellbooks.registries.ItemRegistry;
import jp.aquafactory.apprenticecodex.config.ApprenticeCodexServerConfig;
import jp.aquafactory.apprenticecodex.registry.BlockRegistry;
import jp.aquafactory.apprenticecodex.registry.MenuRegistry;
import jp.aquafactory.apprenticecodex.registry.SoundRegistry;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.Comparator;
import java.util.List;
import java.util.Set;

public class ApprenticeDeskMenu extends AbstractContainerMenu {
    public static final int INPUT_SLOT = 0;
    public static final int RESULT_SLOT = 1;
    private static final int INVENTORY_SLOT_START = 2;
    private static final int INVENTORY_SLOT_END = 29;
    private static final int HOTBAR_SLOT_START = 29;
    private static final int HOTBAR_SLOT_END = 38;
    private static final int NO_TARGET_RARITY = -1;

    private final ContainerLevelAccess access;
    private final DataSlot selectedRecipeIndex = DataSlot.standalone();
    private final List<AbstractSpell> availableSpells = Lists.newArrayList();

    long lastSoundTime;
    final Slot inputSlot;
    final Slot resultSlot;
    Runnable slotUpdateListener = () -> {};

    public final Container container = new SimpleContainer(1) {
        @Override
        public void setChanged() {
            super.setChanged();
            ApprenticeDeskMenu.this.slotsChanged(this);
            ApprenticeDeskMenu.this.slotUpdateListener.run();
        }
    };

    final ResultContainer resultContainer = new ResultContainer();

    public ApprenticeDeskMenu(int containerId, Inventory inventory) {
        this(containerId, inventory, ContainerLevelAccess.NULL);
    }

    public ApprenticeDeskMenu(int containerId, Inventory inventory, ContainerLevelAccess containerAccess) {
        super(MenuRegistry.APPRENTICE_DESK.get(), containerId);
        access = containerAccess;

        inputSlot = addSlot(new Slot(container, INPUT_SLOT, 18, 17) {
            @Override
            public boolean mayPlace(@NotNull ItemStack stack) {
                return isValidInputItem(stack);
            }
        });
        resultSlot = addSlot(new Slot(resultContainer, RESULT_SLOT, 18, 47) {
            @Override
            public boolean mayPlace(@NotNull ItemStack stack) {
                return false;
            }

            @Override
            public void onTake(@NotNull Player player, @NotNull ItemStack stack) {
                stack.onCraftedBy(player.level(), player, stack.getCount());
                ApprenticeDeskMenu.this.inputSlot.remove(1);
                ApprenticeDeskMenu.this.setupResultSlot();

                containerAccess.execute((targetLevel, targetPos) -> {
                    var gameTime = targetLevel.getGameTime();
                    if (ApprenticeDeskMenu.this.lastSoundTime != gameTime) {
                        targetLevel.playSound(null, targetPos, SoundRegistry.VANILLA_USE_DESK.get(), SoundSource.BLOCKS, 1.0F, 1.0F);
                        ApprenticeDeskMenu.this.lastSoundTime = gameTime;
                    }
                });
                super.onTake(player, stack);
            }
        });

        for (var row = 0; row < 3; ++row) {
            for (var col = 0; col < 9; ++col) {
                addSlot(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }

        for (var col = 0; col < 9; ++col) {
            addSlot(new Slot(inventory, col, 8 + col * 18, 142));
        }

        addDataSlot(selectedRecipeIndex);
        selectedRecipeIndex.set(-1);
        refreshAvailableSpells();
    }

    public int getSelectedRecipeIndex() {
        return selectedRecipeIndex.get();
    }

    public List<AbstractSpell> getAvailableSpells() {
        return availableSpells;
    }

    public boolean hasInputItem() {
        return isValidInputItem(inputSlot.getItem());
    }

    @Override
    public boolean stillValid(@NotNull Player player) {
        return stillValid(access, player, BlockRegistry.APPRENTICE_DESK.get());
    }

    @Override
    public boolean clickMenuButton(@NotNull Player player, int recipeIndex) {
        if (isValidSpellIndex(recipeIndex)) {
            selectedRecipeIndex.set(recipeIndex);
            setupResultSlot();
            return true;
        }

        return false;
    }

    private boolean isValidSpellIndex(int recipeIndex) {
        return recipeIndex >= 0 && recipeIndex < availableSpells.size();
    }

    private boolean isValidInputItem(ItemStack stack) {
        return stack.is(ItemRegistry.SCROLL.get());
    }

    @Override
    public void slotsChanged(@NotNull Container container) {
        refreshAvailableSpells();
        if (!isValidSpellIndex(selectedRecipeIndex.get())) {
            selectedRecipeIndex.set(-1);
        }
        setupResultSlot();
    }

    void setupResultSlot() {
        if (hasInputItem() && isValidSpellIndex(selectedRecipeIndex.get())) {
            var spell = availableSpells.get(selectedRecipeIndex.get());
            var result = new ItemStack(ItemRegistry.SCROLL.get());
            ISpellContainer.createScrollContainer(spell, 1, result);
            resultSlot.set(result);
        } else {
            resultSlot.set(ItemStack.EMPTY);
        }

        broadcastChanges();
    }

    private void refreshAvailableSpells() {
        var targetMinRarity = getTargetMinRarity();
        availableSpells.clear();
        if (targetMinRarity == NO_TARGET_RARITY) {
            return;
        }

        var sourceSpell = getSourceSpell();
        var spellCraftBlacklist = getSpellCraftBlacklist();
        availableSpells.addAll(
                SpellRegistry.getEnabledSpells()
                        .stream()
                        .filter(AbstractSpell::allowCrafting)
                        .filter(spell -> SpellConfigManager.getSpellConfigValue(spell, SpellConfigParameter.MIN_RARITY).getValue() == targetMinRarity)
                        .filter(spell -> isAllowedBySameSchoolSetting(spell, sourceSpell))
                        .filter(spell -> !spellCraftBlacklist.contains(spell.getSpellId()))
                        .toList()
        );

        // getDisplayNameのプレイヤーは未解禁時の難読化用なのでソートには使わない.
        availableSpells.sort(Comparator
                .comparing(this::getSchoolTypeSortKey)
                .thenComparing(spell -> spell.getDisplayName(null).getString()));
    }

    private int getTargetMinRarity() {
        var sourceSpellData = getSourceSpellData();
        if (sourceSpellData == null) {
            return SpellRarity.COMMON.getValue();
        }

        // Scroll は 1 スロット構成のため、先頭呪文のレアリティを基準に候補を決定する。
        var sourceRarityValue = sourceSpellData.getRarity().getValue();
        if (ApprenticeCodexServerConfig.apprenticeDeskDisableCommonRarityConversion()
                && sourceRarityValue <= SpellRarity.COMMON.getValue()) {
            return NO_TARGET_RARITY;
        }

        return Math.max(sourceRarityValue - 1, SpellRarity.COMMON.getValue());
    }

    private SpellData getSourceSpellData() {
        var inputItem = inputSlot.getItem();
        if (!isValidInputItem(inputItem) || !ISpellContainer.isSpellContainer(inputItem)) {
            return null;
        }

        var spellContainer = ISpellContainer.get(inputItem);
        if (spellContainer == null || spellContainer.isEmpty()) {
            return null;
        }

        var spellData = spellContainer.getSpellAtIndex(0);
        return spellData == SpellData.EMPTY ? null : spellData;
    }

    private AbstractSpell getSourceSpell() {
        var spellData = getSourceSpellData();
        return spellData == null ? null : spellData.getSpell();
    }

    private boolean isAllowedBySameSchoolSetting(AbstractSpell spell, AbstractSpell sourceSpell) {
        if (!ApprenticeCodexServerConfig.apprenticeDeskRequireSameSchool()) {
            return true;
        }

        if (sourceSpell == null) {
            return false;
        }

        return spell.getSchoolType().equals(sourceSpell.getSchoolType());
    }

    private Set<String> getSpellCraftBlacklist() {
        if (!ApprenticeCodexServerConfig.apprenticeDeskEnableSpellCraftBlacklist()) {
            return Set.of();
        }

        return Set.copyOf(ApprenticeCodexServerConfig.apprenticeDeskSpellCraftBlacklist());
    }

    private String getSchoolTypeSortKey(AbstractSpell spell) {
        return spell.getSchoolType().getId().toString();
    }

    @Override
    public boolean canTakeItemForPickAll(@NotNull ItemStack stack, Slot slot) {
        return slot.container != resultContainer && super.canTakeItemForPickAll(stack, slot);
    }

    @Override
    public @NotNull ItemStack quickMoveStack(@NotNull Player player, int slotIndex) {
        var moved = ItemStack.EMPTY;
        var slot = slots.get(slotIndex);
        if (slot.hasItem()) {
            var stackInSlot = slot.getItem();
            var item = stackInSlot.getItem();
            moved = stackInSlot.copy();
            if (slotIndex == RESULT_SLOT) {
                item.onCraftedBy(stackInSlot, player.level(), player);
                if (!moveItemStackTo(stackInSlot, INVENTORY_SLOT_START, HOTBAR_SLOT_END, true)) {
                    return ItemStack.EMPTY;
                }

                slot.onQuickCraft(stackInSlot, moved);
            } else if (slotIndex == INPUT_SLOT) {
                if (!moveItemStackTo(stackInSlot, INVENTORY_SLOT_START, HOTBAR_SLOT_END, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (slotIndex >= INVENTORY_SLOT_START && slotIndex < HOTBAR_SLOT_END) {
                if (!moveItemStackTo(stackInSlot, INPUT_SLOT, RESULT_SLOT, false)) {
                    if (slotIndex < INVENTORY_SLOT_END) {
                        if (!moveItemStackTo(stackInSlot, HOTBAR_SLOT_START, HOTBAR_SLOT_END, false)) {
                            return ItemStack.EMPTY;
                        }
                    } else if (!moveItemStackTo(stackInSlot, INVENTORY_SLOT_START, INVENTORY_SLOT_END, false)) {
                        return ItemStack.EMPTY;
                    }
                }
            }

            if (stackInSlot.isEmpty()) {
                slot.setByPlayer(ItemStack.EMPTY);
            }

            slot.setChanged();
            if (stackInSlot.getCount() == moved.getCount()) {
                return ItemStack.EMPTY;
            }

            slot.onTake(player, stackInSlot);
            broadcastChanges();
        }

        return moved;
    }

    @Override
    public void removed(@NotNull Player player) {
        super.removed(player);
        resultContainer.removeItemNoUpdate(RESULT_SLOT);
        access.execute((targetLevel, targetPos) -> clearContainer(player, container));
    }
}
