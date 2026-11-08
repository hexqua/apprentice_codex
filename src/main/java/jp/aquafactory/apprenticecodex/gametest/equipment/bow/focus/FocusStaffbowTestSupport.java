package jp.aquafactory.apprenticecodex.gametest.equipment.bow.focus;

import io.redspace.ironsspellbooks.api.magic.MagicData;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.config.ApprenticeCodexServerConfig;
import jp.aquafactory.apprenticecodex.entity.SummonWeaponEntity;
import jp.aquafactory.apprenticecodex.gametest.equipment.bow.support.BowTestScope;
import jp.aquafactory.apprenticecodex.gametest.support.PlayerTestSupport;
import jp.aquafactory.apprenticecodex.item.focusstaffbow.FocusStaffbowCastManager;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArrowItem;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.util.FakePlayer;

import java.util.List;

final class FocusStaffbowTestSupport {
    private FocusStaffbowTestSupport() {}
    static final ResourceLocation FOCUS_STAFFBOW_OVERCHARGE_MODIFIER_ID = ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "focus_staffbow_overcharge");

    static void setFocusStaffbowArrowCatalyst(FakePlayer player, ItemStack arrowStack) {
        player.getInventory().setItem(1, arrowStack.copy());
    }

    static ApprenticeCodexServerConfig.GameTestConfigOverride useFocusStaffbowConfigOverrideForGameTest(
            GameTestHelper helper,
            boolean enableContinuousFocusedCast,
            boolean enableManaLoan,
            boolean enableArrowCatalystRequirement,
            double pendingMaxLoanManaRatio,
            List<String> spellDenylist,
            boolean enableSpellAllowlist,
            List<String> spellAllowlist
    ) {
        var original = ApprenticeCodexServerConfig.useFocusStaffbowConfigOverrideForGameTest(
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
        var closed = new boolean[1];
        ApprenticeCodexServerConfig.GameTestConfigOverride result = () -> {
            if (!closed[0]) { closed[0] = true; original.close(); }
        };
        BowTestScope.of(helper).onClose(result::close);
        return result;
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
    static FakePlayer createEquipmentTestPlayer(GameTestHelper helper, BlockPos pos, String profile) {
        return track(helper, PlayerTestSupport.createEquipmentTestPlayer(helper, pos, profile));
    }

    static FakePlayer createTrackedPlayer(GameTestHelper helper, BlockPos pos, String profile) {
        return track(helper, PlayerTestSupport.createTrackedEquipmentTestPlayer(helper, pos, profile));
    }

    private static FakePlayer track(GameTestHelper helper, FakePlayer player) {
        var scope = BowTestScope.of(helper);
        scope.track(player);
        scope.assertInside(player);
        scope.onClose(() -> {
            FocusStaffbowCastManager.resetPendingState(player, false);
            player.stopUsingItem();
            var data = MagicData.getPlayerMagicData(player);
            if (data != null) { data.resetCastingState(); data.resetAdditionalCastData(); }
            player.removeAllEffects();
        });
        return player;
    }

    static <T extends SummonWeaponEntity> List<T> getOwnedSummonWeapons(GameTestHelper helper, FakePlayer owner, Class<T> type) {
        var scope = BowTestScope.of(helper);
        var result = scope.entities(type).stream().filter(entity -> entity.getOwner() == owner).toList();
        result.forEach(scope::assertInside);
        return result;
    }
}
