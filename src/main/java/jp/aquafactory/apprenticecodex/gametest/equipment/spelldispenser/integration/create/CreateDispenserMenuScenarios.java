package jp.aquafactory.apprenticecodex.gametest.equipment.spelldispenser.integration.create;

import com.mojang.authlib.GameProfile;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import jp.aquafactory.apprenticecodex.block.spelldispenser.SpellDispenserBlockEntity;
import jp.aquafactory.apprenticecodex.block.spelldispenser.SpellDispenserMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.items.ItemStackHandler;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static jp.aquafactory.apprenticecodex.gametest.equipment.bow.SpellScrollTestSupport.createSpellScroll;

final class CreateDispenserMenuScenarios {
    private CreateDispenserMenuScenarios() {}

    static void spellDispenserMountedMenuKeepsContraptionInventoryAccessible(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = new FakePlayer(helper.getLevel(), new GameProfile(UUID.randomUUID(), "spell_dispenser_mounted_menu_test"));
            var mountedInventory = new ItemStackHandler(SpellDispenserBlockEntity.INVENTORY_SLOT_COUNT);
            var scrollStack = createSpellScroll(SpellRegistry.MAGIC_MISSILE_SPELL.get());
            mountedInventory.setStackInSlot(0, scrollStack.copy());
            var currentMana = new AtomicInteger(320);
            var ownerName = "spell_dispenser_mounted_menu_owner_test";

            var menu = SpellDispenserMenu.createMounted(0, new Inventory(player), BlockPos.ZERO, mountedInventory, true, ownerName, currentMana::get);
            helper.assertTrue(menu.stillValid(player), "Spell Dispenser mounted menu closed because it expected a world block entity");
            helper.assertTrue(ItemStack.isSameItemSameTags(menu.getSpellSource(), scrollStack),
                    "Spell Dispenser mounted menu did not expose the mounted inventory stack");
            helper.assertTrue(menu.isReadyToCast(player),
                    "Spell Dispenser mounted menu did not report a valid mounted scroll as ready");
            helper.assertTrue(menu.getCurrentMana() == 320,
                    "Spell Dispenser mounted menu did not expose the mounted mana value");
            helper.assertTrue(ownerName.equals(menu.getOwnerName()),
                    "Spell Dispenser mounted menu did not expose the mounted owner name");

            currentMana.set(180);
            helper.assertTrue(menu.getCurrentMana() == 180,
                    "Spell Dispenser mounted menu did not follow the live contraption mana source");

            var clientMenu = SpellDispenserMenu.createMounted(1, new Inventory(player), BlockPos.ZERO, mountedInventory, true, ownerName, 320);
            clientMenu.setData(0, 180);
            helper.assertTrue(clientMenu.getCurrentMana() == 180,
                    "Spell Dispenser mounted menu did not accept mana updates from menu data sync");
            helper.assertTrue(ownerName.equals(clientMenu.getOwnerName()),
                    "Spell Dispenser mounted client menu did not retain the synced owner name");
        });
    }

    static void spellDispenserMountedMenuTreatsOwnerOptionalSpellAsReadyWithoutOwnerProfile(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = new FakePlayer(helper.getLevel(), new GameProfile(UUID.randomUUID(), "spell_dispenser_owner_optional_menu_test"));
            var mountedInventory = new ItemStackHandler(SpellDispenserBlockEntity.INVENTORY_SLOT_COUNT);
            mountedInventory.setStackInSlot(0, createSpellScroll(SpellRegistry.MAGIC_MISSILE_SPELL.get()));

            var menu = SpellDispenserMenu.createMounted(0, new Inventory(player), BlockPos.ZERO, mountedInventory, false, null, 320);
            helper.assertTrue(menu.isReadyToCast(player),
                    "Spell Dispenser mounted menu still required an owner for an owner-optional spell");
        });
    }
}
