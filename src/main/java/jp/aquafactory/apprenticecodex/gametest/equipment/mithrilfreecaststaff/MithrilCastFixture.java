package jp.aquafactory.apprenticecodex.gametest.equipment.mithrilfreecaststaff;

import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.magic.SpellSelectionManager;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.capabilities.magic.RecastResult;
import io.redspace.ironsspellbooks.gui.overlays.SpellSelection;
import java.util.List;
import jp.aquafactory.apprenticecodex.gametest.crafting.calibration.SpellCalibrationAdjustmentGameTestSupport;
import jp.aquafactory.apprenticecodex.item.mithrilfreecaststaff.MithrilFreecastStaff;
import jp.aquafactory.apprenticecodex.item.mithrilfreecaststaff.MithrilFreecastStaffCastContext;
import jp.aquafactory.apprenticecodex.item.scrollcastergauntlet.ScrollcasterGauntlet;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import jp.aquafactory.apprenticecodex.registry.SpellRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.util.FakePlayer;

import static jp.aquafactory.apprenticecodex.gametest.support.PlayerTestSupport.createEquipmentTestPlayer;
import static jp.aquafactory.apprenticecodex.gametest.support.SpellTestSupport.createSpellScroll;

final class MithrilCastFixture implements AutoCloseable {
    final FakePlayer player;
    final MagicData magicData;
    final ItemStack staff;
    final ItemStack gauntlet;
    final MithrilFreecastStaff staffItem;
    final AbstractSpell magicMissile;

    MithrilCastFixture(GameTestHelper helper) {
        player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0),
                "mithril_freecast_selected_cooldown_test");
        magicData = MagicData.getPlayerMagicData(player);
        helper.assertTrue(magicData != null,
                "Mithril Freecast Staff selected cooldown test could not resolve player magic data");
        magicData.setMana(1000.0F);

        staff = new ItemStack(ItemRegistry.MITHRIL_FREECAST_STAFF.get());
        staffItem = (MithrilFreecastStaff) staff.getItem();
        SpellCalibrationAdjustmentGameTestSupport.setCalibrationAdjustment(
                staff,
                0,
                new ItemStack(io.redspace.ironsspellbooks.registries.ItemRegistry.SILVER_RING.get())
        );
        gauntlet = new ItemStack(ItemRegistry.SCROLLCASTER_GAUNTLET.get());
        magicMissile = io.redspace.ironsspellbooks.api.registry.SpellRegistry.MAGIC_MISSILE_SPELL.get();
        ScrollcasterGauntlet.setCalibrationScroll(gauntlet, 0, createSpellScroll(magicMissile));
        ScrollcasterGauntlet.setSelectedScrollIndex(gauntlet, 0);

        player.setItemInHand(InteractionHand.MAIN_HAND, staff);
        player.setItemInHand(InteractionHand.OFF_HAND, gauntlet);
        magicData.getSyncedData().setSpellSelection(new SpellSelection(SpellSelectionManager.OFFHAND, 0));
    }

    @Override
    public void close() {
        try {
            magicData.resetCastingState();
            magicData.resetAdditionalCastData();
            for (var spell : List.of(magicMissile, SpellRegistry.BOUND_BOW.get())) {
                var recast = magicData.getPlayerRecasts().getRecastInstance(spell.getSpellId());
                if (recast != null) {
                    magicData.getPlayerRecasts().removeRecast(recast, RecastResult.TIMEOUT);
                }
            }
        } finally {
            // 遅延・再詠唱の保持はstatic stateなので、途中失敗でも自分のplayer分を消す。
            for (var spell : List.of(magicMissile,
                    io.redspace.ironsspellbooks.api.registry.SpellRegistry.HEAL_SPELL.get(),
                    SpellRegistry.BOUND_BOW.get(), SpellRegistry.HARVEST_MOON.get(),
                    SpellRegistry.THERMAL_PROCESS.get(), SpellRegistry.ARTISAN_SMASH.get())) {
                MithrilFreecastStaffCastContext.clearPendingCooldownSource(player.getUUID(), spell);
                magicData.getPlayerCooldowns().removeCooldown(spell.getSpellId());
            }
            player.getInventory().clearContent();
        }
    }
}
