package jp.aquafactory.apprenticecodex.gametest.equipment.smashcastscepter;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.gametest.equipment.rightclickweapon.RightClickMagicWeaponGameTests;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import static jp.aquafactory.apprenticecodex.gametest.equipment.EquipmentTestBatches.TEMPLATE;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class SmashcastScepterLegacyScenarios {
    private SmashcastScepterLegacyScenarios() {}

    @GameTest(template = TEMPLATE)
    public static void smashcastScepterKeepsExpectedStatsAndImbueRules(GameTestHelper helper) {
        RightClickMagicWeaponGameTests.smashcastScepterKeepsExpectedStatsAndImbueRules(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void smashcastScepterFallProtectionKeepsFallDistanceAndCancelsNextFall(GameTestHelper helper) {
        RightClickMagicWeaponGameTests.smashcastScepterFallProtectionKeepsFallDistanceAndCancelsNextFall(helper);
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 80)
    public static void smashcastScepterFallProtectionExpiresAfterGracePeriod(GameTestHelper helper) {
        RightClickMagicWeaponGameTests.smashcastScepterFallProtectionExpiresAfterGracePeriod(helper);
    }
}
