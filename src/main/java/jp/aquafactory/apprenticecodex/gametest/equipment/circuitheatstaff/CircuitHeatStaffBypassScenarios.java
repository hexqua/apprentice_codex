package jp.aquafactory.apprenticecodex.gametest.equipment.circuitheatstaff;

import io.redspace.ironsspellbooks.api.events.SpellOnCastEvent;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.magic.MagicHelper;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import jp.aquafactory.apprenticecodex.item.offhand.AbstractOffhandMagicItem;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;

import static jp.aquafactory.apprenticecodex.gametest.support.PlayerTestSupport.createEquipmentTestPlayer;
import static jp.aquafactory.apprenticecodex.gametest.support.SpellContainerTestSupport.setSingleUnlockedSpell;

final class CircuitHeatStaffBypassScenarios {
    private CircuitHeatStaffBypassScenarios() {}

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
        MinecraftForge.EVENT_BUS.post(new SpellOnCastEvent(
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
