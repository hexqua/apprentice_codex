package jp.aquafactory.apprenticecodex.item.curios.endergrimoire;

import io.redspace.ironsspellbooks.api.events.InscribeSpellEvent;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import io.redspace.ironsspellbooks.api.spells.SpellData;
import io.redspace.ironsspellbooks.item.Scroll;
import jp.aquafactory.apprenticecodex.capability.Capabilities;
import jp.aquafactory.apprenticecodex.capability.endergrimoire.EnderGrimoireSpellbookSync;
import jp.aquafactory.apprenticecodex.item.armor.ElementMaidenRobeSchoolPowerBonusEvents;
import jp.aquafactory.apprenticecodex.registry.MenuRegistry;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;
import org.jetbrains.annotations.NotNull;

import java.util.function.Predicate;

public class EnderGrimoireInscriptionMenu extends AbstractContainerMenu {
    public static final int INSCRIBE_BUTTON_ID = -2;

    private static final int HOTBAR_SLOT_COUNT = 9;
    private static final int PLAYER_INVENTORY_ROW_COUNT = 3;
    private static final int PLAYER_INVENTORY_COLUMN_COUNT = 9;
    private static final int PLAYER_INVENTORY_SLOT_COUNT = PLAYER_INVENTORY_COLUMN_COUNT * PLAYER_INVENTORY_ROW_COUNT;
    private static final int VANILLA_SLOT_COUNT = HOTBAR_SLOT_COUNT + PLAYER_INVENTORY_SLOT_COUNT;
    private static final int VANILLA_FIRST_SLOT_INDEX = 0;
    private static final int MENU_SLOT_FIRST_INDEX = VANILLA_FIRST_SLOT_INDEX + VANILLA_SLOT_COUNT;
    private static final int MENU_SLOT_COUNT = 2;

    private static final ISpellContainer FALLBACK_CONTAINER = ISpellContainer.create(15, true, true);

    private final Player player;
    private final Slot scrollSlot;
    private final Slot resultSlot;
    private int selectedSpellIndex = -1;

    protected final ResultContainer resultContainer = new ResultContainer();
    protected final Container scrollContainer = new SimpleContainer(1) {
        @Override
        public void setChanged() {
            super.setChanged();
            EnderGrimoireInscriptionMenu.this.slotsChanged(this);
        }
    };

    public EnderGrimoireInscriptionMenu(int containerId, Inventory inventory) {
        super(MenuRegistry.ENDER_GRIMOIRE_INSCRIPTION.get(), containerId);
        this.player = inventory.player;

        addPlayerInventory(inventory);
        addPlayerHotbar(inventory);

        this.scrollSlot = this.addSlot(new Slot(scrollContainer, 0, 17, 53) {
            @Override
            public boolean mayPlace(@NotNull ItemStack stack) {
                return stack.is(io.redspace.ironsspellbooks.registries.ItemRegistry.SCROLL.get());
            }
        });
        this.resultSlot = this.addSlot(new Slot(resultContainer, 1, 208, 136) {
            @Override
            public boolean mayPlace(@NotNull ItemStack stack) {
                return false;
            }

            @Override
            public void onTake(@NotNull Player player, @NotNull ItemStack stack) {
                if (isValidSpellIndex(selectedSpellIndex)) {
                    editSpellContainer(container -> container.removeSpellAtIndex(selectedSpellIndex));
                    setupResultSlot();
                }
                super.onTake(player, stack);
            }
        });

        setupResultSlot();
    }

    @Override
    public boolean stillValid(@NotNull Player player) {
        return true;
    }

    @Override
    public void slotsChanged(@NotNull Container container) {
        super.slotsChanged(container);
        setupResultSlot();
    }

    public ISpellContainer getSpellContainer() {
        var data = Capabilities.getEnderGrimoireSpellbookOrNull(player);
        return data == null ? FALLBACK_CONTAINER : data.getSpellContainer();
    }

    public boolean hasSpellContainer() {
        return Capabilities.getEnderGrimoireSpellbookOrNull(player) != null;
    }

    public Slot getScrollSlot() {
        return scrollSlot;
    }

    public boolean hasScrollSlotted() {
        return getScrollSlot().hasItem() && getScrollSlot().getItem().getItem() instanceof Scroll;
    }

    @Override
    public boolean clickMenuButton(@NotNull Player player, int buttonId) {
        if (buttonId == INSCRIBE_BUTTON_ID) {
            if (selectedSpellIndex >= 0 && hasScrollSlotted()) {
                var scrollStack = getScrollSlot().getItem();
                var scrollContainer = ISpellContainer.get(scrollStack);
                if (scrollContainer != null) {
                    var spellData = scrollContainer.getSpellAtIndex(0);
                    if (spellData != SpellData.EMPTY && !MinecraftForge.EVENT_BUS.post(new InscribeSpellEvent(player, spellData))) {
                        doInscription();
                    }
                }
            }
            return true;
        }

        setSelectedSpell(buttonId);
        return true;
    }

    private void doInscription() {
        if (!isValidSpellIndex(selectedSpellIndex) || !hasScrollSlotted()) {
            return;
        }

        var targetContainer = getSpellContainer();
        if (targetContainer.getSpellAtIndex(selectedSpellIndex) != SpellData.EMPTY) {
            return;
        }

        var scrollStack = getScrollSlot().getItem();
        var scrollSpellContainer = ISpellContainer.get(scrollStack);
        if (scrollSpellContainer == null) {
            return;
        }

        var scrollSpell = scrollSpellContainer.getSpellAtIndex(0);
        if (scrollSpell == SpellData.EMPTY) {
            return;
        }

        var inscribed = editSpellContainer(container ->
                container.addSpellAtIndex(scrollSpell.getSpell(), scrollSpell.getLevel(), selectedSpellIndex, false));
        if (inscribed) {
            getScrollSlot().remove(1);
            setupResultSlot();
            broadcastChanges();
        }
    }

    private void setSelectedSpell(int index) {
        selectedSpellIndex = isValidSpellIndex(index) ? index : -1;
        setupResultSlot();
    }

    private boolean isValidSpellIndex(int index) {
        return index >= 0 && index < getSpellContainer().getMaxSpellCount();
    }

    private void setupResultSlot() {
        ItemStack resultStack = ItemStack.EMPTY;
        if (isValidSpellIndex(selectedSpellIndex)) {
            var spellData = getSpellContainer().getSpellAtIndex(selectedSpellIndex);
            if (spellData != SpellData.EMPTY && spellData.canRemove()) {
                resultStack = new ItemStack(io.redspace.ironsspellbooks.registries.ItemRegistry.SCROLL.get());
                ISpellContainer.createScrollContainer(spellData.getSpell(), spellData.getLevel(), resultStack);
            }
        }

        if (!ItemStack.matches(resultStack, resultSlot.getItem())) {
            resultSlot.set(resultStack);
        }
    }

    private boolean editSpellContainer(Predicate<io.redspace.ironsspellbooks.api.spells.ISpellContainerMutable> editor) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return false;
        }

        var edited = new boolean[]{false};
        serverPlayer.getCapability(Capabilities.ENDER_GRIMOIRE_SPELLBOOK).ifPresent(data -> {
            var mutable = data.getSpellContainer().mutableCopy();
            edited[0] = editor.test(mutable);
            if (edited[0]) {
                data.setSpellContainer(mutable.toImmutable());
                EnderGrimoireSpellbookSync.syncToClient(serverPlayer);
                ElementMaidenRobeSchoolPowerBonusEvents.refresh(serverPlayer);
            }
        });
        return edited[0];
    }

    @Override
    public @NotNull ItemStack quickMoveStack(@NotNull Player player, int index) {
        Slot sourceSlot = slots.get(index);
        if (!sourceSlot.hasItem()) {
            return ItemStack.EMPTY;
        }

        ItemStack sourceStack = sourceSlot.getItem();
        ItemStack sourceStackCopy = sourceStack.copy();

        if (index < VANILLA_FIRST_SLOT_INDEX + VANILLA_SLOT_COUNT) {
            if (!moveItemStackTo(sourceStack, MENU_SLOT_FIRST_INDEX, MENU_SLOT_FIRST_INDEX + MENU_SLOT_COUNT, false)) {
                return ItemStack.EMPTY;
            }
        } else if (index < MENU_SLOT_FIRST_INDEX + MENU_SLOT_COUNT) {
            if (!moveItemStackTo(sourceStack, VANILLA_FIRST_SLOT_INDEX, VANILLA_FIRST_SLOT_INDEX + VANILLA_SLOT_COUNT, false)) {
                return ItemStack.EMPTY;
            }
        } else {
            return ItemStack.EMPTY;
        }

        if (sourceStack.isEmpty()) {
            sourceSlot.set(ItemStack.EMPTY);
        } else {
            sourceSlot.setChanged();
        }

        sourceSlot.onTake(player, sourceStack);
        return sourceStackCopy;
    }

    private void addPlayerInventory(Inventory playerInventory) {
        for (int row = 0; row < 3; ++row) {
            for (int column = 0; column < 9; ++column) {
                this.addSlot(new Slot(playerInventory, column + row * 9 + 9, 8 + column * 18, 84 + row * 18));
            }
        }
    }

    private void addPlayerHotbar(Inventory playerInventory) {
        for (int column = 0; column < 9; ++column) {
            this.addSlot(new Slot(playerInventory, column, 8 + column * 18, 142));
        }
    }

    @Override
    public void removed(@NotNull Player player) {
        super.removed(player);
        clearContainer(player, scrollContainer);
        resultContainer.removeItemNoUpdate(1);
    }
}
