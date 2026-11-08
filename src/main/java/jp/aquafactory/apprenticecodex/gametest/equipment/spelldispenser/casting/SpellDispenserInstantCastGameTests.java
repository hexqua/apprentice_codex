package jp.aquafactory.apprenticecodex.gametest.equipment.spelldispenser.casting;

import io.redspace.ironsspellbooks.entity.spells.magic_missile.MagicMissileProjectile;
import io.redspace.ironsspellbooks.entity.spells.spectral_hammer.SpectralHammer;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.block.spelldispenser.*;
import jp.aquafactory.apprenticecodex.gametest.equipment.spelldispenser.support.DispenserTestScope;
import jp.aquafactory.apprenticecodex.registry.BlockRegistry;
import jp.aquafactory.apprenticecodex.registry.SpellRegistry;
import jp.aquafactory.apprenticecodex.spell.compoundphial.CompoundPhialProjectileEntity;
import jp.aquafactory.apprenticecodex.spell.precisionjack.PrecisionJackKnifeEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.Map;

import static jp.aquafactory.apprenticecodex.gametest.equipment.spelldispenser.support.SpellDispenserTestSupport.createSpellDispenserOwnerProfile;
import static jp.aquafactory.apprenticecodex.gametest.equipment.spelldispenser.support.SpellDispenserTestSupport.createSpellScroll;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class SpellDispenserInstantCastGameTests {
    private SpellDispenserInstantCastGameTests() {
    }

    @GameTest(template = "gametest/dispenser_casting")
    public static void spellDispenserCastHelperCleansUpProxy(GameTestHelper helper) {
        helper.succeedIf(() -> {
            try (var scope = new DispenserTestScope(helper)) {
                var level = helper.getLevel();
                var castPos = helper.absolutePos(new BlockPos(2, 3, 2));
                var spell = io.redspace.ironsspellbooks.api.registry.SpellRegistry.MAGIC_MISSILE_SPELL.get();
                var scrollStack = createSpellScroll(spell);

                var castResult = scope.capture(() -> SpellDispenserCastHelper.tryCast(
                        (ServerLevel) level,
                        castPos,
                        Direction.NORTH,
                        scrollStack,
                        createSpellDispenserOwnerProfile("spell_dispenser_magic_missile_test")
                ));
                helper.assertTrue(castResult.succeeded(), "Spell Dispenser cast helper failed to cast a Magic Missile scroll");
                helper.assertTrue(castResult.reachedOnCast(), "Spell Dispenser Magic Missile cast did not mark that it reached onCast");
                helper.assertTrue(castResult.cooldownTicks() == spell.getSpellCooldown(),
                        "Spell Dispenser Magic Missile cast returned the wrong cooldown: " + castResult.cooldownTicks());
                scope.assertReleased(helper, "Spell Dispenser proxy caster was left behind after Magic Missile cast");
            }
        });
    }

    @GameTest(template = "gametest/dispenser_casting")
    public static void spellDispenserCastHelperAllowsOwnerOptionalSpellWithoutOwnerProfile(GameTestHelper helper) {
        helper.succeedIf(() -> {
            try (var scope = new DispenserTestScope(helper)) {
                var level = helper.getLevel();
                var castPos = helper.absolutePos(new BlockPos(2, 3, 2));
                var spell = io.redspace.ironsspellbooks.api.registry.SpellRegistry.MAGIC_MISSILE_SPELL.get();
                var scrollStack = createSpellScroll(spell);

                var castResult = scope.capture(() -> SpellDispenserCastHelper.tryCast(
                        (ServerLevel) level,
                        castPos,
                        Direction.NORTH,
                        scrollStack,
                        null
                ));
                helper.assertTrue(castResult.succeeded(), "Spell Dispenser owner-optional cast failed without an owner profile");
                helper.assertTrue(castResult.reachedOnCast(), "Spell Dispenser owner-optional cast did not reach onCast");
                scope.assertReleased(helper, "Spell Dispenser owner-optional cast left proxy state behind");
            }
        });
    }

    @GameTest(template = "gametest/dispenser_casting")
    public static void spellDispenserWizardlampPlacesLanternInAirWithoutBlockTarget(GameTestHelper helper) {
        helper.succeedIf(() -> {
            try (var scope = new DispenserTestScope(helper)) {
                var level = helper.getLevel();
                var castPos = helper.absolutePos(new BlockPos(5, 4, 8));
                var castOrigin = helper.absoluteVec(new Vec3(5.5D, 4.5D, 8.5D));
                var forward = new Vec3(0.0D, 0.0D, -1.0D);
                var expectedPos = BlockPos.containing(castOrigin.add(forward.scale(6.0D)));
                var scrollStack = createSpellScroll(SpellRegistry.WIZARDLAMP.get());

                for (var distance = 0; distance <= 6; distance++) {
                    level.setBlockAndUpdate(BlockPos.containing(castOrigin.add(forward.scale(distance))), Blocks.AIR.defaultBlockState());
                }
                level.setBlockAndUpdate(expectedPos.below(), Blocks.AIR.defaultBlockState());
                helper.assertTrue(level.getBlockState(expectedPos).isAir() && level.getBlockState(expectedPos.below()).isAir(),
                        "Spell Dispenser Wizardlamp aerial placement fixture was not empty");
                try (var ignored = SpellDispenserCastFrameBridge.useProjectorForGameTest((serverLevel, sourcePosition, frame) ->
                        new SpellDispenserCastFrameBridge.CastFrame(castOrigin, forward))) {
                    var castResult = scope.capture(() -> SpellDispenserCastHelper.tryCast(level, castPos, Direction.NORTH, scrollStack, null));

                    helper.assertTrue(castResult.succeeded(),
                            "Spell Dispenser Wizardlamp failed without a block target: failureType="
                                    + castResult.failureType() + ", validation=" + castResult.validation().failureReason());
                    helper.assertTrue(level.getBlockState(expectedPos).is(BlockRegistry.WIZARDLAMP_LANTERN.get()),
                            "Spell Dispenser Wizardlamp did not place a lantern at its aerial range endpoint");
                }
            }
        });
    }

    @GameTest(template = "gametest/dispenser_casting")
    public static void spellDispenserCastHelperUsesNeutralLivingCasterProfileForMagicMissile(GameTestHelper helper) {
        try (var scope = new DispenserTestScope(helper)) {
            var level = (ServerLevel) helper.getLevel();
            var castPos = helper.absolutePos(new BlockPos(2, 3, 2));
            var spell = io.redspace.ironsspellbooks.api.registry.SpellRegistry.MAGIC_MISSILE_SPELL.get();
            var scrollStack = createSpellScroll(spell);
            var neutralProfile = new SpellDispenserSpellProfile(
                    SpellDispenserCastAnchorMode.AUTO,
                    SpellDispenserCasterMode.NEUTRAL_LIVING,
                    0.0D,
                    0.0D,
                    0.0D,
                    0.0F,
                    0.0F,
                    false
            );
            try (var ignored = SpellDispenserSpellProfileManager.useProfilesForGameTest(Map.of(spell.getSpellResource(), neutralProfile))) {
                var castResult = scope.capture(() -> SpellDispenserCastHelper.tryCast(
                        level,
                        castPos,
                        Direction.NORTH,
                        scrollStack,
                        null
                ));
                helper.assertTrue(castResult.succeeded(), "Spell Dispenser neutral living cast failed for Magic Missile");
                helper.assertTrue(castResult.reachedOnCast(), "Spell Dispenser neutral living cast did not reach onCast");
            }

            var spawnedProjectiles = scope.entities(MagicMissileProjectile.class);

            helper.assertTrue(!spawnedProjectiles.isEmpty(), "Spell Dispenser neutral living cast did not spawn Magic Missile");
            var owner = spawnedProjectiles.get(0).getOwner();
            helper.assertTrue(owner instanceof ArmorStand, "Spell Dispenser neutral living cast did not use ArmorStand owner: " + owner);
            helper.assertTrue(owner.hasCustomName(), "Spell Dispenser neutral living caster did not set a display name");
            helper.assertTrue(!owner.isCustomNameVisible(), "Spell Dispenser neutral living caster exposes its nameplate");
            helper.assertTrue(!owner.isSilent(), "Spell Dispenser neutral living caster suppresses cast sound");
            helper.assertTrue(!(owner instanceof Player), "Spell Dispenser neutral living cast used a Player owner: " + owner);
            helper.assertTrue(!(owner instanceof FakePlayer), "Spell Dispenser neutral living cast used a FakePlayer owner: " + owner);
            scope.assertReleased(helper, "Spell Dispenser neutral living cast left proxy state behind");
            helper.succeed();
        }
    }

    @GameTest(template = "gametest/dispenser_casting")
    public static void spellDispenserCastHelperCompletesLongCastImmediately(GameTestHelper helper) {
        helper.succeedIf(() -> {
            try (var scope = new DispenserTestScope(helper)) {
                var level = helper.getLevel();
                var castPos = helper.absolutePos(new BlockPos(2, 3, 2));
                var spell = SpellRegistry.COMPOUND_PHIAL.get();
                var scrollStack = createSpellScroll(spell);

                var castResult = scope.capture(() -> SpellDispenserCastHelper.tryCast(
                        (ServerLevel) level,
                        castPos,
                        Direction.NORTH,
                        scrollStack,
                        createSpellDispenserOwnerProfile("spell_dispenser_long_test")
                ));
                helper.assertTrue(castResult.succeeded(), "Spell Dispenser cast helper failed to cast a LONG Compound Phial scroll");
                var expectedCooldown = spell.getSpellCooldown()
                        + spell.getEffectiveCastTime(1, new FakePlayer((ServerLevel) level, createSpellDispenserOwnerProfile("spell_dispenser_long_probe")));
                helper.assertTrue(castResult.cooldownTicks() == expectedCooldown,
                        "Spell Dispenser LONG cast returned the wrong cooldown: " + castResult.cooldownTicks() + " / expected " + expectedCooldown);
                scope.assertReleased(helper, "Spell Dispenser proxy caster was left behind after LONG cast");

                var projectiles = scope.entities(CompoundPhialProjectileEntity.class);
                helper.assertTrue(!projectiles.isEmpty(), "Spell Dispenser LONG cast completed without spawning a Compound Phial projectile");
            }
        });
    }

    @GameTest(template = "gametest/dispenser_casting")
    public static void spellDispenserCastHelperTicksLongCastBeforeCompletion(GameTestHelper helper) {
        helper.succeedIf(() -> {
            try (var scope = new DispenserTestScope(helper)) {
                var level = helper.getLevel();
                var castPos = helper.absolutePos(new BlockPos(2, 3, 2));
                var spell = SpellRegistry.PRECISION_JACK.get();
                var scrollStack = createSpellScroll(spell);

                var castResult = scope.capture(() -> SpellDispenserCastHelper.tryCast(
                        (ServerLevel) level,
                        castPos,
                        Direction.NORTH,
                        scrollStack,
                        createSpellDispenserOwnerProfile("spell_dispenser_precision_jack_test")
                ));
                helper.assertTrue(castResult.succeeded(), "Spell Dispenser cast helper failed to cast a Precision Jack scroll");

                var knives = scope.entities(PrecisionJackKnifeEntity.class);
                helper.assertTrue(!knives.isEmpty(), "Spell Dispenser Precision Jack cast completed without spawning a knife");
                helper.assertTrue(knives.stream().anyMatch(PrecisionJackKnifeEntity::isTrailActive),
                        "Spell Dispenser LONG cast completed without calling Precision Jack's server cast tick");
            }
        });
    }

    @GameTest(template = "gametest/dispenser_casting")
    public static void spellDispenserPrecisionJackLowManaCleansUpKnife(GameTestHelper helper) {
        var scope = new DispenserTestScope(helper);
        var level = helper.getLevel();
        // ナイフは目線より下、かつ前方へ召喚される。床上と水平範囲の両方を確保し、
        // 強制ロードや手動tickに頼らず寿命による破棄を検証する。
        var pos = new BlockPos(2, 3, 2);
        var absolutePos = helper.absolutePos(pos);
        helper.setBlock(pos, BlockRegistry.SPELL_DISPENSER.get());

        var blockEntity = helper.getBlockEntity(pos);
        helper.assertTrue(blockEntity instanceof SpellDispenserBlockEntity, "Spell Dispenser block entity was not created");
        var spellDispenser = scope.block((SpellDispenserBlockEntity) blockEntity);

        var spell = SpellRegistry.PRECISION_JACK.get();
        var requiredMana = spell.getManaCost(1);
        spellDispenser.getInventory().setStackInSlot(0, createSpellScroll(spell));
        var ownerProfile = createSpellDispenserOwnerProfile("spell_dispenser_precision_jack_low_mana_test");
        spellDispenser.setOwnerProfile(ownerProfile);
        spellDispenser.setCurrentMana(Math.max(0, requiredMana - 1));

        var castResult = scope.capture(() -> spellDispenser.tryActivate());
        helper.assertTrue(!castResult.succeeded(), "Spell Dispenser activated Precision Jack with insufficient mana");
        helper.assertTrue(castResult.insufficientMana(), "Spell Dispenser returned the wrong Precision Jack low-mana failure");

        // 周囲の別テストのナイフを数えず、この発動で生成した個体の破棄を追跡する。
        var knives = scope.entities(PrecisionJackKnifeEntity.class);
        helper.assertTrue(knives.size() == 1, "Low-mana Precision Jack must create one pre-cast knife");
        var knife = knives.getFirst();
        helper.assertTrue(knife.getOwner() instanceof Player owner && owner.getUUID().equals(ownerProfile.getId()),
                "Low-mana Precision Jack knife should belong to this activation");
        helper.assertTrue(level.isPositionEntityTicking(knife.blockPosition()),
                "Low-mana Precision Jack knife must spawn in an entity-ticking chunk");

        helper.runAtTickTime(20, () -> {
            helper.assertTrue(knife.isRemoved(), "Low-mana Precision Jack left its pre-cast knife behind");
            helper.succeed();
        });
    }

    @GameTest(template = "gametest/dispenser_casting")
    public static void spellDispenserCastHelperSupportsDiagonalVectorFacing(GameTestHelper helper) {
        helper.succeedIf(() -> {
            try (var scope = new DispenserTestScope(helper)) {
                var level = helper.getLevel();
                var castPos = helper.absolutePos(new BlockPos(2, 3, 2));
                var scrollStack = createSpellScroll(SpellRegistry.COMPOUND_PHIAL.get());

                var castResult = scope.capture(() -> SpellDispenserCastHelper.tryCast(
                        (ServerLevel) level,
                        castPos,
                        new Vec3(1.0D, 0.0D, 1.0D),
                        scrollStack,
                        createSpellDispenserOwnerProfile("spell_dispenser_diagonal_vector_test")
                ));
                helper.assertTrue(castResult.succeeded(), "Spell Dispenser cast helper failed to cast from a diagonal forward vector");

                var projectiles = scope.entities(CompoundPhialProjectileEntity.class);
                helper.assertTrue(!projectiles.isEmpty(), "Spell Dispenser diagonal vector cast did not spawn a Compound Phial projectile");

                var projectile = projectiles.get(0);
                var motion = projectile.getDeltaMovement();
                helper.assertTrue(Math.abs(motion.x) > 0.01D, "Spell Dispenser diagonal vector cast kept the projectile X motion at zero");
                helper.assertTrue(Math.abs(motion.z) > 0.01D, "Spell Dispenser diagonal vector cast kept the projectile Z motion at zero");
                helper.assertTrue(Math.signum(motion.x) == Math.signum(motion.z),
                        "Spell Dispenser diagonal vector cast did not preserve the intended diagonal quadrant: " + motion);
            }
        });
    }

    @GameTest(template = "gametest/dispenser_casting")
    public static void spellDispenserCastHelperSupportsSpectralHammer(GameTestHelper helper) {
        helper.succeedIf(() -> {
            try (var scope = new DispenserTestScope(helper)) {
                var level = helper.getLevel();
                var relativeCastPos = new BlockPos(6, 3, 2);
                var relativeTargetPos = new BlockPos(6, 3, 5);
                var castPos = helper.absolutePos(relativeCastPos);
                helper.setBlock(relativeTargetPos, Blocks.STONE);

                var castResult = scope.capture(() -> SpellDispenserCastHelper.tryCast(
                        (ServerLevel) level,
                        castPos,
                        Direction.SOUTH,
                        createSpellScroll(io.redspace.ironsspellbooks.api.registry.SpellRegistry.SPECTRAL_HAMMER_SPELL.get()),
                        createSpellDispenserOwnerProfile("spell_dispenser_spectral_hammer_test")
                ));
                helper.assertTrue(castResult.succeeded(), "Spell Dispenser cast helper failed to start Spectral Hammer");

                var hammers = scope.entities(SpectralHammer.class);
                helper.assertTrue(!hammers.isEmpty(), "Spell Dispenser cast did not spawn Spectral Hammer");

                var hammer = hammers.get(0);
                for (var tick = 0; tick < 20 && !hammer.isRemoved(); tick++) {
                    scope.run(() -> hammer.tick());
                }

                helper.assertBlockNotPresent(Blocks.STONE, relativeTargetPos);
            }
        });
    }

}
