package jp.aquafactory.apprenticecodex.gametest.spell.targeting.multipart;

import jp.aquafactory.apprenticecodex.utility.CombatTools;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.fml.ModList;

import static jp.aquafactory.apprenticecodex.gametest.spell.targeting.multipart.DragonDamageGameTests.Scene;
import static jp.aquafactory.apprenticecodex.gametest.spell.targeting.multipart.DragonDamageGameTests.assertDamage;

final class DragonTargetFixture {
    private DragonTargetFixture() {}

    static void assertDragonDamage(GameTestHelper helper, Scene scene, boolean spell) {
        // 比較条件ごとに別個体を使い、部位間で共有する無敵時間やフェーズ遷移を混ぜない。
        var dragon = scene.dragon();
        for (int index = 0; index <= dragon.getSubEntities().length; index++) {
            if (index > 0) dragon = scene.dragon();
            Entity target = index < dragon.getSubEntities().length ? dragon.getSubEntities()[index] : dragon;
            var source = helper.getLevel().damageSources().playerAttack(scene.owner);
            boolean applied = spell
                    ? CombatTools.applyDamage(target, 20, source, null, CombatTools.KnockbackTypes.NO_KNOCKBACK)
                    : CombatTools.applyUnscaledDamage(target, 20, source, CombatTools.KnockbackTypes.NO_KNOCKBACK);
            helper.assertTrue(applied, "Dragon damage should be accepted for target " + index);
            assertDamage(helper, dragon, 20);
        }
    }

    static boolean skipDragonTest(GameTestHelper helper) {
        // Epic Fight系はドラゴンの部位・ダメージ処理を変更するため、バニラ比較の対象外にする。
        if (ModList.get().isLoaded("epicfight") || ModList.get().isLoaded("efn")) {
            helper.succeed();
            return true;
        }
        return false;
    }
}
