package jp.aquafactory.apprenticecodex.event.client;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.registry.SpellRegistry;
import jp.aquafactory.apprenticecodex.spell.fieldoverseer.FieldOverseerStaffEntity;
import jp.aquafactory.apprenticecodex.utility.SchoolAffinityRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

@EventBusSubscriber(modid = ApprenticeCodex.MODID, value = Dist.CLIENT)
public final class FieldOverseerRangeRenderEvent {
    private static final int SEGMENT_COUNT = 32;
    private static final float RING_HEIGHT = 0.18F;
    private static final float RING_ALPHA = 0.48F;
    private static final double SEARCH_DISTANCE = 128.0D;

    private FieldOverseerRangeRenderEvent() {
    }

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) {
            return;
        }

        var minecraft = Minecraft.getInstance();
        var level = minecraft.level;
        var player = minecraft.player;
        if (level == null || player == null) {
            return;
        }

        var staffs = level.getEntitiesOfClass(
                FieldOverseerStaffEntity.class,
                player.getBoundingBox().inflate(SEARCH_DISTANCE),
                staff -> staff.isOwnedBy(player)
        );
        if (staffs.isEmpty()) {
            return;
        }

        var poseStack = event.getPoseStack();
        var cameraPosition = event.getCamera().getPosition();
        var buffers = minecraft.renderBuffers().bufferSource();
        var color = SchoolAffinityRegistry.resolveColor(SpellRegistry.FIELD_OVERSEER.get().getSchoolType());

        poseStack.pushPose();
        poseStack.translate(-cameraPosition.x, -cameraPosition.y, -cameraPosition.z);
        for (var staff : staffs) {
            ClientPlacementPreviewRenderEvent.renderRing(
                    poseStack,
                    buffers,
                    new Vec3(staff.getX(), staff.getY() + 0.03D, staff.getZ()),
                    (float) staff.getTargetingRadius(),
                    RING_HEIGHT,
                    Direction.UP,
                    color,
                    SEGMENT_COUNT,
                    RING_ALPHA
            );
        }
        poseStack.popPose();

        ClientPlacementPreviewRenderEvent.endBatch(buffers);
    }
}
