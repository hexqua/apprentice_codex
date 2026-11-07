package jp.aquafactory.apprenticecodex.gametest.enchantment.application;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class EnchantmentApplicationGameTests {
    private EnchantmentApplicationGameTests() {}

    @GameTest(template = "gametest/basic_floor")
    public static void enchantmentApplicationSurfacesKeepExpectedMatrix(GameTestHelper helper) {
        EnchantmentApplicationGameTestScenarios.itemSurfacesKeepExpectedMatrix(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void enchantmentDirectApplicationPoliciesKeepExpectedMatrix(GameTestHelper helper) {
        EnchantmentApplicationGameTestScenarios.directApplicationPoliciesKeepExpectedMatrix(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void enchantmentSpecialApplicationRulesStayExplicit(GameTestHelper helper) {
        EnchantmentApplicationGameTestScenarios.specialApplicationRulesStayExplicit(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void pastelStaffUsesSwordEnchantments(GameTestHelper helper) {
        EquipmentEnchantmentScenarios.pastelStaffUsesSwordEnchantments(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void multicastEchoStaffUsesSwordEnchantments(GameTestHelper helper) {
        EquipmentEnchantmentScenarios.multicastEchoStaffUsesSwordEnchantments(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void crystalBladedStaffKeepsItsDedicatedEnchantingRules(GameTestHelper helper) {
        EquipmentEnchantmentScenarios.crystalBladedStaffKeepsItsDedicatedEnchantingRules(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void chargedTwinBladeStaffAndManaForceBladeAcceptMalumMagicCapableEnchantments(GameTestHelper helper) {
        EquipmentEnchantmentScenarios.chargedTwinBladeStaffAndManaForceBladeAcceptMalumMagicCapableEnchantments(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void spellGunsKeepExpectedEnchantmentSurfaces(GameTestHelper helper) {
        EquipmentEnchantmentScenarios.spellGunsKeepExpectedEnchantmentSurfaces(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void allRegisteredArmorMatchesVanillaEnchantmentSurfaces(GameTestHelper helper) {
        EquipmentEnchantmentScenarios.allRegisteredArmorMatchesVanillaEnchantmentSurfaces(helper);
    }
}
