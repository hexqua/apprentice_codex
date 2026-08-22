package jp.aquafactory.apprenticecodex.block.alchemybrewer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import jp.aquafactory.apprenticecodex.client.render.ColorCubeRenderTools;
import jp.aquafactory.apprenticecodex.item.flask.SpellcastersFlask;
import jp.aquafactory.apprenticecodex.renderer.ApprenticeRenderTypes;
import jp.aquafactory.apprenticecodex.utility.PotionContentsHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

public final class AlchemyBrewerBlockEntityRenderer implements BlockEntityRenderer<AlchemyBrewerBlockEntity> {
    private static final double MAX_RENDER_DISTANCE = 48.0D;
    private static final double MAX_RENDER_DISTANCE_SQR = MAX_RENDER_DISTANCE * MAX_RENDER_DISTANCE;
    private static final double OUTER_CUBE_MAX_DISTANCE = 16.0D;
    private static final double OUTER_CUBE_MAX_DISTANCE_SQR = OUTER_CUBE_MAX_DISTANCE * OUTER_CUBE_MAX_DISTANCE;
    private static final float CUBE_CENTER_X = 12.0F / 16.0F;
    private static final float CUBE_CENTER_Z = 4.0F / 16.0F;
    private static final float SUPPORT_TOP_Y = 11.0F / 16.0F;
    private static final float CUBE_MAX_DIAMETER = 4.0F / 16.0F;
    private static final float CUBE_MIN_DIAMETER = CUBE_MAX_DIAMETER * 0.5F;
    private static final float CUBE_FLOAT_HEIGHT = 2.5F / 16.0F;
    private static final float INNER_CUBE_SCALE = 0.62F;
    private static final RenderType CUBE_RENDER_TYPE =
            ApprenticeRenderTypes.color("alchemy_brewer_tank_cube");

    public AlchemyBrewerBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
        // do nothing.
    }

    @Override
    public void render(@NotNull AlchemyBrewerBlockEntity blockEntity, float partialTick,
                       @NotNull PoseStack poseStack, @NotNull MultiBufferSource buffer,
                       int packedLight, int packedOverlay) {
        var level = blockEntity.getLevel();
        var potionId = blockEntity.getTankPotionId();
        if (level == null || potionId == null || blockEntity.getTankAmountMb() <= 0) {
            return;
        }

        var potion = ForgeRegistries.POTIONS.getValue(potionId);
        if (potion == null) {
            return;
        }

        var representative = PotionContentsHelper.createPotionStack(Items.POTION, potion);
        if (representative.isEmpty()) {
            return;
        }

        var cameraPos = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
        var distanceSqr = cameraPos.distanceToSqr(getRenderCenter(blockEntity));
        var fillRatio = Mth.clamp(
                blockEntity.getTankAmountMb() / (float) AlchemyBrewerBlockEntity.TANK_CAPACITY_MB,
                0.0F,
                1.0F
        );
        var cubeDiameter = Mth.lerp(fillRatio, CUBE_MIN_DIAMETER, CUBE_MAX_DIAMETER);
        var color = SpellcastersFlask.getStoredItemTintColorForDisplay(representative);
        var time = level.getGameTime() + partialTick + (blockEntity.getBlockPos().asLong() & 31L);

        poseStack.pushPose();
        applyBlockRotation(poseStack, blockEntity);
        poseStack.translate(CUBE_CENTER_X, SUPPORT_TOP_Y + CUBE_FLOAT_HEIGHT + cubeDiameter * 0.5F, CUBE_CENTER_Z);
        poseStack.mulPose(Axis.XP.rotationDegrees(time * 0.55F));
        poseStack.mulPose(Axis.YP.rotationDegrees(time * 0.85F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(time * 0.70F));

        ColorCubeRenderTools.drawCube(poseStack, buffer.getBuffer(CUBE_RENDER_TYPE),
                cubeDiameter * INNER_CUBE_SCALE, 255, 255, 255, 255, false);
        if (distanceSqr <= OUTER_CUBE_MAX_DISTANCE_SQR) {
            ColorCubeRenderTools.drawCube(poseStack, buffer.getBuffer(CUBE_RENDER_TYPE), cubeDiameter,
                    (color >> 16) & 0xFF, (color >> 8) & 0xFF, color & 0xFF, 255, true);
        }

        poseStack.popPose();
    }

    @Override
    public boolean shouldRender(@NotNull AlchemyBrewerBlockEntity blockEntity, @NotNull Vec3 cameraPos) {
        return cameraPos.distanceToSqr(getRenderCenter(blockEntity)) <= MAX_RENDER_DISTANCE_SQR;
    }

    private static void applyBlockRotation(PoseStack poseStack, AlchemyBrewerBlockEntity blockEntity) {
        var state = blockEntity.getBlockState();
        if (!state.hasProperty(AlchemyBrewer.FACING)) {
            return;
        }

        poseStack.translate(0.5F, 0.0F, 0.5F);
        // 演出側のlocalToWorldと同じ見た目になるよう、PoseStackの回転系では東西を逆符号で補正する。
        poseStack.mulPose(Axis.YP.rotationDegrees(switch (state.getValue(AlchemyBrewer.FACING)) {
            case NORTH -> 0.0F;
            case EAST -> 270.0F;
            case SOUTH -> 180.0F;
            case WEST -> 90.0F;
            default -> 0.0F;
        }));
        poseStack.translate(-0.5F, 0.0F, -0.5F);
    }

    private static Vec3 getRenderCenter(AlchemyBrewerBlockEntity blockEntity) {
        return Vec3.atCenterOf(blockEntity.getBlockPos()).add(0.0D, 0.25D, 0.0D);
    }
}
