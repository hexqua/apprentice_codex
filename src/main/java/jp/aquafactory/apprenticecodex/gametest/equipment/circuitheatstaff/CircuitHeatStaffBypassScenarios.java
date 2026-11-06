package jp.aquafactory.apprenticecodex.gametest.equipment.circuitheatstaff;

import io.redspace.ironsspellbooks.api.events.SpellOnCastEvent;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.magic.MagicHelper;
import io.redspace.ironsspellbooks.api.magic.SpellSelectionManager;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.util.Utils;
import jp.aquafactory.apprenticecodex.item.circuitheatstaff.CircuitHeatStaffOverheatManager;
import jp.aquafactory.apprenticecodex.item.circuitheatstaff.CircuitHeatStaffRightClickItemEvent;
import jp.aquafactory.apprenticecodex.item.offhand.AbstractOffhandMagicItem;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import jp.aquafactory.apprenticecodex.registry.SpellRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

import static jp.aquafactory.apprenticecodex.gametest.support.PlayerTestSupport.createEquipmentTestPlayer;
import static jp.aquafactory.apprenticecodex.gametest.support.SpellContainerTestSupport.setSingleUnlockedSpell;
import static jp.aquafactory.apprenticecodex.gametest.support.SpellContainerTestSupport.setSingleUnlockedSpell;

final class CircuitHeatStaffBypassScenarios {
    private CircuitHeatStaffBypassScenarios() {}

    static void circuitHeatStaffRightClickBypassesCooldownButShortcutDoesNot(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "circuit_heat_staff_right_click_test");
            var staffStack = new ItemStack(ItemRegistry.CIRCUIT_HEAT_STAFF.get());
            var amplifierItem = (AbstractOffhandMagicItem) ItemRegistry.COPPER_SPELL_AMPLIFIER.get();
            var amplifierStack = new ItemStack(amplifierItem);
            amplifierItem.initializeSpellContainer(amplifierStack);
            var spell = SpellRegistry.MANA_SLASH.get();
            setSingleUnlockedSpell(helper, amplifierStack, spell, 1);

            player.setItemInHand(InteractionHand.MAIN_HAND, staffStack);
            player.setItemInHand(InteractionHand.OFF_HAND, amplifierStack);
            var magicData = MagicData.getPlayerMagicData(player);
            helper.assertTrue(magicData != null, "Circuit Heat Staff cast packet test could not resolve player mana data");
            magicData.setMana(spell.getManaCost(1) * 4.0F);

            var selection = new SpellSelectionManager(player).getSelection();
            helper.assertTrue(selection != null && selection.spellData.getSpell() == spell,
                    "Circuit Heat Staff cast packet test could not resolve the selected spell: " + selection);
            MagicHelper.MAGIC_MANAGER.addCooldown(player, spell, selection.getCastSource());
            var skippedCooldownTicks = Math.max(0,
                    magicData.getPlayerCooldowns().getSpellCooldowns().get(spell.getSpellId()).getCooldownRemaining());

            helper.assertFalse(Utils.serverSideInitiateCast(player),
                    "Circuit Heat Staff should not bypass cooldown from Iron's shortcut cast packet path");
            helper.assertFalse(magicData.isCasting(),
                    "Circuit Heat Staff shortcut path should not start casting during normal cooldown");
            helper.assertTrue(magicData.getPlayerCooldowns().isOnCooldown(spell),
                    "Circuit Heat Staff shortcut path should keep the original cooldown");
            helper.assertFalse(CircuitHeatStaffOverheatManager
                            .getState(player, spell.getSpellId()).active(),
                    "Circuit Heat Staff shortcut path should not store overheat state");

            var rightClickEvent = new PlayerInteractEvent.RightClickItem(
                    player,
                    InteractionHand.MAIN_HAND
            );
            CircuitHeatStaffRightClickItemEvent.onRightClickItem(rightClickEvent);
            helper.assertTrue(rightClickEvent.isCanceled(),
                    "Circuit Heat Staff right click event should claim the casting item path before Iron's handler");
            helper.assertTrue(rightClickEvent.getCancellationResult().consumesAction(),
                    "Circuit Heat Staff right click should consume the event while bypassing cooldown: "
                            + rightClickEvent.getCancellationResult());
            helper.assertTrue(magicData.isCasting() && magicData.getCastingSpellId().equals(spell.getSpellId()),
                    "Circuit Heat Staff right click path should start casting the selected spell");
            helper.assertTrue(magicData.getPlayerCastingItem() == staffStack,
                    "Circuit Heat Staff right click path should keep the held staff as casting item");
            helper.assertFalse(magicData.getPlayerCooldowns().isOnCooldown(spell),
                    "Circuit Heat Staff should remove the spell cooldown while the right-click bypass starts");
            helper.assertTrue(CircuitHeatStaffOverheatManager
                            .getState(player, spell.getSpellId()).active(),
                    "Circuit Heat Staff should store bypass overheat state after right-click bypass");

            var baseManaCost = spell.getManaCost(1);
            var expectedOverheatManaCost = baseManaCost
                    + CircuitHeatStaffOverheatManager
                    .getAdditionalManaCost(baseManaCost, 1, skippedCooldownTicks);
            var manaEvent = new SpellOnCastEvent(
                    player,
                    spell.getSpellId(),
                    1,
                    baseManaCost,
                    spell.getSchoolType(),
                    selection.getCastSource()
            );
            NeoForge.EVENT_BUS.post(manaEvent);
            helper.assertTrue(manaEvent.getManaCost() == expectedOverheatManaCost,
                    "Circuit Heat Staff right-click bypass should apply step 1 extra mana: " + manaEvent.getManaCost());

            magicData.resetCastingState();
        });
    }

    static CircuitHeatStaffBypassTestContext createCircuitHeatStaffBypassTestContext(
            GameTestHelper helper,
            String playerName,
            AbstractSpell spell
    ) {
        var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), playerName);
        var staffStack = new ItemStack(ItemRegistry.CIRCUIT_HEAT_STAFF.get());
        var amplifierItem = (AbstractOffhandMagicItem) ItemRegistry.COPPER_SPELL_AMPLIFIER.get();
        var amplifierStack = new ItemStack(amplifierItem);
        amplifierItem.initializeSpellContainer(amplifierStack);
        setSingleUnlockedSpell(helper, amplifierStack, spell, 1);

        player.setItemInHand(InteractionHand.MAIN_HAND, staffStack);
        player.setItemInHand(InteractionHand.OFF_HAND, amplifierStack);

        var magicData = MagicData.getPlayerMagicData(player);
        helper.assertTrue(magicData != null, "Circuit Heat Staff config test could not resolve player mana data");
        MagicHelper.MAGIC_MANAGER.addCooldown(player, spell, CastSource.SPELLBOOK);
        return new CircuitHeatStaffBypassTestContext(player, staffStack, magicData, spell);
    }

    static void postCircuitHeatStaffSpellOnCastEvent(CircuitHeatStaffBypassTestContext context, int manaCost) {
        NeoForge.EVENT_BUS.post(new SpellOnCastEvent(
                context.player(),
                context.spell().getSpellId(),
                1,
                manaCost,
                context.spell().getSchoolType(),
                CastSource.SPELLBOOK
        ));
    }

    record CircuitHeatStaffBypassTestContext(
            ServerPlayer player,
            ItemStack staffStack,
            MagicData magicData,
            AbstractSpell spell
    ) {
    }
}
