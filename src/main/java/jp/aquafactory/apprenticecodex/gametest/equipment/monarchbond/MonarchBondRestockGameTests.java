package jp.aquafactory.apprenticecodex.gametest.equipment.monarchbond;

import io.redspace.ironsspellbooks.api.magic.MagicData;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.gametest.support.DeployedSpellTestFixture;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import static jp.aquafactory.apprenticecodex.gametest.equipment.monarchbond.MonarchBondTestSupport.*;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class MonarchBondRestockGameTests {
    private MonarchBondRestockGameTests() {
    }

    @GameTest(template = DeployedSpellTestFixture.TEMPLATE)
    public static void monarchBondAutoRestockRefillsEmptyOwnedTurretAtRangeBoundary(GameTestHelper helper) {
        var wearer = createWearer(helper, "monarch_bond_auto_restock_boundary");
        var magicData = MagicData.getPlayerMagicData(wearer);
        magicData.setMana(17.0F);
        var turret = createAutoTurret(
                helper, wearer, new BlockPos(32, 2, 0), 5, 17);
        turret.setRestBulletCount(0);

        tickCharmAtRestockInterval(wearer);

        helper.assertTrue(turret.getRestBulletCount() == 5,
                "Monarch Bond should refill an empty owned AutoTurret at the range boundary");
        helper.assertTrue(Math.abs(magicData.getMana()) <= EPSILON,
                "Monarch Bond should spend the exact AutoTurret restock mana cost");
        helper.succeed();
    }

    @GameTest(template = DeployedSpellTestFixture.TEMPLATE)
    public static void monarchBondAutoRestockUsesNearestDeterministicPriority(GameTestHelper helper) {
        var wearer = createWearer(helper, "monarch_bond_auto_restock_priority");
        var magicData = MagicData.getPlayerMagicData(wearer);
        magicData.setMana(10.0F);
        var farther = createAutoTurret(helper, wearer, new BlockPos(6, 2, 0), 5, 10);
        var nearer = createAutoTurret(helper, wearer, new BlockPos(2, 2, 0), 5, 10);
        farther.setRestBulletCount(0);
        nearer.setRestBulletCount(0);

        tickCharmAtRestockInterval(wearer);

        helper.assertTrue(nearer.getRestBulletCount() == 5,
                "Monarch Bond should prioritize the nearest empty AutoTurret");
        helper.assertTrue(farther.getRestBulletCount() == 0,
                "Monarch Bond should leave the farther AutoTurret empty after mana is exhausted");
        helper.succeed();
    }

    @GameTest(template = DeployedSpellTestFixture.TEMPLATE)
    public static void monarchBondAutoRestockSkipsIneligibleAndUnaffordableTurrets(GameTestHelper helper) {
        var wearer = createWearer(helper, "monarch_bond_auto_restock_filter");
        var magicData = MagicData.getPlayerMagicData(wearer);
        magicData.setMana(10.0F);
        var expensive = createAutoTurret(helper, wearer, new BlockPos(2, 2, 0), 5, 20);
        var partial = createAutoTurret(helper, wearer, new BlockPos(3, 2, 0), 5, 10);
        var affordable = createAutoTurret(helper, wearer, new BlockPos(4, 2, 0), 5, 10);
        var outside = createAutoTurret(helper, wearer, new BlockPos(33, 2, 0), 5, 0);
        var otherOwner = DeployedSpellTestFixture.of(helper).player(new BlockPos(0, 2, 2), "monarch_bond_auto_restock_other_owner");
        var foreign = createAutoTurret(helper, otherOwner, new BlockPos(5, 2, 0), 5, 0);
        expensive.setRestBulletCount(0);
        partial.setRestBulletCount(1);
        affordable.setRestBulletCount(0);
        outside.setRestBulletCount(0);
        foreign.setRestBulletCount(0);

        tickCharmAtRestockInterval(wearer);

        helper.assertTrue(expensive.getRestBulletCount() == 0,
                "Monarch Bond should skip an AutoTurret whose restock cost cannot be paid");
        helper.assertTrue(partial.getRestBulletCount() == 1,
                "Monarch Bond should not refill an AutoTurret that still has ammunition");
        helper.assertTrue(affordable.getRestBulletCount() == 5,
                "Monarch Bond should continue to a later affordable AutoTurret");
        helper.assertTrue(outside.getRestBulletCount() == 0,
                "Monarch Bond should not refill an AutoTurret outside its range");
        helper.assertTrue(foreign.getRestBulletCount() == 0,
                "Monarch Bond should not refill another owner's AutoTurret");
        helper.succeed();
    }
}
