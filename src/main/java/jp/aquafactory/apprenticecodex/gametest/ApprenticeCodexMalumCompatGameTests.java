package jp.aquafactory.apprenticecodex.gametest;

import io.redspace.ironsspellbooks.api.magic.MagicData;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.lang.reflect.InvocationTargetException;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class ApprenticeCodexMalumCompatGameTests {
    private static final String TEMPLATE = "gametest/basic_floor";
    private static final String MALUM_MOD_ID = "malum";
    private static final String IRONS_SPELLS_COMPAT =
            "com.sammy.malum.compability.irons_spellbooks.IronsSpellsCompat";

    private ApprenticeCodexMalumCompatGameTests() {
    }

    @GameTest(template = TEMPLATE, batch = "apprenticecodex.malum_compat")
    public static void malumManaweavingManaRecoveryUsesCurrentIronsSync(GameTestHelper helper) {
        if (!ModList.get().isLoaded(MALUM_MOD_ID)) {
            helper.succeed();
            return;
        }

        var player = ApprenticeCodexGameTestScenarios.createTrackedEquipmentTestPlayer(
                helper, new BlockPos(0, 2, 0), "malum_manaweaving_mana_recovery_test"
        );
        helper.runAtTickTime(1, () -> {
            var magicData = MagicData.getPlayerMagicData(player);
            magicData.setMana(0.0F);
            invokeMalumGenerateMana(player, 17.5F);
            helper.assertTrue(Math.abs(magicData.getMana() - 17.5F) < 0.001F,
                    "Malum mana recovery should add mana without calling the removed UpdateClient API");
            helper.succeed();
        });
    }

    private static void invokeMalumGenerateMana(ServerPlayer player, float amount) {
        try {
            var compatClass = Class.forName(IRONS_SPELLS_COMPAT);
            compatClass.getMethod("generateMana", ServerPlayer.class, float.class).invoke(null, player, amount);
        } catch (InvocationTargetException exception) {
            throw new AssertionError("Malum mana recovery threw an exception", exception.getCause());
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError("Unable to invoke Malum mana recovery compatibility API", exception);
        }
    }
}
