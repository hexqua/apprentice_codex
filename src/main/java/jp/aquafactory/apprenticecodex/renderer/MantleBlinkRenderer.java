package jp.aquafactory.apprenticecodex.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import jp.aquafactory.apprenticecodex.item.curios.shootingstarmantle.MantleBlink;
import jp.aquafactory.apprenticecodex.item.curios.shootingstarmantle.ShootingStarMantleRuntime;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

/** 装備ごとの材質を変更せず、外側の白い光で短い消失を表現する。 */
public final class MantleBlinkRenderer {
    private static final RenderType CORE = ApprenticeRenderTypes.mantleBlink(false);
    private static final RenderType GLOW = ApprenticeRenderTypes.mantleBlink(true);

    private MantleBlinkRenderer() { }

    public static double elapsed(Entity entity, float partialTick) {
        if (!(entity instanceof Player player) || !player.isAlive()) return -1;
        var minecraft = Minecraft.getInstance();
        if (player == minecraft.getCameraEntity() && minecraft.options.getCameraType().isFirstPerson()) return -1;
        var state = ShootingStarMantleRuntime.state(player);
        if (!state.equipped || state.blink.start() < 0) return -1;
        // 他playerの通常の3tick位置補間と、消失・出現の時系列を合わせる。
        int interpolationDelay = player == minecraft.player ? 0 : 3;
        return state.blink.renderElapsed(player.level().getGameTime() - interpolationDelay, partialTick);
    }

    public static boolean active(Entity entity, float partialTick) {
        double age = elapsed(entity, partialTick);
        return age >= 0 && age < MantleBlink.DURATION;
    }

    public static float distortion(double age) {
        float progress = (float) (age < MantleBlink.DISAPPEAR_TICKS
                ? age / MantleBlink.DISAPPEAR_TICKS
                : (MantleBlink.DURATION - age) / MantleBlink.DISAPPEAR_TICKS);
        progress = Mth.clamp(progress, 0, 1);
        return progress * progress * (3 - 2 * progress);
    }

    public static void transform(Entity entity, PoseStack pose, float amount) {
        double center = entity.getBbHeight() * 0.5;
        pose.translate(0, center, 0);
        pose.scale(Mth.lerp(amount, 1, 0.05F), Mth.lerp(amount, 1, 2.5F), Mth.lerp(amount, 1, 0.05F));
        pose.translate(0, -center, 0);
    }

    public static void drawLight(Entity entity, PoseStack pose, MultiBufferSource buffers, float amount) {
        if (amount <= 0) return;
        // 通常モデルの固定bufferより白い面が先に描かれると上書きされるため、この短い演出だけ順序を確定する。
        if (buffers instanceof MultiBufferSource.BufferSource source) source.endBatch();
        box(pose, buffers.getBuffer(CORE), entity.getBbWidth() * 0.75F,
                entity.getBbHeight(), 1.05F, Math.round(amount * 210));
        box(pose, buffers.getBuffer(GLOW), entity.getBbWidth() * 0.82F,
                entity.getBbHeight(), 1.12F, Math.round(amount * 65));
        if (buffers instanceof MultiBufferSource.BufferSource source) {
            source.endBatch(CORE);
            source.endBatch(GLOW);
        }
    }

    private static void box(PoseStack pose, VertexConsumer buffer, float radius, float height, float scaleY, int alpha) {
        float bottom = height * (1 - scaleY) * 0.5F;
        float top = height - bottom;
        float[][] vertices = {
                {-radius, bottom, -radius}, {radius, bottom, -radius},
                {radius, top, -radius}, {-radius, top, -radius},
                {-radius, bottom, radius}, {radius, bottom, radius},
                {radius, top, radius}, {-radius, top, radius}
        };
        int[][] faces = {{0, 3, 2, 1}, {4, 5, 6, 7}, {0, 4, 7, 3}, {1, 2, 6, 5}, {3, 7, 6, 2}, {0, 1, 5, 4}};
        for (var face : faces) {
            for (int index : face) {
                var vertex = vertices[index];
                buffer.vertex(pose.last().pose(), vertex[0], vertex[1], vertex[2])
                        .color(255, 255, 255, alpha).endVertex();
            }
        }
    }
}
