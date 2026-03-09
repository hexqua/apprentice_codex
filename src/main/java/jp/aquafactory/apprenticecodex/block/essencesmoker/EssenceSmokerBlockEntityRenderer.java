package jp.aquafactory.apprenticecodex.block.essencesmoker;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3f;

public class EssenceSmokerBlockEntityRenderer implements BlockEntityRenderer<EssenceSmokerBlockEntity> {
    private static final float CATALYST_SCALE = 0.5f;
    private static final float CATALYST_CENTER_X = 8.0f / 16.0f;
    private static final float CATALYST_CENTER_Y = 5.1f / 16.0f;
    private static final float CATALYST_CENTER_Z = 8.0f / 16.0f;
    private static final float MATERIAL_SCALE = 0.35f;
    private static final float MATERIAL_CENTER_Y = 9.5f / 16.0f;
    private static final float COLORED_DUST_SCALE = 0.85f;
    private static final double COLORED_PARTICLE_MIN_RADIUS = 0.55d;
    private static final double COLORED_PARTICLE_MAX_RADIUS = 1.0d;
    private static final double COLORED_PARTICLE_MIN_Y = 0.35d;
    private static final double COLORED_PARTICLE_Y_RANGE = 0.45d;
    private static final double COLORED_PARTICLE_MAX_DISTANCE = 18.0d;
    private static final double COLORED_PARTICLE_MAX_DISTANCE_SQR = COLORED_PARTICLE_MAX_DISTANCE * COLORED_PARTICLE_MAX_DISTANCE;
    private static final MaterialSlot[] MATERIAL_SLOTS = {
            // 現行モデルの chain1..4 板ポリ位置に寄せた吊り下げ座標. 1本につき2個まで表示する.
            new MaterialSlot(5.5f / 16.0f, MATERIAL_CENTER_Y, 2.6f / 16.0f, 0.0f),
            new MaterialSlot(10.5f / 16.0f, MATERIAL_CENTER_Y, 2.6f / 16.0f, 0.0f),
            new MaterialSlot(2.6f / 16.0f, MATERIAL_CENTER_Y, 5.5f / 16.0f, 90.0f),
            new MaterialSlot(2.6f / 16.0f, MATERIAL_CENTER_Y, 10.5f / 16.0f, 90.0f),
            new MaterialSlot(5.5f / 16.0f, MATERIAL_CENTER_Y, 13.4f / 16.0f, 0.0f),
            new MaterialSlot(10.5f / 16.0f, MATERIAL_CENTER_Y, 13.4f / 16.0f, 0.0f),
            new MaterialSlot(13.4f / 16.0f, MATERIAL_CENTER_Y, 5.5f / 16.0f, 90.0f),
            new MaterialSlot(13.4f / 16.0f, MATERIAL_CENTER_Y, 10.5f / 16.0f, 90.0f)
    };

    public EssenceSmokerBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
        // do nothing.
    }

    @Override
    public void render(@NotNull EssenceSmokerBlockEntity blockEntity, float partialTick, @NotNull PoseStack poseStack,
                       @NotNull MultiBufferSource buffer, int packedLight, int packedOverlay) {
        var level = blockEntity.getLevel();
        if (level == null) {
            return;
        }

        spawnColoredProcessingParticle(blockEntity);

        poseStack.pushPose();
        applyBlockRotation(poseStack, blockEntity);

        renderCatalyst(blockEntity, partialTick, poseStack, buffer, packedLight);
        renderMaterials(blockEntity, poseStack, buffer, packedLight);

        poseStack.popPose();
    }

    private static void spawnColoredProcessingParticle(EssenceSmokerBlockEntity blockEntity) {
        if (!blockEntity.isProcessing()) {
            return;
        }

        var level = blockEntity.getLevel();
        if (level == null) {
            return;
        }

        var catalyst = blockEntity.getCatalyst();
        if (catalyst.isEmpty()) {
            return;
        }

        var cameraPos = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
        var center = Vec3.atCenterOf(blockEntity.getBlockPos());
        if (cameraPos.distanceToSqr(center) > COLORED_PARTICLE_MAX_DISTANCE_SQR) {
            return;
        }

        var gameTime = level.getGameTime();
        if (!blockEntity.markColoredParticleGameTime(gameTime)) {
            return;
        }

        var random = level.getRandom();
        var angle = random.nextDouble() * (Math.PI * 2.0d);
        var radius = Mth.lerp(random.nextDouble(), COLORED_PARTICLE_MIN_RADIUS, COLORED_PARTICLE_MAX_RADIUS);
        var x = center.x + Math.cos(angle) * radius;
        var y = blockEntity.getBlockPos().getY() + COLORED_PARTICLE_MIN_Y + random.nextDouble() * COLORED_PARTICLE_Y_RANGE;
        var z = center.z + Math.sin(angle) * radius;
        var rgb = EssenceSmokerParticlePaletteCache.pickColor(catalyst, level, random);
        var particle = new DustParticleOptions(toVectorColor(rgb), COLORED_DUST_SCALE + random.nextFloat() * 0.2f);
        var motionX = (random.nextDouble() - 0.5d) * 0.01d;
        var motionY = 0.01d + random.nextDouble() * 0.02d;
        var motionZ = (random.nextDouble() - 0.5d) * 0.01d;

        level.addParticle(particle, x, y, z, motionX, motionY, motionZ);
    }

    private static void applyBlockRotation(PoseStack poseStack, EssenceSmokerBlockEntity blockEntity) {
        var state = blockEntity.getBlockState();
        if (!state.hasProperty(EssenceSmoker.FACING)) {
            return;
        }

        poseStack.translate(0.5f, 0.0f, 0.5f);
        poseStack.mulPose(Axis.YP.rotationDegrees(switch (state.getValue(EssenceSmoker.FACING)) {
            case NORTH -> 0.0f;
            case EAST -> 90.0f;
            case SOUTH -> 180.0f;
            case WEST -> 270.0f;
            default -> 0.0f;
        }));
        poseStack.translate(-0.5f, 0.0f, -0.5f);
    }

    private static void renderCatalyst(EssenceSmokerBlockEntity blockEntity, float partialTick, PoseStack poseStack,
                                       MultiBufferSource buffer, int packedLight) {
        var catalyst = blockEntity.getCatalyst();
        if (catalyst.isEmpty()) {
            return;
        }

        var level = blockEntity.getLevel();
        if (level == null) {
            return;
        }

        var time = level.getGameTime() + partialTick;
        var bobbing = Mth.sin(time * 0.08f) * (0.35f / 16.0f);

        poseStack.pushPose();
        poseStack.translate(CATALYST_CENTER_X, CATALYST_CENTER_Y + bobbing, CATALYST_CENTER_Z);
        poseStack.mulPose(Axis.YP.rotationDegrees(time * 2.0f));
        poseStack.scale(CATALYST_SCALE, CATALYST_SCALE, CATALYST_SCALE);

        Minecraft.getInstance().getItemRenderer().renderStatic(
                catalyst,
                ItemDisplayContext.GROUND,
                packedLight,
                OverlayTexture.NO_OVERLAY,
                poseStack,
                buffer,
                level,
                getRenderSeed(catalyst, 0)
        );

        poseStack.popPose();
    }

    private static void renderMaterials(EssenceSmokerBlockEntity blockEntity, PoseStack poseStack,
                                        MultiBufferSource buffer, int packedLight) {
        var level = blockEntity.getLevel();
        if (level == null) {
            return;
        }

        var materials = blockEntity.getMaterials();
        var itemRenderer = Minecraft.getInstance().getItemRenderer();
        var maxSlots = Math.min(materials.size(), MATERIAL_SLOTS.length);

        for (var slotIndex = 0; slotIndex < maxSlots; slotIndex++) {
            var stack = materials.get(slotIndex);
            if (stack.isEmpty()) {
                continue;
            }

            var slot = MATERIAL_SLOTS[slotIndex];
            poseStack.pushPose();
            poseStack.translate(slot.x(), slot.y(), slot.z());
            poseStack.mulPose(Axis.YP.rotationDegrees(slot.yRotDeg()));
            poseStack.scale(MATERIAL_SCALE, MATERIAL_SCALE, MATERIAL_SCALE);

            itemRenderer.renderStatic(
                    stack,
                    ItemDisplayContext.NONE,
                    packedLight,
                    OverlayTexture.NO_OVERLAY,
                    poseStack,
                    buffer,
                    level,
                    getRenderSeed(stack, slotIndex + 1)
            );

            poseStack.popPose();
        }
    }

    private static int getRenderSeed(ItemStack stack, int salt) {
        return (net.minecraft.world.item.Item.getId(stack.getItem()) * 37)
                + (stack.getDamageValue() * 17)
                + stack.toString().hashCode()
                + (salt * 31);
    }

    private static Vector3f toVectorColor(int rgb) {
        var red = ((rgb >> 16) & 0xFF) / 255.0f;
        var green = ((rgb >> 8) & 0xFF) / 255.0f;
        var blue = (rgb & 0xFF) / 255.0f;
        return new Vector3f(red, green, blue);
    }

    private record MaterialSlot(float x, float y, float z, float yRotDeg) {
    }
}
