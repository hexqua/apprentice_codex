package jp.aquafactory.apprenticecodex.compat.accessories;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStartingEvent;

import java.util.List;

@EventBusSubscriber(modid = ApprenticeCodex.MODID)
public final class AccessoriesCompatibility {
    public static final String MOD_ID = "accessories_compat_layer";
    public static final String CASE_WARNING_KEY = "item.apprenticecodex.spellcaster_accessory_case.incompatible_acl";
    public static final String CHAT_WARNING_KEY = "chat.apprenticecodex.warning_incompatible_acl";

    private AccessoriesCompatibility() {
    }

    public static boolean isUnsupportedLayerLoaded() {
        return ModList.get().isLoaded(MOD_ID);
    }

    public static void appendCaseWarning(List<Component> lines) {
        if (isUnsupportedLayerLoaded()) {
            lines.add(Component.translatable(CASE_WARNING_KEY).withStyle(ChatFormatting.YELLOW));
        }
    }

    @SubscribeEvent
    public static void onServerStarting(ServerStartingEvent event) {
        // 再ログインでは繰り返さず、専用・統合サーバーの起動ごとに管理者へ通知する。
        if (isUnsupportedLayerLoaded()) {
            ApprenticeCodex.LOGGER.warn("Accessories Compatibility Layer is not officially supported by Apprentice's Codex. "
                    + "Curios equipment may not function correctly, including equipment validation and slot updates in the Spellcaster Accessory Case. "
                    + "The existing back-slot workaround does not provide full compatibility.");
        }
    }
}
