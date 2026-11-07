package jp.aquafactory.apprenticecodex.gametest.equipment.consumable;

import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.registry.EffectRegistry;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import jp.aquafactory.apprenticecodex.registry.PotionRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;

import static jp.aquafactory.apprenticecodex.gametest.support.PlayerTestSupport.createEquipmentTestPlayer;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class ManaConsumableGameTests {
    private ManaConsumableGameTests() {}

    @GameTest(template = "gametest/basic_floor")
    public static void comfortFoodAppliesManaRegenerationWhenConsumed(GameTestHelper helper) {
        for (var food : List.of(new ItemStack(ItemRegistry.COMFORT_BERRIES.get()),
                new ItemStack(ItemRegistry.COMFORT_SANDWICH.get()))) {
            var player = createEquipmentTestPlayer(helper, new BlockPos(2, 1, 2), "mana_food");
            var baseline = player.getAttributeValue(AttributeRegistry.MANA_REGEN);
            helper.assertTrue(player.canEat(food.getFoodProperties(player).canAlwaysEat()),
                    food + " should be usable at full hunger");
            var result = food.getItem().finishUsingItem(food, helper.getLevel(), player);
            helper.assertTrue(result.isEmpty(), "Eating should consume one food item");
            assertAppliedRegen(helper, player, baseline, "Consumed food");
        }
        helper.succeed();
    }

    @GameTest(template = "gametest/basic_floor")
    public static void meditationPotionsApplyManaRegenerationAndReturnBottle(GameTestHelper helper) {
        for (var potion : List.of(PotionRegistry.MEDITATION, PotionRegistry.LONG_MEDITATION,
                PotionRegistry.STRONG_MEDITATION)) {
            var player = createEquipmentTestPlayer(helper, new BlockPos(2, 1, 2), "mana_potion");
            var baseline = player.getAttributeValue(AttributeRegistry.MANA_REGEN);
            var stack = new ItemStack(Items.POTION);
            stack.set(DataComponents.POTION_CONTENTS, new PotionContents(potion));
            var result = stack.getItem().finishUsingItem(stack, helper.getLevel(), player);
            helper.assertTrue(result.is(Items.GLASS_BOTTLE), "Drinking should return a glass bottle: " + potion.getId());
            assertAppliedRegen(helper, player, baseline, potion.getId().toString());
        }
        helper.succeed();
    }

    @GameTest(template = "gametest/basic_floor")
    public static void manaRegenerationEffectUpdatesWithoutStackingAndRestoresBaseline(GameTestHelper helper) {
        var player = createEquipmentTestPlayer(helper, new BlockPos(2, 1, 2), "mana_regen_lifecycle");
        var baseline = player.getAttributeValue(AttributeRegistry.MANA_REGEN);
        try {
            player.addEffect(new MobEffectInstance(EffectRegistry.MANA_REGENERATION, 100, 0));
            assertRegen(helper, player, baseline * 1.25D, "Initial level");
            player.addEffect(new MobEffectInstance(EffectRegistry.MANA_REGENERATION, 200, 0));
            assertRegen(helper, player, baseline * 1.25D, "Refresh must not stack");
            player.addEffect(new MobEffectInstance(EffectRegistry.MANA_REGENERATION, 100, 1));
            assertRegen(helper, player, baseline * 1.5D, "Stronger level replaces modifier");
            player.removeEffect(EffectRegistry.MANA_REGENERATION);
            assertRegen(helper, player, baseline, "Removal restores baseline");
        } finally {
            player.removeEffect(EffectRegistry.MANA_REGENERATION);
        }
        helper.succeed();
    }

    private static void assertAppliedRegen(GameTestHelper helper, FakePlayer player, double baseline, String context) {
        var effect = player.getEffect(EffectRegistry.MANA_REGENERATION);
        helper.assertTrue(effect != null && effect.getDuration() > 0, context + " must apply mana regeneration");
        assertRegen(helper, player, baseline * (1.0D + 0.25D * (effect.getAmplifier() + 1)), context);
        player.removeEffect(EffectRegistry.MANA_REGENERATION);
        assertRegen(helper, player, baseline, context + " removal");
    }

    private static void assertRegen(GameTestHelper helper, FakePlayer player, double expected, String context) {
        helper.assertTrue(expected > 0 && Math.abs(player.getAttributeValue(AttributeRegistry.MANA_REGEN) - expected) < 1.0e-9D,
                context + ": unexpected mana regeneration attribute");
    }
}
