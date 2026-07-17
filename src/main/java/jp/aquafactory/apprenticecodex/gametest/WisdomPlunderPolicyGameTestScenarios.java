package jp.aquafactory.apprenticecodex.gametest;

import com.mojang.authlib.GameProfile;
import jp.aquafactory.apprenticecodex.enchantment.PlunderLootingLevelEvent;
import jp.aquafactory.apprenticecodex.enchantment.PlunderTarget;
import jp.aquafactory.apprenticecodex.enchantment.WisdomExperienceDropEvent;
import jp.aquafactory.apprenticecodex.enchantment.WisdomPolicy;
import jp.aquafactory.apprenticecodex.registry.EnchantmentRegistry;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import jp.aquafactory.apprenticecodex.registry.TagRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.entity.living.LootingLevelEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

final class WisdomPlunderPolicyGameTestScenarios extends ApprenticeCodexGameTestScenarios {
    private WisdomPlunderPolicyGameTestScenarios() {
    }

    static void directApplicationPoliciesMatchItemsAndGeneratedTags(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var wisdom = EnchantmentRegistry.WISDOM.get();
            var plunder = EnchantmentRegistry.PLUNDER.get();
            var mismatches = new ArrayList<String>();

            for (var entry : ItemRegistry.ITEMS.getEntries()) {
                var item = entry.get();
                var stack = new ItemStack(item);
                verifyPolicySurface(
                        mismatches,
                        entry.getId() + " Wisdom",
                        WisdomPolicy.supportsDirectApplication(item),
                        item.canApplyAtEnchantingTable(stack, wisdom),
                        stack.is(TagRegistry.Items.ENCHANTABLE_WISDOM)
                );
                verifyPolicySurface(
                        mismatches,
                        entry.getId() + " Plunder",
                        item instanceof PlunderTarget,
                        item.canApplyAtEnchantingTable(stack, plunder),
                        stack.is(TagRegistry.Items.ENCHANTABLE_PLUNDER)
                );
            }

            helper.assertTrue(mismatches.isEmpty(),
                    "Wisdom/Plunder policy surface mismatches: " + String.join(", ", mismatches));
        });
    }

    static void newlyUnifiedHeldTargetsApplyWisdomAndPlunder(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var wisdomTargets = List.of(
                    ItemRegistry.FOCUS_STAFFBOW.get(),
                    ItemRegistry.CIRCUIT_HEAT_STAFF.get(),
                    ItemRegistry.CHARGED_TWIN_BLADE_STAFF.get(),
                    ItemRegistry.SPELLCHARGED_GREATSWORD.get(),
                    ItemRegistry.ALCHEMISTS_FLASK.get()
            );
            for (var item : wisdomTargets) {
                assertHeldWisdom(helper, item, true);
            }

            assertHeldPlunder(helper, ItemRegistry.FOCUS_STAFFBOW.get());
            assertHeldPlunder(helper, ItemRegistry.CIRCUIT_HEAT_STAFF.get());
        });
    }

    static void equipmentOnlyAndUnsupportedItemsDoNotApplyWhileHeld(GameTestHelper helper) {
        helper.succeedIf(() -> {
            assertHeldWisdom(helper, ItemRegistry.ENCHANTRESS_ROBE.get(), false);
            assertHeldWisdom(helper, ItemRegistry.ENCHANTED_CIRCLET.get(), false);
            assertHeldWisdom(helper, Items.STICK, false);

            var level = helper.getLevel();
            var armorPlayer = new FakePlayer(level, new GameProfile(UUID.randomUUID(), "wisdom_armor_policy_test"));
            var robe = new ItemStack(ItemRegistry.ENCHANTRESS_ROBE.get());
            robe.enchant(EnchantmentRegistry.WISDOM.get(), 1);
            armorPlayer.setItemSlot(EquipmentSlot.CHEST, robe);
            var armorEvent = new BlockEvent.BreakEvent(level, new BlockPos(1, 2, 0),
                    Blocks.DIAMOND_ORE.defaultBlockState(), armorPlayer);
            armorEvent.setExpToDrop(20);
            WisdomExperienceDropEvent.onBlockBreak(armorEvent);
            helper.assertTrue(armorEvent.getExpToDrop() == 21,
                    "Equipped Wisdom armor should apply the +5% equipment rate");

            var unsupportedPlayer = new FakePlayer(level,
                    new GameProfile(UUID.randomUUID(), "unsupported_plunder_policy_test"));
            var unsupported = new ItemStack(Items.STICK);
            unsupported.enchant(EnchantmentRegistry.PLUNDER.get(), 3);
            unsupportedPlayer.setItemInHand(InteractionHand.MAIN_HAND, unsupported);
            var target = helper.spawn(EntityType.ZOMBIE, new BlockPos(2, 2, 0));
            var lootingEvent = new LootingLevelEvent(
                    target,
                    unsupportedPlayer.damageSources().playerAttack(unsupportedPlayer),
                    0
            );
            PlunderLootingLevelEvent.onLootingLevel(lootingEvent);
            helper.assertTrue(lootingEvent.getLootingLevel() == 0,
                    "A forced Plunder enchantment on an unsupported item should not apply");
        });
    }

    private static void verifyPolicySurface(
            List<String> mismatches,
            String name,
            boolean policy,
            boolean directApplication,
            boolean generatedTag
    ) {
        if (policy != directApplication || policy != generatedTag) {
            mismatches.add(name + " policy=" + policy + " direct=" + directApplication + " tag=" + generatedTag);
        }
    }

    private static void assertHeldWisdom(GameTestHelper helper, Item item, boolean expectedActive) {
        var level = helper.getLevel();
        for (var hand : InteractionHand.values()) {
            var player = new FakePlayer(level, new GameProfile(UUID.randomUUID(), "held_wisdom_policy_test"));
            var stack = new ItemStack(item);
            stack.enchant(EnchantmentRegistry.WISDOM.get(), 1);
            player.setItemInHand(hand, stack);

            var event = new BlockEvent.BreakEvent(level, new BlockPos(0, 2, 0),
                    Blocks.DIAMOND_ORE.defaultBlockState(), player);
            event.setExpToDrop(20);
            WisdomExperienceDropEvent.onBlockBreak(event);
            var expectedExperience = expectedActive ? 24 : 20;
            helper.assertTrue(event.getExpToDrop() == expectedExperience,
                    ForgeRegistries.ITEMS.getKey(item) + " " + hand + " Wisdom expected=" + expectedExperience
                            + " actual=" + event.getExpToDrop());
        }
    }

    private static void assertHeldPlunder(GameTestHelper helper, Item item) {
        var level = helper.getLevel();
        for (var hand : InteractionHand.values()) {
            var player = new FakePlayer(level, new GameProfile(UUID.randomUUID(), "held_plunder_policy_test"));
            var stack = new ItemStack(item);
            stack.enchant(EnchantmentRegistry.PLUNDER.get(), 2);
            player.setItemInHand(hand, stack);

            var target = helper.spawn(EntityType.ZOMBIE, new BlockPos(3, 2, 0));
            var event = new LootingLevelEvent(target, player.damageSources().playerAttack(player), 0);
            PlunderLootingLevelEvent.onLootingLevel(event);
            helper.assertTrue(event.getLootingLevel() == 2,
                    ForgeRegistries.ITEMS.getKey(item) + " " + hand + " Plunder should set looting level to 2");
        }
    }
}
