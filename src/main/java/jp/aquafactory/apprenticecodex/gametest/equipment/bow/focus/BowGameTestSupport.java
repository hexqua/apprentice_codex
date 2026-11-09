package jp.aquafactory.apprenticecodex.gametest.equipment.bow.focus;

import jp.aquafactory.apprenticecodex.config.ApprenticeCodexServerConfig;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArrowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.util.FakePlayer;

import java.util.List;
import java.util.UUID;

final class BowGameTestSupport {
    private BowGameTestSupport() {}

    static final UUID FOCUS_STAFFBOW_OVERCHARGE_MODIFIER_ID = UUID.fromString("a7dc54b6-a83c-4a5f-ae93-0cb49780fc8f");

    static void setFocusStaffbowArrowCatalyst(FakePlayer player, ItemStack arrowStack) {
        player.getInventory().setItem(1, arrowStack.copy());
    }

    static ApprenticeCodexServerConfig.GameTestConfigOverride useFocusStaffbowConfigOverrideForGameTest(
            boolean enableContinuousFocusedCast,
            boolean enableManaLoan,
            boolean enableArrowCatalystRequirement,
            double pendingMaxLoanManaRatio,
            List<String> spellDenylist,
            boolean enableSpellAllowlist,
            List<String> spellAllowlist
    ) {
        return ApprenticeCodexServerConfig.useFocusStaffbowConfigOverrideForGameTest(
                enableContinuousFocusedCast,
                enableManaLoan,
                enableArrowCatalystRequirement,
                List.of("minecraft:arrow"),
                3.0D,
                2.0D,
                20,
                2.0D,
                1.0D,
                pendingMaxLoanManaRatio,
                spellDenylist,
                enableSpellAllowlist,
                spellAllowlist
        );
    }

    static int getFocusStaffbowArrowCount(Player player) {
        int count = 0;
        for (var stack : player.getInventory().items) {
            if (stack.getItem() instanceof ArrowItem) {
                count += stack.getCount();
            }
        }
        for (var stack : player.getInventory().offhand) {
            if (stack.getItem() instanceof ArrowItem) {
                count += stack.getCount();
            }
        }
        return count;
    }
}
