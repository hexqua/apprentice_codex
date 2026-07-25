package jp.aquafactory.apprenticecodex.compat.arsnouveau;

import java.lang.reflect.Method;
import java.util.function.Function;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.item.luminousdevice.LuminousDevice;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;

public final class ArsNouveauLuminousDeviceCompat {
    public static final String MOD_ID = "ars_nouveau";

    private static final String LIGHT_MANAGER_CLASS = "com.hollingsworth.arsnouveau.common.light.LightManager";
    private static final int LIGHT_LEVEL = 15;
    private static boolean initializationAttempted;

    private ArsNouveauLuminousDeviceCompat() {
    }

    public static void register() {
        if (initializationAttempted || !ModList.get().isLoaded(MOD_ID)) {
            return;
        }
        initializationAttempted = true;

        try {
            Class<?> lightManagerClass = Class.forName(LIGHT_MANAGER_CLASS);
            Method registerMethod = lightManagerClass.getMethod("register", EntityType.class, Function.class);

            // Ars Nouveau 5.12.1 の内蔵動的光源は外部の item JSON を参照しないため、公開登録 API へ直接追加する。
            Function<Player, Integer> playerLight = player ->
                    isLuminousDevice(player.getMainHandItem()) || isLuminousDevice(player.getOffhandItem())
                            ? LIGHT_LEVEL
                            : 0;
            Function<ItemEntity, Integer> itemLight = itemEntity ->
                    isLuminousDevice(itemEntity.getItem()) ? LIGHT_LEVEL : 0;

            registerMethod.invoke(null, EntityType.PLAYER, playerLight);
            registerMethod.invoke(null, EntityType.ITEM, itemLight);
            ApprenticeCodex.LOGGER.info("Ars Nouveau の Luminous Device 動的光源連携を有効化しました");
        } catch (ReflectiveOperationException | LinkageError exception) {
            ApprenticeCodex.LOGGER.warn(
                    "Ars Nouveau の Luminous Device 動的光源連携を初期化できなかったため、連携を無効化します",
                    exception);
        }
    }

    private static boolean isLuminousDevice(ItemStack stack) {
        return stack.getItem() instanceof LuminousDevice;
    }
}
