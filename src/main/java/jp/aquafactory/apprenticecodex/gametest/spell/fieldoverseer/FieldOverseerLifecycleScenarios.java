package jp.aquafactory.apprenticecodex.gametest.spell.fieldoverseer;

import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import jp.aquafactory.apprenticecodex.registry.EntityRegistry;
import jp.aquafactory.apprenticecodex.registry.SpellRegistry;
import jp.aquafactory.apprenticecodex.spell.fieldoverseer.FieldOverseer;
import jp.aquafactory.apprenticecodex.spell.fieldoverseer.FieldOverseerManager;
import jp.aquafactory.apprenticecodex.spell.fieldoverseer.FieldOverseerStaffEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Blocks;

import static jp.aquafactory.apprenticecodex.gametest.equipment.monarchbond.MonarchBondLegacyScenarios.createFieldOverseerTestEntity;
import static jp.aquafactory.apprenticecodex.gametest.spell.fieldoverseer.FieldOverseerTestSupport.createDurationBoundFieldOverseer;
import static jp.aquafactory.apprenticecodex.gametest.support.PlayerTestSupport.createEquipmentTestPlayer;

final class FieldOverseerLifecycleScenarios {
    private FieldOverseerLifecycleScenarios() {}

    static void fieldOverseerUsesDurationBoundPersistencePolicy(GameTestHelper helper) {
        var owner = createEquipmentTestPlayer(helper, new BlockPos(0, 2, -2), "field_overseer_persistence_test");
        var anchorPos = helper.absolutePos(new BlockPos(0, 2, 0));
        helper.setBlock(new BlockPos(0, 1, 0), Blocks.STONE);
        var staff = createFieldOverseerTestEntity(helper, owner, anchorPos, 100.0F, 40);

        helper.assertTrue(staff.shouldBeSaved(),
                "FieldOverseer staff should survive chunk save and reload during its summon duration");
        helper.assertFalse(staff.removeWhenFarAway(Double.MAX_VALUE),
                "FieldOverseer staff should not despawn when the owner moves far away");
        var savedStaff = new CompoundTag();
        staff.saveWithoutId(savedStaff);
        helper.assertTrue(savedStaff.contains("ExpirationGameTime"),
                "FieldOverseer staff should persist its absolute expiration time");
        helper.succeed();
    }

    static void fieldOverseerRecastRemovesPlacedStaff(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var owner = createEquipmentTestPlayer(helper, new BlockPos(0, 2, -2), "field_overseer_recast_remove_test");
            var anchorPos = new BlockPos(0, 2, 0);
            helper.setBlock(anchorPos.below(), Blocks.STONE);
            var staff = createDurationBoundFieldOverseer(
                    helper, owner, 1, anchorPos, ((FieldOverseer) SpellRegistry.FIELD_OVERSEER.get()).getDuration());
            var magicData = MagicData.getPlayerMagicData(owner);
            helper.assertTrue(magicData.getPlayerRecasts().hasRecastForSpell(SpellRegistry.FIELD_OVERSEER.get()),
                    "FieldOverseer should create recast data after placement");

            ((FieldOverseer) SpellRegistry.FIELD_OVERSEER.get())
                    .castSpell(helper.getLevel(), 1, owner, CastSource.SPELLBOOK, true);

            helper.assertTrue(staff.isRemoved(),
                    "FieldOverseer manual recast should remove the placed staff");
            helper.assertFalse(magicData.getPlayerRecasts().hasRecastForSpell(SpellRegistry.FIELD_OVERSEER.get()),
                    "FieldOverseer manual recast should consume the active recast");
        });
    }

    static void fieldOverseerTimeoutRemovesPlacedStaff(GameTestHelper helper) {
        var owner = createEquipmentTestPlayer(helper, new BlockPos(0, 2, -2), "field_overseer_timeout_test");
        var anchorPos = new BlockPos(0, 2, 0);
        helper.setBlock(anchorPos.below(), Blocks.STONE);
        var staff = createDurationBoundFieldOverseer(helper, owner, 1, anchorPos, 1);
        var magicData = MagicData.getPlayerMagicData(owner);
        helper.assertTrue(magicData.getPlayerRecasts().hasRecastForSpell(SpellRegistry.FIELD_OVERSEER.get()),
                "FieldOverseer should have active recast data before timeout");

        helper.succeedWhen(() -> {
            helper.assertTrue(staff.isRemoved(), "FieldOverseer timeout should remove the placed staff");
            helper.assertFalse(magicData.getPlayerRecasts().hasRecastForSpell(SpellRegistry.FIELD_OVERSEER.get()),
                    "FieldOverseer timeout should consume the active recast");
        });
    }

    static void fieldOverseerCancelledWhileUnloadedDoesNotReturn(GameTestHelper helper) {
        var owner = createEquipmentTestPlayer(helper, new BlockPos(0, 2, -2), "field_overseer_unloaded_cancel_test");
        var anchorPos = new BlockPos(0, 2, 0);
        helper.setBlock(anchorPos.below(), Blocks.STONE);
        var staff = createDurationBoundFieldOverseer(helper, owner, 1, anchorPos, 200);
        var savedStaff = new CompoundTag();
        staff.saveWithoutId(savedStaff);
        staff.remove(Entity.RemovalReason.UNLOADED_TO_CHUNK);

        FieldOverseerManager.cancel(owner);
        var magicData = MagicData.getPlayerMagicData(owner);
        helper.assertFalse(magicData.getPlayerRecasts().hasRecastForSpell(SpellRegistry.FIELD_OVERSEER.get()),
                "FieldOverseer cancel should remove recast data while the staff is unloaded");
        helper.assertTrue(magicData.getPlayerCooldowns().isOnCooldown(SpellRegistry.FIELD_OVERSEER.get()),
                "FieldOverseer cancel should apply the normal cooldown");

        var restored = new FieldOverseerStaffEntity(EntityRegistry.FIELD_OVERSEER_STAFF.get(), helper.getLevel());
        restored.load(savedStaff);
        helper.getLevel().addFreshEntity(restored);
        helper.succeedWhen(() -> helper.assertTrue(restored.isRemoved(),
                "FieldOverseer loaded after cancellation should discard itself before acting"));
    }

    static void fieldOverseerDestructionEndsMatchingRecast(GameTestHelper helper) {
        var owner = createEquipmentTestPlayer(helper, new BlockPos(0, 2, -2), "field_overseer_destroy_test");
        var anchorPos = new BlockPos(0, 2, 0);
        helper.setBlock(anchorPos.below(), Blocks.STONE);
        var staff = createDurationBoundFieldOverseer(helper, owner, 1, anchorPos, 200);
        var magicData = MagicData.getPlayerMagicData(owner);

        staff.remove(Entity.RemovalReason.KILLED);

        helper.assertFalse(magicData.getPlayerRecasts().hasRecastForSpell(SpellRegistry.FIELD_OVERSEER.get()),
                "Destroying FieldOverseer should remove its matching recast");
        helper.assertTrue(magicData.getPlayerCooldowns().isOnCooldown(SpellRegistry.FIELD_OVERSEER.get()),
                "Destroying FieldOverseer should apply the normal cooldown");
        helper.succeed();
    }
}
