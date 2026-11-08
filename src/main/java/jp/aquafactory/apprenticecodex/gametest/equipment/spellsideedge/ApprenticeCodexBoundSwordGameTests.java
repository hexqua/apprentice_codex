package jp.aquafactory.apprenticecodex.gametest.equipment.spellsideedge;

import io.redspace.ironsspellbooks.api.magic.MagicData;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraftforge.common.util.FakePlayer;

public final class ApprenticeCodexBoundSwordGameTests {
    private ApprenticeCodexBoundSwordGameTests() {}

    public static MagicData resolveMagicData(GameTestHelper helper, FakePlayer player) {
        var magicData = MagicData.getPlayerMagicData(player);
        helper.assertTrue(magicData != null, "Bound Sword test could not resolve player magic data");
        return magicData;
    }
}
