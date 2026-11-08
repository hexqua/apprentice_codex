package jp.aquafactory.apprenticecodex.gametest.equipment.scrollcastergauntlet;

import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.SpellData;
import jp.aquafactory.apprenticecodex.compat.epicfight.EpicFightCompat;
import jp.aquafactory.apprenticecodex.item.scrollcastergauntlet.ScrollcasterGauntlet;
import jp.aquafactory.apprenticecodex.registry.EnchantmentRegistry;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import jp.aquafactory.apprenticecodex.utility.MagicTools;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.ModList;

import static jp.aquafactory.apprenticecodex.gametest.equipment.spelldispenser.support.SpellDispenserTestSupport.createSpellScroll;
import static jp.aquafactory.apprenticecodex.gametest.support.AttributeTestSupport.*;

final class ScrollcasterEquipmentScenarios {
    private ScrollcasterEquipmentScenarios() {}

    static void scrollcasterGauntletKeepsExpectedStatsAndBenchRules(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var stack = new ItemStack(ItemRegistry.SCROLLCASTER_GAUNTLET.get());
            ScrollcasterGauntlet.setCalibrationScroll(
                    stack,
                    0,
                    createSpellScroll(SpellRegistry.MAGIC_MISSILE_SPELL.get())
            );
            stack.enchant(EnchantmentRegistry.SURGE.get(), 1);
            stack.enchant(EnchantmentRegistry.ATTUNEMENT.get(), 1);
            var modifiers = stack.getAttributeModifiers(EquipmentSlot.MAINHAND);
            var epicFightLoaded = ModList.get().isLoaded(EpicFightCompat.MOD_ID);
            var expectedAttackDamageBonus = epicFightLoaded ? 2.0D : 5.0D;
            var expectedAttackSpeedBonus = epicFightLoaded ? 0.0D : -2.2D;
            assertModifierWithId(
                    helper,
                    modifiers.get(Attributes.ATTACK_DAMAGE),
                    VANILLA_BASE_ATTACK_DAMAGE_MODIFIER_ID,
                    AttributeModifier.Operation.ADDITION,
                    expectedAttackDamageBonus,
                    "Scrollcaster Gauntlet attack damage modifier should match the loaded combat environment"
            );
            assertModifierWithId(
                    helper,
                    modifiers.get(Attributes.ATTACK_SPEED),
                    VANILLA_BASE_ATTACK_SPEED_MODIFIER_ID,
                    AttributeModifier.Operation.ADDITION,
                    expectedAttackSpeedBonus,
                    "Scrollcaster Gauntlet attack speed modifier should match the loaded combat environment"
            );
            assertSingleModifierAmount(
                    helper,
                    modifiers.get(AttributeRegistry.SPELL_POWER.get()),
                    AttributeModifier.Operation.MULTIPLY_BASE,
                    0.10D,
                    "Scrollcaster Gauntlet spell power should ignore Surge"
            );
            var selectedSpell = ScrollcasterGauntlet.getSelectedSpellData(stack);
            var imbuedSchool = selectedSpell == SpellData.EMPTY ? null : selectedSpell.getSpell().getSchoolType();
            helper.assertTrue(imbuedSchool != null,
                    "Scrollcaster Gauntlet test could not resolve the selected spell school");
            var attunementAttribute = MagicTools
                    .resolveSchoolPowerAttribute(imbuedSchool);
            helper.assertTrue(attunementAttribute != null,
                    "Scrollcaster Gauntlet test could not resolve the Attunement spell power attribute: " + imbuedSchool.getId());
            helper.assertTrue(modifiers.get(attunementAttribute).isEmpty(),
                    "Scrollcaster Gauntlet spell power should ignore Attunement");
        });
    }
}
