package jp.aquafactory.apprenticecodex.gametest.equipment.spelldispenser.casting;

import io.redspace.ironsspellbooks.entity.spells.magic_missile.MagicMissileProjectile;
import io.redspace.ironsspellbooks.entity.spells.spectral_hammer.SpectralHammer;
import jp.aquafactory.apprenticecodex.block.spelldispenser.*;
import jp.aquafactory.apprenticecodex.registry.BlockRegistry;
import jp.aquafactory.apprenticecodex.registry.SpellRegistry;
import jp.aquafactory.apprenticecodex.spell.compoundphial.CompoundPhialProjectileEntity;
import jp.aquafactory.apprenticecodex.spell.precisionjack.PrecisionJackKnifeEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;

import java.util.ArrayList;
import java.util.Map;
import java.util.function.Consumer;

import static jp.aquafactory.apprenticecodex.gametest.equipment.bow.SpellScrollTestSupport.createSpellScroll;
import static jp.aquafactory.apprenticecodex.gametest.integration.create.CreateDispenserTestSupport.assertNoSpellDispenserProxy;
import static jp.aquafactory.apprenticecodex.gametest.integration.create.CreateDispenserTestSupport.createSpellDispenserOwnerProfile;

final class SpellDispenserInstantCastScenarios {
    private SpellDispenserInstantCastScenarios() {}

    static void spellDispenserCastHelperCleansUpProxy(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var level = helper.getLevel();
            var castPos = new BlockPos(0, 1, 0);
            var spell = io.redspace.ironsspellbooks.api.registry.SpellRegistry.MAGIC_MISSILE_SPELL.get();
            var scrollStack = createSpellScroll(spell);

            var castResult = SpellDispenserCastHelper.tryCast(
                    (ServerLevel) level,
                    castPos,
                    Direction.NORTH,
                    scrollStack,
                    createSpellDispenserOwnerProfile("spell_dispenser_magic_missile_test")
            );
            helper.assertTrue(castResult.succeeded(), "Spell Dispenser cast helper failed to cast a Magic Missile scroll");
            helper.assertTrue(castResult.reachedOnCast(), "Spell Dispenser Magic Missile cast did not mark that it reached onCast");
            helper.assertTrue(castResult.cooldownTicks() == spell.getSpellCooldown(),
                    "Spell Dispenser Magic Missile cast returned the wrong cooldown: " + castResult.cooldownTicks());
            assertNoSpellDispenserProxy(helper, castPos, scrollStack, "Spell Dispenser proxy caster was left behind after Magic Missile cast");
        });
    }

    static void spellDispenserCastHelperAllowsOwnerOptionalSpellWithoutOwnerProfile(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var level = helper.getLevel();
            var castPos = new BlockPos(0, 1, 0);
            var spell = io.redspace.ironsspellbooks.api.registry.SpellRegistry.MAGIC_MISSILE_SPELL.get();
            var scrollStack = createSpellScroll(spell);

            var castResult = SpellDispenserCastHelper.tryCast(
                    (ServerLevel) level,
                    castPos,
                    Direction.NORTH,
                    scrollStack,
                    null
            );
            helper.assertTrue(castResult.succeeded(), "Spell Dispenser owner-optional cast failed without an owner profile");
            helper.assertTrue(castResult.reachedOnCast(), "Spell Dispenser owner-optional cast did not reach onCast");
            assertNoSpellDispenserProxy(helper, castPos, scrollStack, "Spell Dispenser owner-optional cast left proxy state behind");
        });
    }

    static void spellDispenserWizardlampPlacesLanternInAirWithoutBlockTarget(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var level = helper.getLevel();
            var castPos = helper.absolutePos(new BlockPos(5, 4, 8));
            var forward = new Vec3(0.0D, 0.0D, -1.0D);
            var castBasePosition = Vec3.atCenterOf(castPos);
            // 1.20.1のSpellDispenserCastHelperが適用する標準forward offsetに合わせて実際の視点位置を求める。
            var castOrigin = castBasePosition.add(forward.scale(0.7D));
            var expectedPos = BlockPos.containing(castOrigin.add(forward.scale(6.0D)));
            var scrollStack = createSpellScroll(SpellRegistry.WIZARDLAMP.get());

            for (var distance = 0; distance <= 6; distance++) {
                level.setBlockAndUpdate(BlockPos.containing(castOrigin.add(forward.scale(distance))), Blocks.AIR.defaultBlockState());
            }
            level.setBlockAndUpdate(expectedPos.below(), Blocks.AIR.defaultBlockState());
            helper.assertTrue(level.getBlockState(expectedPos).isAir() && level.getBlockState(expectedPos.below()).isAir(),
                    "Spell Dispenser Wizardlamp aerial placement fixture was not empty");
            var castResult = SpellDispenserCastHelper.tryCast(
                    level, castBasePosition, forward, scrollStack, null
            );

            helper.assertTrue(castResult.succeeded(),
                    "Spell Dispenser Wizardlamp failed without a block target: failureType="
                            + castResult.failureType() + ", validation=" + castResult.validation().failureReason());
            helper.assertTrue(level.getBlockState(expectedPos).is(BlockRegistry.WIZARDLAMP_LANTERN.get()),
                    "Spell Dispenser Wizardlamp did not place a lantern at its aerial range endpoint");
        });
    }

    static void spellDispenserCastHelperUsesNeutralLivingCasterProfileForMagicMissile(GameTestHelper helper) {
        var level = (ServerLevel) helper.getLevel();
        var castPos = new BlockPos(0, 1, 0);
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
        var spawnedProjectiles = new ArrayList<MagicMissileProjectile>();
        Consumer<EntityJoinLevelEvent> projectileListener = event -> {
            if (event.getLevel() == level
                    && event.getEntity() instanceof MagicMissileProjectile projectile) {
                spawnedProjectiles.add(projectile);
            }
        };

        MinecraftForge.EVENT_BUS.addListener(projectileListener);
        try (var ignored = SpellDispenserSpellProfileManager.useProfilesForGameTest(Map.of(spell.getSpellResource(), neutralProfile))) {
            var castResult = SpellDispenserCastHelper.tryCast(
                    level,
                    castPos,
                    Direction.NORTH,
                    scrollStack,
                    null
            );
            helper.assertTrue(castResult.succeeded(), "Spell Dispenser neutral living cast failed for Magic Missile");
            helper.assertTrue(castResult.reachedOnCast(), "Spell Dispenser neutral living cast did not reach onCast");
        } finally {
            MinecraftForge.EVENT_BUS.unregister(projectileListener);
        }

        helper.assertTrue(!spawnedProjectiles.isEmpty(), "Spell Dispenser neutral living cast did not spawn Magic Missile");
        var owner = spawnedProjectiles.get(0).getOwner();
        helper.assertTrue(owner instanceof ArmorStand, "Spell Dispenser neutral living cast did not use ArmorStand owner: " + owner);
        helper.assertTrue(owner.hasCustomName(), "Spell Dispenser neutral living caster did not set a display name");
        helper.assertTrue(!owner.isCustomNameVisible(), "Spell Dispenser neutral living caster exposes its nameplate");
        helper.assertTrue(!owner.isSilent(), "Spell Dispenser neutral living caster suppresses cast sound");
        helper.assertTrue(!(owner instanceof Player), "Spell Dispenser neutral living cast used a Player owner: " + owner);
        helper.assertTrue(!(owner instanceof FakePlayer), "Spell Dispenser neutral living cast used a FakePlayer owner: " + owner);
        assertNoSpellDispenserProxy(helper, castPos, scrollStack, "Spell Dispenser neutral living cast left proxy state behind");
        helper.succeed();
    }

    static void spellDispenserCastHelperCompletesLongCastImmediately(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var level = helper.getLevel();
            var castPos = new BlockPos(0, 1, 0);
            var spell = SpellRegistry.COMPOUND_PHIAL.get();
            var scrollStack = createSpellScroll(spell);

            var castResult = SpellDispenserCastHelper.tryCast(
                    (ServerLevel) level,
                    castPos,
                    Direction.NORTH,
                    scrollStack,
                    createSpellDispenserOwnerProfile("spell_dispenser_long_test")
            );
            helper.assertTrue(castResult.succeeded(), "Spell Dispenser cast helper failed to cast a LONG Compound Phial scroll");
            var expectedCooldown = spell.getSpellCooldown()
                    + spell.getEffectiveCastTime(1, new FakePlayer((ServerLevel) level, createSpellDispenserOwnerProfile("spell_dispenser_long_probe")));
            helper.assertTrue(castResult.cooldownTicks() == expectedCooldown,
                    "Spell Dispenser LONG cast returned the wrong cooldown: " + castResult.cooldownTicks() + " / expected " + expectedCooldown);
            assertNoSpellDispenserProxy(helper, castPos, scrollStack, "Spell Dispenser proxy caster was left behind after LONG cast");

            var projectileBox = new AABB(castPos).inflate(5.0D);
            var projectiles = level.getEntitiesOfClass(CompoundPhialProjectileEntity.class, projectileBox);
            helper.assertTrue(!projectiles.isEmpty(), "Spell Dispenser LONG cast completed without spawning a Compound Phial projectile");
        });
    }

    static void spellDispenserCastHelperTicksLongCastBeforeCompletion(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var level = helper.getLevel();
            var castPos = new BlockPos(0, 1, 0);
            var spell = SpellRegistry.PRECISION_JACK.get();
            var scrollStack = createSpellScroll(spell);

            var castResult = SpellDispenserCastHelper.tryCast(
                    (ServerLevel) level,
                    castPos,
                    Direction.NORTH,
                    scrollStack,
                    createSpellDispenserOwnerProfile("spell_dispenser_precision_jack_test")
            );
            helper.assertTrue(castResult.succeeded(), "Spell Dispenser cast helper failed to cast a Precision Jack scroll");

            var knifeBox = new AABB(castPos).inflate(6.0D);
            var knives = level.getEntitiesOfClass(PrecisionJackKnifeEntity.class, knifeBox);
            helper.assertTrue(!knives.isEmpty(), "Spell Dispenser Precision Jack cast completed without spawning a knife");
            helper.assertTrue(knives.stream().anyMatch(PrecisionJackKnifeEntity::isTrailActive),
                    "Spell Dispenser LONG cast completed without calling Precision Jack's server cast tick");
        });
    }

    static void spellDispenserPrecisionJackLowManaCleansUpKnife(GameTestHelper helper) {
        var level = helper.getLevel();
        // 北向きの召喚位置はブロック中心から前方へずれるため、端では隣接する非tickチャンクへ出る。
        // basic_floor の水平範囲内へ収め、強制ロードや手動tickに頼らず寿命による破棄を検証する。
        var pos = new BlockPos(2, 1, 2);
        var absolutePos = helper.absolutePos(pos);
        helper.setBlock(pos, BlockRegistry.SPELL_DISPENSER.get());

        var blockEntity = helper.getBlockEntity(pos);
        helper.assertTrue(blockEntity instanceof SpellDispenserBlockEntity, "Spell Dispenser block entity was not created");
        var spellDispenser = (SpellDispenserBlockEntity) blockEntity;

        var spell = SpellRegistry.PRECISION_JACK.get();
        var requiredMana = spell.getManaCost(1);
        spellDispenser.getInventory().setStackInSlot(0, createSpellScroll(spell));
        var ownerProfile = createSpellDispenserOwnerProfile("spell_dispenser_precision_jack_low_mana_test");
        spellDispenser.setOwnerProfile(ownerProfile);
        spellDispenser.setCurrentMana(Math.max(0, requiredMana - 1));

        var castResult = spellDispenser.tryActivate();
        helper.assertTrue(!castResult.succeeded(), "Spell Dispenser activated Precision Jack with insufficient mana");
        helper.assertTrue(castResult.insufficientMana(), "Spell Dispenser returned the wrong Precision Jack low-mana failure");

        // 周囲の別テストのナイフを数えず、この発動で生成した個体の破棄を追跡する。
        var knives = level.getEntitiesOfClass(PrecisionJackKnifeEntity.class, new AABB(absolutePos).inflate(8.0D),
                knife -> knife.getOwner() instanceof Player owner && owner.getUUID().equals(ownerProfile.getId()));
        helper.assertTrue(knives.size() == 1, "Low-mana Precision Jack must create one pre-cast knife");
        var knife = knives.get(0);
        helper.assertTrue(level.isPositionEntityTicking(knife.blockPosition()),
                "Low-mana Precision Jack knife must spawn in an entity-ticking chunk");

        helper.runAtTickTime(20, () -> {
            helper.assertTrue(knife.isRemoved(), "Low-mana Precision Jack left its pre-cast knife behind");
            helper.succeed();
        });
    }

    static void spellDispenserCastHelperSupportsDiagonalVectorFacing(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var level = helper.getLevel();
            var castPos = new BlockPos(0, 1, 0);
            var scrollStack = createSpellScroll(SpellRegistry.COMPOUND_PHIAL.get());

            var castResult = SpellDispenserCastHelper.tryCast(
                    (ServerLevel) level,
                    castPos,
                    new Vec3(1.0D, 0.0D, 1.0D),
                    scrollStack,
                    createSpellDispenserOwnerProfile("spell_dispenser_diagonal_vector_test")
            );
            helper.assertTrue(castResult.succeeded(), "Spell Dispenser cast helper failed to cast from a diagonal forward vector");

            var projectileBox = new AABB(castPos).inflate(5.0D);
            var projectiles = level.getEntitiesOfClass(CompoundPhialProjectileEntity.class, projectileBox);
            helper.assertTrue(!projectiles.isEmpty(), "Spell Dispenser diagonal vector cast did not spawn a Compound Phial projectile");

            var projectile = projectiles.get(0);
            var motion = projectile.getDeltaMovement();
            helper.assertTrue(Math.abs(motion.x) > 0.01D, "Spell Dispenser diagonal vector cast kept the projectile X motion at zero");
            helper.assertTrue(Math.abs(motion.z) > 0.01D, "Spell Dispenser diagonal vector cast kept the projectile Z motion at zero");
            helper.assertTrue(Math.signum(motion.x) == Math.signum(motion.z),
                    "Spell Dispenser diagonal vector cast did not preserve the intended diagonal quadrant: " + motion);
        });
    }

    static void spellDispenserCastHelperSupportsSpectralHammer(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var level = helper.getLevel();
            var relativeCastPos = new BlockPos(0, 1, 0);
            var relativeTargetPos = new BlockPos(0, 1, 3);
            var castPos = helper.absolutePos(relativeCastPos);
            helper.setBlock(relativeTargetPos, Blocks.STONE);

            var castResult = SpellDispenserCastHelper.tryCast(
                    (ServerLevel) level,
                    castPos,
                    Direction.SOUTH,
                    createSpellScroll(io.redspace.ironsspellbooks.api.registry.SpellRegistry.SPECTRAL_HAMMER_SPELL.get()),
                    createSpellDispenserOwnerProfile("spell_dispenser_spectral_hammer_test")
            );
            helper.assertTrue(castResult.succeeded(), "Spell Dispenser cast helper failed to start Spectral Hammer");

            var hammerBox = new AABB(castPos).inflate(8.0D);
            var hammers = level.getEntitiesOfClass(SpectralHammer.class, hammerBox);
            helper.assertTrue(!hammers.isEmpty(), "Spell Dispenser cast did not spawn Spectral Hammer");

            var hammer = hammers.get(0);
            for (var tick = 0; tick < 20 && !hammer.isRemoved(); tick++) {
                hammer.tick();
            }

            helper.assertTrue(true, "Spectral Hammer ticked without crashing");
        });
    }
}
