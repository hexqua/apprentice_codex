package jp.aquafactory.apprenticecodex.compat.sodiumdynamiclights;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.item.Item;
import net.neoforged.fml.ModList;

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

            // Sodium Dynamic Lights 1.0.9 では外部 JSON が反映されない環境があるため、同版の登録 API へ直接追加する。
            Object lightSource = constructor.newInstance(
                    ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "luminous_device"),
                    ItemRegistry.LUMINOUS_DEVICE.get(),
                    LIGHT_LEVEL,
                    false);
            registerMethod.invoke(null, lightSource);
            // 初回リソース読込後の登録でも現在の光源テーブルへ反映されるよう、静的登録を含めて再読込する。
            loadMethod.invoke(null, Minecraft.getInstance().getResourceManager());
            ApprenticeCodex.LOGGER.info("Sodium Dynamic Lights の Luminous Device 動的光源連携を有効化しました");
        } catch (ReflectiveOperationException | LinkageError exception) {
            ApprenticeCodex.LOGGER.warn(
                    "Sodium Dynamic Lights の Luminous Device 動的光源連携を初期化できなかったため、JSON 定義のみを使用します",
                    exception);
        }
    }
}
