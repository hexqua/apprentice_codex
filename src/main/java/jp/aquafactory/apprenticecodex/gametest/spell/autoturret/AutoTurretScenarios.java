package jp.aquafactory.apprenticecodex.gametest.spell.autoturret;

import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import java.util.ArrayList;
import jp.aquafactory.apprenticecodex.registry.EntityRegistry;
import jp.aquafactory.apprenticecodex.registry.SpellRegistry;
import jp.aquafactory.apprenticecodex.spell.autoturret.AutoTurret;
import jp.aquafactory.apprenticecodex.spell.autoturret.AutoTurretEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.block.state.properties.StairsShape;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;

import static jp.aquafactory.apprenticecodex.gametest.support.PlayerTestSupport.createEquipmentTestPlayer;

final class AutoTurretScenarios {
    private AutoTurretScenarios() {}

    static void autoTurretCanBePlacedOnSupportedSlab(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var owner = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "auto_turret_slab_test");
            var anchorPos = new BlockPos(0, 2, 0);
            helper.setBlock(anchorPos.below(), Blocks.STONE_SLAB.defaultBlockState()
                    .setValue(SlabBlock.TYPE, SlabType.BOTTOM));

            castAutoTurret(helper, owner, 1, anchorPos);

            var turret = getSingleLivingAutoTurret(helper, owner);
            var expectedY = helper.absolutePos(anchorPos).below().getY() + 0.5D;
            helper.assertTrue(Math.abs(turret.getY() - expectedY) < 0.01D,
                    "AutoTurret should sit on the slab support top: " + turret.getY());
        });
    }

    static void autoTurretCanBePlacedOnSupportedStairs(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var owner = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "auto_turret_stairs_test");
            var anchorPos = new BlockPos(0, 2, 0);
            helper.setBlock(anchorPos.below(), Blocks.STONE_STAIRS.defaultBlockState()
                    .setValue(StairBlock.FACING, Direction.NORTH)
                    .setValue(StairBlock.HALF, Half.BOTTOM)
                    .setValue(StairBlock.SHAPE, StairsShape.STRAIGHT));

            castAutoTurret(helper, owner, 1, anchorPos);

            var turret = getSingleLivingAutoTurret(helper, owner);
            var expectedY = helper.absolutePos(anchorPos).getY();
            helper.assertTrue(Math.abs(turret.getY() - expectedY) < 0.01D,
                    "AutoTurret should accept a normal stair as support: " + turret.getY());
        });
    }

    static void autoTurretFallsWhenSupportRemoved(GameTestHelper helper) {
        var owner = createEquipmentTestPlayer(helper, new BlockPos(0, 6, 0), "auto_turret_fall_test");
        var anchorPos = new BlockPos(0, 6, 0);
        helper.setBlock(anchorPos.below(), Blocks.STONE);
        helper.getLevel().addFreshEntity(owner);
        castAutoTurret(helper, owner, 1, anchorPos);
        var turret = getSingleLivingAutoTurret(helper, owner);
        var initialY = turret.getY();

        helper.runAtTickTime(1, () -> helper.setBlock(anchorPos.below(), Blocks.AIR));

        helper.succeedWhen(() -> {
            helper.assertTrue(turret.isAlive() && !turret.isRemoved(),
                    "AutoTurret should remain alive while falling after support removal");
            helper.assertTrue(turret.getY() < initialY - 0.05D,
                    "AutoTurret should fall below its anchored position after support removal: " + turret.getY());
        });
    }

    static void autoTurretRestockConsumesManaAndRestoresAmmo(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var owner = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "auto_turret_restock_test");
            var turret = createAutoTurretRestockTestEntity(helper, owner, new BlockPos(1, 2, 0), 5, 17);
            var magicData = MagicData.getPlayerMagicData(owner);
            magicData.setMana(30.0F);
            turret.setRestBulletCount(2);

            var result = turret.mobInteract(owner, InteractionHand.MAIN_HAND);

            helper.assertTrue(result == InteractionResult.CONSUME,
                    "AutoTurret restock should consume the owner interaction");
            helper.assertTrue(turret.getRestBulletCount() == 5,
                    "AutoTurret restock should restore ammo to the initial count: " + turret.getRestBulletCount());
            helper.assertTrue(Math.abs(magicData.getMana() - 13.0F) < 1.0E-4F,
                    "AutoTurret restock should spend configured mana: " + magicData.getMana());
        });
    }

    static void autoTurretRestockFullAmmoDoesNotSpendMana(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var owner = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "auto_turret_restock_full_test");
            var turret = createAutoTurretRestockTestEntity(helper, owner, new BlockPos(1, 2, 0), 5, 17);
            var magicData = MagicData.getPlayerMagicData(owner);
            magicData.setMana(30.0F);

            var result = turret.mobInteract(owner, InteractionHand.MAIN_HAND);

            helper.assertTrue(result == InteractionResult.CONSUME,
                    "AutoTurret full-ammo restock should consume the owner interaction");
            helper.assertTrue(turret.getRestBulletCount() == 5,
                    "AutoTurret full-ammo restock should not change ammo: " + turret.getRestBulletCount());
            helper.assertTrue(Math.abs(magicData.getMana() - 30.0F) < 1.0E-4F,
                    "AutoTurret full-ammo restock should not spend mana: " + magicData.getMana());
        });
    }

    static void autoTurretRestockInsufficientManaDoesNotRestoreAmmo(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var owner = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "auto_turret_restock_insufficient_test");
            var turret = createAutoTurretRestockTestEntity(helper, owner, new BlockPos(1, 2, 0), 5, 17);
            var magicData = MagicData.getPlayerMagicData(owner);
            magicData.setMana(16.0F);
            turret.setRestBulletCount(2);

            var result = turret.mobInteract(owner, InteractionHand.MAIN_HAND);

            helper.assertTrue(result == InteractionResult.CONSUME,
                    "AutoTurret insufficient-mana restock should consume the owner interaction");
            helper.assertTrue(turret.getRestBulletCount() == 2,
                    "AutoTurret insufficient-mana restock should not restore ammo: " + turret.getRestBulletCount());
            helper.assertTrue(Math.abs(magicData.getMana() - 16.0F) < 1.0E-4F,
                    "AutoTurret insufficient-mana restock should not spend mana: " + magicData.getMana());
        });
    }

    static void autoTurretAmmoDepletionKeepsAliveAndRestockClearsDiscardDelay(GameTestHelper helper) {
        var level = helper.getLevel();
        var owner = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "auto_turret_discard_delay_test");
        var turret = createAutoTurretRestockTestEntity(helper, owner, new BlockPos(1, 2, 0), 3, 10);
        var magicData = MagicData.getPlayerMagicData(owner);
        magicData.setMana(10.0F);
        turret.setDamage(100.0F);
        turret.setRestBulletCount(1);
        level.addFreshEntity(owner);
        helper.spawn(EntityType.ZOMBIE, new BlockPos(1, 2, 4));

        helper.runAtTickTime(60, () -> {
            helper.assertTrue(!turret.isRemoved() && turret.isAlive(),
                    "AutoTurret should remain alive during the ammo depletion grace period");
            helper.assertTrue(turret.getRestBulletCount() <= 0,
                    "AutoTurret should have spent its final shot before restock: " + turret.getRestBulletCount());
            var result = turret.mobInteract(owner, InteractionHand.MAIN_HAND);
            helper.assertTrue(result == InteractionResult.CONSUME,
                    "AutoTurret restock during discard delay should consume the owner interaction");
            helper.assertTrue(turret.getRestBulletCount() == 3,
                    "AutoTurret restock should restore ammo during discard delay: " + turret.getRestBulletCount());
        });

        helper.runAtTickTime(140, () -> {
            helper.assertTrue(!turret.isRemoved() && turret.isAlive(),
                    "AutoTurret restock should clear the ammo depletion discard timer");
            helper.succeed();
        });
    }

    private static AutoTurretEntity createAutoTurretRestockTestEntity(
            GameTestHelper helper,
            FakePlayer owner,
            BlockPos localPos,
            int initialBulletCount,
            int restockManaCost
    ) {
        var level = helper.getLevel();
        var absolutePos = helper.absolutePos(localPos);
        var center = Vec3.atBottomCenterOf(absolutePos);
        var turret = new AutoTurretEntity(EntityRegistry.AUTO_TURRET.get(), level);
        turret.setOwner(owner);
        turret.setAnchorPos(absolutePos);
        turret.setDamage(4.0F);
        turret.setRestockData(initialBulletCount, restockManaCost);
        turret.setTurretMaxHealth(20.0F);
        turret.moveTo(center.x, center.y, center.z, 0.0F, 0.0F);
        level.addFreshEntity(turret);
        return turret;
    }

    private static void castAutoTurret(GameTestHelper helper, FakePlayer player, int spellLevel, BlockPos anchorPos) {
        var spell = (AutoTurret) SpellRegistry.AUTO_TURRET.get();
        var castData = new AutoTurret.AutoTurretCastData();
        var absoluteAnchorPos = helper.absolutePos(anchorPos);
        var tag = new CompoundTag();
        tag.putInt("PositionX", absoluteAnchorPos.getX());
        tag.putInt("PositionY", absoluteAnchorPos.getY());
        tag.putInt("PositionZ", absoluteAnchorPos.getZ());
        castData.deserializeNBT(helper.getLevel().registryAccess(), tag);
        var magicData = MagicData.getPlayerMagicData(player);
        magicData.setAdditionalCastData(castData);
        spell.onCast(helper.getLevel(), spellLevel, player, CastSource.SPELLBOOK, magicData);
    }

    private static AutoTurretEntity getSingleLivingAutoTurret(GameTestHelper helper, FakePlayer owner) {
        var turrets = new ArrayList<AutoTurretEntity>();
        for (var entity : helper.getLevel().getAllEntities()) {
            if (entity instanceof AutoTurretEntity turret
                    && turret.isAlive()
                    && turret.getOwner() != null
                    && owner.getUUID().equals(turret.getOwner().getUUID())) {
                turrets.add(turret);
            }
        }
        helper.assertTrue(turrets.size() == 1, "Expected exactly one living AutoTurret but found " + turrets.size());
        return turrets.get(0);
    }
}
