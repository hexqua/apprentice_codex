package jp.aquafactory.apprenticecodex.gametest;

import com.google.common.collect.ImmutableMultimap;
import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import io.redspace.ironsspellbooks.api.spells.SpellData;
import jp.aquafactory.apprenticecodex.item.spellsideedge.SpellSideEdge;
import jp.aquafactory.apprenticecodex.item.spellsideedge.SpellSideEdgeOffhandAttributeBridge;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import jp.aquafactory.apprenticecodex.registry.SpellRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.UUID;

final class SpellSideEdgeGameTestScenarios extends ApprenticeCodexGameTestScenarios {
    private static final double TOLERANCE = 1.0e-9D;

    private SpellSideEdgeGameTestScenarios() {
    }

    static void spellSideEdgeStartsWithEdgeDancerAndExpectedStats(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var item = (SpellSideEdge) ItemRegistry.SPELL_SIDE_EDGE.get();
            var stack = item.getDefaultInstance();
            helper.assertTrue(stack.getMaxDamage() == 1561,
                    "Spell Side Edge durability should be 1561 but got " + stack.getMaxDamage());
            helper.assertTrue(item.getEnchantmentValue(stack) == 22,
                    "Spell Side Edge enchantability should be 22 but got " + item.getEnchantmentValue(stack));
            helper.assertTrue(item instanceof io.redspace.ironsspellbooks.item.UniqueItem,
                    "Spell Side Edge should be a UniqueItem");

            var spellContainer = ISpellContainer.get(stack);
            helper.assertTrue(spellContainer != null, "Spell Side Edge did not initialize a spell container");
            helper.assertTrue(spellContainer != null && spellContainer.getMaxSpellCount() == 1,
                    "Spell Side Edge should have exactly one spell slot");
            var spellData = spellContainer == null ? SpellData.EMPTY : spellContainer.getSpellAtIndex(0);
            helper.assertTrue(spellData != SpellData.EMPTY
                            && spellData.getSpell() == SpellRegistry.EDGE_DANCER.get()
                            && spellData.getLevel() == 1
                            && spellData.isLocked(),
                    "Spell Side Edge should start with locked Edge Dancer Lv1 but got " + spellData);

            var modifiers = item.getAttributeModifiers(EquipmentSlot.MAINHAND, stack);
            assertSingleModifierAmount(
                    helper,
                    modifiers.get(Attributes.ATTACK_DAMAGE),
                    AttributeModifier.Operation.ADDITION,
                    3.0D,
                    "Spell Side Edge attack damage modifier should display as 4 damage"
            );
            assertSingleModifierAmount(
                    helper,
                    modifiers.get(Attributes.ATTACK_SPEED),
                    AttributeModifier.Operation.ADDITION,
                    -1.6D,
                    "Spell Side Edge attack speed modifier should display as 2.4 speed"
            );

            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "spell_side_edge_use_test");
            player.setItemInHand(InteractionHand.MAIN_HAND, stack.copy());
            var useResult = stack.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
            helper.assertTrue(useResult.getResult() == net.minecraft.world.InteractionResult.PASS,
                    "Spell Side Edge should keep vanilla sword right-click behavior but got "
                            + useResult.getResult());
        });
    }

    static void spellSideEdgeKeepsExpectedTagsAndEnchantments(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var stack = new ItemStack(ItemRegistry.SPELL_SIDE_EDGE.get());
            helper.assertTrue(stack.is(MALUM_SOUL_HUNTER_WEAPON),
                    "Spell Side Edge is missing malum:soul_hunter_weapon");
            assertUpgradeable(helper, stack, "Spell Side Edge should accept Iron's upgrade orbs");
            assertExactEnchantmentSurfaces(
                    helper,
                    stack,
                    expectedSpellSideEdgeEnchantments(stack),
                    "Spell Side Edge"
            );
        });
    }

    static void spellSideEdgeBridgeUsesHigherComparableMainhandAttribute(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var spellPower = AttributeRegistry.SPELL_POWER.get();
            var maxMana = AttributeRegistry.MAX_MANA.get();
            var mainhandModifiers = ImmutableMultimap.<Attribute, AttributeModifier>builder()
                    .put(spellPower, modifier("main_spell_power", 0.10D, AttributeModifier.Operation.MULTIPLY_BASE))
                    .put(maxMana, modifier("main_max_mana", 100.0D, AttributeModifier.Operation.ADDITION))
                    .build();
            var offhandModifiers = ImmutableMultimap.<Attribute, AttributeModifier>builder()
                    .put(spellPower, modifier("offhand_spell_power", 0.05D, AttributeModifier.Operation.MULTIPLY_BASE))
                    .put(maxMana, modifier("offhand_max_mana", 150.0D, AttributeModifier.Operation.ADDITION))
                    .build();

            var bridgedModifiers = SpellSideEdgeOffhandAttributeBridge.buildBridgeModifiers(mainhandModifiers, offhandModifiers);
            assertSingleModifierAmount(
                    helper,
                    bridgedModifiers.get(spellPower),
                    AttributeModifier.Operation.MULTIPLY_BASE,
                    0.05D,
                    "Spell Side Edge bridge should add only the missing higher spell power amount"
            );
            helper.assertTrue(bridgedModifiers.get(maxMana).isEmpty(),
                    "Spell Side Edge bridge should not add max mana when offhand amount is already higher: "
                            + describeModifiers(bridgedModifiers));
        });
    }

    static void spellSideEdgeBridgeSkipsMultiplyTotalWhenOffhandAlreadyHasAttribute(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var mainhandModifiers = ImmutableMultimap.<Attribute, AttributeModifier>builder()
                    .put(Attributes.MOVEMENT_SPEED,
                            modifier("main_speed_total", 0.10D, AttributeModifier.Operation.MULTIPLY_TOTAL))
                    .build();
            var offhandModifiers = ImmutableMultimap.<Attribute, AttributeModifier>builder()
                    .put(Attributes.MOVEMENT_SPEED,
                            modifier("offhand_speed_total", 0.05D, AttributeModifier.Operation.MULTIPLY_TOTAL))
                    .build();

            var skippedModifiers = SpellSideEdgeOffhandAttributeBridge.buildBridgeModifiers(mainhandModifiers, offhandModifiers);
            helper.assertTrue(skippedModifiers.isEmpty(),
                    "Spell Side Edge bridge should skip duplicated MULTIPLY_TOTAL attributes but got "
                            + describeModifiers(skippedModifiers));

            var copiedModifiers = SpellSideEdgeOffhandAttributeBridge.buildBridgeModifiers(
                    mainhandModifiers,
                    ImmutableMultimap.of()
            );
            assertSingleModifierAmount(
                    helper,
                    copiedModifiers.get(Attributes.MOVEMENT_SPEED),
                    AttributeModifier.Operation.MULTIPLY_TOTAL,
                    0.10D,
                    "Spell Side Edge bridge should copy MULTIPLY_TOTAL when offhand has no matching attribute"
            );
        });
    }

    static void spellSideEdgeBridgeSyncsOnlyWhileHeldInMainhand(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "spell_side_edge_bridge_sync_test");
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ItemRegistry.SPELL_SIDE_EDGE.get()));
            player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(ItemRegistry.UNITE_LUNA_STAFF.get()));

            var spellPowerAttribute = AttributeRegistry.SPELL_POWER.get();
            var spellPowerInstance = player.getAttribute(spellPowerAttribute);
            helper.assertTrue(spellPowerInstance != null,
                    "Spell Side Edge bridge test could not resolve player spell power attribute");

            SpellSideEdgeOffhandAttributeBridge.sync(player);
            var bridgedAmount = sumModifierAmount(
                    spellPowerInstance.getModifiers(),
                    AttributeModifier.Operation.MULTIPLY_BASE
            );
            helper.assertTrue(Math.abs(bridgedAmount - 0.05D) < TOLERANCE,
                    "Spell Side Edge bridge should copy Unite Luna Staff mainhand spell power while held, got "
                            + bridgedAmount + " modifiers=" + spellPowerInstance.getModifiers());

            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.AIR));
            SpellSideEdgeOffhandAttributeBridge.sync(player);
            var clearedAmount = sumModifierAmount(
                    spellPowerInstance.getModifiers(),
                    AttributeModifier.Operation.MULTIPLY_BASE
            );
            helper.assertTrue(Math.abs(clearedAmount) < TOLERANCE,
                    "Spell Side Edge bridge should clear copied modifiers when unequipped, got "
                            + clearedAmount + " modifiers=" + spellPowerInstance.getModifiers());
        });
    }

    private static AttributeModifier modifier(
            String name,
            double amount,
            AttributeModifier.Operation operation
    ) {
        return new AttributeModifier(UUID.nameUUIDFromBytes(name.getBytes(java.nio.charset.StandardCharsets.UTF_8)),
                name, amount, operation);
    }
}
