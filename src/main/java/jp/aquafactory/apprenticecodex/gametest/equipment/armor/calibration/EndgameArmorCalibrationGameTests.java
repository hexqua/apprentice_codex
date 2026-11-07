package jp.aquafactory.apprenticecodex.gametest.equipment.armor.calibration;

import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import java.util.List;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.gametest.crafting.calibration.SpellCalibrationAdjustmentGameTestSupport;
import jp.aquafactory.apprenticecodex.item.armor.MagiAgentSuitItem;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import static jp.aquafactory.apprenticecodex.gametest.support.PlayerTestSupport.createEquipmentTestPlayer;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class EndgameArmorCalibrationGameTests {
    private EndgameArmorCalibrationGameTests() {}

    @GameTest(template = "gametest/basic_floor")
    public static void antiGravityWorksInEveryArmorSlotAndRestoresAfterRemoval(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var endgameArmor = new ItemStack[]{
                    new ItemStack(ItemRegistry.CHROMATIC_MAGIA_DRESS_HAT.get()),
                    new ItemStack(ItemRegistry.CHROMATIC_MAGIA_DRESS_COAT.get()),
                    new ItemStack(ItemRegistry.CHROMATIC_MAGIA_DRESS_LEGGINGS.get()),
                    new ItemStack(ItemRegistry.CHROMATIC_MAGIA_DRESS_BOOTS.get()),
                    new ItemStack(ItemRegistry.MAGI_AGENT_SUIT_HOOD.get()),
                    new ItemStack(ItemRegistry.MAGI_AGENT_SUIT_COAT.get()),
                    new ItemStack(ItemRegistry.MAGI_AGENT_SUIT_LEGGINGS.get()),
                    new ItemStack(ItemRegistry.MAGI_AGENT_SUIT_BOOTS.get()),
                    new ItemStack(ItemRegistry.ELEMENT_MAIDEN_ROBE_RIBBON.get()),
                    new ItemStack(ItemRegistry.ELEMENT_MAIDEN_ROBE_ROBE.get()),
                    new ItemStack(ItemRegistry.ELEMENT_MAIDEN_ROBE_LEGGINGS.get()),
                    new ItemStack(ItemRegistry.ELEMENT_MAIDEN_ROBE_BOOTS.get())
            };
            try (var fixture = new ArmorEquipmentFixture(helper)) {
                // 非ゼロの基礎値を使い、定義の合算ではなく実装備後の10%減少を確認する。
                fixture.player().getAttribute(Attributes.GRAVITY).setBaseValue(0.08);
                for (var stack : endgameArmor) {
                    var slot = ((ArmorItem) stack.getItem()).getEquipmentSlot();
                    var context = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
                    var weave = new ItemStack(ItemRegistry.ANTI_GRAVITY_WEAVE.get());
                    helper.assertTrue(SpellCalibrationAdjustmentGameTestSupport.setCalibrationAdjustment(stack, 0, weave),
                            "Anti-Gravity Weave should be accepted: " + context);
                    helper.assertFalse(SpellCalibrationAdjustmentGameTestSupport.setCalibrationAdjustment(stack, 1, weave),
                            "Duplicate weave should be rejected: " + context);
                    fixture.equip(stack, slot);
                    fixture.assertAttribute(Attributes.GRAVITY, 0.072, context + " equipped");
                    fixture.equip(stack, slot);
                    fixture.assertAttribute(Attributes.GRAVITY, 0.072, context + " re-equipped");
                    fixture.equip(ItemStack.EMPTY, slot);
                    fixture.assertAttribute(Attributes.GRAVITY, 0.08, context + " removed");
                }
                for (var index = 0; index < 4; ++index) {
                    var stack = endgameArmor[index];
                    fixture.equip(stack, ((ArmorItem) stack.getItem()).getEquipmentSlot());
                }
                fixture.assertAttribute(Attributes.GRAVITY, 0.048, "Four independent armor pieces");
                fixture.equip(ItemStack.EMPTY, EquipmentSlot.HEAD);
                fixture.assertAttribute(Attributes.GRAVITY, 0.056, "One of four pieces removed");
            }
        });
    }

    @GameTest(template = "gametest/basic_floor")
    public static void runeAndPlateAttributesUpdateOnReplacementAndRemoval(GameTestHelper helper) {
        helper.succeedIf(() -> {
            try (var fixture = new ArmorEquipmentFixture(helper)) {
                var nakedMana = fixture.player().getAttributeValue(AttributeRegistry.MAX_MANA);
                var plain = new ItemStack(ItemRegistry.CHROMATIC_MAGIA_DRESS_LEGGINGS.get());
                fixture.equip(plain, EquipmentSlot.LEGS);
                var baseline = fixture.player().getAttributeValue(AttributeRegistry.MAX_MANA);
                var tuned = plain.copy();
                for (var slot = 0; slot < 3; ++slot) {
                    helper.assertTrue(SpellCalibrationAdjustmentGameTestSupport.setCalibrationAdjustment(tuned, slot,
                                    new ItemStack(io.redspace.ironsspellbooks.registries.ItemRegistry.MANA_RUNE.get())),
                            "Mana Rune should be repeatable");
                }
                fixture.equip(tuned, EquipmentSlot.LEGS);
                fixture.assertAttribute(AttributeRegistry.MAX_MANA, baseline + 75, "Three stored mana runes");
                fixture.equip(plain, EquipmentSlot.LEGS);
                fixture.assertAttribute(AttributeRegistry.MAX_MANA, baseline, "Tuned leggings replaced");
                fixture.equip(tuned, EquipmentSlot.LEGS);
                fixture.assertAttribute(AttributeRegistry.MAX_MANA, baseline + 75, "Tuned leggings re-equipped");
                fixture.equip(ItemStack.EMPTY, EquipmentSlot.LEGS);
                fixture.assertAttribute(AttributeRegistry.MAX_MANA, nakedMana, "Mana leggings removed");
                var nakedResistance = fixture.player().getAttributeValue(AttributeRegistry.SPELL_RESIST);
                var coat = new ItemStack(ItemRegistry.MAGI_AGENT_SUIT_COAT.get());
                fixture.equip(coat, EquipmentSlot.CHEST);
                var resistance = fixture.player().getAttributeValue(AttributeRegistry.SPELL_RESIST);
                var rune = new ItemStack(io.redspace.ironsspellbooks.registries.ItemRegistry.PROTECTION_RUNE.get());
                helper.assertTrue(SpellCalibrationAdjustmentGameTestSupport.setCalibrationAdjustment(coat, 0, rune),
                        "Protection Rune should be accepted");
                helper.assertFalse(SpellCalibrationAdjustmentGameTestSupport.setCalibrationAdjustment(coat, 1, rune),
                        "Protection Rune should remain unique within a piece");
                fixture.equip(coat, EquipmentSlot.CHEST);
                fixture.assertAttribute(AttributeRegistry.SPELL_RESIST, resistance + 0.05, "Protection Rune equipped");
                fixture.equip(ItemStack.EMPTY, EquipmentSlot.CHEST);
                fixture.assertAttribute(AttributeRegistry.SPELL_RESIST, nakedResistance, "Protection Rune removed");
                var boots = new ItemStack(ItemRegistry.ELEMENT_MAIDEN_ROBE_BOOTS.get());
                var baseBlast = fixture.player().getAttributeValue(Attributes.EXPLOSION_KNOCKBACK_RESISTANCE);
                var baseKnockback = fixture.player().getAttributeValue(Attributes.KNOCKBACK_RESISTANCE);
                for (var adjustment : List.of(ItemRegistry.BLAST_REACTIVE_PLATE.get(),
                        ItemRegistry.WIND_ACCUMULATION_WEAVE.get(), ItemRegistry.SHOCK_ABSORPTION_PLATE.get())) {
                    var slot = adjustment == ItemRegistry.BLAST_REACTIVE_PLATE.get() ? 0
                            : adjustment == ItemRegistry.WIND_ACCUMULATION_WEAVE.get() ? 1 : 2;
                    helper.assertTrue(SpellCalibrationAdjustmentGameTestSupport.setCalibrationAdjustment(
                                    boots, slot, new ItemStack(adjustment)), "Distinct plates should coexist");
                }
                fixture.equip(boots, EquipmentSlot.FEET);
                fixture.assertAttribute(Attributes.EXPLOSION_KNOCKBACK_RESISTANCE, baseBlast + 0.05,
                        "Blast and wind combine additively");
                fixture.assertAttribute(Attributes.KNOCKBACK_RESISTANCE, baseKnockback + 0.1,
                        "Shock Absorption Plate equipped");
                fixture.equip(ItemStack.EMPTY, EquipmentSlot.FEET);
                fixture.assertAttribute(Attributes.EXPLOSION_KNOCKBACK_RESISTANCE, baseBlast, "Plates removed");
                fixture.assertAttribute(Attributes.KNOCKBACK_RESISTANCE, baseKnockback, "Plates removed");
            }
        });
    }

    @GameTest(template = "gametest/basic_floor")
    public static void armorAdjustmentsRetainSlotSpecificCapabilities(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var scrollLeggings = new ItemStack(ItemRegistry.ELEMENT_MAIDEN_ROBE_LEGGINGS.get());
            helper.assertFalse(ISpellContainer.isSpellContainer(scrollLeggings),
                    "Scrollwoven armor should start without a spell container");
            helper.assertTrue(SpellCalibrationAdjustmentGameTestSupport.setCalibrationAdjustment(
                            scrollLeggings, 0, new ItemStack(ItemRegistry.SCROLLWOVEN_PARCHMENT.get())),
                    "Scrollwoven Parchment should be accepted by non-chest armor");
            helper.assertFalse(ISpellContainer.isSpellContainer(scrollLeggings),
                    "Scrollwoven Parchment should use independent storage instead of a spell container");
            helper.assertFalse(SpellCalibrationAdjustmentGameTestSupport.canPlaceCalibrationAdjustment(
                            new ItemStack(ItemRegistry.ELEMENT_MAIDEN_ROBE_ROBE.get()),
                            0,
                            new ItemStack(ItemRegistry.SCROLLWOVEN_PARCHMENT.get())),
                    "Chest armor should reject Scrollwoven Parchment");

            var leatherBootsAdjustment = new ItemStack(ItemRegistry.CHROMATIC_MAGIA_DRESS_BOOTS.get());
            helper.assertTrue(SpellCalibrationAdjustmentGameTestSupport.setCalibrationAdjustment(
                            leatherBootsAdjustment, 0, new ItemStack(Items.LEATHER_BOOTS)),
                    "Endgame boots should accept Leather Boots");
            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "endgame_armor_powder_snow_test");
            helper.assertTrue(((ArmorItem) leatherBootsAdjustment.getItem())
                            .canWalkOnPowderedSnow(leatherBootsAdjustment, player),
                    "Leather Boots adjustment should allow walking on powder snow");
            helper.assertFalse(leatherBootsAdjustment.is(ItemTags.FREEZE_IMMUNE_WEARABLES),
                    "Leather Boots adjustment should not grant freeze immunity");

            var fireRune = new ItemStack(io.redspace.ironsspellbooks.registries.ItemRegistry.FIRE_RUNE.get());
            var suitHood = new ItemStack(ItemRegistry.MAGI_AGENT_SUIT_HOOD.get());
            helper.assertTrue(SpellCalibrationAdjustmentGameTestSupport.setCalibrationAdjustment(suitHood, 2, fireRune),
                    "Magi Agent Suit should accept its school rune in the third slot");
            helper.assertTrue(MagiAgentSuitItem.getResolvedCalibrationSchool(suitHood) != null,
                    "Magi Agent Suit should resolve a school rune from any adjustment slot");
            helper.assertFalse(SpellCalibrationAdjustmentGameTestSupport.canPlaceCalibrationAdjustment(
                            new ItemStack(ItemRegistry.CHROMATIC_MAGIA_DRESS_HAT.get()), 0, fireRune),
                    "Chromatic Magia Dress should reject school runes");
            helper.assertFalse(SpellCalibrationAdjustmentGameTestSupport.canPlaceCalibrationAdjustment(
                            new ItemStack(ItemRegistry.ELEMENT_MAIDEN_ROBE_RIBBON.get()), 0, fireRune),
                    "Element Maiden Robe should reject school runes");

        });
    }
}
