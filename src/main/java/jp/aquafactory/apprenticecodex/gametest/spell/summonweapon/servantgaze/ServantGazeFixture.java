package jp.aquafactory.apprenticecodex.gametest.spell.summonweapon.servantgaze;

import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.capabilities.magic.RecastInstance;
import jp.aquafactory.apprenticecodex.registry.EntityRegistry;
import jp.aquafactory.apprenticecodex.spell.servantgaze.ServantGaze;
import jp.aquafactory.apprenticecodex.spell.servantgaze.ServantGazeManager;
import jp.aquafactory.apprenticecodex.spell.servantgaze.ServantGazeStaffEntity;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraftforge.common.util.FakePlayer;

import static jp.aquafactory.apprenticecodex.gametest.spell.summonweapon.servantgaze.ServantGazeLifecycleGameTests.servantGaze;

final class ServantGazeFixture {
    private ServantGazeFixture() {}

    // 周辺 Entity を扱う GameTest 同士の並列干渉を避け、ライフサイクル系と攻撃対象系も分離する。
    static final String LIFECYCLE_ISOLATED_BATCH = "apprenticecodex.servant_gaze_lifecycle_isolated";

    static final String TARGETING_ISOLATED_BATCH = "apprenticecodex.servant_gaze_targeting_isolated";

    static void activate(GameTestHelper helper, FakePlayer player, int duration, int spellLevel,
                                 float damage, double radius, int manaCost) {
        var magicData = MagicData.getPlayerMagicData(player);
        var castData = new ServantGaze.ServantGazeCastData(spellLevel, damage, radius, manaCost);
        var staff = new ServantGazeStaffEntity(EntityRegistry.SERVANT_GAZE_STAFF.get(), helper.getLevel(), player);
        ServantGazeManager.initialize(player, staff, duration, castData);
        helper.getLevel().addFreshEntity(staff);
        var recast = new RecastInstance(servantGaze().getSpellId(), spellLevel, 2, duration,
                CastSource.SPELLBOOK, castData);
        magicData.getPlayerRecasts().addRecast(recast, magicData);
    }
}
