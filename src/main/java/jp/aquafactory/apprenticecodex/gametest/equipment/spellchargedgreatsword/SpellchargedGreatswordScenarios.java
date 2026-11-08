package jp.aquafactory.apprenticecodex.gametest.equipment.spellchargedgreatsword;

import jp.aquafactory.apprenticecodex.config.ApprenticeCodexServerConfig;
import jp.aquafactory.apprenticecodex.item.spellchargedgreatsword.SpellchargedGreatsword;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;

import static jp.aquafactory.apprenticecodex.gametest.equipment.spellchargedgreatsword.SpellchargedGreatswordGameTestScenarios.assertSpellchargedGreatswordEntityReach;
import static jp.aquafactory.apprenticecodex.gametest.integration.malum.MalumTestSupport.MALUM_SOUL_HUNTER_WEAPON;
import static jp.aquafactory.apprenticecodex.gametest.support.AttributeTestSupport.*;

final class SpellchargedGreatswordScenarios {
    private SpellchargedGreatswordScenarios() {}

    static void spellchargedGreatswordKeepsExpectedStatsAndTags(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var item = (SpellchargedGreatsword) ItemRegistry.SPELLCHARGED_GREATSWORD.get();
            var stack = new ItemStack(item);
            helper.assertTrue(stack.getMaxDamage() == SpellchargedGreatsword.DURABILITY,
                    "Spellcharged Greatsword durability should be " + SpellchargedGreatsword.DURABILITY
                            + " but got " + stack.getMaxDamage());
            helper.assertTrue(item.getEnchantmentValue(stack) == SpellchargedGreatsword.ENCHANTMENT_VALUE,
                    "Spellcharged Greatsword enchantability should be " + SpellchargedGreatsword.ENCHANTMENT_VALUE
                            + " but got " + item.getEnchantmentValue(stack));

            var modifiers = item.getAttributeModifiers(EquipmentSlot.MAINHAND, stack);
            assertModifierWithId(
                    helper,
                    modifiers.get(Attributes.ATTACK_DAMAGE),
                    VANILLA_BASE_ATTACK_DAMAGE_MODIFIER_ID,
                    AttributeModifier.Operation.ADDITION,
                    SpellchargedGreatsword.DISPLAY_ATTACK_DAMAGE - 1.0D,
                    "Spellcharged Greatsword attack damage modifier should display as 8 damage"
            );
            assertModifierWithId(
                    helper,
                    modifiers.get(Attributes.ATTACK_SPEED),
                    VANILLA_BASE_ATTACK_SPEED_MODIFIER_ID,
                    AttributeModifier.Operation.ADDITION,
                    SpellchargedGreatsword.DISPLAY_ATTACK_SPEED - 4.0D,
                    "Spellcharged Greatsword attack speed modifier should display as 1.1 speed"
            );
            assertSpellchargedGreatswordEntityReach(
                    helper,
                    item,
                    stack,
                    ApprenticeCodexServerConfig.spellchargedGreatswordConfig().normalEntityReachBonus(),
                    "Spellcharged Greatsword entity reach modifier should match the normal server config"
            );

            helper.assertTrue(stack.is(MALUM_SOUL_HUNTER_WEAPON),
                    "Spellcharged Greatsword is missing malum:soul_hunter_weapon");
        });
    }
}
