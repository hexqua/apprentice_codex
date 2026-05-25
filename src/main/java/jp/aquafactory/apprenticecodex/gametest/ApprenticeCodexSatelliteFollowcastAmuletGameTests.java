package jp.aquafactory.apprenticecodex.gametest;

import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import io.redspace.ironsspellbooks.item.SpellSlotUpgradeItem;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.config.ApprenticeCodexServerConfig;
import jp.aquafactory.apprenticecodex.item.curios.satellitefollowcastamulet.SatelliteFollowcastAmulet;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import jp.aquafactory.apprenticecodex.registry.SpellRegistry;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.List;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class ApprenticeCodexSatelliteFollowcastAmuletGameTests {
    private static final String TEMPLATE = "gametest/basic_floor";
    private static final String DENYLIST_CONFIG_BATCH =
            "apprenticecodex.satellite_followcast_amulet_denylist_config";

    private ApprenticeCodexSatelliteFollowcastAmuletGameTests() {
    }

    @GameTest(template = TEMPLATE)
    public static void satelliteFollowcastAmuletAcceptsDispenserProfileAndRejectsSharedDenylists(GameTestHelper helper) {
        var amulet = (SatelliteFollowcastAmulet) ItemRegistry.SATELLITE_FOLLOWCAST_AMULET.get();

        helper.assertTrue(amulet.canImbueSpell(SpellRegistry.MAGE_LIGHT.get(), 1),
                "Satellite Followcast Amulet should accept profiled Spell Dispenser spells.");
        helper.assertFalse(amulet.canImbueSpell(SpellRegistry.AUTO_MAGNET.get(), 1),
                "Satellite Followcast Amulet should reject Spell Dispenser data-driven denylisted spells.");

        helper.succeed();
    }

    @GameTest(template = TEMPLATE, batch = DENYLIST_CONFIG_BATCH)
    public static void satelliteFollowcastAmuletServerDenylistBlocksImbue(GameTestHelper helper) {
        try (var ignored = ApprenticeCodexServerConfig.useSatelliteFollowcastAmuletSpellDenylistOverrideForGameTest(
                List.of(SpellRegistry.MAGE_LIGHT.get().getSpellId())
        )) {
            var amulet = (SatelliteFollowcastAmulet) ItemRegistry.SATELLITE_FOLLOWCAST_AMULET.get();
            helper.assertFalse(amulet.canImbueSpell(SpellRegistry.MAGE_LIGHT.get(), 1),
                    "Satellite Followcast Amulet server denylist should block otherwise supported spells.");
        }

        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void satelliteFollowcastAmuletLesserUpgradeStopsAtTwoSlots(GameTestHelper helper) {
        var amulet = (SatelliteFollowcastAmulet) ItemRegistry.SATELLITE_FOLLOWCAST_AMULET.get();
        var stack = new ItemStack(amulet);
        amulet.initializeSpellContainer(stack);

        var upgradeItem = (SpellSlotUpgradeItem) io.redspace.ironsspellbooks.registries.ItemRegistry.LESSER_SPELL_SLOT_UPGRADE.get();
        var upgraded = amulet.createSpellSlotUpgradeResult(stack, upgradeItem);
        helper.assertFalse(upgraded.isEmpty(), "Satellite Followcast Amulet should accept the first lesser slot upgrade.");
        helper.assertTrue(ISpellContainer.get(upgraded).getMaxSpellCount() == SatelliteFollowcastAmulet.MAX_SPELL_SLOTS,
                "Satellite Followcast Amulet should upgrade to exactly two spell slots.");

        var rejected = amulet.createSpellSlotUpgradeResult(upgraded, upgradeItem);
        helper.assertTrue(rejected.isEmpty(), "Satellite Followcast Amulet should reject upgrades beyond two spell slots.");

        helper.succeed();
    }
}
