package jp.aquafactory.apprenticecodex.gametest;

import io.redspace.ironsspellbooks.api.magic.MagicData;
import jp.aquafactory.apprenticecodex.compat.create.MagiCompressorGadgetAirBridge;
import jp.aquafactory.apprenticecodex.config.ApprenticeCodexServerConfig;
import jp.aquafactory.apprenticecodex.item.curios.CuriosSlotConstants;
import jp.aquafactory.apprenticecodex.item.curios.magicompressorgadget.MagiCompressorGadget;
import jp.aquafactory.apprenticecodex.item.curios.magicompressorgadget.MagiCompressorGadgetChargeManager;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Map;

final class MagiCompressorGadgetGameTestScenarios extends ApprenticeCodexGameTestScenarios {
    private static final TagKey<Item> CURIOS_BELT = TagKey.create(
            Registries.ITEM,
            ResourceLocation.fromNamespaceAndPath("curios", CuriosSlotConstants.BELT)
    );

    private MagiCompressorGadgetGameTestScenarios() {
    }

    static void magiCompressorGadgetUsesBeltSlotAndDedicatedImplementation(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var stack = new ItemStack(ItemRegistry.MAGI_COMPRESSOR_GADGET.get());
            helper.assertTrue(stack.is(CURIOS_BELT),
                    "Magi-Compressor Gadget should be tagged for the Curios belt slot");
            helper.assertTrue(stack.getItem() instanceof MagiCompressorGadget,
                    "Magi-Compressor Gadget should resolve to the dedicated curio item implementation");
        });
    }

    static void magiCompressorGadgetCreateUnavailableDoesNotSpendManaOrStoreAir(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = createGadgetTestPlayer(helper, "magi_compressor_no_create_test");
            var magicData = magicData(helper, player, "Create unavailable");
            magicData.setMana(100.0F);
            var stack = new ItemStack(ItemRegistry.MAGI_COMPRESSOR_GADGET.get());

            MagiCompressorGadgetAirBridge.setForceUnavailableForGameTest(true);
            try (var ignored = ApprenticeCodexServerConfig.useMagiCompressorGadgetConfigOverrideForGameTest(
                    40.0D,
                    5.0D,
                    50.0D
            )) {
                var converted = MagiCompressorGadgetChargeManager.convertManaToAir(player, stack);
                helper.assertFalse(converted,
                        "Magi-Compressor Gadget should not convert mana when Create air bridge is unavailable");
                helper.assertTrue(Math.abs(magicData.getMana() - 100.0F) < 1.0e-4F,
                        "Magi-Compressor Gadget should not spend mana without Create: " + magicData.getMana());
                helper.assertTrue(Math.abs(MagiCompressorGadgetAirBridge.getStoredAir(stack)) < 1.0e-4F,
                        "Magi-Compressor Gadget should not store air without Create");
            } finally {
                MagiCompressorGadgetAirBridge.setForceUnavailableForGameTest(false);
            }
        });
    }

    static void magiCompressorGadgetConvertsConfiguredManaIntoAir(GameTestHelper helper) {
        if (!ModList.get().isLoaded(CREATE_MOD_ID)) {
            helper.succeed();
            return;
        }

        helper.succeedIf(() -> {
            var player = createGadgetTestPlayer(helper, "magi_compressor_config_test");
            var magicData = magicData(helper, player, "config");
            magicData.setMana(100.0F);
            var stack = new ItemStack(ItemRegistry.MAGI_COMPRESSOR_GADGET.get());

            try (var ignored = ApprenticeCodexServerConfig.useMagiCompressorGadgetConfigOverrideForGameTest(
                    24.0D,
                    6.0D,
                    30.0D
            )) {
                var maxAir = MagiCompressorGadgetAirBridge.getMaxAir(stack);
                helper.assertTrue(Math.abs(maxAir - 30.0F) < 1.0e-4F,
                        "Magi-Compressor Gadget should use configured air capacity: " + maxAir);

                var converted = MagiCompressorGadgetChargeManager.convertManaToAir(player, stack);
                helper.assertTrue(converted, "Magi-Compressor Gadget should convert mana into compressed air");
            }

            helper.assertTrue(Math.abs(magicData.getMana() - 88.0F) < 1.0e-4F,
                    "Magi-Compressor Gadget should spend configured mana: " + magicData.getMana());
            var expectedAir = 3.0F;
            helper.assertTrue(Math.abs(MagiCompressorGadgetAirBridge.getStoredAir(stack) - expectedAir) < 1.0e-3F,
                    "Magi-Compressor Gadget should store configured air amount: "
                            + MagiCompressorGadgetAirBridge.getStoredAir(stack) + " expected " + expectedAir);
        });
    }

    static void magiCompressorGadgetConvertsProportionallyUpToMaxAir(GameTestHelper helper) {
        if (!ModList.get().isLoaded(CREATE_MOD_ID)) {
            helper.succeed();
            return;
        }

        helper.succeedIf(() -> {
            var player = createGadgetTestPlayer(helper, "magi_compressor_threshold_test");
            var magicData = magicData(helper, player, "threshold");
            magicData.setMana(100.0F);
            var stack = new ItemStack(ItemRegistry.MAGI_COMPRESSOR_GADGET.get());

            try (var ignored = ApprenticeCodexServerConfig.useMagiCompressorGadgetConfigOverrideForGameTest(
                    20.0D,
                    5.0D,
                    30.0D
            )) {
                var maxAir = MagiCompressorGadgetAirBridge.getMaxAir(stack);
                MagiCompressorGadgetAirBridge.setStoredAir(stack, maxAir - 1.0F);

                var converted = MagiCompressorGadgetChargeManager.convertManaToAir(player, stack);
                helper.assertTrue(converted,
                        "Magi-Compressor Gadget should convert the remaining air up to max capacity");
            }

            helper.assertTrue(Math.abs(magicData.getMana() - 96.0F) < 1.0e-4F,
                    "Magi-Compressor Gadget should spend proportional mana near capacity: " + magicData.getMana());
            helper.assertTrue(Math.abs(MagiCompressorGadgetAirBridge.getStoredAir(stack) - 30.0F) < 1.0e-3F,
                    "Magi-Compressor Gadget should fill exactly to max capacity");
        });
    }

    static void magiCompressorGadgetInsufficientManaDoesNotStoreAir(GameTestHelper helper) {
        if (!ModList.get().isLoaded(CREATE_MOD_ID)) {
            helper.succeed();
            return;
        }

        helper.succeedIf(() -> {
            var player = createGadgetTestPlayer(helper, "magi_compressor_low_mana_test");
            var magicData = magicData(helper, player, "low mana");
            magicData.setMana(5.0F);
            var stack = new ItemStack(ItemRegistry.MAGI_COMPRESSOR_GADGET.get());

            try (var ignored = ApprenticeCodexServerConfig.useMagiCompressorGadgetConfigOverrideForGameTest(
                    24.0D,
                    6.0D,
                    30.0D
            )) {
                var converted = MagiCompressorGadgetChargeManager.convertManaToAir(player, stack);
                helper.assertFalse(converted,
                        "Magi-Compressor Gadget should not convert when mana is insufficient");
            }

            helper.assertTrue(Math.abs(magicData.getMana() - 5.0F) < 1.0e-4F,
                    "Magi-Compressor Gadget should not spend insufficient mana: " + magicData.getMana());
            helper.assertTrue(Math.abs(MagiCompressorGadgetAirBridge.getStoredAir(stack)) < 1.0e-4F,
                    "Magi-Compressor Gadget should not store air without enough mana");
        });
    }

    static void magiCompressorGadgetRejectsEnchantments(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var stack = new ItemStack(ItemRegistry.MAGI_COMPRESSOR_GADGET.get());
            helper.assertFalse(stack.isEnchantable(),
                    "Magi-Compressor Gadget should not be enchantable");
            helper.assertFalse(stack.getItem().canApplyAtEnchantingTable(stack, Enchantments.UNBREAKING),
                    "Magi-Compressor Gadget should reject vanilla enchanting table enchantments");

            var book = new ItemStack(Items.ENCHANTED_BOOK);
            EnchantmentHelper.setEnchantments(Map.of(Enchantments.UNBREAKING, 1), book);
            helper.assertFalse(stack.getItem().isBookEnchantable(stack, book),
                    "Magi-Compressor Gadget should reject enchanted books");

            var capacity = ForgeRegistries.ENCHANTMENTS.getValue(
                    ResourceLocation.fromNamespaceAndPath(CREATE_MOD_ID, "capacity")
            );
            if (capacity != null) {
                helper.assertFalse(stack.getItem().canApplyAtEnchantingTable(stack, capacity),
                        "Magi-Compressor Gadget should reject Create Capacity");
            }
        });
    }

    private static net.minecraftforge.common.util.FakePlayer createGadgetTestPlayer(
            GameTestHelper helper,
            String profileName
    ) {
        var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), profileName);
        equipCurio(player, CuriosSlotConstants.BELT, new ItemStack(ItemRegistry.MAGI_COMPRESSOR_GADGET.get()));
        return player;
    }

    private static MagicData magicData(
            GameTestHelper helper,
            net.minecraftforge.common.util.FakePlayer player,
            String label
    ) {
        var magicData = MagicData.getPlayerMagicData(player);
        helper.assertTrue(magicData != null,
                "Magi-Compressor Gadget " + label + " test could not resolve player mana data");
        return magicData;
    }
}
