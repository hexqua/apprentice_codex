package jp.aquafactory.apprenticecodex.block.atelierstation;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import jp.aquafactory.apprenticecodex.renderer.ApprenticeRenderTypes;
import jp.aquafactory.apprenticecodex.item.flask.SpellcastersFlask;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;

public final class AtelierStationBlockEntityRenderer implements BlockEntityRenderer<AtelierStationBlockEntity> {
    private static final double MAX_RENDER_DISTANCE = 48.0D;
    private static final double MAX_RENDER_DISTANCE_SQR = MAX_RENDER_DISTANCE * MAX_RENDER_DISTANCE;
    private static final double OUTER_CUBE_MAX_DISTANCE = 16.0D;
    private static final double OUTER_CUBE_MAX_DISTANCE_SQR = OUTER_CUBE_MAX_DISTANCE * OUTER_CUBE_MAX_DISTANCE;
    private static final float TABLE_TOP_Y = 10.0F / 16.0F;
    private static final float FLASK_ROW_Z = 5.0F / 16.0F;
    private static final float FLASK_CENTER_Y = TABLE_TOP_Y + (2.2F / 16.0F);
    private static final float FLASK_SCALE = 0.34F;
    private static final float[] FLASK_SLOT_X = {
            3.0F / 16.0F,
            5.5F / 16.0F,
            8.0F / 16.0F,
            10.5F / 16.0F,
            13.0F / 16.0F
    };
    private static final float[] FLASK_SLOT_Z_OFFSET = {
            0.6F / 16.0F,
            -0.6F / 16.0F,
            0.6F / 16.0F,
            -0.6F / 16.0F,
            0.6F / 16.0F
    };
    private static final float[] FLASK_Y_ROTATION = {-18.0F, -9.0F, 0.0F, 9.0F, 18.0F};
    private static final float CUBE_MAX_DIAMETER = 4.0F / 16.0F;
    private static final float CUBE_MIN_DIAMETER = CUBE_MAX_DIAMETER * 0.5F;
    private static final float CUBE_FLOAT_HEIGHT = 2.5F / 16.0F;
    private static final float SUPPORT_MIN_X = 10.0F / 16.0F;
    private static final float SUPPORT_MAX_X = 15.0F / 16.0F;
    private static final float SUPPORT_MIN_Z = 10.0F / 16.0F;
    private static final float SUPPORT_MAX_Z = 15.0F / 16.0F;
    private static final float SUPPORT_TOP_Y = 12.0F / 16.0F;
    private static final float INNER_CUBE_SCALE = 0.62F;
    private static final float COLOR_TRANSITION_TICKS = 40.0F;
    private static final RenderType CUBE_RENDER_TYPE =
            ApprenticeRenderTypes.color("atelier_station_tank_cube");

    public AtelierStationBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
        // do nothing.
    }

    @Override
    public void render(@NotNull AtelierStationBlockEntity blockEntity, float partialTick, @NotNull PoseStack poseStack,
                       @NotNull MultiBufferSource buffer, int packedLight, int packedOverlay) {
        var level = blockEntity.getLevel();
        if (level == null) {
            return;
        }

        var cameraPos = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
        var distanceSqr = cameraPos.distanceToSqr(getRenderCenter(blockEntity));

        poseStack.pushPose();
        applyBlockRotation(poseStack, blockEntity);

        renderFlasks(blockEntity, poseStack, buffer, packedLight);
        renderTankCube(blockEntity, partialTick, poseStack, buffer, distanceSqr);

        poseStack.popPose();
    }

    @Override
    public boolean shouldRender(@NotNull AtelierStationBlockEntity blockEntity, @NotNull Vec3 cameraPos) {
        return cameraPos.distanceToSqr(getRenderCenter(blockEntity)) <= MAX_RENDER_DISTANCE_SQR;
    }

    private static void applyBlockRotation(PoseStack poseStack, AtelierStationBlockEntity blockEntity) {
        var state = blockEntity.getBlockState();
        if (!state.hasProperty(AtelierStation.FACING)) {
            return;
        }

        poseStack.translate(0.5F, 0.0F, 0.5F);
        // 演出側の localToWorld と同じ見た目になるよう、PoseStack の回転系では東西を逆符号で補正する。
        poseStack.mulPose(Axis.YP.rotationDegrees(switch (state.getValue(AtelierStation.FACING)) {
            case NORTH -> 0.0F;
            case EAST -> 270.0F;
            case SOUTH -> 180.0F;
            case WEST -> 90.0F;
            default -> 0.0F;
        }));
        poseStack.translate(-0.5F, 0.0F, -0.5F);
    }

    private static Vec3 getRenderCenter(AtelierStationBlockEntity blockEntity) {
        return Vec3.atCenterOf(blockEntity.getBlockPos()).add(0.0D, 0.25D, 0.0D);
    }

    private static void renderFlasks(AtelierStationBlockEntity blockEntity, PoseStack poseStack,
                                     MultiBufferSource buffer, int packedLight) {
        var level = blockEntity.getLevel();
        if (level == null) {
            return;
        }

        var itemRenderer = Minecraft.getInstance().getItemRenderer();
        var inventory = blockEntity.getFlaskInventory();
        var maxSlots = Math.min(inventory.getSlots(), FLASK_SLOT_X.length);

        for (var slot = 0; slot < maxSlots; slot++) {
            var stack = inventory.getStackInSlot(maxSlots - 1 - slot);
            if (stack.isEmpty()) {
                continue;
            }

            poseStack.pushPose();
            poseStack.translate(FLASK_SLOT_X[slot], FLASK_CENTER_Y, FLASK_ROW_Z + FLASK_SLOT_Z_OFFSET[slot]);
            poseStack.mulPose(Axis.YP.rotationDegrees(FLASK_Y_ROTATION[slot]));
            poseStack.scale(FLASK_SCALE, FLASK_SCALE, FLASK_SCALE);

            itemRenderer.renderStatic(
                    stack,
                    ItemDisplayContext.FIXED,
                    packedLight,
                    OverlayTexture.NO_OVERLAY,
                    poseStack,
                    buffer,
                    level,
                    getRenderSeed(stack, slot)
            );

            poseStack.popPose();
        }
    }

    private static void renderTankCube(AtelierStationBlockEntity blockEntity, float partialTick, PoseStack poseStack,
                                       MultiBufferSource buffer, double distanceSqr) {
        if (blockEntity.getStoredFluidAmount() <= 0) {
            return;
        }

        var colors = collectTankColors(blockEntity);
        if (colors.isEmpty()) {
            return;
        }

        var fillRatio = Mth.clamp(
                blockEntity.getStoredFluidAmount() / (float) AtelierStationBlockEntity.MAX_STORED_FLUID_AMOUNT,
                0.0F,
                1.0F
        );
        var cubeDiameter = Mth.lerp(fillRatio, CUBE_MIN_DIAMETER, CUBE_MAX_DIAMETER);
        var cubeHalf = cubeDiameter * 0.5F;
        var centerX = (SUPPORT_MIN_X + SUPPORT_MAX_X) * 0.5F;
        var centerY = SUPPORT_TOP_Y + CUBE_FLOAT_HEIGHT + cubeHalf;
        var centerZ = (SUPPORT_MIN_Z + SUPPORT_MAX_Z) * 0.5F;

        var color = resolveCurrentTankColor(blockEntity, partialTick, colors);
        var level = blockEntity.getLevel();
        if (level == null) {
            return;
        }
        var time = level.getGameTime() + partialTick + (blockEntity.getBlockPos().asLong() & 31L);
        var outerRed = (color >> 16) & 0xFF;
        var outerGreen = (color >> 8) & 0xFF;
        var outerBlue = color & 0xFF;

        poseStack.pushPose();
        poseStack.translate(centerX, centerY, centerZ);
        poseStack.mulPose(Axis.XP.rotationDegrees(time * 0.55F));
        poseStack.mulPose(Axis.YP.rotationDegrees(time * 0.85F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(time * 0.70F));

        drawCube(poseStack, buffer.getBuffer(CUBE_RENDER_TYPE), cubeDiameter * INNER_CUBE_SCALE,
                255, 255, 255, 255, false);
        if (distanceSqr <= OUTER_CUBE_MAX_DISTANCE_SQR) {
            drawCube(poseStack, buffer.getBuffer(CUBE_RENDER_TYPE), cubeDiameter,
                    outerRed, outerGreen, outerBlue, 255, true);
        }

        poseStack.popPose();
    }

    private static List<Integer> collectTankColors(AtelierStationBlockEntity blockEntity) {
        var colors = new ArrayList<Integer>();
        for (var entry : blockEntity.getStoredFluidsForDisplay()) {
            if (entry.amountMb() <= 0 || entry.representativeItem().isEmpty()) {
                continue;
            }

            colors.add(SpellcastersFlask.getStoredItemTintColorForDisplay(entry.representativeItem()) & 0x00FFFFFF);
        }
        return colors;
    }

    private static int resolveCurrentTankColor(AtelierStationBlockEntity blockEntity, float partialTick, List<Integer> colors) {
        if (colors.size() == 1) {
            return colors.get(0);
        }

        var level = blockEntity.getLevel();
        if (level == null) {
            return colors.get(0);
        }

        var cycleTime = (level.getGameTime() + partialTick + (blockEntity.getBlockPos().asLong() & 31L)) / COLOR_TRANSITION_TICKS;
        var currentIndex = Mth.floor(cycleTime) % colors.size();
        var nextIndex = (currentIndex + 1) % colors.size();
        var blend = cycleTime - Mth.floor(cycleTime);
        return lerpColor(colors.get(currentIndex), colors.get(nextIndex), blend);
    }

    private static int lerpColor(int fromColor, int toColor, float progress) {
        var red = toChannel(Mth.lerp(progress, ((fromColor >> 16) & 0xFF) / 255.0F, ((toColor >> 16) & 0xFF) / 255.0F));
        var green = toChannel(Mth.lerp(progress, ((fromColor >> 8) & 0xFF) / 255.0F, ((toColor >> 8) & 0xFF) / 255.0F));
        var blue = toChannel(Mth.lerp(progress, (fromColor & 0xFF) / 255.0F, (toColor & 0xFF) / 255.0F));
        return (red << 16) | (green << 8) | blue;
    }

    private static int toChannel(float value) {
        return Mth.clamp((int) (value * 255.0F), 0, 255);
    }

    private static void drawCube(PoseStack poseStack, VertexConsumer consumer, float size,
                                 int red, int green, int blue, int alpha, boolean reverse) {
        var half = size * 0.5F;
        var pose = poseStack.last().pose();

        quad(consumer, pose, -half, -half, half, half, -half, half, half, half, half, -half, half, half,
                red, green, blue, alpha, reverse);
        quad(consumer, pose, half, -half, -half, -half, -half, -half, -half, half, -half, half, half, -half,
                red, green, blue, alpha, reverse);
        quad(consumer, pose, -half, -half, -half, -half, -half, half, -half, half, half, -half, half, -half,
                red, green, blue, alpha, reverse);
        quad(consumer, pose, half, -half, half, half, -half, -half, half, half, -half, half, half, half,
                red, green, blue, alpha, reverse);
        quad(consumer, pose, -half, half, half, half, half, half, half, half, -half, -half, half, -half,
                red, green, blue, alpha, reverse);
        quad(consumer, pose, -half, -half, -half, half, -half, -half, half, -half, half, -half, -half, half,
                red, green, blue, alpha, reverse);
    }

    private static void quad(VertexConsumer consumer, Matrix4f pose,
                             float x1, float y1, float z1,
                             float x2, float y2, float z2,
                             float x3, float y3, float z3,
                             float x4, float y4, float z4,
                             int red, int green, int blue, int alpha, boolean reverse) {
        if (reverse) {
            vertex(consumer, pose, x4, y4, z4, red, green, blue, alpha);
            vertex(consumer, pose, x3, y3, z3, red, green, blue, alpha);
            vertex(consumer, pose, x2, y2, z2, red, green, blue, alpha);
            vertex(consumer, pose, x1, y1, z1, red, green, blue, alpha);
            return;
        }

        vertex(consumer, pose, x1, y1, z1, red, green, blue, alpha);
        vertex(consumer, pose, x2, y2, z2, red, green, blue, alpha);
        vertex(consumer, pose, x3, y3, z3, red, green, blue, alpha);
        vertex(consumer, pose, x4, y4, z4, red, green, blue, alpha);
    }

    private static void vertex(VertexConsumer consumer, Matrix4f pose, float x, float y, float z,
                               int red, int green, int blue, int alpha) {
        consumer.addVertex(pose, x, y, z).setColor(red, green, blue, alpha);
    }

    private static int getRenderSeed(ItemStack stack, int salt) {
        return (net.minecraft.world.item.Item.getId(stack.getItem()) * 37)
                + (stack.getDamageValue() * 17)
                + (salt * 31);
    }
}
