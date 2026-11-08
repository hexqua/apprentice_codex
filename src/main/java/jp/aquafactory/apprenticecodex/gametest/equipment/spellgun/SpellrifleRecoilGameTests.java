package jp.aquafactory.apprenticecodex.gametest.equipment.spellgun;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.item.fullautorapidcastspellrifle.FullautoRapidcastSpellrifleRecoil;
import jp.aquafactory.apprenticecodex.item.multipurposestaffrifle.MultipurposeStaffrifleRecoil;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class SpellrifleRecoilGameTests {
    private SpellrifleRecoilGameTests() {}

    // 明示時刻を使い、ワールドtickの進み方や他のテストの実行順に依存させない。
    @GameTest(template = "gametest/basic_floor")
    public static void fullautoRecoilDistributesAndOverlapsWithoutLosingAngle(GameTestHelper helper) {
        var recoil = new FullautoRapidcastSpellrifleRecoil();
        recoil.addImpulse(3.0F, 0);
        helper.assertTrue(recoil.advanceImpulses(0) == 0, "Recoil must not jump when a shot arrives");
        var firstQuarter = recoil.advanceImpulses(25_000_000);
        helper.assertTrue(Math.abs(firstQuarter - 1.3125F) < 0.001F, "Ease-out must apply 43.75 percent of the angle at 25 ms");
        var firstHalf = firstQuarter + recoil.advanceImpulses(50_000_000);
        helper.assertTrue(Math.abs(firstHalf - 2.25F) < 0.001F, "Ease-out must apply 75 percent of the angle at 50 ms");
        recoil.addImpulse(1.5F, 50_000_000);
        helper.assertTrue(recoil.advanceImpulses(50_000_000) == 0, "Repeated sampling must not duplicate recoil");
        var total = firstHalf + recoil.advanceImpulses(100_000_000) + recoil.advanceImpulses(150_000_000);
        helper.assertTrue(Math.abs(total - 4.5F) < 0.001F, "Overlapping shots must preserve their combined angle");
        helper.assertTrue(recoil.advanceImpulses(200_000_000) == 0, "Completed recoil must not move or restore the view");
        recoil.addImpulse(3.0F, 200_000_000);
        helper.assertTrue(Math.abs(recoil.advanceImpulses(350_000_000) - 3.0F) < 0.001F,
                "Sparse frames must preserve the same total angle");
        recoil.addImpulse(3.0F, 350_000_000);
        recoil.advanceImpulses(375_000_000);
        recoil.clearImpulses();
        helper.assertTrue(recoil.advanceImpulses(500_000_000) == 0, "Discarded recoil must not resume after a pause or item switch");
        recoil.addImpulse(3.0F, 500_000_000);
        var previousDelta = Float.MAX_VALUE;
        for (var sample = 1; sample <= 4; sample++) {
            var delta = recoil.advanceImpulses(500_000_000L + sample * 25_000_000L);
            helper.assertTrue(delta >= 0 && delta < previousDelta,
                    "Ease-out must decelerate without moving the view backward");
            previousDelta = delta;
        }
        helper.succeed();
    }

    @GameTest(template = "gametest/basic_floor")
    public static void fullautoRecoilBuildsRecoversAndReducesWhileAiming(GameTestHelper helper) {
        var hip = new FullautoRapidcastSpellrifleRecoil();
        var ads = new FullautoRapidcastSpellrifleRecoil();
        helper.assertTrue(Math.abs(hip.fire(0, false) - 1.5F) < 0.001F, "First hip shot must kick by 1.5 degrees");
        helper.assertTrue(Math.abs(ads.fire(0, true) - 0.675F) < 0.001F, "ADS must reduce the first kick");
        var hipSecond = hip.fire(3, false);
        var adsSecond = ads.fire(3, true);
        helper.assertTrue(hipSecond > 1.5F && adsSecond < hipSecond * 0.45F,
                "ADS must reduce both kick and buildup");
        for (var tick = 6; tick <= 60; tick += 3) {
            helper.assertTrue(hip.fire(tick, false) <= 3.0F, "Continuous hip fire must cap at 3 degrees");
        }
        helper.assertTrue(Math.abs(hip.fire(63, false) - 3.0F) < 0.001F, "Sustained fire must reach maximum kick");
        helper.assertTrue(Math.abs(hip.fire(67, false) - 3.0F) < 0.001F, "Buildup must be held for four ticks");
        var recovered = hip.fire(77, false);
        helper.assertTrue(recovered > 1.5F && recovered < 3.0F, "Buildup must recover gradually");
        helper.assertTrue(Math.abs(hip.fire(93, false) - 1.5F) < 0.001F, "Sixteen idle ticks must fully recover buildup");
        helper.assertTrue(Math.abs(hip.fire(0, false) - 1.5F) < 0.001F, "Clock reset must discard previous buildup");
        helper.succeed();
    }

    @GameTest(template = "gametest/basic_floor")
    public static void multipurposeRepeatedShotsKeepTheSameRecoilWithoutBuildup(GameTestHelper helper) {
        var recoil = new MultipurposeStaffrifleRecoil();
        for (var shot = 0; shot < 5; shot++) {
            var now = shot * 200_000_000L;
            helper.assertTrue(Math.abs(MultipurposeStaffrifleRecoil.shotPitch(true) - 0.675F) < 0.001F, "ADS must reduce every shot equally");
            recoil.addImpulse(MultipurposeStaffrifleRecoil.shotPitch(false), now);
            helper.assertTrue(Math.abs(recoil.advanceImpulses(now + 100_000_000) - 1.5F) < 0.001F, "Repeated shots must not build recoil");
            helper.assertTrue(recoil.advanceImpulses(now + 150_000_000) == 0, "View must not recover automatically");
        }
        helper.succeed();
    }
}
