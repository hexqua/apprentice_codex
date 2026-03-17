package jp.aquafactory.apprenticecodex.datagen;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

public final class ConditionalAdvancementDataGenerator implements DataProvider {
    private static final String APOTHEOSIS_MOD_ID = "apotheosis";
    private static final String ENCHANT_MAX_LEVEL_CRITERION = "enchant_max_level";

    private final PackOutput.PathProvider pathProvider;

    public ConditionalAdvancementDataGenerator(PackOutput output) {
        this.pathProvider = output.createPathProvider(PackOutput.Target.DATA_PACK, "advancement");
    }

    @Override
    public @NotNull CompletableFuture<?> run(@NotNull CachedOutput cachedOutput) {
        var advancementId = advancementId("enchant_max_level");
        return DataProvider.saveStable(cachedOutput, createEnchantMaxLevelAdvancement(), pathProvider.json(advancementId));
    }

    @Override
    public @NotNull String getName() {
        return "ApprenticeCodex Conditional Advancements";
    }

    private static JsonObject createEnchantMaxLevelAdvancement() {
        var root = new JsonObject();

        var conditions = new JsonArray();
        var condition = new JsonObject();
        condition.addProperty("type", "neoforge:not");
        var nestedCondition = new JsonObject();
        nestedCondition.addProperty("type", "neoforge:mod_loaded");
        nestedCondition.addProperty("modid", APOTHEOSIS_MOD_ID);
        condition.add("value", nestedCondition);
        conditions.add(condition);
        root.add("neoforge:conditions", conditions);

        root.addProperty("parent", advancementId("equip_enchantress_robe").toString());

        var display = new JsonObject();
        var icon = new JsonObject();
        icon.addProperty("id", "minecraft:enchanting_table");
        icon.addProperty("count", 1);
        display.add("icon", icon);
        display.addProperty("frame", "challenge");
        display.addProperty("show_toast", true);
        display.addProperty("announce_to_chat", true);
        display.addProperty("hidden", false);
        var title = new JsonObject();
        title.addProperty("translate", "advancements.apprenticecodex.apprentice_codex.enchant_max_level.title");
        display.add("title", title);
        var description = new JsonObject();
        description.addProperty("translate", "advancements.apprenticecodex.apprentice_codex.enchant_max_level.description");
        display.add("description", description);
        root.add("display", display);

        var criteria = new JsonObject();
        var criterion = new JsonObject();
        criterion.addProperty("trigger", "minecraft:impossible");
        criteria.add(ENCHANT_MAX_LEVEL_CRITERION, criterion);
        root.add("criteria", criteria);

        var requirementGroup = new JsonArray();
        requirementGroup.add(ENCHANT_MAX_LEVEL_CRITERION);
        var requirements = new JsonArray();
        requirements.add(requirementGroup);
        root.add("requirements", requirements);
        root.addProperty("sends_telemetry_event", true);

        return root;
    }

    private static ResourceLocation advancementId(String path) {
        return ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "apprentice_codex/" + path);
    }
}
