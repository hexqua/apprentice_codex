package jp.aquafactory.apprenticecodex.compat.sodiumdynamiclights;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.item.Item;
import net.minecraftforge.fml.ModList;

public final class SodiumDynamicLightsLuminousDeviceCompat {
    public static final String MOD_ID = "sodiumdynamiclights";

    private static final String ITEM_LIGHT_SOURCE_CLASS =
            "dev.lambdaurora.lambdynlights.api.item.ItemLightSource";
    private static final String STATIC_ITEM_LIGHT_SOURCE_CLASS =
            ITEM_LIGHT_SOURCE_CLASS + "$StaticItemLightSource";
    private static final String ITEM_LIGHT_SOURCES_CLASS =
            "dev.lambdaurora.lambdynlights.api.item.ItemLightSources";
    private static final int LIGHT_LEVEL = 15;
    private static boolean initializationAttempted;

    private SodiumDynamicLightsLuminousDeviceCompat() {
    }

    public static void register() {
        if (initializationAttempted || !ModList.get().isLoaded(MOD_ID)) {
            return;
        }
        initializationAttempted = true;

        try {
            Class<?> itemLightSourceClass = Class.forName(ITEM_LIGHT_SOURCE_CLASS);
            Class<?> staticItemLightSourceClass = Class.forName(STATIC_ITEM_LIGHT_SOURCE_CLASS);
            Class<?> itemLightSourcesClass = Class.forName(ITEM_LIGHT_SOURCES_CLASS);
            Constructor<?> constructor = staticItemLightSourceClass.getConstructor(
                    ResourceLocation.class,
                    Item.class,
                    int.class,
                    boolean.class);
            Method registerMethod = itemLightSourcesClass.getMethod(
                    "registerItemLightSource",
                    itemLightSourceClass);
            Method loadMethod = itemLightSourcesClass.getMethod("load", ResourceManager.class);

            // 1.20.1 の Sodium Dynamic Lights 1.0.9 で外部 JSON が反映されない環境にも対応するため、
            // 同版が公開する登録 API へ直接追加する。1.21.1 では JSON の動作確認後にこの compat を削除してよい。
            Object lightSource = constructor.newInstance(
                    ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "luminous_device"),
                    ItemRegistry.LUMINOUS_DEVICE.get(),
                    LIGHT_LEVEL,
                    false);
            registerMethod.invoke(null, lightSource);
            // 静的登録先から現在の光源テーブルへコピーする処理はリソース読込時にしか行われないため、
            // client setup より先に初回読込が終わっている環境でも即時反映されるよう、登録後に再読込する。
            loadMethod.invoke(null, Minecraft.getInstance().getResourceManager());
            ApprenticeCodex.LOGGER.info("Sodium Dynamic Lights の Luminous Device 動的光源連携を有効化しました");
        } catch (ReflectiveOperationException | LinkageError exception) {
            ApprenticeCodex.LOGGER.warn(
                    "Sodium Dynamic Lights の Luminous Device 動的光源連携を初期化できなかったため、JSON 定義のみを使用します",
                    exception);
        }
    }
}
