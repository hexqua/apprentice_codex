package jp.aquafactory.apprenticecodex.event.client;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.entity.broom.AbstractBroomEntity;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;

public final class BroomDurabilityHud implements LayeredDraw.Layer {
    public static final BroomDurabilityHud INSTANCE = new BroomDurabilityHud();

    private static final ResourceLocation CONTAINER = texture("broom_durability_container.png");
    private static final ResourceLocation FULL = texture("broom_durability_full.png");
    private static final ResourceLocation HALF = texture("broom_durability_half.png");
    private static final ResourceLocation BLINKING_OVERLAY = texture("broom_durability_blinking_overlay.png");
    private static final int ICON_COUNT = 10;
    private static final int ICON_SIZE = 9;
    private static final int ICON_SPACING = 8;
    private static final int BLINK_DURATION_TICKS = 20;
    private static final int BLINK_INTERVAL_TICKS = 3;

    private final long[] blinkUntilTicks = new long[ICON_COUNT];
    private int lastBroomId = -1;
    private int lastDurabilitySteps = -1;

    private BroomDurabilityHud() {
    }

    @Override
    public void render(@NotNull GuiGraphics guiGraphics, @NotNull DeltaTracker deltaTracker) {
        var minecraft = Minecraft.getInstance();
        var player = minecraft.player;
        if (minecraft.options.hideGui || player == null || player.isSpectator()
                || !(player.getVehicle() instanceof AbstractBroomEntity broom)) {
            resetTracking();
            return;
        }

        var durabilitySteps = broom.getRemainingDurabilitySteps();
        updateBlinkingState(broom.getId(), durabilitySteps, player.tickCount);

        var right = guiGraphics.guiWidth() / 2 + 91;
        var y = guiGraphics.guiHeight() - minecraft.gui.rightHeight;
        for (var icon = 0; icon < ICON_COUNT; icon++) {
            var x = right - icon * ICON_SPACING - ICON_SIZE;
            blit(guiGraphics, CONTAINER, x, y);
            var iconSteps = durabilitySteps - icon * 2;
            if (iconSteps >= 2) {
                blit(guiGraphics, FULL, x, y);
            } else if (iconSteps == 1) {
                blit(guiGraphics, HALF, x, y);
            }
            if (isBlinkVisible(icon, player.tickCount)) {
                blit(guiGraphics, BLINKING_OVERLAY, x, y);
            }
        }
        // AIR_LEVELより先に右側の高さを予約し、満腹度と酸素の間へ配置する。
        minecraft.gui.rightHeight += 10;
    }

    private void updateBlinkingState(int broomId, int durabilitySteps, long now) {
        if (broomId != lastBroomId) {
            lastBroomId = broomId;
            lastDurabilitySteps = durabilitySteps;
            Arrays.fill(blinkUntilTicks, 0L);
            return;
        }
        if (durabilitySteps == lastDurabilitySteps) {
            return;
        }

        var start = Math.min(lastDurabilitySteps, durabilitySteps);
        var end = Math.max(lastDurabilitySteps, durabilitySteps);
        for (var step = start; step < end; step++) {
            blinkUntilTicks[Math.min(step / 2, ICON_COUNT - 1)] = now + BLINK_DURATION_TICKS;
        }
        lastDurabilitySteps = durabilitySteps;
    }

    private boolean isBlinkVisible(int icon, long now) {
        var remaining = blinkUntilTicks[icon] - now;
        return remaining > 0L && remaining / BLINK_INTERVAL_TICKS % 2L == 1L;
    }

    private void resetTracking() {
        lastBroomId = -1;
        lastDurabilitySteps = -1;
        Arrays.fill(blinkUntilTicks, 0L);
    }

    private static void blit(GuiGraphics guiGraphics, ResourceLocation texture, int x, int y) {
        guiGraphics.blit(texture, x, y, 0.0F, 0.0F, ICON_SIZE, ICON_SIZE, ICON_SIZE, ICON_SIZE);
    }

    private static ResourceLocation texture(String name) {
        return ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "textures/gui/" + name);
    }
}
