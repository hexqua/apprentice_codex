package jp.aquafactory.apprenticecodex.gametest.spell.mirageavoidance;

import io.redspace.ironsspellbooks.api.events.SpellOnCastEvent;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.util.Utils;
import jp.aquafactory.apprenticecodex.capability.Capabilities;
import jp.aquafactory.apprenticecodex.capability.codexspelldata.CodexSpellStateTypeRegister;
import jp.aquafactory.apprenticecodex.item.curios.CuriosSlotConstants;
import jp.aquafactory.apprenticecodex.item.curios.shootingstarmantle.MantleMovement;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import jp.aquafactory.apprenticecodex.registry.SpellRegistry;
import jp.aquafactory.apprenticecodex.spell.SpellMovementInput;
import jp.aquafactory.apprenticecodex.spell.mirageavoidance.MirageAvoidanceEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import static jp.aquafactory.apprenticecodex.gametest.support.CurioTestSupport.equipCurio;
import static jp.aquafactory.apprenticecodex.gametest.support.CurioTestSupport.postLivingAttackEventForGameTest;
import static jp.aquafactory.apprenticecodex.gametest.support.EquipmentStateTestSupport.getMirageAvoidanceState;
import static jp.aquafactory.apprenticecodex.gametest.support.PlayerTestSupport.createEquipmentTestPlayer;
import static jp.aquafactory.apprenticecodex.gametest.support.PlayerTestSupport.createTrackedEquipmentTestPlayer;

final class MirageAvoidanceScenarios {
    private MirageAvoidanceScenarios() {}

    static void mirageAvoidanceUsesFifteenTickInvulnerabilityAndActiveRecastLock(GameTestHelper helper) {
        var level = helper.getLevel();
        var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "mirage_avoidance_window_test");
        var spell = SpellRegistry.MIRAGE_AVOIDANCE.get();
        var magicData = MagicData.getPlayerMagicData(player);

        SpellMovementInput.update(player, 0.0F, -1.0F);
        spell.onCast(level, 1, player, CastSource.SCROLL, magicData);

        var state = getMirageAvoidanceState(player);
        helper.assertTrue(state.activeUntilGameTime == level.getGameTime() + MirageAvoidanceEvents.EFFECT_DURATION_TICKS,
                "MirageAvoidance should keep a 25 tick active window");
        helper.assertTrue(state.invulnerableUntilGameTime == level.getGameTime() + MirageAvoidanceEvents.INVULNERABLE_TICKS,
                "MirageAvoidance should keep only 15 ticks of invulnerability");
        helper.assertTrue(Math.abs(state.movementForward) < 1.0E-4F && state.movementStrafe < -0.99F,
                "MirageAvoidance should store the activation input direction");
        helper.assertFalse(
                spell.checkPreCastConditions(level, 1, player, magicData),
                "MirageAvoidance should reject recast while the effect is active"
        );

        var blockedAttack = postLivingAttackEventForGameTest(player, level.damageSources().lava(), 4.0F);
        helper.assertTrue(blockedAttack.isCanceled(), "MirageAvoidance should cancel all damage during the invulnerability window");

        Capabilities.withSpellData(player, data -> data.edit(CodexSpellStateTypeRegister.MIRAGE_AVOIDANCE_STATE, s -> {
            s.startGameTime = level.getGameTime() - MirageAvoidanceEvents.INVULNERABLE_TICKS;
            s.invulnerableUntilGameTime = level.getGameTime();
            s.activeUntilGameTime = level.getGameTime() + 30;
        }));

        var vulnerableAttack = postLivingAttackEventForGameTest(player, level.damageSources().lava(), 4.0F);
        helper.assertFalse(vulnerableAttack.isCanceled(), "MirageAvoidance should allow normal damage from tick 15");
        var fallAttack = postLivingAttackEventForGameTest(player, level.damageSources().fall(), 4.0F);
        helper.assertFalse(fallAttack.isCanceled(), "MirageAvoidance should allow fall damage after invulnerability ends");
        helper.succeed();
    }

    static void mirageAvoidanceSupporterUsesCastTimeEquipmentAndTimingBoundaries(GameTestHelper helper) {
        var level = helper.getLevel();
        var spell = SpellRegistry.MIRAGE_AVOIDANCE.get();
        // 未装着、装着継続、発動後に取り外しの三通りで同じ境界を通す。
        for (int mode = 0; mode < 3; mode++) {
            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "mirage_supporter_" + mode);
            boolean supported = mode != 0;
            equipCurio(player, CuriosSlotConstants.BELT,
                    supported ? new ItemStack(ItemRegistry.PROTECTION_SPELL_SUPPORTER.get()) : ItemStack.EMPTY);
            int invulnerableTicks = supported ? 20 : 15;
            int activeTicks = supported ? 22 : 25;
            var expectedTooltip = Component.translatable("ui.irons_spellbooks.effect_length",
                    Utils.timeFromTicks(invulnerableTicks, 1));
            helper.assertTrue(spell.getUniqueInfo(1, player).getFirst().equals(expectedTooltip),
                    "MirageAvoidance tooltip must show the equipped invulnerability duration");
            var manaEvent = new SpellOnCastEvent(player, spell.getSpellId(), 1, spell.getManaCost(1),
                    spell.getSchoolType(), CastSource.SPELLBOOK);
            NeoForge.EVENT_BUS.post(manaEvent);
            int expectedMana = supported ? Math.max(1, Math.round(spell.getManaCost(1) * 0.5f)) : spell.getManaCost(1);
            helper.assertTrue(manaEvent.getManaCost() == expectedMana,
                    "MirageAvoidance mana discount must follow equipped supporter state");

            SpellMovementInput.update(player, 1.0F, 0.0F);
            spell.onCast(level, 1, player, CastSource.SPELLBOOK, MagicData.getPlayerMagicData(player));
            var castState = getMirageAvoidanceState(player);
            helper.assertTrue(castState.invulnerableUntilGameTime == level.getGameTime() + invulnerableTicks
                            && castState.activeUntilGameTime == level.getGameTime() + activeTicks,
                    "MirageAvoidance must snapshot both timing windows at cast time");
            if (mode == 2) {
                equipCurio(player, CuriosSlotConstants.BELT, ItemStack.EMPTY);
            }

            // 時刻を状態側で進め、実際のPre/Post処理と保存時刻補正を含めて境界を検証する。
            for (int elapsed : new int[]{0, 1, 2, 14, 15, 19, 20, 21, 22, 24, 25}) {
                long start = level.getGameTime() - elapsed;
                Capabilities.withSpellData(player, data -> data.edit(CodexSpellStateTypeRegister.MIRAGE_AVOIDANCE_STATE, s -> {
                    s.startGameTime = start;
                    s.invulnerableUntilGameTime = start + invulnerableTicks;
                    s.activeUntilGameTime = start + activeTicks;
                }));
                player.setDeltaMovement(0.3D, -0.5D, 0.2D);
                MirageAvoidanceEvents.onPlayerTick(new PlayerTickEvent.Pre(player));
                MirageAvoidanceEvents.onPlayerTick(new PlayerTickEvent.Post(player));
                boolean active = elapsed < activeTicks;
                helper.assertTrue(MirageAvoidanceEvents.isInputLocked(player) == active,
                        "MirageAvoidance input lock mismatch at tick " + elapsed + " in mode " + mode);
                var attack = postLivingAttackEventForGameTest(player, level.damageSources().lava(), 4.0F);
                helper.assertTrue(attack.isCanceled() == (elapsed < invulnerableTicks),
                        "MirageAvoidance invulnerability mismatch at tick " + elapsed + " in mode " + mode);
                if (active) {
                    var state = getMirageAvoidanceState(player);
                    helper.assertTrue(state.invulnerableUntilGameTime == start + invulnerableTicks
                                    && state.activeUntilGameTime == start + activeTicks,
                            "MirageAvoidance sanitizer must preserve cast timing windows");
                    var movement = player.getDeltaMovement();
                    boolean sliding = elapsed >= 2 && elapsed < 20;
                    helper.assertTrue(sliding ? movement.horizontalDistanceSqr() > 0 : movement.lengthSqr() < 1.0E-6D,
                            "MirageAvoidance movement phase mismatch at tick " + elapsed);
                } else {
                    helper.assertTrue(player.getDeltaMovement().equals(new Vec3(0.3D, -0.5D, 0.2D)),
                            "MirageAvoidance must release movement after recovery");
                }
            }
        }
        helper.succeed();
    }

    static void mirageAvoidanceFreezesThenSlidesAndResetsFallDistance(GameTestHelper helper) {
        var level = helper.getLevel();
        var player = createTrackedEquipmentTestPlayer(helper, new BlockPos(0, 5, 0), "mirage_avoidance_motion_test");
        var spell = SpellRegistry.MIRAGE_AVOIDANCE.get();
        var magicData = MagicData.getPlayerMagicData(player);

        SpellMovementInput.update(player, 1.0F, 0.0F);
        spell.onCast(level, 1, player, CastSource.SWORD, magicData);
        player.setDeltaMovement(0.3D, -0.5D, 0.2D);
        player.fallDistance = 8.0F;

        MirageAvoidanceEvents.onPlayerTick(new PlayerTickEvent.Pre(player));
        MirageAvoidanceEvents.onPlayerTick(new PlayerTickEvent.Post(player));
        var startupMovement = player.getDeltaMovement();
        helper.assertTrue(startupMovement.lengthSqr() < 1.0E-6D,
                "MirageAvoidance freeze startup should remove movement");
        helper.assertTrue(player.fallDistance == 8.0F,
                "MirageAvoidance should not reset fall distance before sliding starts");

        Capabilities.withSpellData(player, data -> data.edit(CodexSpellStateTypeRegister.MIRAGE_AVOIDANCE_STATE, s -> {
            s.startGameTime = level.getGameTime() - MirageAvoidanceEvents.FREEZE_TICKS - 2;
            s.activeUntilGameTime = level.getGameTime() + 30;
        }));
        player.setDeltaMovement(0.0D, -0.5D, 0.0D);
        player.fallDistance = 8.0F;
        MirageAvoidanceEvents.onPlayerTick(new PlayerTickEvent.Pre(player));
        MirageAvoidanceEvents.onPlayerTick(new PlayerTickEvent.Post(player));
        var slideMovement = player.getDeltaMovement();
        var horizontalSpeedSqr = slideMovement.x * slideMovement.x + slideMovement.z * slideMovement.z;
        helper.assertTrue(horizontalSpeedSqr > 0.01D,
                "MirageAvoidance should slide after startup");
        helper.assertTrue(slideMovement.y >= -0.081D,
                "MirageAvoidance slide should clamp falling speed");
        helper.assertTrue(player.fallDistance == 0.0F,
                "MirageAvoidance should keep fall distance reset while sliding");

        Capabilities.withSpellData(player, data -> data.edit(CodexSpellStateTypeRegister.MIRAGE_AVOIDANCE_STATE, s -> {
            s.startGameTime = level.getGameTime() - MirageAvoidanceEvents.VULNERABLE_RECOVERY_START_TICK;
            s.activeUntilGameTime = level.getGameTime() + 5;
        }));
        player.fallDistance = 6.0F;
        MirageAvoidanceEvents.onPlayerTick(new PlayerTickEvent.Pre(player));
        MirageAvoidanceEvents.onPlayerTick(new PlayerTickEvent.Post(player));
        helper.assertTrue(player.fallDistance == 6.0F,
                "MirageAvoidance should stop resetting fall distance during recovery");

        Capabilities.withSpellData(player, data -> data.edit(CodexSpellStateTypeRegister.MIRAGE_AVOIDANCE_STATE, s -> {
            s.startGameTime = level.getGameTime() - MirageAvoidanceEvents.EFFECT_DURATION_TICKS;
            s.activeUntilGameTime = level.getGameTime();
        }));
        player.fallDistance = 7.0F;
        MirageAvoidanceEvents.onPlayerTick(new PlayerTickEvent.Pre(player));
        helper.assertTrue(player.fallDistance == 7.0F,
                "MirageAvoidance should not reset fall distance after the effect ends");

        SpellMovementInput.update(player, 0.0F, 0.0F);
        spell.onCast(level, 1, player, CastSource.SPELLBOOK, magicData);
        var noInputState = getMirageAvoidanceState(player);
        helper.assertTrue(noInputState.movementForward < -0.99F && Math.abs(noInputState.movementStrafe) < 1.0E-4F,
                "MirageAvoidance must use the Quick Blink backward fallback without input");
        Capabilities.withSpellData(player, data -> data.edit(CodexSpellStateTypeRegister.MIRAGE_AVOIDANCE_STATE, s ->
                s.startGameTime = level.getGameTime() - MirageAvoidanceEvents.FREEZE_TICKS));
        MirageAvoidanceEvents.onPlayerTick(new PlayerTickEvent.Pre(player));
        var expectedBackward = MantleMovement.direction(0.0F, 0.0F, player.getYRot());
        helper.assertTrue(player.getDeltaMovement().normalize().distanceToSqr(expectedBackward) < 1.0E-8D,
                "MirageAvoidance must slide backward without input");
        player.setYRot(player.getYRot() + 90.0F);
        MirageAvoidanceEvents.onPlayerTick(new PlayerTickEvent.Pre(player));
        var expectedTurned = MantleMovement.direction(0.0F, 0.0F, player.getYRot());
        helper.assertTrue(player.getDeltaMovement().normalize().distanceToSqr(expectedTurned) < 1.0E-8D,
                "MirageAvoidance must keep camera steering after activation");
        SpellMovementInput.update(player, 0.0F, 1.0F);
        spell.onCast(level, 1, player, CastSource.SPELLBOOK, magicData);
        Capabilities.withSpellData(player, data -> data.edit(CodexSpellStateTypeRegister.MIRAGE_AVOIDANCE_STATE, s ->
                s.startGameTime = level.getGameTime() - MirageAvoidanceEvents.FREEZE_TICKS));
        MirageAvoidanceEvents.onPlayerTick(new PlayerTickEvent.Pre(player));
        var expectedStrafe = MantleMovement.direction(0.0F, 1.0F, player.getYRot());
        helper.assertTrue(player.getDeltaMovement().normalize().distanceToSqr(expectedStrafe) < 1.0E-8D,
                "MirageAvoidance strafe must match Quick Blink");
        helper.succeed();
    }
}
