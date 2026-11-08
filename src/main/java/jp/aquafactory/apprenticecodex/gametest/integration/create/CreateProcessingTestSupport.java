package jp.aquafactory.apprenticecodex.gametest.integration.create;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;

import static jp.aquafactory.apprenticecodex.gametest.equipment.spelldispenser.integration.create.CreateDispenserTestSupport.CREATE_GAMETEST_HOOKS_CLASS;

public final class CreateProcessingTestSupport {
    private CreateProcessingTestSupport() {}

    public static final String CREATE_MOD_ID = "create";

    public static void invokeCreateGameTestHookVoid(String methodName, Class<?>[] parameterTypes, Object... args) {
        try {
            var hookClass = Class.forName(CREATE_GAMETEST_HOOKS_CLASS);
            hookClass.getMethod(methodName, parameterTypes).invoke(null, args);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Create GameTest hook invocation failed: " + methodName, exception);
        }
    }

    public static ItemStack invokeCreateGameTestHookItemStack(String methodName, ServerLevel level, BlockPos pos) {
        return invokeCreateGameTestHookItemStack(
                methodName,
                new Class<?>[]{ServerLevel.class, BlockPos.class},
                level,
                pos
        );
    }

    public static ItemStack invokeCreateGameTestHookItemStack(
            String methodName,
            Class<?>[] parameterTypes,
            Object... args
    ) {
        try {
            var hookClass = Class.forName(CREATE_GAMETEST_HOOKS_CLASS);
            var result = hookClass.getMethod(methodName, parameterTypes).invoke(null, args);
            return result instanceof ItemStack stack ? stack : ItemStack.EMPTY;
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Create GameTest hook invocation failed: " + methodName, exception);
        }
    }

    public static void placeCreateBasinWithItems(ServerLevel level, BlockPos pos, ItemStack... stacks) {
        invokeCreateGameTestHookVoid(
                "placeBasinWithItems",
                new Class<?>[]{ServerLevel.class, BlockPos.class, ItemStack[].class},
                level,
                pos,
                stacks
        );
    }

    public static int getCreateBasinItemCount(ServerLevel level, BlockPos pos, ItemStack prototype) {
        try {
            var hookClass = Class.forName(CREATE_GAMETEST_HOOKS_CLASS);
            var result = hookClass.getMethod("getBasinItemCount", ServerLevel.class, BlockPos.class, ItemStack.class)
                    .invoke(null, level, pos, prototype);
            return result instanceof Integer count ? count : 0;
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Create GameTest hook invocation failed: getBasinItemCount", exception);
        }
    }
}
