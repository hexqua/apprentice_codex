package jp.aquafactory.apprenticecodex.gametest.equipment.spellsideedge;

import io.redspace.ironsspellbooks.api.magic.MagicData;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.player.Player;

final class SpellSideEdgeGameTestScenariosHelpers {
    private SpellSideEdgeGameTestScenariosHelpers() {}

    static MagicData resolveMagicData(GameTestHelper helper, Player player) {
        var magicData = MagicData.getPlayerMagicData(player);
        helper.assertTrue(magicData != null, "Edge Dancer test could not resolve player magic data");
        return magicData;
    }
}
