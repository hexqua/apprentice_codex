package jp.aquafactory.apprenticecodex.event.client;

import com.mojang.blaze3d.platform.GlStateManager.DestFactor;
import com.mojang.blaze3d.platform.GlStateManager.SourceFactor;
import com.mojang.blaze3d.systems.RenderSystem;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.compat.epicfight.EpicFightClientCompat;
import jp.aquafactory.apprenticecodex.item.psychicdrivegauntlets.PsychicDriveGauntletsHudState;
import jp.aquafactory.apprenticecodex.item.psychicdrivegauntlets.PsychicDriveGauntletsHudState.Gauge;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.neoforged.fml.ModList;
import org.jetbrains.annotations.NotNull;

public final class PsychicDriveGauntletsHud implements LayeredDraw.Layer {
    public static final PsychicDriveGauntletsHud INSTANCE = new PsychicDriveGauntletsHud();
    private static final ResourceLocation LEFT = texture("left");
    private static final ResourceLocation RIGHT = texture("right");
    private static final ResourceLocation LEFT_BACK = texture("left_back");
    private static final ResourceLocation RIGHT_BACK = texture("right_back");
    private static final int SIZE = 16;
    // 前景画像の実画素はY=3〜13。透明余白まで充填に数えると開始と完了が遅れて見える。
    private static final int FILL_BOTTOM = 14;
    private static final int FILL_HEIGHT = 11;

    private PsychicDriveGauntletsHud() {}

    @Override
    public void render(@NotNull GuiGraphics graphics, @NotNull DeltaTracker deltaTracker) {
        var minecraft = Minecraft.getInstance();
        var player = minecraft.player;
        if (player == null || minecraft.level == null || minecraft.options.hideGui || minecraft.screen != null
                || minecraft.getCameraEntity() != player || !PsychicDriveGauntletsHudState.canDisplay(player)) return;
        if (!minecraft.options.getCameraType().isFirstPerson()
                && !(ModList.get().isLoaded(EpicFightClientCompat.MOD_ID) && EpicFightClientCompat.isTpsMode())) return;
        if (minecraft.getDebugOverlay().showDebugScreen() && !player.isReducedDebugInfo()
                && !minecraft.options.reducedDebugInfo().get()) return;

        var partialTick = deltaTracker.getGameTimeDeltaPartialTick(true);
        var left = PsychicDriveGauntletsClientState.hud(player.getUUID(), false, partialTick);
        var right = PsychicDriveGauntletsClientState.hud(player.getUUID(), true, partialTick);
        int center = graphics.guiWidth() / 2;
        int y = graphics.guiHeight() / 2 - SIZE / 2;
        graphics.flush();
        graphics.setColor(1, 1, 1, 1);
        RenderSystem.enableBlend();
        try {
            // バニラのレティクル下の攻撃ゲージと同じ背景依存の合成を使う。
            RenderSystem.blendFuncSeparate(SourceFactor.ONE_MINUS_DST_COLOR, DestFactor.ONE_MINUS_SRC_COLOR,
                    SourceFactor.ONE, DestFactor.ZERO);
            normal(graphics, LEFT_BACK, LEFT, center - 28, y, left);
            normal(graphics, RIGHT_BACK, RIGHT, center + 12, y, right);
            graphics.flush();

            // 反転系合成で着色すると背景次第で赤以外になるため、溜めだけ通常合成へ切り替える。
            RenderSystem.defaultBlendFunc();
            graphics.setColor(1, 0, 0, 1);
            fill(graphics, LEFT, center - 28, y, left.charge());
            fill(graphics, RIGHT, center + 12, y, right.charge());
            graphics.flush();
        } finally {
            graphics.setColor(1, 1, 1, 1);
            RenderSystem.defaultBlendFunc();
            RenderSystem.disableBlend();
        }
    }

    private static void normal(GuiGraphics graphics, ResourceLocation background, ResourceLocation foreground,
                               int x, int y, Gauge gauge) {
        graphics.blit(background, x, y, 0, 0, SIZE, SIZE, SIZE, SIZE);
        fill(graphics, foreground, x, y, gauge.recovery());
    }

    private static void fill(GuiGraphics graphics, ResourceLocation texture, int x, int y, float amount) {
        int height = Mth.floor(Mth.clamp(amount, 0, 1) * FILL_HEIGHT);
        if (height == 0) return;
        int top = FILL_BOTTOM - height;
        graphics.blit(texture, x, y + top, 0, top, SIZE, height, SIZE, SIZE);
    }

    private static ResourceLocation texture(String suffix) {
        return ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID,
                "textures/gui/psychic_drive_gauntlets_indicator_" + suffix + ".png");
    }
}
