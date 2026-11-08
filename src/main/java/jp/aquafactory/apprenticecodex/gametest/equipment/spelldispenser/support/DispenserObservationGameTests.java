package jp.aquafactory.apprenticecodex.gametest.equipment.spelldispenser.support;

import java.util.concurrent.atomic.AtomicInteger;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.projectile.Snowball;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class DispenserObservationGameTests {
    private DispenserObservationGameTests() {}

    @GameTest(template = "gametest/basic_floor")
    public static void ownedObservationExcludesNeighborsAndCleansUpOnException(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var neighbor = helper.spawn(EntityType.ARMOR_STAND, new BlockPos(1, 1, 1));
            var scope = new DispenserTestScope(helper);
            var closes = new AtomicInteger();
            ArmorStand owned = null;
            Snowball child = null;
            try {
                try (scope) {
                    owned = scope.capture(() -> helper.spawn(EntityType.ARMOR_STAND, new BlockPos(2, 1, 2)));
                    var observed = scope.entities(ArmorStand.class);
                    helper.assertTrue(observed.size() == 1 && observed.getFirst() == owned,
                            "Observation should select this operation's entity and exclude its neighbor");
                    child = EntityType.SNOWBALL.create(helper.getLevel());
                    child.setPos(owned.position());
                    child.setOwner(owned);
                    helper.assertTrue(helper.getLevel().addFreshEntity(child), "Owned child fixture should enter the level");
                    helper.assertTrue(scope.entities(Snowball.class).size() == 1,
                            "A child created later by the same owner should remain observable");
                    scope.onClose(closes::incrementAndGet);
                    throw new ProbeFailure();
                }
            } catch (ProbeFailure expected) {
                helper.assertTrue(owned != null && owned.isRemoved() && child != null && child.isRemoved(),
                        "Assertion-like failure should release all owned entities");
                helper.assertFalse(neighbor.isRemoved(), "Cleanup should preserve the unrelated neighboring entity");
                scope.close();
                helper.assertTrue(closes.get() == 1, "Repeated cleanup should run resources exactly once");
            } finally {
                scope.close();
                neighbor.discard();
            }
        });
    }

    private static final class ProbeFailure extends RuntimeException {}
}
