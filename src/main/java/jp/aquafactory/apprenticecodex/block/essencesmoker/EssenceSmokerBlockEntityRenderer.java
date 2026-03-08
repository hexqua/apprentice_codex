package jp.aquafactory.apprenticecodex.block.essencesmoker;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

public class EssenceSmokerBlockEntityRenderer implements BlockEntityRenderer<EssenceSmokerBlockEntity> {
    private static final float CATALYST_SCALE = 0.5f;
    private static final float CATALYST_CENTER_X = 8.0f / 16.0f;
    private static final float CATALYST_CENTER_Y = 5.1f / 16.0f;
    private static final float CATALYST_CENTER_Z = 8.0f / 16.0f;
    private static final float MATERIAL_SCALE = 0.35f;
    private static final float MATERIAL_CENTER_Y = 9.5f / 16.0f;
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

        poseStack.pushPose();
        applyBlockRotation(poseStack, blockEntity);

        renderCatalyst(blockEntity, partialTick, poseStack, buffer, packedLight);
        renderMaterials(blockEntity, poseStack, buffer, packedLight);

        poseStack.popPose();
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
        if (stack.getTag() == null){
            return 0;
        }

        return (net.minecraft.world.item.Item.getId(stack.getItem()) * 37)
                + (stack.getDamageValue() * 17)
                + (stack.hasTag() ? stack.getTag().hashCode() : 0)
                + (salt * 31);
    }

    private record MaterialSlot(float x, float y, float z, float yRotDeg) {
    }
}
