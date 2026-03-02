package jp.aquafactory.apprenticecodex.compat.jei;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.NotNull;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@SuppressWarnings("unused")
@JeiPlugin
public class ApprenticeCodexJeiPlugin implements IModPlugin {
    private static final String EN_US_RESOURCE_PATH = "assets/" + ApprenticeCodex.MODID + "/lang/en_us.json";
    private static final String JEI_INFO_KEY_PREFIX = "item.%s.%s.jei.desc_";
    private static final int MAX_INFO_LINES = 32;

    private static final ResourceLocation PLUGIN_UID =
            ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "jei_plugin");
    private static final Set<String> EN_US_TRANSLATION_KEYS = loadEnUsTranslationKeys();

    @Override
    public @NotNull ResourceLocation getPluginUid() {
        return PLUGIN_UID;
    }

    @Override
    public void registerRecipes(@NotNull IRecipeRegistration registration) {
        for (var item : ForgeRegistries.ITEMS.getValues()) {
            var itemId = ForgeRegistries.ITEMS.getKey(item);
            if (itemId == null || !ApprenticeCodex.MODID.equals(itemId.getNamespace())) {
                continue;
            }

            var infoComponents = collectInfoComponents(itemId);
            if (infoComponents.isEmpty()) {
                continue;
            }

            registration.addItemStackInfo(new ItemStack(item), infoComponents.toArray(Component[]::new));
        }
    }

    private static List<Component> collectInfoComponents(ResourceLocation itemId) {
        var keyPattern = JEI_INFO_KEY_PREFIX.formatted(itemId.getNamespace(), itemId.getPath());
        List<Component> components = new ArrayList<>();

        for (int line = 1; line <= MAX_INFO_LINES; line++) {
            var key = keyPattern + line;
            if (!EN_US_TRANSLATION_KEYS.contains(key)) {
                break;
            }

            components.add(Component.translatable(key));
        }
        return components;
    }

    private static Set<String> loadEnUsTranslationKeys() {
        try (var stream = ApprenticeCodexJeiPlugin.class.getClassLoader().getResourceAsStream(EN_US_RESOURCE_PATH)) {
            if (stream == null) {
                ApprenticeCodex.LOGGER.warn("JEI info disabled: {} was not found.", EN_US_RESOURCE_PATH);
                return Collections.emptySet();
            }

            try (var reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
                return new HashSet<>(json.keySet());
            }
        } catch (Exception e) {
            ApprenticeCodex.LOGGER.warn("JEI info disabled: failed to read {}.", EN_US_RESOURCE_PATH, e);
            return Collections.emptySet();
        }
    }
}
