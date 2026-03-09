package jp.aquafactory.apprenticecodex.block.essencesmoker;

import jp.aquafactory.apprenticecodex.config.ApprenticeCodexClientConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.FastColor;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@OnlyIn(Dist.CLIENT)
public final class EssenceSmokerParticlePaletteCache {
    private static final int MAX_SAMPLE_AXIS = 32;
    private static final int MIN_ALPHA = 32;
    private static final int[] FALLBACK_COLORS = {
            0x00D1D1,
            0x7733FF
    };
    private static final Map<CacheKey, int[]> PALETTE_CACHE = new ConcurrentHashMap<>();

    private EssenceSmokerParticlePaletteCache() {
    }

    public static void clear() {
        PALETTE_CACHE.clear();
    }

    public static int pickColor(@NotNull ItemStack stack, @Nullable Level level, @NotNull RandomSource random) {
        var palette = resolvePalette(stack, level);
        return palette[random.nextInt(palette.length)];
    }

    private static int[] resolvePalette(ItemStack stack, @Nullable Level level) {
        if (stack.isEmpty()) {
            return FALLBACK_COLORS;
        }

        // 緊急回避設定ではテクスチャやモデルへ一切触れず、必ず固定色へ落とす。
        if (ApprenticeCodexClientConfig.disableEssenceSmokerParticleTextureAnalysis()) {
            return FALLBACK_COLORS;
        }

        return PALETTE_CACHE.computeIfAbsent(CacheKey.from(stack), ignored -> samplePalette(stack, level));
    }

    private static int[] samplePalette(ItemStack stack, @Nullable Level level) {
        var itemRenderer = Minecraft.getInstance().getItemRenderer();
        var model = itemRenderer.getModel(stack, level, null, 0);
        if (model.isCustomRenderer()) {
            return FALLBACK_COLORS;
        }

        var sprite = model.getParticleIcon();
        if (sprite.contents().name().equals(MissingTextureAtlasSprite.getLocation())) {
            return FALLBACK_COLORS;
        }

        var width = sprite.contents().width();
        var height = sprite.contents().height();
        if (width <= 0 || height <= 0) {
            return FALLBACK_COLORS;
        }

        var sampleWidth = Math.min(width, MAX_SAMPLE_AXIS);
        var sampleHeight = Math.min(height, MAX_SAMPLE_AXIS);
        var colors = new int[sampleWidth * sampleHeight];
        var colorCount = 0;

        for (var yIndex = 0; yIndex < sampleHeight; yIndex++) {
            var y = resolveSampleCoordinate(yIndex, sampleHeight, height);
            for (var xIndex = 0; xIndex < sampleWidth; xIndex++) {
                var x = resolveSampleCoordinate(xIndex, sampleWidth, width);
                var abgr = sprite.getPixelRGBA(0, x, y);
                if (FastColor.ABGR32.alpha(abgr) < MIN_ALPHA) {
                    continue;
                }

                colors[colorCount++] = FastColor.ABGR32.red(abgr) << 16
                        | FastColor.ABGR32.green(abgr) << 8
                        | FastColor.ABGR32.blue(abgr);
            }
        }

        if (colorCount == 0) {
            return FALLBACK_COLORS;
        }

        return Arrays.copyOf(colors, colorCount);
    }

    private static int resolveSampleCoordinate(int sampleIndex, int sampleCount, int textureSize) {
        if (sampleCount <= 1 || textureSize <= 1) {
            return 0;
        }

        return Math.min(textureSize - 1, Math.round(sampleIndex * (textureSize - 1.0f) / (sampleCount - 1.0f)));
    }

    private record CacheKey(String itemId, int damageValue, String tagData) {
        private static CacheKey from(ItemStack stack) {
            var itemId = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
            var tagData = stack.toString();
            return new CacheKey(itemId, stack.getDamageValue(), tagData);
        }
    }
}
