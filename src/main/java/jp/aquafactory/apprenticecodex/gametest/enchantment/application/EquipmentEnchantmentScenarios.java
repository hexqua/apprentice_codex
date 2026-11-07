package jp.aquafactory.apprenticecodex.gametest.enchantment.application;

import jp.aquafactory.apprenticecodex.item.crystalbladedstaff.CrystalBladedStaff;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.fml.ModList;

import static jp.aquafactory.apprenticecodex.gametest.enchantment.support.EnchantmentTestSupport.assertExactEnchantmentSurfaces;
import static jp.aquafactory.apprenticecodex.gametest.enchantment.support.EnchantmentTestSupport.assertMalumMagicCapableWeaponEnchantmentAccepted;
import static jp.aquafactory.apprenticecodex.gametest.enchantment.support.EnchantmentTestSupport.assertRejectedExtraEnchantments;
import static jp.aquafactory.apprenticecodex.gametest.enchantment.support.EnchantmentTestSupport.assertRequiredExtraEnchantments;
import static jp.aquafactory.apprenticecodex.gametest.enchantment.support.EnchantmentTestSupport.expectedRightClickMagicWeaponEnchantments;
import static jp.aquafactory.apprenticecodex.gametest.enchantment.support.EnchantmentTestSupport.rejectedPastelStaffExtraEnchantments;
import static jp.aquafactory.apprenticecodex.gametest.enchantment.support.EnchantmentTestSupport.requiredMalumMagicCapableWeaponEnchantments;
import static jp.aquafactory.apprenticecodex.gametest.enchantment.support.EnchantmentTestSupport.requiredStaffExtraEnchantments;
import static jp.aquafactory.apprenticecodex.gametest.enchantment.support.EnchantmentTestSupport.staffMiningEnchantments;
import static jp.aquafactory.apprenticecodex.gametest.integration.malum.MalumTestSupport.MALUM_ANIMATED;
import static jp.aquafactory.apprenticecodex.gametest.integration.malum.MalumTestSupport.MALUM_HAUNTED;
import static jp.aquafactory.apprenticecodex.gametest.integration.malum.MalumTestSupport.MALUM_MAGIC_CAPABLE_WEAPON;
import static jp.aquafactory.apprenticecodex.gametest.integration.malum.MalumTestSupport.MALUM_MOD_ID;
import java.util.function.Predicate;
import jp.aquafactory.apprenticecodex.item.spellgun.AbstractSpellGunItem;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.enchantment.Enchantment;
import static jp.aquafactory.apprenticecodex.gametest.enchantment.support.EnchantmentTestSupport.allRegisteredEnchantmentIds;
import static jp.aquafactory.apprenticecodex.gametest.enchantment.support.EnchantmentTestSupport.collectAllowedEnchantments;
import static jp.aquafactory.apprenticecodex.gametest.enchantment.support.EnchantmentTestSupport.createArmorProbeStack;
import static jp.aquafactory.apprenticecodex.gametest.enchantment.support.EnchantmentTestSupport.createEnchantedBook;
import static jp.aquafactory.apprenticecodex.gametest.enchantment.support.EnchantmentTestSupport.describeEnchantmentDifference;
import static jp.aquafactory.apprenticecodex.gametest.enchantment.support.EnchantmentTestSupport.expectedSpellGunEnchantments;
import static jp.aquafactory.apprenticecodex.gametest.enchantment.support.EnchantmentTestSupport.getRegisteredItemStacks;

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

    public static void spellGunsKeepExpectedEnchantmentSurfaces(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var expectedBookEnchantments = allRegisteredEnchantmentIds(helper.getLevel().registryAccess());
            var stacks = getRegisteredItemStacks(item -> item instanceof AbstractSpellGunItem);
            helper.assertFalse(stacks.isEmpty(), "No items matched enchantment test category: Spell Gun");

            for (var stack : stacks) {
                var expectedEnchantments = expectedSpellGunEnchantments(stack);
                assertExactEnchantmentSurfaces(
                        helper,
                        stack,
                        expectedEnchantments,
                        expectedEnchantments,
                        expectedEnchantments,
                        expectedBookEnchantments,
                        expectedEnchantments,
                        "Spell Gun " + BuiltInRegistries.ITEM.getKey(stack.getItem())
                );
            }
        });
    }

    /**
     * 1.21.1 では防具のバニラ付呪可否が item tag で決まるため、登録済みの全防具を部位ごとの革防具と照合する。
     * 追加のバニラ／MOD エンチャントは個別のバランス検証に委ね、ここでは移植時に欠けやすい部位基準のバニラ面だけを監視する。
     */
    public static void allRegisteredArmorMatchesVanillaEnchantmentSurfaces(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var registryAccess = helper.getLevel().registryAccess();
            var testedArmorCount = 0;
            for (var itemEntry : ItemRegistry.ITEMS.getEntries()) {
                var item = itemEntry.get();
                if (!(item instanceof ArmorItem armorItem) || armorItem.getType() == ArmorItem.Type.BODY) {
                    continue;
                }

                var stack = new ItemStack(item);
                var referenceStack = createArmorProbeStack(stack);
                assertVanillaArmorEnchantmentSurface(helper, registryAccess, stack, referenceStack, "primary",
                        enchantment -> item.isPrimaryItemFor(stack, enchantment),
                        enchantment -> referenceStack.getItem().isPrimaryItemFor(referenceStack, enchantment));
                assertVanillaArmorEnchantmentSurface(helper, registryAccess, stack, referenceStack, "supported",
                        enchantment -> item.supportsEnchantment(stack, enchantment),
                        enchantment -> referenceStack.getItem().supportsEnchantment(referenceStack, enchantment));
                assertVanillaArmorEnchantmentSurface(helper, registryAccess, stack, referenceStack, "definition",
                        enchantment -> enchantment.value().canEnchant(stack),
                        enchantment -> enchantment.value().canEnchant(referenceStack));
                assertVanillaArmorEnchantmentSurface(helper, registryAccess, stack, referenceStack, "book",
                        enchantment -> item.isBookEnchantable(stack, createEnchantedBook(enchantment)),
                        enchantment -> referenceStack.getItem().isBookEnchantable(referenceStack, createEnchantedBook(enchantment)));
                testedArmorCount++;
            }
            helper.assertTrue(testedArmorCount > 0, "No Apprentice's Codex armor was found for vanilla enchantment checks");
        });
    }

    private static void assertVanillaArmorEnchantmentSurface(
            GameTestHelper helper,
            RegistryAccess registryAccess,
            ItemStack stack,
            ItemStack referenceStack,
            String surface,
            Predicate<Holder<Enchantment>> actualPredicate,
            Predicate<Holder<Enchantment>> expectedPredicate
    ) {
        var expected = collectAllowedEnchantments(registryAccess,
                enchantment -> isVanillaEnchantment(enchantment) && expectedPredicate.test(enchantment));
        var actual = collectAllowedEnchantments(registryAccess,
                enchantment -> isVanillaEnchantment(enchantment) && actualPredicate.test(enchantment));
        helper.assertTrue(actual.equals(expected),
                "Vanilla armor enchantment " + surface + " differs from " + referenceStack.getItem()
                        + " for " + stack.getItem() + ": " + describeEnchantmentDifference(expected, actual));
    }

    private static boolean isVanillaEnchantment(Holder<Enchantment> enchantment) {
        return enchantment.unwrapKey().map(key -> key.location().getNamespace().equals(ResourceLocation.DEFAULT_NAMESPACE))
                .orElse(false);
    }
}
