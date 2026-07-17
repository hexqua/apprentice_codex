package jp.aquafactory.apprenticecodex.gametest;

import com.mojang.authlib.GameProfile;
import jp.aquafactory.apprenticecodex.enchantment.Enchantments;
import jp.aquafactory.apprenticecodex.enchantment.PlunderLootingEvent;
import jp.aquafactory.apprenticecodex.enchantment.PlunderTarget;
import jp.aquafactory.apprenticecodex.enchantment.WisdomExperienceDropEvent;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.entity.living.LivingExperienceDropEvent;

import java.util.List;
import java.util.UUID;

final class WisdomPlunderEffectGameTestScenarios extends ApprenticeCodexGameTestScenarios {
    private WisdomPlunderEffectGameTestScenarios() {
    }

    static void newlyUnifiedHeldTargetsApplyWisdomAndPlunder(GameTestHelper helper) {
        helper.succeedIf(() -> {
            for (var item : List.of(
                    ItemRegistry.FOCUS_STAFFBOW.get(),
                    ItemRegistry.CIRCUIT_HEAT_STAFF.get(),
                    ItemRegistry.CHARGED_TWIN_BLADE_STAFF.get(),
                    ItemRegistry.SPELLCHARGED_GREATSWORD.get(),
                    ItemRegistry.ALCHEMISTS_FLASK.get()
            )) {
                assertHeldWisdom(helper, item, true);
            }
            assertActivePlunder(helper, ItemRegistry.FOCUS_STAFFBOW.get(), 2);
        });
    }

    static void equipmentOnlyAndUnsupportedItemsDoNotApplyWhileHeld(GameTestHelper helper) {
        helper.succeedIf(() -> {
            assertHeldWisdom(helper, ItemRegistry.ENCHANTRESS_ROBE.get(), false);
            assertHeldWisdom(helper, ItemRegistry.ENCHANTED_CIRCLET.get(), false);
            assertHeldWisdom(helper, Items.STICK, false);

            var player = new FakePlayer(helper.getLevel(),
                    new GameProfile(UUID.randomUUID(), "wisdom_armor_policy_test"));
            var robe = enchantedStack(helper, ItemRegistry.ENCHANTRESS_ROBE.get(), Enchantments.WISDOM, 1);
            player.setItemSlot(EquipmentSlot.CHEST, robe);
            var event = new LivingExperienceDropEvent(
                    helper.spawn(EntityType.ZOMBIE, new BlockPos(1, 2, 0)), player, 20
            );
            WisdomExperienceDropEvent.onLivingExperienceDrop(event);
            helper.assertTrue(event.getDroppedExperience() == 21,
                    "Equipped Wisdom armor should apply the +5% equipment rate");

            var unsupported = enchantedStack(helper, Items.STICK, Enchantments.PLUNDER, 3);
            helper.assertTrue(PlunderLootingEvent.getActivePlunderLevel(unsupported) == 0,
                    "Forced Plunder on an unsupported item must stay inactive");
        });
    }

    static void circuitHeatStaffKeepsVanillaLooting(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var stack = enchantedStack(helper, ItemRegistry.CIRCUIT_HEAT_STAFF.get(), Enchantments.PLUNDER, 2);
            helper.assertFalse(stack.getItem() instanceof PlunderTarget,
                    "Circuit Heat Staff must not opt into Plunder's Looting conversion");
            helper.assertTrue(PlunderLootingEvent.getActivePlunderLevel(stack) == 0,
                    "Forced Plunder must remain inactive on Circuit Heat Staff");

            var looting = helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT)
                    .getOrThrow(net.minecraft.world.item.enchantment.Enchantments.LOOTING);
            helper.assertTrue(stack.getItem().supportsEnchantment(stack, looting),
                    "Circuit Heat Staff should keep vanilla Looting through the 1.21.1 sword tag");
        });
    }

    private static void assertHeldWisdom(GameTestHelper helper, Item item, boolean expectedActive) {
        for (var hand : InteractionHand.values()) {
            var player = new FakePlayer(helper.getLevel(),
                    new GameProfile(UUID.randomUUID(), "held_wisdom_policy_test"));
            player.setItemInHand(hand, enchantedStack(helper, item, Enchantments.WISDOM, 1));
            var event = new LivingExperienceDropEvent(
                    helper.spawn(EntityType.ZOMBIE, new BlockPos(0, 2, 0)), player, 20
            );
            WisdomExperienceDropEvent.onLivingExperienceDrop(event);
            // Better Combat が両手武器を論理 offhand から隠す場合は、通常の装備参照と同様に効果対象外となる。
            var logicallyEquipped = !player.getItemInHand(hand).isEmpty();
            var expectedExperience = expectedActive && logicallyEquipped ? 24 : 20;
            helper.assertTrue(event.getDroppedExperience() == expectedExperience,
                    item + " " + hand + " Wisdom expected=" + expectedExperience
                            + " actual=" + event.getDroppedExperience());
        }
    }

    private static void assertActivePlunder(GameTestHelper helper, Item item, int expectedLevel) {
        var stack = enchantedStack(helper, item, Enchantments.PLUNDER, expectedLevel);
        helper.assertTrue(PlunderLootingEvent.getActivePlunderLevel(stack) == expectedLevel,
                item + " should expose active Plunder level " + expectedLevel);
    }

    private static ItemStack enchantedStack(
            GameTestHelper helper,
            Item item,
            net.minecraft.resources.ResourceKey<net.minecraft.world.item.enchantment.Enchantment> enchantmentKey,
            int level
    ) {
        var stack = new ItemStack(item);
        var enchantment = helper.getLevel().registryAccess()
                .lookupOrThrow(Registries.ENCHANTMENT)
                .getOrThrow(enchantmentKey);
        stack.enchant(enchantment, level);
        return stack;
    }
}
