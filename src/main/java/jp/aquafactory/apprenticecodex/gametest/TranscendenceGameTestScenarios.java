package jp.aquafactory.apprenticecodex.gametest;

import com.mojang.authlib.GameProfile;
import io.redspace.ironsspellbooks.api.events.ModifySpellLevelEvent;
import io.redspace.ironsspellbooks.api.item.curios.AffinityData;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import io.redspace.ironsspellbooks.api.spells.SpellData;
import jp.aquafactory.apprenticecodex.enchantment.TranscendenceHelper;
import jp.aquafactory.apprenticecodex.enchantment.TranscendenceTarget;
import jp.aquafactory.apprenticecodex.item.curios.CuriosSlotConstants;
import jp.aquafactory.apprenticecodex.item.fullautorapidcastspellrifle.FullautoRapidcastSpellrifle;
import jp.aquafactory.apprenticecodex.item.multipurposestaffrifle.MultipurposeStaffrifle;
import jp.aquafactory.apprenticecodex.registry.EnchantmentRegistry;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.common.MinecraftForge;
import top.theillusivec4.curios.api.CuriosApi;

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

    static void scrollBonusIsFixedAndPreservesLegacyData(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var spell = SpellRegistry.MAGIC_MISSILE_SPELL.get();
            var source = new SpellData(spell, spell.getMaxLevel());
            var targets = List.of(ItemRegistry.ELEMENTAL_BOW.get(), ItemRegistry.MULTIPURPOSE_STAFFRIFLE.get(),
                    ItemRegistry.FULLAUTO_RAPIDCAST_SPELLRIFLE.get(),
                    ItemRegistry.REVOLVERCAST_STAFF.get(), ItemRegistry.SCROLLCASTER_GAUNTLET.get(),
                    ItemRegistry.CHARGECAST_CATALYSTBOOK.get(), ItemRegistry.ARCHIVISTS_GRIMOIRE.get(),
                    ItemRegistry.FLOATMOUNT_BROOM.get(), ItemRegistry.HOVERRIDE_BROOM.get(),
                    ItemRegistry.QUICKCAST_SCROLL_CARTRIDGE.get(), ItemRegistry.AUTOCAST_AMULET.get(),
                    ItemRegistry.SATELLITE_FOLLOWCAST_AMULET.get(), ItemRegistry.SHOOTING_STAR_MANTLE.get());
            var enchantment = EnchantmentRegistry.TRANSCENDENCE.get();
            helper.assertTrue(enchantment.getMaxLevel() == 1, "New Transcendence must have a single level");
            for (var entry : ItemRegistry.ITEMS.getEntries()) {
                var item = entry.get();
                helper.assertTrue(TranscendenceTarget.supportsDirectApplication(item) == targets.contains(item),
                        "Only supported internal scroll weapons may accept Transcendence: " + item);
                for (int level : new int[]{0, 1, 3, 10}) {
                    var stack = new ItemStack(item);
                    if (level > 0) stack.enchant(enchantment, level);
                    var before = stack.copy();
                    int expected = source.getLevel() + (targets.contains(item) && level > 0 ? 1 : 0);
                    helper.assertTrue(TranscendenceHelper.resolveScrollSpellLevel(stack, source.getLevel()) == expected,
                            "Legacy levels must give only one bonus on supported items: " + item);
                    if (item instanceof MultipurposeStaffrifle) {
                        helper.assertTrue(MultipurposeStaffrifle.resolveImbuedSpellLevel(stack, source) == expected,
                                "Multipurpose scroll must exceed its normal maximum by one");
                    } else if (item instanceof FullautoRapidcastSpellrifle) {
                        helper.assertTrue(FullautoRapidcastSpellrifle.resolveImbuedSpellLevel(stack, source) == expected,
                                "Fullauto scroll must exceed its normal maximum by one");
                    }
                    helper.assertTrue(TranscendenceHelper.resolveScrollSpellLevel(stack, source.getLevel()) == expected,
                            "Repeated resolution must not accumulate a bonus");
                    helper.assertTrue(ItemStack.isSameItemSameTags(before, stack),
                            "Legacy enchantment levels must remain unchanged");
                }
            }
        });
    }

    static void legacyEquipmentDoesNotModifySpellLevels(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var spell = SpellRegistry.MAGIC_MISSILE_SPELL.get();
            var player = new FakePlayer(
                    helper.getLevel(),
                    new GameProfile(UUID.randomUUID(), "transcendence_equipment_test")
            );

            player.setItemInHand(InteractionHand.MAIN_HAND, createStack(ItemRegistry.MANA_FORCE_BLADE.get(), 1, spell));
            assertEventLevel(helper, player, spell, 1, "Mainhand policy item must not modify spell levels");
            player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);

            player.setItemInHand(InteractionHand.OFF_HAND,
                    createStack(ItemRegistry.COPPER_SPELL_AMPLIFIER.get(), 2, spell));
            assertEventLevel(helper, player, spell, 1, "Offhand magic item must not modify spell levels");
            player.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);

            var heldRobe = createStack(ItemRegistry.ELEMENT_MAIDEN_ROBE_ROBE.get(), 3, spell);
            player.setItemInHand(InteractionHand.MAIN_HAND, heldRobe);
            assertEventLevel(helper, player, spell, 1, "Armor policy should not participate while held");
            player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);

            for (var slot : ARMOR_SLOTS) {
                player.setItemSlot(slot, createStack(ItemRegistry.ELEMENT_MAIDEN_ROBE_ROBE.get(), 1, spell));
                assertEventLevel(helper, player, spell, 1, "Armor policy must not modify spell levels in " + slot);
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
                assertEventLevel(helper, player, spell, 1, "Supported Curios slot must not modify spell levels: " + slot);
                curios.setEquippedCurio(slot, 0, ItemStack.EMPTY);
            }

            curios.setEquippedCurio(CuriosSlotConstants.BACK, 0,
                    createStack(ItemRegistry.ENCHANTED_CIRCLET.get(), 3, spell));
            assertEventLevel(helper, player, spell, 1, "Non-head Curios slot must not modify spell levels");
        });
    }

    static void internalBonusStacksWithAffinityOnly(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var mithril = ItemRegistry.MITHRIL_FREECAST_STAFF.get();
            var gauntlet = ItemRegistry.SCROLLCASTER_GAUNTLET.get();
            var elementalBow = ItemRegistry.ELEMENTAL_BOW.get();

            var spell = SpellRegistry.MAGIC_MISSILE_SPELL.get();
            var player = new FakePlayer(
                    helper.getLevel(),
                    new GameProfile(UUID.randomUUID(), "transcendence_special_item_test")
            );
            player.setItemInHand(InteractionHand.OFF_HAND, createStack(gauntlet, 2, spell));
            assertEventLevel(helper, player, spell, 1,
                    "Projected Scrollcaster Gauntlet Transcendence must remain inactive in offhand");
            player.setItemInHand(InteractionHand.OFF_HAND, createStack(mithril, 3, spell));
            assertEventLevel(helper, player, spell, 1,
                    "Forced Mithril Freecast Staff Transcendence should remain disabled");
            player.setItemInHand(InteractionHand.OFF_HAND, createStack(elementalBow, 3, spell));
            assertEventLevel(helper, player, spell, 1,
                    "Internal Elemental Bow Transcendence should not enter event aggregation");
            var ring = new ItemStack(ItemRegistry.ENCHANTED_CIRCLET.get());
            AffinityData.setAffinityData(ring, spell, 2);
            CuriosApi.getCuriosInventory(player)
                    .orElseThrow(() -> new IllegalStateException("Missing Curios inventory"))
                    .setEquippedCurio(CuriosSlotConstants.HEAD, 0, ring);
            var rifle = createStack(ItemRegistry.MULTIPURPOSE_STAFFRIFLE.get(), 3, spell);
            player.setItemInHand(InteractionHand.MAIN_HAND, rifle);
            int baseLevel = MultipurposeStaffrifle.resolveImbuedSpellLevel(rifle, new SpellData(spell, spell.getMaxLevel()));
            helper.assertTrue(spell.getLevelFor(baseLevel, player) == spell.getMaxLevel() + 3,
                    "Affinity must stack once with the internal bonus, without another equipment bonus");
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
        MinecraftForge.EVENT_BUS.post(event);
        helper.assertTrue(event.getLevel() == expectedLevel,
                message + ": expected=" + expectedLevel + ", actual=" + event.getLevel());
    }
}
