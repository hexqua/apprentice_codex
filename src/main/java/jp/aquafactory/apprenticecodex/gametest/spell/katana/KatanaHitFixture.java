package jp.aquafactory.apprenticecodex.gametest.spell.katana;

import jp.aquafactory.apprenticecodex.utility.RaycastTools;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;

final class KatanaHitFixture {
    private KatanaHitFixture() {}

    static RaycastTools.OrientedBoxHit singleHit(
            GameTestHelper helper,
            ServerLevel level,
            Entity source,
            RaycastTools.HorizontalOrientedBox attackBox
    ) {
        var hits = RaycastTools.hitsHorizontalOrientedBox(level, source, attackBox, entity -> true);
        helper.assertTrue(hits.size() == 1, "Expected exactly one oriented-box hit");
        return hits.get(0);
    }
}
