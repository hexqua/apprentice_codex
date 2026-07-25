package jp.aquafactory.apprenticecodex.gametest;

import com.mojang.authlib.GameProfile;
import jp.aquafactory.apprenticecodex.config.ApprenticeCodexServerConfig;
import jp.aquafactory.apprenticecodex.item.InventoryInsertTarget;
import jp.aquafactory.apprenticecodex.item.curios.spellcasterammopouch.SpellcasterAmmoPouch;
import jp.aquafactory.apprenticecodex.item.curios.spellcasterquiver.SpellcasterQuiver;
import jp.aquafactory.apprenticecodex.item.luminousdevice.LuminousDevice;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import jp.aquafactory.apprenticecodex.utility.PotionContentsHelper;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.common.util.FakePlayer;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

final class InventoryInsertHintGameTestScenarios {
    private InventoryInsertHintGameTestScenarios() {
    }

    static void inventoryInsertHintsMatchStorageRules(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = new FakePlayer(
                    helper.getLevel(),
                    new GameProfile(UUID.randomUUID(), "inventory_insert_hint_test")
            );

            try (var ignored = ApprenticeCodexServerConfig.useLuminousDeviceConfigOverrideForGameTest(2, 2000)) {
                verifyLuminousDeviceHints(helper, player);
                verifyQuiverHints(helper, player);
                verifyAmmoPouchHints(helper, player);
                verifyRestrictedSlotsRejectInsertion(helper, player);
            }
        });
    }

    private static void verifyLuminousDeviceHints(GameTestHelper helper, FakePlayer player) {
        var deviceStack = new ItemStack(ItemRegistry.LUMINOUS_DEVICE.get());
        helper.assertTrue(resolveHint(deviceStack, new ItemStack(Items.TORCH), player)
                        == InventoryInsertTarget.InsertHint.ITEM,
                "Luminous Device should advertise supported items while it has space");
        helper.assertTrue(resolveHint(deviceStack, new ItemStack(Items.DIRT), player)
                        == InventoryInsertTarget.InsertHint.NONE,
                "Luminous Device should not advertise unsupported items");

        LuminousDevice.addToDevice(deviceStack, new ItemStack(Items.TORCH, 2));
        helper.assertTrue(resolveHint(deviceStack, new ItemStack(Items.LANTERN), player)
                        == InventoryInsertTarget.InsertHint.NONE,
                "A full Luminous Device should not advertise item insertion");

        var manaPotion = PotionContentsHelper.createPotionStack(
                Items.POTION,
                io.redspace.ironsspellbooks.registries.PotionRegistry.INSTANT_MANA_ONE.get()
        );
        helper.assertTrue(resolveHint(deviceStack, manaPotion, player)
                        == InventoryInsertTarget.InsertHint.MANA,
                "Luminous Device should advertise mana refill independently from item capacity");
        LuminousDevice.setStoredMana(deviceStack, 2000);
        helper.assertTrue(resolveHint(deviceStack, manaPotion, player)
                        == InventoryInsertTarget.InsertHint.NONE,
                "A mana-full Luminous Device should not advertise mana refill");
    }

    private static void verifyQuiverHints(GameTestHelper helper, FakePlayer player) {
        var quiverStack = new ItemStack(ItemRegistry.SPELLCASTER_QUIVER.get());
        helper.assertTrue(resolveHint(quiverStack, new ItemStack(Items.ARROW), player)
                        == InventoryInsertTarget.InsertHint.ITEM,
                "Spellcaster Quiver should advertise arrows while it has space");
        helper.assertTrue(resolveHint(quiverStack, new ItemStack(Items.DIRT), player)
                        == InventoryInsertTarget.InsertHint.NONE,
                "Spellcaster Quiver should not advertise unsupported items");

        SpellcasterQuiver.store(quiverStack, new ItemStack(Items.ARROW, 512));
        helper.assertTrue(resolveHint(quiverStack, new ItemStack(Items.SPECTRAL_ARROW), player)
                        == InventoryInsertTarget.InsertHint.NONE,
                "A full Spellcaster Quiver should not advertise insertion");
    }

    private static void verifyAmmoPouchHints(GameTestHelper helper, FakePlayer player) {
        var pouchStack = new ItemStack(ItemRegistry.SPELLCASTER_AMMO_POUCH.get());
        var ammoItem = ItemRegistry.BASIC_SPELLCASTER_ROUND.get();
        helper.assertTrue(resolveHint(pouchStack, new ItemStack(ammoItem), player)
                        == InventoryInsertTarget.InsertHint.ITEM,
                "Spellcaster Ammo Pouch should advertise supported ammunition while it has space");
        helper.assertTrue(resolveHint(pouchStack, new ItemStack(Items.DIRT), player)
                        == InventoryInsertTarget.InsertHint.NONE,
                "Spellcaster Ammo Pouch should not advertise unsupported items");

        for (var i = 0; i < 16; i++) {
            helper.assertTrue(rightClickStorage(pouchStack, new ItemStack(ammoItem, 64), player, false),
                    "Spellcaster Ammo Pouch should accept ammunition before reaching capacity");
        }
        helper.assertTrue(SpellcasterAmmoPouch.getStoredItemCount(pouchStack) == 1024,
                "Spellcaster Ammo Pouch test setup should reach its capacity");
        helper.assertTrue(resolveHint(pouchStack, new ItemStack(ammoItem), player)
                        == InventoryInsertTarget.InsertHint.NONE,
                "A full Spellcaster Ammo Pouch should not advertise insertion");
    }

    private static void verifyRestrictedSlotsRejectInsertion(GameTestHelper helper, FakePlayer player) {
        var cases = new StorageCase[]{
                new StorageCase(
                        new ItemStack(ItemRegistry.LUMINOUS_DEVICE.get()),
                        new ItemStack(Items.TORCH)
                ),
                new StorageCase(
                        new ItemStack(ItemRegistry.SPELLCASTER_QUIVER.get()),
                        new ItemStack(Items.ARROW)
                ),
                new StorageCase(
                        new ItemStack(ItemRegistry.SPELLCASTER_AMMO_POUCH.get()),
                        new ItemStack(ItemRegistry.BASIC_SPELLCASTER_ROUND.get())
                )
        };

        for (var storageCase : cases) {
            helper.assertFalse(rightClickStorage(
                            storageCase.storageStack(),
                            storageCase.incomingStack(),
                            player,
                            true
                    ),
                    storageCase.storageStack().getHoverName().getString()
                            + " should reject insertion from a restricted slot");
        }
    }

    private static InventoryInsertTarget.InsertHint resolveHint(
            ItemStack storageStack,
            ItemStack incomingStack,
            FakePlayer player
    ) {
        return ((InventoryInsertTarget) storageStack.getItem())
                .getInventoryInsertHint(storageStack, incomingStack, player);
    }

    private static boolean rightClickStorage(
            ItemStack storageStack,
            ItemStack incomingStack,
            FakePlayer player,
            boolean restricted
    ) {
        var storageContainer = new SimpleContainer(storageStack);
        var incomingContainer = new SimpleContainer(incomingStack);
        Slot storageSlot = restricted
                ? new RestrictedSlot(storageContainer)
                : new Slot(storageContainer, 0, 0, 0);
        return storageStack.getItem().overrideOtherStackedOnMe(
                storageStack,
                incomingContainer.getItem(0),
                storageSlot,
                ClickAction.SECONDARY,
                player,
                SlotAccess.forContainer(incomingContainer, 0)
        );
    }

    private record StorageCase(ItemStack storageStack, ItemStack incomingStack) {
    }

    private static final class RestrictedSlot extends Slot {
        private RestrictedSlot(SimpleContainer container) {
            super(container, 0, 0, 0);
        }

        @Override
        public boolean mayPlace(@NotNull ItemStack stack) {
            return false;
        }
    }
}
