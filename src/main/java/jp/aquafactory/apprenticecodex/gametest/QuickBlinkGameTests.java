package jp.aquafactory.apprenticecodex.gametest;

import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.item.curios.shootingstarmantle.MantleBlink;
import jp.aquafactory.apprenticecodex.item.curios.shootingstarmantle.MantleMovement;
import jp.aquafactory.apprenticecodex.registry.SpellRegistry;
import jp.aquafactory.apprenticecodex.spell.quickblink.QuickBlink;
import jp.aquafactory.apprenticecodex.spell.quickblink.QuickBlinkRuntime;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.damagesource.DamageContainer;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class QuickBlinkGameTests {
    private static final String TEMPLATE = "gametest/basic_floor";

    private QuickBlinkGameTests() { }

    @GameTest(template = TEMPLATE)
    public static void castMovesFiveBlocksWithoutMantleAndRejectsOverlap(GameTestHelper helper) {
        var player = ApprenticeCodexGameTestScenarios.createEquipmentTestPlayer(helper, new BlockPos(1, 3, 1), "quick_blink_move");
        var origin = helper.absoluteVec(new Vec3(0.5, 2, 0.5));
        for (int x = 0; x <= 7; x++) for (int y = 1; y <= 5; y++) {
            helper.setBlock(new BlockPos(x, y, 0), Blocks.AIR);
        }
        player.setPos(origin);
        player.setYRot(-90);
        var spell = (QuickBlink) SpellRegistry.QUICK_BLINK.get();
        var magic = MagicData.getPlayerMagicData(player);
        for (int tick = 0; tick < MantleBlink.DURATION; tick++) {
            int age = tick;
            helper.runAtTickTime(tick + 1, () -> {
                if (age == 0) {
                    QuickBlinkRuntime.input(player, 1, 0);
                    spell.onCast(player.level(), 1, player, CastSource.SPELLBOOK, magic);
                    var recast = magic.getPlayerRecasts().getRecastInstance(spell.getSpellId());
                    helper.assertTrue(recast != null && recast.getTotalRecasts() == 2 && recast.getTicksToLive() == 40,
                            "Level one must allow two total casts within forty ticks");
                    helper.assertFalse(spell.checkPreCastConditions(player.level(), 1, player, magic),
                            "A second cast must be rejected while the blink is active");
                }
                var blink = QuickBlinkRuntime.state(player).blink;
                QuickBlinkRuntime.tick(player);
                blink.travel(player);
                double expected = Math.min(4, Math.max(0, age - 2)) * 1.25;
                helper.assertTrue(Math.abs(player.position().subtract(origin).horizontalDistance() - expected) < 1.0e-5,
                        "Quick Blink must move five blocks over the four hidden ticks");
                helper.assertTrue(Math.abs(player.getY() - origin.y) < 1.0e-6,
                        "Quick Blink must hold its starting height");
                var damage = new LivingIncomingDamageEvent(player, new DamageContainer(player.damageSources().generic(), 5));
                helper.assertTrue(QuickBlinkRuntime.cancelIncomingDamageIfInvulnerable(damage) && damage.isCanceled(),
                        "Quick Blink must cancel incoming damage during every phase");
            });
        }
        helper.runAtTickTime(11, () -> {
            QuickBlinkRuntime.tick(player);
            helper.assertFalse(QuickBlinkRuntime.active(player), "Invulnerability must end after ten ticks");
            var damage = new LivingIncomingDamageEvent(player, new DamageContainer(player.damageSources().generic(), 5));
            helper.assertFalse(QuickBlinkRuntime.cancelIncomingDamageIfInvulnerable(damage),
                    "Damage immunity must end with the blink");
            spell.castSpell(player.level(), 1, player, CastSource.SPELLBOOK, true);
            helper.assertTrue(QuickBlinkRuntime.active(player)
                    && !magic.getPlayerRecasts().hasRecastForSpell(spell),
                    "The remaining recast must work after the effect ends and consume its last count");
            QuickBlinkRuntime.clear(player);
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE)
    public static void blockedAndInvalidInputStayWithinRange(GameTestHelper helper) {
        var player = ApprenticeCodexGameTestScenarios.createEquipmentTestPlayer(helper, new BlockPos(1, 3, 1), "quick_blink_wall");
        var origin = helper.absoluteVec(new Vec3(0.5, 2, 0.5));
        player.setPos(origin);
        player.setYRot(-90);
        helper.setBlock(new BlockPos(2, 2, 0), Blocks.STONE);
        QuickBlinkRuntime.input(player, Float.NaN, 0);
        QuickBlinkRuntime.input(player, 1, 0);
        QuickBlinkRuntime.begin(player, 5);
        for (int tick = 1; tick <= 8; tick++) {
            helper.runAtTickTime(tick, () -> {
                QuickBlinkRuntime.state(player).blink.travel(player);
                helper.assertTrue(player.position().subtract(origin).horizontalDistance() < 2,
                        "Blink must stop at a wall without stepping over it");
            });
        }
        helper.runAtTickTime(9, () -> {
            QuickBlinkRuntime.clear(player);
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE)
    public static void noInputUsesMantleBackwardDirection(GameTestHelper helper) {
        var player = ApprenticeCodexGameTestScenarios.createEquipmentTestPlayer(helper, new BlockPos(1, 3, 1), "quick_blink_no_input");
        player.setYRot(-90);
        QuickBlinkRuntime.input(player, 0, 0);
        QuickBlinkRuntime.begin(player, 5);
        var expected = MantleMovement.direction(0, 0, -90);
        helper.assertTrue(QuickBlinkRuntime.state(player).blink.direction().distanceToSqr(expected) < 1.0e-8,
                "No-input casting must use the mantle's backward fallback");
        QuickBlinkRuntime.clear(player);
        helper.succeed();
    }
}
