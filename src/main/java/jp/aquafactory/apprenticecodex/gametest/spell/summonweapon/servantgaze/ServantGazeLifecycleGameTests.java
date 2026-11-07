package jp.aquafactory.apprenticecodex.gametest.spell.summonweapon.servantgaze;

import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.capabilities.magic.RecastResult;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.spell.servantgaze.ServantGaze;
import jp.aquafactory.apprenticecodex.spell.servantgaze.ServantGazeManager;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import static jp.aquafactory.apprenticecodex.gametest.spell.summonweapon.servantgaze.ServantGazeFixture.*;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class ServantGazeLifecycleGameTests {
    private ServantGazeLifecycleGameTests() {
    }

    @GameTest(template = TEMPLATE, batch = LIFECYCLE_ISOLATED_BATCH)
    public static void servantGazeRecastCreatesAndRemovesSingleStaff(GameTestHelper helper) {
        var scene = new ServantGazeFixture(helper, "servant_gaze_recast");
        var player = scene.player;
        var magicData = MagicData.getPlayerMagicData(player);
        spell().onCast(helper.getLevel(), 1, player, CastSource.SPELLBOOK, magicData);

        helper.runAtTickTime(2, () -> {
            helper.assertTrue(scene.staffs().size() == 1,
                    "First Servant Gaze cast should create exactly one staff");
            helper.assertTrue(magicData.getPlayerRecasts().hasRecastForSpell(spell()),
                    "First Servant Gaze cast should create an active recast");

            spell().castSpell(helper.getLevel(), 1, player, CastSource.SPELLBOOK, true);
        });
        helper.runAtTickTime(4, () -> {
            helper.assertTrue(scene.staffs().isEmpty(),
                    "Servant Gaze recast should remove its staff");
            helper.assertFalse(magicData.getPlayerRecasts().hasRecastForSpell(spell()),
                    "Servant Gaze recast should finish the active recast");
            helper.assertTrue(magicData.getPlayerCooldowns().isOnCooldown(spell()),
                    "Servant Gaze recast should start the normal cooldown");
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, batch = LIFECYCLE_ISOLATED_BATCH)
    public static void directCounterspellRemovesServantGazeAndStartsCooldown(GameTestHelper helper) {
        var scene = new ServantGazeFixture(helper, "servant_gaze_direct_counterspell");
        var player = scene.player;
        var magicData = MagicData.getPlayerMagicData(player);
        scene.activate(200, 1, 5.0F, 20.0, 15);

        helper.runAtTickTime(2, () -> {
            var staffs = scene.staffs();
            helper.assertTrue(staffs.size() == 1,
                    "Servant Gaze should expose exactly one staff to Counterspell");
            staffs.get(0).onAntiMagic(magicData);
        });
        helper.runAtTickTime(4, () -> {
            helper.assertTrue(scene.staffs().isEmpty(),
                    "Direct Counterspell should remove the Servant Gaze staff");
            helper.assertFalse(magicData.getPlayerRecasts().hasRecastForSpell(spell()),
                    "Direct Counterspell should remove the Servant Gaze recast");
            helper.assertTrue(magicData.getPlayerCooldowns().isOnCooldown(spell()),
                    "Direct Counterspell should start the normal cooldown");
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, batch = LIFECYCLE_ISOLATED_BATCH)
    public static void ownerCounterspellRemovesServantGazeAndStartsCooldown(GameTestHelper helper) {
        var scene = new ServantGazeFixture(helper, "servant_gaze_owner_counterspell");
        var player = scene.player;
        var magicData = MagicData.getPlayerMagicData(player);
        scene.activate(200, 1, 5.0F, 20.0, 15);
        var recast = magicData.getPlayerRecasts().getRecastInstance(spell().getSpellId());
        helper.assertTrue(recast != null, "Servant Gaze should create a recast before owner Counterspell");

        magicData.getPlayerRecasts().removeRecast(recast, RecastResult.COUNTERSPELL);

        helper.assertTrue(scene.staffs().isEmpty(),
                "Counterspelling the owner should remove the Servant Gaze staff");
        helper.assertFalse(magicData.getPlayerRecasts().hasRecastForSpell(spell()),
                "Counterspelling the owner should remove the Servant Gaze recast");
        helper.assertTrue(magicData.getPlayerCooldowns().isOnCooldown(spell()),
                "Counterspelling the owner should start the normal cooldown");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, batch = LIFECYCLE_ISOLATED_BATCH)
    public static void servantGazeTimeoutStartsCooldownWithoutTalisman(GameTestHelper helper) {
        var scene = new ServantGazeFixture(helper, "servant_gaze_timeout");
        var player = scene.player;
        var magicData = MagicData.getPlayerMagicData(player);
        scene.activate(1, 1, 5.0F, 20.0, 15);

        helper.runAtTickTime(3, () -> {
            helper.assertTrue(scene.staffs().isEmpty(),
                    "Servant Gaze should remove its staff after the fixed duration");
            helper.assertTrue(magicData.getPlayerCooldowns().isOnCooldown(spell()),
                    "Servant Gaze timeout should start cooldown without the talisman");
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, batch = LIFECYCLE_ISOLATED_BATCH)
    public static void servantGazeTalismanSkipsCooldownOnlyOnTimeout(GameTestHelper helper) {
        var scene = new ServantGazeFixture(helper, "servant_gaze_talisman_timeout");
        var player = scene.player;
        var magicData = MagicData.getPlayerMagicData(player);
        scene.equipGreaterConjurersTalisman();
        scene.activate(1, 1, 5.0F, 20.0, 15);

        helper.runAtTickTime(3, () -> {
            helper.assertTrue(scene.staffs().isEmpty(),
                    "Servant Gaze should still remove its staff when the talisman suppresses cooldown");
            helper.assertFalse(magicData.getPlayerCooldowns().isOnCooldown(spell()),
                    "Greater Conjurer's Talisman should suppress cooldown only after Servant Gaze timeout");
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, batch = LIFECYCLE_ISOLATED_BATCH)
    public static void servantGazeRelocationRecreatesStaffWithoutResettingRecast(GameTestHelper helper) {
        var scene = new ServantGazeFixture(helper, "servant_gaze_relocation");
        var player = scene.player;
        var magicData = MagicData.getPlayerMagicData(player);
        scene.activate(200, 3, 9.0F, 12.0, 25);
        var recast = magicData.getPlayerRecasts().getRecastInstance(spell().getSpellId());
        helper.assertTrue(recast != null, "Servant Gaze should create a recast before relocation");
        var oldUuid = ((ServantGaze.ServantGazeCastData) recast.getCastData()).getStaffUuid();
        var remaining = recast.getTicksRemaining();

        ServantGazeManager.relocate(player);

        var relocated = magicData.getPlayerRecasts().getRecastInstance(spell().getSpellId());
        helper.assertTrue(relocated == recast, "Servant Gaze relocation should preserve the active recast");
        helper.assertTrue(relocated.getTicksRemaining() == remaining,
                "Servant Gaze relocation should preserve the remaining duration");
        var castData = (ServantGaze.ServantGazeCastData) relocated.getCastData();
        helper.assertTrue(!oldUuid.equals(castData.getStaffUuid()),
                "Servant Gaze relocation should bind the recast to a newly created staff");
        helper.assertTrue(scene.staffs().size() == 1,
                "Servant Gaze relocation should leave exactly one staff");
        helper.succeed();
    }
}
