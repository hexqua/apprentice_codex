package jp.aquafactory.apprenticecodex.gametest;

import com.mojang.authlib.GameProfile;
import io.redspace.ironsspellbooks.api.events.ModifySpellLevelEvent;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import jp.aquafactory.apprenticecodex.enchantment.TranscendencePolicy;
import jp.aquafactory.apprenticecodex.enchantment.TranscendenceResolver;
import jp.aquafactory.apprenticecodex.enchantment.TranscendenceSpellLevelEvent;
import jp.aquafactory.apprenticecodex.item.curios.CuriosSlotConstants;
import jp.aquafactory.apprenticecodex.registry.EnchantmentRegistry;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import jp.aquafactory.apprenticecodex.registry.TagRegistry;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.util.FakePlayer;
import top.theillusivec4.curios.api.CuriosApi;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

final class TranscendenceGameTestScenarios {
    private static final EquipmentSlot[] ARMOR_SLOTS = {
            EquipmentSlot.HEAD,
            EquipmentSlot.CHEST,
            EquipmentSlot.LEGS,
            EquipmentSlot.FEET
    };

    private TranscendenceGameTestScenarios() {
    }

    static void resolverUsesMaximumMatchingEventLevel(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var magicMissile = io.redspace.ironsspellbooks.api.registry.SpellRegistry.MAGIC_MISSILE_SPELL.get();
            var heal = io.redspace.ironsspellbooks.api.registry.SpellRegistry.HEAL_SPELL.get();
            var levelOne = createStack(ItemRegistry.MANA_FORCE_BLADE.get(), 1, magicMissile);
            var levelThreeWithTwoSpells = createStack(ItemRegistry.MANA_FORCE_BLADE.get(), 3, magicMissile, heal);
            var differentSpell = createStack(ItemRegistry.MANA_FORCE_BLADE.get(), 2, heal);
            var internal = createStack(ItemRegistry.ELEMENTAL_BOW.get(), 3, magicMissile);
            var disabled = createStack(ItemRegistry.MITHRIL_FREECAST_STAFF.get(), 3, magicMissile);
            var candidates = List.of(
                    new TranscendenceResolver.Candidate(levelOne, true),
                    new TranscendenceResolver.Candidate(levelThreeWithTwoSpells, false),
                    new TranscendenceResolver.Candidate(differentSpell, true),
                    new TranscendenceResolver.Candidate(internal, true),
                    new TranscendenceResolver.Candidate(disabled, true)
            );

            var magicMissileLevel = TranscendenceResolver.resolveMaxEventLevel(
                    magicMissile,
                    candidates,
                    TranscendenceGameTestScenarios::getTranscendenceLevel
            );
            helper.assertTrue(magicMissileLevel == 3,
                    "Matching Transcendence levels should use the maximum instead of summing but got " + magicMissileLevel);

            var healLevel = TranscendenceResolver.resolveMaxEventLevel(
                    heal,
                    candidates,
                    TranscendenceGameTestScenarios::getTranscendenceLevel
            );
            helper.assertTrue(healLevel == 3,
                    "Every active spell should match the maximum Transcendence level but got " + healLevel);
        });
    }

    static void eventCollectsHeldArmorAndConfiguredCurios(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var spell = io.redspace.ironsspellbooks.api.registry.SpellRegistry.MAGIC_MISSILE_SPELL.get();
            var player = new FakePlayer(
                    helper.getLevel(),
                    new GameProfile(UUID.randomUUID(), "transcendence_equipment_test")
            );

            player.setItemInHand(InteractionHand.MAIN_HAND, createStack(ItemRegistry.MANA_FORCE_BLADE.get(), 1, spell));
            assertEventLevel(helper, player, spell, 2, "Mainhand policy item should participate");
            player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);

            player.setItemInHand(InteractionHand.OFF_HAND,
                    createStack(ItemRegistry.COPPER_SPELL_AMPLIFIER.get(), 2, spell));
            assertEventLevel(helper, player, spell, 3, "Offhand magic item should participate");
            player.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);

            var heldRobe = createStack(ItemRegistry.ENCHANTRESS_ROBE.get(), 3, spell);
            player.setItemInHand(InteractionHand.MAIN_HAND, heldRobe);
            assertEventLevel(helper, player, spell, 1, "Armor policy should not participate while held");
            player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);

            for (var slot : ARMOR_SLOTS) {
                player.setItemSlot(slot, createStack(ItemRegistry.ENCHANTRESS_ROBE.get(), 1, spell));
                assertEventLevel(helper, player, spell, 2, "Armor policy should participate in " + slot);
                player.setItemSlot(slot, ItemStack.EMPTY);
            }

            var curios = CuriosApi.getCuriosInventory(player)
                    .orElseThrow(() -> new IllegalStateException("Missing curios inventory for Transcendence test"));
            for (var slot : List.of(
                    CuriosSlotConstants.HEAD,
                    CuriosSlotConstants.NECKLACE,
                    CuriosSlotConstants.FEET
            )) {
                curios.setEquippedCurio(slot, 0, createStack(ItemRegistry.ENCHANTED_CIRCLET.get(), 2, spell));
                assertEventLevel(helper, player, spell, 3, "Supported Curios slot should participate: " + slot);
                curios.setEquippedCurio(slot, 0, ItemStack.EMPTY);
            }

            curios.setEquippedCurio(CuriosSlotConstants.BACK, 0,
                    createStack(ItemRegistry.ENCHANTED_CIRCLET.get(), 3, spell));
            assertEventLevel(helper, player, spell, 1, "Unsupported Curios slot should not participate");
        });
    }

    static void specialItemPoliciesStayExplicit(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var transcendence = EnchantmentRegistry.TRANSCENDENCE.get();
            var mithril = ItemRegistry.MITHRIL_FREECAST_STAFF.get();
            var revolver = ItemRegistry.REVOLVERCAST_STAFF.get();
            var gauntlet = ItemRegistry.SCROLLCASTER_GAUNTLET.get();
            var elementalBow = ItemRegistry.ELEMENTAL_BOW.get();

            helper.assertFalse(mithril.canApplyAtEnchantingTable(new ItemStack(mithril), transcendence),
                    "Mithril Freecast Staff should reject Transcendence");
            helper.assertTrue(((TranscendencePolicy) mithril).transcendenceHandling()
                            == TranscendencePolicy.Handling.DISABLED,
                    "Mithril Freecast Staff should keep Transcendence disabled");
            helper.assertTrue(revolver.canApplyAtEnchantingTable(new ItemStack(revolver), transcendence),
                    "Revolvercast Staff should accept Transcendence like Swingcast Staffs");
            helper.assertTrue(ItemRegistry.MANA_FORCE_BLADE.get().canApplyAtEnchantingTable(
                            new ItemStack(ItemRegistry.MANA_FORCE_BLADE.get()), transcendence),
                    "Mana Force Blade should accept Transcendence");
            helper.assertFalse(((TranscendencePolicy) gauntlet).supportsDirectTranscendenceApplication(),
                    "Scrollcaster Gauntlet should only receive projected Transcendence");
            helper.assertTrue(((TranscendencePolicy) elementalBow).transcendenceHandling()
                            == TranscendencePolicy.Handling.INTERNAL,
                    "Elemental Bow should keep internal Transcendence handling");

            var spell = io.redspace.ironsspellbooks.api.registry.SpellRegistry.MAGIC_MISSILE_SPELL.get();
            var player = new FakePlayer(
                    helper.getLevel(),
                    new GameProfile(UUID.randomUUID(), "transcendence_special_item_test")
            );
            player.setItemInHand(InteractionHand.OFF_HAND, createStack(gauntlet, 2, spell));
            assertEventLevel(helper, player, spell, 3,
                    "Projected Scrollcaster Gauntlet Transcendence should work in offhand");
            player.setItemInHand(InteractionHand.OFF_HAND, createStack(mithril, 3, spell));
            assertEventLevel(helper, player, spell, 1,
                    "Forced Mithril Freecast Staff Transcendence should remain disabled");
            player.setItemInHand(InteractionHand.OFF_HAND, createStack(elementalBow, 3, spell));
            assertEventLevel(helper, player, spell, 1,
                    "Internal Elemental Bow Transcendence should not enter event aggregation");
        });
    }

    static void directApplicationTagMatchesPolicy(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var mismatches = new ArrayList<String>();
            for (var entry : ItemRegistry.ITEMS.getEntries()) {
                var item = entry.get();
                var expected = TranscendencePolicy.supportsDirectApplication(item);
                var actual = new ItemStack(item).is(TagRegistry.Items.ENCHANTABLE_TRANSCENDENCE);
                if (expected != actual) {
                    mismatches.add(entry.getId() + " expected=" + expected + " actual=" + actual);
                }
            }
            helper.assertTrue(mismatches.isEmpty(),
                    "Transcendence direct-application tag differs from policy: " + mismatches);
        });
    }

    static ItemStack createStack(Item item, int transcendenceLevel, AbstractSpell... spells) {
        var stack = new ItemStack(item);
        var mutable = ISpellContainer.create(Math.max(1, spells.length), true, false).mutableCopy();
        for (var index = 0; index < spells.length; ++index) {
            mutable.addSpellAtIndex(spells[index], 1, index, false);
        }
        ISpellContainer.set(stack, mutable.toImmutable());
        if (transcendenceLevel > 0) {
            stack.enchant(EnchantmentRegistry.TRANSCENDENCE.get(), transcendenceLevel);
        }
        return stack;
    }

    private static int getTranscendenceLevel(ItemStack stack) {
        return stack.getEnchantmentLevel(EnchantmentRegistry.TRANSCENDENCE.get());
    }

    static void assertEventLevel(
            GameTestHelper helper,
            FakePlayer player,
            AbstractSpell spell,
            int expectedLevel,
            String message
    ) {
        var event = new ModifySpellLevelEvent(spell, player, 1, 1);
        TranscendenceSpellLevelEvent.onModifySpellLevel(event);
        helper.assertTrue(event.getLevel() == expectedLevel,
                message + ": expected=" + expectedLevel + ", actual=" + event.getLevel());
    }
}
