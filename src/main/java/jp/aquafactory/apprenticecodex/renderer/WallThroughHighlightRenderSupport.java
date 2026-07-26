package jp.aquafactory.apprenticecodex.renderer;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.VertexConsumer;
import jp.aquafactory.apprenticecodex.config.ApprenticeCodexClientConfig;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

public final class WallThroughHighlightRenderSupport {
    private static final ByteBufferBuilder BUFFER_BUILDER = new ByteBufferBuilder(256);
    private static final MultiBufferSource.BufferSource BUFFER_SOURCE =
            MultiBufferSource.immediate(BUFFER_BUILDER);

    private WallThroughHighlightRenderSupport() {
    }

    public static boolean shouldRenderAt(RenderLevelStageEvent.Stage stage) {
        return switch (ApprenticeCodexClientConfig.wallThroughHighlightRenderMode()) {
            case WORLD -> stage == RenderLevelStageEvent.Stage.AFTER_PARTICLES;
            case COMPAT_OVERLAY -> stage == RenderLevelStageEvent.Stage.AFTER_LEVEL;
        };
    }

    public static VertexConsumer getBuffer(RenderType renderType) {
        return BUFFER_SOURCE.getBuffer(renderType);
    }

    public static void endBatch(RenderType renderType) {
        // Iris/Oculus が置換する共有バッファを避け、深度無視の状態でこの場で描き切る。
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        try {
            BUFFER_SOURCE.endBatch(renderType);
        } finally {
            RenderSystem.depthMask(true);
            RenderSystem.enableDepthTest();
        }
    }
}
