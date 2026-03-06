package jp.aquafactory.apprenticecodex.utility;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

public final class AdvancementTools {
    public static final ResourceLocation RETRIEVE_ONCE_ARCANUM_IN_A_JAR =
            modAdvancement("retrieve_once_arcanum_in_a_jar");
    public static final ResourceLocation RETRIEVE_MAX_ARCANUM_IN_A_JAR =
            modAdvancement("retrieve_max_arcanum_in_a_jar");
    public static final String RETRIEVE_ARCANE_ESSENCE_CRITERION = "retrieve_arcane_essence";
    public static final String RETRIEVE_FULLY_CHARGED_ARCANUM_CRITERION = "retrieve_fully_charged_arcanum";

    private AdvancementTools() {
    }

    public static ResourceLocation modAdvancement(String path) {
        return ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "apprentice_codex/" + path);
    }

    // 標準トリガーではなく直接 criterion を達成させる.
    public static void award(ServerPlayer player, ResourceLocation advancementId, String criterion) {
        var advancement = player.server.getAdvancements().getAdvancement(advancementId);
        if (advancement == null) {
            ApprenticeCodex.LOGGER.warn("Missing advancement: {}", advancementId);
            return;
        }

        player.getAdvancements().award(advancement, criterion);
    }
}
