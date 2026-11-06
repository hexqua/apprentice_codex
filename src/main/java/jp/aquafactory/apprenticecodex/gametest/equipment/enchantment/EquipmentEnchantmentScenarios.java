package jp.aquafactory.apprenticecodex.gametest.equipment.enchantment;

import jp.aquafactory.apprenticecodex.item.AbstractImbuedMagicWeaponItem;
import jp.aquafactory.apprenticecodex.item.crystalbladedstaff.CrystalBladedStaff;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.fml.ModList;

import static jp.aquafactory.apprenticecodex.gametest.equipment.enchantment.EnchantmentTestSupport.assertExactEnchantmentSurfaces;
import static jp.aquafactory.apprenticecodex.gametest.equipment.enchantment.EnchantmentTestSupport.assertMalumMagicCapableWeaponEnchantmentAccepted;
import static jp.aquafactory.apprenticecodex.gametest.equipment.enchantment.EnchantmentTestSupport.assertRejectedExtraEnchantments;
import static jp.aquafactory.apprenticecodex.gametest.equipment.enchantment.EnchantmentTestSupport.assertRequiredExtraEnchantments;
import static jp.aquafactory.apprenticecodex.gametest.equipment.enchantment.EnchantmentTestSupport.expectedRightClickMagicWeaponEnchantments;
import static jp.aquafactory.apprenticecodex.gametest.equipment.enchantment.EnchantmentTestSupport.rejectedPastelStaffExtraEnchantments;
import static jp.aquafactory.apprenticecodex.gametest.equipment.enchantment.EnchantmentTestSupport.requiredMalumMagicCapableWeaponEnchantments;
import static jp.aquafactory.apprenticecodex.gametest.equipment.enchantment.EnchantmentTestSupport.requiredStaffExtraEnchantments;
import static jp.aquafactory.apprenticecodex.gametest.equipment.enchantment.EnchantmentTestSupport.staffMiningEnchantments;
import static jp.aquafactory.apprenticecodex.gametest.integration.malum.MalumTestSupport.MALUM_ANIMATED;
import static jp.aquafactory.apprenticecodex.gametest.integration.malum.MalumTestSupport.MALUM_HAUNTED;
import static jp.aquafactory.apprenticecodex.gametest.integration.malum.MalumTestSupport.MALUM_MAGIC_CAPABLE_WEAPON;
import static jp.aquafactory.apprenticecodex.gametest.integration.malum.MalumTestSupport.MALUM_MOD_ID;

final class EquipmentEnchantmentScenarios {
    private EquipmentEnchantmentScenarios() {}

    static void chargedTwinBladeStaffAndManaForceBladeAcceptMalumMagicCapableEnchantments(GameTestHelper helper) {
        helper.succeedIf(() -> {
            if (!ModList.get().isLoaded(MALUM_MOD_ID)) {
                return;
            }

            var enchantmentLookup = helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
            var haunted = enchantmentLookup.get(ResourceKey.create(Registries.ENCHANTMENT, MALUM_HAUNTED)).orElse(null);
            var animated = enchantmentLookup.get(ResourceKey.create(Registries.ENCHANTMENT, MALUM_ANIMATED)).orElse(null);
            helper.assertTrue(haunted != null, "Missing malum:haunted enchantment");
            helper.assertTrue(animated != null, "Missing malum:animated enchantment");
            if (haunted == null || animated == null) {
                return;
            }

            assertMalumMagicCapableWeaponEnchantmentAccepted(
                    helper,
                    new ItemStack(ItemRegistry.CHARGED_TWIN_BLADE_STAFF.get()),
                    haunted,
                    "Charged Twin Blade Staff haunted"
            );
            assertMalumMagicCapableWeaponEnchantmentAccepted(
                    helper,
                    new ItemStack(ItemRegistry.CHARGED_TWIN_BLADE_STAFF.get()),
                    animated,
                    "Charged Twin Blade Staff animated"
            );
            assertMalumMagicCapableWeaponEnchantmentAccepted(
                    helper,
                    new ItemStack(ItemRegistry.MANA_FORCE_BLADE.get()),
                    haunted,
                    "Mana Force Blade haunted"
            );
            assertMalumMagicCapableWeaponEnchantmentAccepted(
                    helper,
                    new ItemStack(ItemRegistry.MANA_FORCE_BLADE.get()),
                    animated,
                    "Mana Force Blade animated"
            );
        });
    }

    static void pastelStaffUsesSwordEnchantments(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var stack = new ItemStack(ItemRegistry.PASTEL_STAFF.get());
            assertRequiredExtraEnchantments(
                    helper,
                    stack,
                    requiredStaffExtraEnchantments(),
                    null,
                    "Pastel Staff required extra enchantment"
            );
            assertRejectedExtraEnchantments(
                    helper,
                    stack,
                    staffMiningEnchantments(),
                    null,
                    "Pastel Staff should reject mining enchantment"
            );
            assertRejectedExtraEnchantments(
                    helper,
                    stack,
                    rejectedPastelStaffExtraEnchantments(),
                    null,
                    "Pastel Staff should keep rejecting"
            );

            if (ModList.get().isLoaded(MALUM_MOD_ID)) {
                helper.assertTrue(stack.is(MALUM_MAGIC_CAPABLE_WEAPON),
                        "Pastel Staff is missing malum:magic_capable_weapon");
                assertRequiredExtraEnchantments(
                        helper,
                        stack,
                        requiredMalumMagicCapableWeaponEnchantments(),
                        null,
                        "Pastel Staff malum extra enchantment"
                );
            }
        });
    }

    static void multicastEchoStaffUsesSwordEnchantments(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var stack = new ItemStack(ItemRegistry.MULTICAST_ECHO_STAFF.get());
            assertRequiredExtraEnchantments(
                    helper,
                    stack,
                    requiredStaffExtraEnchantments(),
                    null,
                    "Multicast Echo Staff required extra enchantment"
            );
            assertRejectedExtraEnchantments(
                    helper,
                    stack,
                    staffMiningEnchantments(),
                    null,
                    "Multicast Echo Staff should reject mining enchantment"
            );
        });
    }

    static void crystalBladedStaffKeepsItsDedicatedEnchantingRules(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var stack = new ItemStack(ItemRegistry.CRYSTAL_BLADED_STAFF.get());
            var item = (CrystalBladedStaff) stack.getItem();
            helper.assertTrue(item instanceof AbstractImbuedMagicWeaponItem,
                    "Crystal Bladed Staff should return to the right-click magic weapon inheritance path");
            assertExactEnchantmentSurfaces(
                    helper,
                    stack,
                    expectedRightClickMagicWeaponEnchantments(helper.getLevel().registryAccess(), stack),
                    "Crystal Bladed Staff"
            );

            helper.assertTrue(item.isValidRepairItem(stack, new ItemStack(Items.DIAMOND)),
                    "Crystal Bladed Staff should keep accepting diamonds as its repair material");
        });
    }
}
