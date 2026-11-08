package jp.aquafactory.apprenticecodex.gametest.spell.dualacrobat;

import io.redspace.ironsspellbooks.api.events.CounterSpellEvent;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.magic.SpellSelectionManager;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.capabilities.magic.SyncedSpellData;
import jp.aquafactory.apprenticecodex.registry.SpellRegistry;
import jp.aquafactory.apprenticecodex.spell.dualacrobat.DualAcrobat;
import jp.aquafactory.apprenticecodex.spell.dualacrobat.DualAcrobatCounterSpellEvent;
import jp.aquafactory.apprenticecodex.spell.dualacrobat.DualAcrobatSmgEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import org.jetbrains.annotations.Nullable;

import static jp.aquafactory.apprenticecodex.gametest.support.PlayerTestSupport.createTrackedEquipmentTestPlayer;

final class DualAcrobatScenarios {
    private DualAcrobatScenarios() {}

    static DualAcrobat beginDualAcrobatCast(ServerLevel level, FakePlayer player, int spellLevel) {
        var spell = (DualAcrobat) SpellRegistry.DUAL_ACROBAT.get();
        var magicData = MagicData.getPlayerMagicData(player);
        magicData.setSyncedData(new SyncedSpellData(player));
        magicData.setMana(1000.0f);
        magicData.getPlayerCooldowns().removeCooldown(spell.getSpellId());
        magicData.getSyncedData();
        magicData.initiateCast(
                spell,
                spellLevel,
                spell.getEffectiveCastTime(spellLevel, player),
                CastSource.SPELLBOOK,
                SpellSelectionManager.MAINHAND
        );
        magicData.setPlayerCastingItem(new ItemStack(Items.STICK));
        spell.onCast(level, spellLevel, player, CastSource.SPELLBOOK, magicData);
        return spell;
    }

    static @Nullable DualAcrobatSmgEntity findDualAcrobatSmg(ServerLevel level, Player player) {
        var weapons = level.getEntitiesOfClass(
                DualAcrobatSmgEntity.class,
                new AABB(player.position(), player.position()).inflate(16.0D),
                weapon -> {
                    var owner = weapon.getOwner();
                    return !weapon.isRemoved() && owner != null && owner.getUUID().equals(player.getUUID());
                }
        );
        return weapons.isEmpty() ? null : weapons.get(0);
    }

    static void dualAcrobatStartsFiringAfterStartupAndContinuesWhileCasting(GameTestHelper helper) {
        helper.runAtTickTime(1, () -> {
            var level = helper.getLevel();
            prepareDualAcrobatShootingLane(helper);
            var player = createTrackedEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "dual_acrobat_startup_test");
            player.setYRot(0.0f);
            player.setYBodyRot(0.0f);
            player.setYHeadRot(0.0f);
            player.setXRot(0.0f);
            var target = helper.spawn(EntityType.HUSK, new BlockPos(0, 2, 8));
            target.setNoAi(true);
            target.getAttribute(Attributes.MAX_HEALTH).setBaseValue(200.0D);
            target.setHealth(200.0F);
            var initialHealth = target.getHealth();

            var spell = beginDualAcrobatCast(level, player, 1);
            var magicData = MagicData.getPlayerMagicData(player);
            var weapon = findDualAcrobatSmg(level, player);
            helper.assertTrue(weapon != null, "Dual Acrobat should spawn an SMG pair while casting");
            helper.assertTrue(weapon.getStartupTicksRemaining() == DualAcrobatSmgEntity.STARTUP_TICKS,
                    "Dual Acrobat should start with the full startup duration");

            helper.runAtTickTime(DualAcrobatSmgEntity.STARTUP_TICKS, () -> {
                helper.assertTrue(Math.abs(target.getHealth() - initialHealth) < 1.0e-6f,
                        "Dual Acrobat should not deal damage before completing ten startup ticks");
                helper.assertTrue(weapon.getStartupTicksRemaining() > 0,
                        "Dual Acrobat should still be starting before the tenth elapsed entity tick");
            });

            helper.runAtTickTime(DualAcrobatSmgEntity.STARTUP_TICKS + 3L, () -> {
                helper.assertTrue(target.getHealth() < initialHealth,
                        "Dual Acrobat should start damaging targets after startup completes");
                var healthAfterStartup = target.getHealth();
                target.invulnerableTime = 20;
                target.setDeltaMovement(Vec3.ZERO);

                helper.runAtTickTime(DualAcrobatSmgEntity.STARTUP_TICKS + 7L, () -> {
                    helper.assertTrue(target.getHealth() < healthAfterStartup,
                            "Dual Acrobat should keep firing during CONTINUOUS casting and bypass damage i-frames");
                    var movement = target.getDeltaMovement();
                    helper.assertTrue(Math.abs(movement.x) < 1.0e-6 && Math.abs(movement.z) < 1.0e-6,
                            "Dual Acrobat shots should not knock targets back");
                    helper.assertFalse(weapon.isRemoved(),
                            "Dual Acrobat SMGs should remain while CONTINUOUS casting is active");
                    var healthBeforeSustainedContinuousFire = target.getHealth();

                    helper.runAtTickTime(30, () -> {
                        helper.assertTrue(target.getHealth() < healthBeforeSustainedContinuousFire,
                                "Dual Acrobat should continue firing throughout a sustained CONTINUOUS cast");
                        helper.assertFalse(weapon.isRemoved(),
                                "Dual Acrobat SMGs should remain active throughout a sustained CONTINUOUS cast");
                        spell.onServerCastComplete(level, 1, player, magicData, true);
                        helper.succeed();
                    });
                });
            });
        });
    }

    static void dualAcrobatCompletionDiscardsImmediately(GameTestHelper helper) {
        helper.runAtTickTime(1, () -> {
            var level = helper.getLevel();
            prepareDualAcrobatShootingLane(helper);
            var player = createTrackedEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "dual_acrobat_completion_test");
            player.setYRot(0.0f);
            player.setYBodyRot(0.0f);
            player.setYHeadRot(0.0f);
            player.setXRot(0.0f);
            var target = helper.spawn(EntityType.HUSK, new BlockPos(0, 2, 8));
            target.setNoAi(true);
            var initialHealth = target.getHealth();

            var spell = beginDualAcrobatCast(level, player, 1);
            var magicData = MagicData.getPlayerMagicData(player);
            var weapon = findDualAcrobatSmg(level, player);
            helper.assertTrue(weapon != null, "Dual Acrobat should spawn an SMG pair while casting");

            helper.runAtTickTime(DualAcrobatSmgEntity.STARTUP_TICKS + 5L, () -> {
                helper.assertTrue(target.getHealth() < initialHealth,
                        "Dual Acrobat should be firing before normal completion is tested");
                spell.onServerCastComplete(level, 1, player, magicData, false);
                helper.assertTrue(weapon.isRemoved(),
                        "Completing Dual Acrobat should discard its SMGs immediately");
                var healthAtCompletion = target.getHealth();

                helper.runAtTickTime(DualAcrobatSmgEntity.STARTUP_TICKS + 10L, () -> {
                    helper.assertTrue(Math.abs(target.getHealth() - healthAtCompletion) < 1.0e-6f,
                            "Completed Dual Acrobat should not deal damage after its SMGs are discarded");
                    helper.succeed();
                });
            });
        });
    }

    static void dualAcrobatCancelledInterruptionDiscardsImmediately(GameTestHelper helper) {
        helper.runAtTickTime(1, () -> {
            var level = helper.getLevel();
            prepareDualAcrobatShootingLane(helper);
            var player = createTrackedEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "dual_acrobat_cancelled_test");
            player.setYRot(0.0f);
            player.setYBodyRot(0.0f);
            player.setYHeadRot(0.0f);
            player.setXRot(0.0f);
            var target = helper.spawn(EntityType.HUSK, new BlockPos(0, 2, 8));
            target.setNoAi(true);
            var initialHealth = target.getHealth();

            var spell = beginDualAcrobatCast(level, player, 1);
            var magicData = MagicData.getPlayerMagicData(player);
            var weapon = findDualAcrobatSmg(level, player);
            helper.assertTrue(weapon != null, "Dual Acrobat should spawn an SMG pair while casting");

            helper.runAtTickTime(4, () -> {
                helper.assertTrue(weapon.getStartupTicksRemaining() > 0,
                        "Cancelled Dual Acrobat should still be in startup before interruption");
                spell.onServerCastComplete(level, 1, player, magicData, true);
                helper.assertTrue(weapon.isRemoved(),
                        "Cancelling Dual Acrobat should discard its SMGs immediately");

                helper.runAtTickTime(DualAcrobatSmgEntity.STARTUP_TICKS + 3L, () -> {
                    helper.assertTrue(Math.abs(target.getHealth() - initialHealth) < 1.0e-6f,
                            "Cancelled Dual Acrobat should not start firing after the interruption");
                    helper.succeed();
                });
            });
        });
    }

    static void prepareDualAcrobatShootingLane(GameTestHelper helper) {
        for (var x = -1; x <= 1; ++x) {
            for (var z = 0; z <= 8; ++z) {
                helper.setBlock(new BlockPos(x, 1, z), Blocks.STONE);
                helper.setBlock(new BlockPos(x, 2, z), Blocks.AIR);
                helper.setBlock(new BlockPos(x, 3, z), Blocks.AIR);
                helper.setBlock(new BlockPos(x, 4, z), Blocks.AIR);
            }
        }
    }

    static void dualAcrobatCounterspellInterruptDiscardsImmediately(GameTestHelper helper) {
        helper.runAtTickTime(1, () -> {
            var level = helper.getLevel();
            var player = createTrackedEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "dual_acrobat_counter_target_test");
            player.setYRot(0.0f);
            player.setYBodyRot(0.0f);
            player.setYHeadRot(0.0f);
            player.setXRot(0.0f);
            var counterCaster = createTrackedEquipmentTestPlayer(helper, new BlockPos(0, 2, 4), "dual_acrobat_counter_caster_test");
            var spell = beginDualAcrobatCast(level, player, 1);
            var magicData = MagicData.getPlayerMagicData(player);
            var weapon = findDualAcrobatSmg(level, player);
            helper.assertTrue(weapon != null, "Dual Acrobat should have an SMG pair before Counterspell");

            helper.runAtTickTime(DualAcrobatSmgEntity.STARTUP_TICKS + 5L, () -> {
                helper.assertTrue(weapon.getStartupTicksRemaining() == 0,
                        "Counterspell regression should exercise Dual Acrobat while it is firing");
                var counterspell = new CounterSpellEvent(counterCaster, player);
                DualAcrobatCounterSpellEvent.onCounterSpell(counterspell);
                helper.assertTrue(weapon.isRemoved(),
                        "Counterspell should discard the target's Dual Acrobat SMGs immediately");
                spell.onServerCastComplete(level, 1, player, magicData, true);
                helper.succeed();
            });
        });
    }

    static void dualAcrobatCounterspellDoesNotInterruptNearbyOtherOwnerWeapon(GameTestHelper helper) {
        helper.runAtTickTime(1, () -> {
            var level = helper.getLevel();
            var weaponOwner = createTrackedEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "dual_acrobat_other_owner_test");
            var counterTarget = createTrackedEquipmentTestPlayer(helper, new BlockPos(1, 2, 0), "dual_acrobat_unrelated_target_test");
            var counterCaster = createTrackedEquipmentTestPlayer(helper, new BlockPos(0, 2, 4), "dual_acrobat_unrelated_counter_caster_test");
            var spell = beginDualAcrobatCast(level, weaponOwner, 1);
            var magicData = MagicData.getPlayerMagicData(weaponOwner);
            var weapon = findDualAcrobatSmg(level, weaponOwner);
            helper.assertTrue(weapon != null, "Dual Acrobat should have an SMG pair before unrelated Counterspell");

            helper.runAtTickTime(DualAcrobatSmgEntity.STARTUP_TICKS + 5L, () -> {
                helper.assertTrue(weapon.getStartupTicksRemaining() == 0,
                        "Other-owner Counterspell regression should exercise a firing Dual Acrobat");
                var counterspell = new CounterSpellEvent(counterCaster, counterTarget);
                DualAcrobatCounterSpellEvent.onCounterSpell(counterspell);

                helper.assertFalse(weapon.isRemoved(),
                        "Counterspell should not discard a nearby other owner's Dual Acrobat SMGs");
                spell.onServerCastComplete(level, 1, weaponOwner, magicData, true);
                helper.succeed();
            });
        });
    }
}
