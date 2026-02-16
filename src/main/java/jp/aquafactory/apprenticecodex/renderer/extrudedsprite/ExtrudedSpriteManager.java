package jp.aquafactory.apprenticecodex.renderer.extrudedsprite;

import com.mojang.blaze3d.platform.NativeImage;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;

import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class ExtrudedSpriteManager {
    private static final Map<ResourceLocation, ExtrudedSpriteMesh> CACHE = new ConcurrentHashMap<>();
    private static final float THICKNESS = 1.0f / 16.0f;

    public static ExtrudedSpriteMesh get(ResourceLocation texture) {
        return CACHE.computeIfAbsent(texture, key -> {
            try (NativeImage img = readTexture(key)) {
                return ExtrudedSpriteMesher.bake(img, THICKNESS);
            } catch (Exception e) {
                ApprenticeCodex.LOGGER.error("Failed to load texture: {}", key, e);
                return new ExtrudedSpriteMesh(java.util.List.of());
            }
        });
    }

    public static void clear() {
        CACHE.clear();
    }

    private static NativeImage readTexture(ResourceLocation texture) throws Exception {
        var rm = Minecraft.getInstance().getResourceManager();
        var res = rm.getResource(texture).orElseThrow();
        try (InputStream in = res.open()) {
            return NativeImage.read(in);
        }
    }
}
