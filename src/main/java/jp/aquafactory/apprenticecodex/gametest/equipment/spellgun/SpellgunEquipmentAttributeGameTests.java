package jp.aquafactory.apprenticecodex.gametest.equipment.spellgun;

import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.registries.UpgradeOrbTypeRegistry;
import java.util.List;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.enchantment.Enchantments;
import jp.aquafactory.apprenticecodex.gametest.crafting.calibration.SpellCalibrationAdjustmentGameTestSupport;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import jp.aquafactory.apprenticecodex.utility.MagicTools;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import static jp.aquafactory.apprenticecodex.gametest.support.ConnectedPlayerTestSupport.createPlayer;
import static jp.aquafactory.apprenticecodex.gametest.support.SpellContainerTestSupport.roundTripItemStack;
import static jp.aquafactory.apprenticecodex.gametest.support.UpgradeTestSupport.createUpgradeData;
import static jp.aquafactory.apprenticecodex.gametest.support.WorkbenchTestSupport.createInitializedPresetStack;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class SpellgunEquipmentAttributeGameTests {
    private SpellgunEquipmentAttributeGameTests() {}

    @GameTest(template = "gametest/basic_floor")
    public static void spellcasterGunsRemoveBaseSpellPowerButKeepSurge(GameTestHelper helper) {
        try (var fixture = createPlayer(helper)) {
            var player = fixture.player();
            prepareAttribute(player, AttributeRegistry.SPELL_POWER);
            for (var item : List.of(ItemRegistry.IRON_SPELLCASTER_GUN.get(), ItemRegistry.COPPER_SPELLCASTER_GUN.get(),
                    ItemRegistry.GOLD_SPELLCASTER_GUN.get(), ItemRegistry.DIAMOND_SPELLCASTER_GUN.get())) {
                var stack = new ItemStack(item);
                equip(player, stack, EquipmentSlot.MAINHAND);
                assertValue(helper, player, AttributeRegistry.SPELL_POWER, expected(2, 0, 0), "Unenchanted " + item);
                stack.enchant(helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT)
                        .getOrThrow(Enchantments.SURGE), 1);
                equip(player, stack.copy(), EquipmentSlot.MAINHAND);
                assertValue(helper, player, AttributeRegistry.SPELL_POWER, expected(2, 0, 0.02), "Surge " + item);
            }
            equip(player, ItemStack.EMPTY, EquipmentSlot.MAINHAND);
            assertValue(helper, player, AttributeRegistry.SPELL_POWER, expected(2, 0, 0), "Removed gun");
        }
        helper.succeed();
    }

    @GameTest(template = "gametest/basic_floor")
    public static void silverSpellAmplifierMovesAllSpellgunAttributesToOffhand(GameTestHelper helper) {
        var spell = SpellRegistry.MAGIC_MISSILE_SPELL.get();
        var school = MagicTools.resolveSchoolPowerAttribute(spell.getSchoolType());
        helper.assertTrue(school != null, "Fixture must resolve the imbued spell's school attribute");
        var cases = List.of(
                new AttributeCase(Enchantments.ALACRITY, AttributeRegistry.COOLDOWN_REDUCTION, 0, 0.02),
                new AttributeCase(Enchantments.REFLUX, AttributeRegistry.MANA_REGEN, 0, 0.05),
                new AttributeCase(Enchantments.RESERVOIR, AttributeRegistry.MAX_MANA, 20, 0),
                new AttributeCase(Enchantments.SURGE, AttributeRegistry.SPELL_POWER, 0, 0.02),
                new AttributeCase(Enchantments.ATTUNEMENT, BuiltInRegistries.ATTRIBUTE.wrapAsHolder(school), 0, 0.03),
                new AttributeCase(Enchantments.TENSE, AttributeRegistry.CAST_TIME_REDUCTION, 0, 0.04));
        for (var attributeCase : cases) {
            try (var fixture = createPlayer(helper)) {
                var player = fixture.player();
                var attribute = attributeCase.attribute();
                double base = prepareAttribute(player, attribute);
                var stack = createInitializedPresetStack(ItemRegistry.IRON_SPELLCASTER_GUN.get());
                stack.enchant(helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT)
                        .getOrThrow(attributeCase.enchantment()), 1);
                double enabled = expected(base, attributeCase.flat(), attributeCase.multipliedBase());
                double disabled = expected(base, 0, 0);
                equip(player, stack, EquipmentSlot.MAINHAND);
                assertValue(helper, player, attribute, enabled, "Unadjusted mainhand");
                equip(player, stack, EquipmentSlot.OFFHAND);
                assertValue(helper, player, attribute, disabled, "Unadjusted offhand");
                calibrate(helper, stack, true);
                equip(player, stack, EquipmentSlot.MAINHAND);
                assertValue(helper, player, attribute, disabled, "Adjusted mainhand");
                equip(player, stack, EquipmentSlot.OFFHAND);
                assertValue(helper, player, attribute, enabled, "Adjusted offhand");
                calibrate(helper, stack, false);
                equip(player, stack.copy(), EquipmentSlot.OFFHAND);
                assertValue(helper, player, attribute, disabled, "Removed calibration offhand");
                equip(player, stack, EquipmentSlot.MAINHAND);
                assertValue(helper, player, attribute, enabled, "Removed calibration mainhand");
                equip(player, ItemStack.EMPTY, EquipmentSlot.MAINHAND);
                assertValue(helper, player, attribute, disabled, "Removed equipment");
            }
        }
        helper.succeed();
    }

    @GameTest(template = "gametest/basic_floor")
    public static void silverSpellAmplifierKeepsDualSpellgunModifiersIndependent(GameTestHelper helper) {
        try (var fixture = createPlayer(helper)) {
            var player = fixture.player();
            prepareAttribute(player, AttributeRegistry.SPELL_POWER);
            var mainhand = createInitializedPresetStack(ItemRegistry.IRON_SPELLCASTER_GUN.get());
            mainhand.enchant(helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT)
                    .getOrThrow(Enchantments.SURGE), 1);
            var offhand = mainhand.copy();
            calibrate(helper, offhand, true);
            player.setItemSlot(EquipmentSlot.MAINHAND, mainhand);
            player.setItemSlot(EquipmentSlot.OFFHAND, offhand);
            player.doTick();
            player.doTick();
            assertValue(helper, player, AttributeRegistry.SPELL_POWER, expected(2, 0, 0.04), "Both hands");
            player.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
            player.doTick();
            assertValue(helper, player, AttributeRegistry.SPELL_POWER, expected(2, 0, 0.02), "Only offhand remains");
            player.setItemSlot(EquipmentSlot.MAINHAND, mainhand.copy());
            player.setItemSlot(EquipmentSlot.OFFHAND, ItemStack.EMPTY);
            player.doTick();
            assertValue(helper, player, AttributeRegistry.SPELL_POWER, expected(2, 0, 0.02), "Only mainhand remains");
            equip(player, ItemStack.EMPTY, EquipmentSlot.MAINHAND);
            assertValue(helper, player, AttributeRegistry.SPELL_POWER, expected(2, 0, 0), "Both removed");
        }
        helper.succeed();
    }

    @GameTest(template = "gametest/basic_floor")
    public static void silverSpellAmplifierMovesUpgradeOrbModifiersToOffhand(GameTestHelper helper) {
        try (var fixture = createPlayer(helper)) {
            var player = fixture.player();
            double base = prepareAttribute(player, AttributeRegistry.MAX_MANA);
            for (boolean orbFirst : new boolean[]{true, false}) {
                var stack = createInitializedPresetStack(ItemRegistry.IRON_SPELLCASTER_GUN.get());
                stack.enchant(helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT)
                        .getOrThrow(Enchantments.RESERVOIR), 1);
                if (orbFirst) addManaOrb(helper, stack);
                calibrate(helper, stack, true);
                if (!orbFirst) addManaOrb(helper, stack);
                for (var candidate : List.of(stack, roundTripItemStack(helper, stack))) {
                    equip(player, candidate, EquipmentSlot.MAINHAND);
                    assertValue(helper, player, AttributeRegistry.MAX_MANA, expected(base, 0, 0), "Adjusted orb mainhand");
                    equip(player, candidate, EquipmentSlot.OFFHAND);
                    assertValue(helper, player, AttributeRegistry.MAX_MANA, expected(base, 70, 0), "Adjusted orb offhand");
                }
                calibrate(helper, stack, false);
                equip(player, stack, EquipmentSlot.OFFHAND);
                assertValue(helper, player, AttributeRegistry.MAX_MANA, expected(base, 0, 0), "Unadjusted orb offhand");
                equip(player, stack, EquipmentSlot.MAINHAND);
                assertValue(helper, player, AttributeRegistry.MAX_MANA, expected(base, 70, 0), "Unadjusted orb mainhand");
            }
            equip(player, ItemStack.EMPTY, EquipmentSlot.MAINHAND);
            assertValue(helper, player, AttributeRegistry.MAX_MANA, expected(base, 0, 0), "Removed orb equipment");
        }
        helper.succeed();
    }

    private static void addManaOrb(GameTestHelper helper, ItemStack stack) {
        createUpgradeData(helper.getLevel().registryAccess(), stack, UpgradeOrbTypeRegistry.MANA, EquipmentSlot.MAINHAND.getName());
    }

    private static void calibrate(GameTestHelper helper, ItemStack stack, boolean enabled) {
        helper.assertTrue(SpellCalibrationAdjustmentGameTestSupport.setCalibrationAdjustment(stack, 0,
                enabled ? new ItemStack(ItemRegistry.SILVER_SPELL_AMPLIFIER.get()) : ItemStack.EMPTY), "Calibration must update");
    }

    private static void equip(ServerPlayer player, ItemStack stack, EquipmentSlot slot) {
        player.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
        player.setItemSlot(EquipmentSlot.OFFHAND, ItemStack.EMPTY);
        player.setItemSlot(slot, stack.copy());
        player.doTick();
    }

    private static double prepareAttribute(ServerPlayer player, Holder<Attribute> attribute) {
        var instance = player.getAttribute(attribute);
        double base = attribute.equals(AttributeRegistry.MAX_MANA) ? 200 : 2;
        instance.setBaseValue(base);
        // 加算・基礎値への乗算・最終乗算を併用し、演算種別の取り違えも実値で検出する。
        instance.addTransientModifier(new AttributeModifier(id("flat"), 3, AttributeModifier.Operation.ADD_VALUE));
        instance.addTransientModifier(new AttributeModifier(id("base"), 0.1, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
        instance.addTransientModifier(new AttributeModifier(id("total"), 0.2, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        return base;
    }

    private static ResourceLocation id(String suffix) {
        return ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "gametest_spellgun_" + suffix);
    }

    private static double expected(double base, double flat, double multipliedBase) {
        return (base + 3 + flat) * (1.1 + multipliedBase) * 1.2;
    }

    private static void assertValue(GameTestHelper helper, ServerPlayer player, Holder<Attribute> attribute,
                                    double expected, String context) {
        double actual = player.getAttributeValue(attribute);
        helper.assertTrue(Math.abs(actual - expected) < 1.0e-6,
                context + " " + attribute + ": expected=" + expected + ", actual=" + actual);
    }

    private record AttributeCase(ResourceKey<Enchantment> enchantment, Holder<Attribute> attribute,
                                 double flat, double multipliedBase) {}
}
