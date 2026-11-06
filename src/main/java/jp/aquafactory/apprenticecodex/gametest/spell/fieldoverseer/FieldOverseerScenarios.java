package jp.aquafactory.apprenticecodex.gametest.spell.fieldoverseer;

import io.netty.buffer.Unpooled;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.capabilities.magic.RecastInstance;
import io.redspace.ironsspellbooks.capabilities.magic.SummonManager;
import java.util.UUID;
import jp.aquafactory.apprenticecodex.registry.EntityRegistry;
import jp.aquafactory.apprenticecodex.registry.SpellRegistry;
import jp.aquafactory.apprenticecodex.spell.fieldoverseer.FieldOverseer;
import jp.aquafactory.apprenticecodex.spell.fieldoverseer.FieldOverseerManager;
import jp.aquafactory.apprenticecodex.spell.fieldoverseer.FieldOverseerStaffEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;

import static jp.aquafactory.apprenticecodex.gametest.support.PlayerTestSupport.createEquipmentTestPlayer;

public final class FieldOverseerScenarios {
    private FieldOverseerScenarios() {}

    static void fieldOverseerFallsWhenSupportRemoved(GameTestHelper helper) {
        var owner = createEquipmentTestPlayer(helper, new BlockPos(0, 2, -2), "field_overseer_fall_test");
        var anchorPos = helper.absolutePos(new BlockPos(0, 2, 0));
        helper.setBlock(new BlockPos(0, 1, 0), Blocks.STONE);
        var staff = createFieldOverseerTestEntity(helper, owner, anchorPos, 100.0F, 40);
        var initialY = staff.getY();

        helper.runAtTickTime(5, () -> helper.setBlock(new BlockPos(0, 1, 0), Blocks.AIR));
        helper.succeedWhen(() -> {
            helper.assertTrue(staff.isAlive() && !staff.isRemoved(),
                    "FieldOverseer staff should remain alive after losing support");
            helper.assertTrue(staff.getY() < initialY - 0.05D,
                    "FieldOverseer staff should fall after losing support: " + staff.getY());
        });
    }

    static void fieldOverseerCastDataRoundTripsPlacementAndIdentity(GameTestHelper helper) {
        var source = new FieldOverseer.FieldOverseerCastData();
        var expectedPosition = helper.absolutePos(new BlockPos(1, 2, 3));
        var expectedStaffUuid = UUID.randomUUID();
        var expectedDimension = helper.getLevel().dimension().location();
        var sourceTag = new CompoundTag();
        sourceTag.putLong("Position", expectedPosition.asLong());
        sourceTag.putUUID("Staff", expectedStaffUuid);
        sourceTag.putString("Dimension", expectedDimension.toString());
        source.deserializeNBT(helper.getLevel().registryAccess(), sourceTag);

        var buffer = new FriendlyByteBuf(Unpooled.buffer());
        source.writeToBuffer(buffer);

        var restored = new FieldOverseer.FieldOverseerCastData();
        restored.readFromBuffer(buffer);
        var restoredTag = restored.serializeNBT(helper.getLevel().registryAccess());
        helper.assertTrue(restoredTag.getLong("Position") == expectedPosition.asLong(),
                "FieldOverseer cast data should preserve placement through network serialization");
        helper.assertTrue(restoredTag.getUUID("Staff").equals(expectedStaffUuid),
                "FieldOverseer cast data should preserve the managed staff UUID");
        helper.assertTrue(restoredTag.getString("Dimension").equals(expectedDimension.toString()),
                "FieldOverseer cast data should preserve the managed dimension");
        helper.succeed();
    }

    static void fieldOverseerIgnoresOwnerDamage(GameTestHelper helper) {
        var owner = createEquipmentTestPlayer(helper, new BlockPos(0, 2, -2), "field_overseer_owner_damage_test");
        var anchorPos = helper.absolutePos(new BlockPos(0, 2, 0));
        helper.setBlock(new BlockPos(0, 1, 0), Blocks.STONE);
        var staff = createFieldOverseerTestEntity(helper, owner, anchorPos, 100.0F, 40);
        var initialHealth = staff.getHealth();

        var damaged = staff.hurt(owner.damageSources().playerAttack(owner), 5.0F);
        helper.assertFalse(damaged, "FieldOverseer staff should reject owner damage");
        helper.assertTrue(Math.abs(staff.getHealth() - initialHealth) < 0.0001F,
                "FieldOverseer staff health changed after owner damage");
        helper.succeed();
    }

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

    static void fieldOverseerPrioritizesHealthAndTransfersMana(GameTestHelper helper) {
        var owner = createEquipmentTestPlayer(helper, new BlockPos(2, 2, 0), "field_overseer_attack_test");
        var manaRegen = owner.getAttribute(AttributeRegistry.MANA_REGEN);
        if (manaRegen != null) {
            manaRegen.setBaseValue(0.0D);
        }
        var ownerMagicData = MagicData.getPlayerMagicData(owner);
        ownerMagicData.setMana(75.0F);
        var anchorPos = helper.absolutePos(new BlockPos(2, 2, 2));
        helper.setBlock(new BlockPos(2, 1, 2), Blocks.STONE);
        var staff = createFieldOverseerTestEntity(helper, owner, anchorPos, 100.0F, 40);

        // 未攻撃対象の体力不変を検証するため、日光ダメージを受けないHuskを使う。
        var highHealthTarget = helper.spawnWithNoFreeWill(EntityType.HUSK, new BlockPos(4, 2, 2));
        var mediumHealthTarget = helper.spawnWithNoFreeWill(EntityType.HUSK, new BlockPos(2, 2, 4));
        var lowHealthTarget = helper.spawnWithNoFreeWill(EntityType.HUSK, new BlockPos(0, 2, 2));
        highHealthTarget.setHealth(20.0F);
        mediumHealthTarget.setHealth(10.0F);
        lowHealthTarget.setHealth(5.0F);

        helper.runAtTickTime(52, () -> {
            helper.assertTrue(highHealthTarget.getHealth() < 20.0F,
                    "FieldOverseer should strike the highest-current-health target first");
            helper.assertTrue(Math.abs(mediumHealthTarget.getHealth() - 10.0F) < 0.0001F,
                    "FieldOverseer second strike should wait three ticks");
            helper.assertTrue(Math.abs(lowHealthTarget.getHealth() - 5.0F) < 0.0001F,
                    "FieldOverseer should not strike the lower-health target before its sequence turn");
        });
        helper.runAtTickTime(58, () -> {
            helper.assertTrue(mediumHealthTarget.getHealth() < 10.0F,
                    "FieldOverseer should perform the second strike three ticks after the first");
        });
        helper.runAtTickTime(62, () -> {
            helper.assertTrue(lowHealthTarget.getHealth() < 5.0F,
                    "FieldOverseer should perform the third strike six ticks after the first");
            helper.assertTrue(Math.abs(staff.getCurrentMana() - 100.0F) < 0.0001F,
                    "FieldOverseer should receive two 20 mana transfers after spending one attack cost");
            helper.assertTrue(Math.abs(ownerMagicData.getMana() - 35.0F) < 0.0001F,
                    "FieldOverseer should drain the same amount from the owner: " + ownerMagicData.getMana());
            helper.succeed();
        });
    }

    public static FieldOverseerStaffEntity createFieldOverseerTestEntity(
            GameTestHelper helper, FakePlayer owner, BlockPos anchorPos, float maxMana, int attackManaCost) {
        return createDurationBoundFieldOverseer(
                helper, owner, 1, anchorPos, maxMana, attackManaCost,
                ((FieldOverseer) SpellRegistry.FIELD_OVERSEER.get()).getDuration());
    }

    private static FieldOverseerStaffEntity createDurationBoundFieldOverseer(
            GameTestHelper helper, FakePlayer owner, int spellLevel, BlockPos anchorPos, int duration) {
        return createDurationBoundFieldOverseer(
                helper, owner, spellLevel, helper.absolutePos(anchorPos), 100.0F, 40, duration);
    }

    private static FieldOverseerStaffEntity createDurationBoundFieldOverseer(
            GameTestHelper helper, FakePlayer owner, int spellLevel, BlockPos anchorPos,
            float maxMana, int attackManaCost, int duration) {
        var level = helper.getLevel();
        level.addFreshEntity(owner);
        var center = Vec3.atBottomCenterOf(anchorPos);
        var staff = new FieldOverseerStaffEntity(EntityRegistry.FIELD_OVERSEER_STAFF.get(), level);
        staff.configure(anchorPos, 4.0F, 24.0D, attackManaCost, maxMana, 20);
        staff.moveTo(center.x, center.y, center.z, 0.0F, 0.0F);
        var castData = new FieldOverseer.FieldOverseerCastData();
        FieldOverseerManager.initialize(owner, staff, anchorPos, duration, castData);
        level.addFreshEntity(staff);
        var spell = (FieldOverseer) SpellRegistry.FIELD_OVERSEER.get();
        var magicData = MagicData.getPlayerMagicData(owner);
        magicData.getPlayerRecasts().addRecast(new RecastInstance(
                spell.getSpellId(), spellLevel, spell.getRecastCount(spellLevel, owner),
                duration, CastSource.SPELLBOOK, castData), magicData);
        helper.assertFalse(SummonManager.getSummons(owner).contains(staff.getUUID()),
                "FieldOverseer should not register itself with Iron's SummonManager");
        return staff;
    }
}
