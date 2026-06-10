package jp.aquafactory.apprenticecodex.gametest;

import io.redspace.ironsspellbooks.api.magic.MagicData;
import jp.aquafactory.apprenticecodex.config.ApprenticeCodexServerConfig;
import jp.aquafactory.apprenticecodex.item.curios.CuriosSlotConstants;
import jp.aquafactory.apprenticecodex.item.curios.manathruster.ManaThruster;
import jp.aquafactory.apprenticecodex.item.curios.manathruster.ManaThrusterFlightManager;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

final class ManaThrusterGameTestScenarios extends ApprenticeCodexGameTestScenarios {
    private static final TagKey<Item> CURIOS_FEET = TagKey.create(
            Registries.ITEM,
            ResourceLocation.fromNamespaceAndPath("curios", CuriosSlotConstants.FEET)
    );

    private ManaThrusterGameTestScenarios() {
    }

    static void manaThrusterUsesFeetSlotAndDedicatedImplementation(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var stack = new ItemStack(ItemRegistry.MANA_THRUSTER.get());
            helper.assertTrue(stack.is(CURIOS_FEET),
                    "Mana Thruster should be tagged for the Curios feet slot");
            helper.assertTrue(stack.getItem() instanceof ManaThruster,
                    "Mana Thruster should resolve to the dedicated curio item implementation");
        });
    }

    static void manaThrusterAppliesFixedThrustAndUsesServerConfigManaCost(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = createManaThrusterTestPlayer(helper, "mana_thruster_config_test");
            var magicData = magicData(helper, player, "config");
            magicData.setMana(50.0F);
            player.setOnGround(false);
            player.setDeltaMovement(new Vec3(0.0D, 0.20D, 0.0D));
            player.fallDistance = 5.0F;

            try (var ignored = ApprenticeCodexServerConfig.useManaThrusterConfigOverrideForGameTest(7.0D)) {
                ManaThrusterFlightManager.setJumpInput(player, true);
                ManaThrusterFlightManager.tickEquippedPlayer(player);
            }

            helper.assertTrue(player.getDeltaMovement().y > 0.20D,
                    "Mana Thruster should apply fixed upward thrust: " + player.getDeltaMovement().y);
            helper.assertTrue(Math.abs(magicData.getMana() - 43.0F) < 1.0e-4F,
                    "Mana Thruster should consume configured mana per tick: " + magicData.getMana());
            helper.assertTrue(Math.abs(player.fallDistance) < 1.0e-4F,
                    "Mana Thruster should reset fall distance on a successful thrust tick");
        });
    }

    static void manaThrusterInsufficientManaDoesNotAccelerate(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = createManaThrusterTestPlayer(helper, "mana_thruster_low_mana_test");
            var magicData = magicData(helper, player, "low mana");
            magicData.setMana(4.0F);
            player.setOnGround(false);
            player.setDeltaMovement(new Vec3(0.0D, -0.35D, 0.0D));
            player.fallDistance = 6.0F;

            try (var ignored = ApprenticeCodexServerConfig.useManaThrusterConfigOverrideForGameTest(5.0D)) {
                ManaThrusterFlightManager.setJumpInput(player, true);
                ManaThrusterFlightManager.tickEquippedPlayer(player);
            }

            helper.assertTrue(Math.abs(player.getDeltaMovement().y + 0.35D) < 1.0e-4D,
                    "Mana Thruster should not accelerate when mana is insufficient: " + player.getDeltaMovement().y);
            helper.assertTrue(Math.abs(magicData.getMana() - 4.0F) < 1.0e-4F,
                    "Mana Thruster should not spend mana when it cannot thrust: " + magicData.getMana());
            helper.assertTrue(Math.abs(player.fallDistance - 6.0F) < 1.0e-4F,
                    "Mana Thruster should not reset fall distance when it cannot thrust");
        });
    }

    static void manaThrusterSuppressesManaRecoveryUntilLanding(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = createManaThrusterTestPlayer(helper, "mana_thruster_regen_test");
            var magicData = magicData(helper, player, "regen suppression");
            magicData.setMana(50.0F);
            player.setOnGround(false);

            try (var ignored = ApprenticeCodexServerConfig.useManaThrusterConfigOverrideForGameTest(5.0D)) {
                ManaThrusterFlightManager.setJumpInput(player, true);
                ManaThrusterFlightManager.tickEquippedPlayer(player);
            }

            helper.assertTrue(ManaThrusterFlightManager.isManaRecoverySuppressed(player),
                    "Mana Thruster should suppress mana recovery after the first successful thrust");
            magicData.setMana(magicData.getMana() + 10.0F);
            helper.assertTrue(Math.abs(magicData.getMana() - 45.0F) < 1.0e-4F,
                    "Mana Thruster should block mana increases before landing: " + magicData.getMana());

            player.setOnGround(true);
            ManaThrusterFlightManager.tickEquippedPlayer(player);
            helper.assertFalse(ManaThrusterFlightManager.isManaRecoverySuppressed(player),
                    "Mana Thruster should clear mana recovery suppression on landing");
            magicData.setMana(magicData.getMana() + 10.0F);
            helper.assertTrue(Math.abs(magicData.getMana() - 55.0F) < 1.0e-4F,
                    "Mana Thruster should allow mana recovery after landing: " + magicData.getMana());
        });
    }

    static void manaThrusterGroundHeldJumpDoesNotStartAfterTakeoff(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = createManaThrusterTestPlayer(helper, "mana_thruster_ground_hold_test");
            var magicData = magicData(helper, player, "ground held jump");
            magicData.setMana(50.0F);
            player.setOnGround(true);
            player.setDeltaMovement(Vec3.ZERO);

            ManaThrusterFlightManager.setJumpInput(player, true);
            player.setOnGround(false);
            ManaThrusterFlightManager.tickEquippedPlayer(player);

            helper.assertTrue(Math.abs(player.getDeltaMovement().y) < 1.0e-4D,
                    "Mana Thruster should not activate from a jump key held before leaving the ground");
            helper.assertTrue(Math.abs(magicData.getMana() - 50.0F) < 1.0e-4F,
                    "Mana Thruster should not spend mana for a ground-held jump: " + magicData.getMana());
        });
    }

    private static net.minecraftforge.common.util.FakePlayer createManaThrusterTestPlayer(
            GameTestHelper helper,
            String profileName
    ) {
        var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), profileName);
        equipCurio(player, CuriosSlotConstants.FEET, new ItemStack(ItemRegistry.MANA_THRUSTER.get()));
        ManaThrusterFlightManager.clear(player);
        return player;
    }

    private static MagicData magicData(
            GameTestHelper helper,
            net.minecraftforge.common.util.FakePlayer player,
            String label
    ) {
        var magicData = MagicData.getPlayerMagicData(player);
        helper.assertTrue(magicData != null, "Mana Thruster " + label + " test could not resolve player mana data");
        return magicData;
    }
}
