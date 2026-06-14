package jp.aquafactory.apprenticecodex.gametest;

import io.redspace.ironsspellbooks.api.magic.MagicData;
import jp.aquafactory.apprenticecodex.compat.create.MagiCompressorGadgetAirBridge;
import jp.aquafactory.apprenticecodex.config.ApprenticeCodexServerConfig;
import jp.aquafactory.apprenticecodex.item.curios.CuriosSlotConstants;
import jp.aquafactory.apprenticecodex.item.curios.magicompressorgadget.MagiCompressorGadget;
import jp.aquafactory.apprenticecodex.item.curios.magicompressorgadget.MagiCompressorGadgetChargeManager;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.common.util.FakePlayer;

import java.util.List;

final class MagiCompressorGadgetGameTestScenarios extends ApprenticeCodexGameTestScenarios {
    private static final String AIR_TAG = "Air";
    private static final String CREATE_BACKTANK_UTIL_CLASS =
            "com.simibubi.create.content.equipment.armor.BacktankUtil";
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

    static void magiCompressorGadgetBacktankSupplierClampsLegacyAirTag(GameTestHelper helper) {
        if (!ModList.get().isLoaded(CREATE_MOD_ID)) {
            helper.succeed();
            return;
        }

        helper.succeedIf(() -> {
            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0),
                    "magi_compressor_legacy_air_test");
            var stack = new ItemStack(ItemRegistry.MAGI_COMPRESSOR_GADGET.get());

            try (var ignored = ApprenticeCodexServerConfig.useMagiCompressorGadgetConfigOverrideForGameTest(
                    20.0D,
                    5.0D,
                    30.0D
            )) {
                var expectedAir = MagiCompressorGadgetAirBridge.getMaxAir(stack);
                MagiCompressorGadgetAirBridge.setStoredAir(stack, expectedAir);
                CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.putFloat(AIR_TAG, 90.0F));
                equipCurio(player, CuriosSlotConstants.BELT, stack);

                var backtanks = getCreateBacktanksWithAir(player);
                helper.assertTrue(backtanks.stream().anyMatch(backtankStack ->
                                backtankStack.getItem() instanceof MagiCompressorGadget),
                        "Magi-Compressor Gadget should be exposed as a Create backtank source");
                helper.assertTrue(Math.abs(readStoredAir(stack) - expectedAir) < 1.0e-3F,
                        "Magi-Compressor Gadget should clamp legacy Air tag before Create consumes it: "
                                + readStoredAir(stack) + " expected " + expectedAir);
            }
        });
    }

    static void magiCompressorGadgetRejectsEnchantments(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var stack = new ItemStack(ItemRegistry.MAGI_COMPRESSOR_GADGET.get());
            helper.assertFalse(stack.isEnchantable(),
                    "Magi-Compressor Gadget should not be enchantable");
            var enchantmentLookup = helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
            var unbreaking = enchantmentLookup.getOrThrow(Enchantments.UNBREAKING);
            helper.assertFalse(stack.getItem().supportsEnchantment(stack, unbreaking),
                    "Magi-Compressor Gadget should reject vanilla enchantments");
            helper.assertFalse(unbreaking.value().canEnchant(stack),
                    "Magi-Compressor Gadget should not be supported by vanilla enchantment definitions");

            var book = createEnchantedBook(unbreaking);
            helper.assertFalse(stack.getItem().isBookEnchantable(stack, book),
                    "Magi-Compressor Gadget should reject enchanted books");

            var capacity = enchantmentLookup.get(net.minecraft.resources.ResourceKey.create(
                    Registries.ENCHANTMENT,
                    ResourceLocation.fromNamespaceAndPath(CREATE_MOD_ID, "capacity")
            )).orElse(null);
            if (capacity != null) {
                helper.assertFalse(stack.getItem().supportsEnchantment(stack, capacity),
                        "Magi-Compressor Gadget should reject Create Capacity");
                helper.assertFalse(capacity.value().canEnchant(stack),
                        "Magi-Compressor Gadget should not be supported by Create Capacity definitions");
            }
        });
    }

    private static FakePlayer createGadgetTestPlayer(
            GameTestHelper helper,
            String profileName
    ) {
        var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), profileName);
        equipCurio(player, CuriosSlotConstants.BELT, new ItemStack(ItemRegistry.MAGI_COMPRESSOR_GADGET.get()));
        return player;
    }

    private static List<ItemStack> getCreateBacktanksWithAir(net.minecraft.world.entity.LivingEntity entity) {
        try {
            var backtankUtilClass = Class.forName(CREATE_BACKTANK_UTIL_CLASS);
            var result = backtankUtilClass
                    .getMethod("getAllWithAir", net.minecraft.world.entity.LivingEntity.class)
                    .invoke(null, entity);
            if (!(result instanceof List<?> rawStacks)) {
                return List.of();
            }

            return rawStacks.stream()
                    .filter(ItemStack.class::isInstance)
                    .map(ItemStack.class::cast)
                    .toList();
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Create BacktankUtil getAllWithAir call failed", exception);
        }
    }

    private static MagicData magicData(
            GameTestHelper helper,
            FakePlayer player,
            String label
    ) {
        var magicData = MagicData.getPlayerMagicData(player);
        helper.assertTrue(magicData != null,
                "Magi-Compressor Gadget " + label + " test could not resolve player mana data");
        return magicData;
    }

    private static float readStoredAir(ItemStack stack) {
        var customData = stack.get(DataComponents.CUSTOM_DATA);
        return customData == null ? 0.0F : customData.copyTag().getFloat(AIR_TAG);
    }
}
