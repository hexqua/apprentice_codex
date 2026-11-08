package jp.aquafactory.apprenticecodex.gametest.equipment.spelldispenser.integration.create;

import com.mojang.authlib.GameProfile;
import jp.aquafactory.apprenticecodex.gametest.equipment.spelldispenser.support.DispenserTestScope;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.items.ItemStackHandler;

final class CreateDispenserTestSupport {
    private static final String CREATE_GAMETEST_HOOKS_CLASS =
            "jp.aquafactory.apprenticecodex.gametest.integration.create.CreateGameTestHooks";

    private CreateDispenserTestSupport() {}

    static boolean skipWhenCreateMissing(GameTestHelper helper) {
        if (ModList.get().isLoaded("create")) {
            return false;
        }

        // optional 依存の absent 環境では Create 専用テストを成功扱いで抜け、通常検証の起動性を優先する。
        helper.succeed();
        return true;
    }

    static Object createSpellDispenserMovementHarness(
            ServerLevel level,
            BlockPos worldPos,
            ItemStackHandler mountedInventory,
            GameProfile ownerProfile
    ) {
        return invokeCreateGameTestHook(
                "createSpellDispenserMovementHarness",
                new Class<?>[]{ServerLevel.class, BlockPos.class, ItemStackHandler.class, GameProfile.class},
                level, worldPos, mountedInventory, ownerProfile
        );
    }

    static Object createSpellDispenserMovementHarness(
            ServerLevel level,
            BlockPos worldPos,
            ItemStackHandler mountedInventory,
            GameProfile ownerProfile,
            int currentMana,
            ItemStackHandler externalInventory,
            boolean externalAvailableForFuel
    ) {
        return invokeCreateGameTestHook(
                "createSpellDispenserMovementHarness",
                new Class<?>[]{ServerLevel.class, BlockPos.class, ItemStackHandler.class, GameProfile.class, int.class, ItemStackHandler.class, boolean.class},
                level, worldPos, mountedInventory, ownerProfile, currentMana, externalInventory, externalAvailableForFuel
        );
    }

    static void startCreateSpellDispenserMovement(DispenserTestScope scope, Object harness) {
        scope.run(() -> invokeCreateGameTestHook("startMoving", new Class<?>[]{Object.class}, harness));
    }

    static void tickCreateSpellDispenserMovement(DispenserTestScope scope, Object harness) {
        scope.run(() -> invokeCreateGameTestHook("tick", new Class<?>[]{Object.class}, harness));
    }

    static Object invokeCreateGameTestHook(String methodName, Class<?>[] parameterTypes, Object... args) {
        try {
            var hooksClass = Class.forName(CREATE_GAMETEST_HOOKS_CLASS);
            return hooksClass.getMethod(methodName, parameterTypes).invoke(null, args);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Create GameTest helper call failed: " + methodName, exception);
        }
    }

    static Object trackHarness(DispenserTestScope scope, Object harness) {
        scope.onClose(() -> invokeCreateGameTestHook("stopMoving", new Class<?>[]{Object.class}, harness));
        return harness;
    }
}
