package jp.aquafactory.apprenticecodex.gametest.equipment.multicastechostaff.lifecycle;

import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.entity.spells.target_area.TargetedAreaEntity;
import jp.aquafactory.apprenticecodex.item.multicastechostaff.MulticastEchoStaffCastHelper;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;

import static jp.aquafactory.apprenticecodex.gametest.equipment.multicastechostaff.support.EchoStaffCastSupport.completeMulticastEchoStaffCast;
import static jp.aquafactory.apprenticecodex.gametest.support.PlayerTestSupport.createEquipmentTestPlayer;

final class EchoStaffIndicatorScenarios {
    private EchoStaffIndicatorScenarios() {}

    static void multicastEchoStaffRepeatedFortifyClearsTargetAreaIndicator(GameTestHelper helper) {
        var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "multicast_echo_staff_fortify_indicator_test");
        var staffStack = new ItemStack(ItemRegistry.MULTICAST_ECHO_STAFF.get());
        var spell = SpellRegistry.FORTIFY_SPELL.get();
        var spellLevel = 1;
        var amplifier = 0;
        var manaCost = spell.getManaCost(spellLevel);
        player.setItemInHand(InteractionHand.MAIN_HAND, staffStack);

        helper.runAtTickTime(1, () -> {
            completeMulticastEchoStaffCast(helper.getLevel(), player, staffStack, spell, spellLevel, amplifier, manaCost * 3.0F);
        });

        helper.runAtTickTime(4, () -> {
            MulticastEchoStaffCastHelper.onPlayerTick(new TickEvent.PlayerTickEvent(TickEvent.Phase.END, player));

            var magicData = MagicData.getPlayerMagicData(player);
            var targetAreas = helper.getLevel().getEntitiesOfClass(
                    TargetedAreaEntity.class,
                    player.getBoundingBox().inflate(16.0D),
                    targetArea -> !targetArea.isRemoved()
            );

            helper.assertTrue(magicData.getAdditionalCastData() == null,
                    "Repeated Fortify multicast should clear temporary cast data after sending cast data to the client");
            helper.assertTrue(targetAreas.isEmpty(),
                    "Repeated Fortify multicast should discard temporary target area indicators: " + targetAreas.size());
            helper.succeed();
        });
    }
}
