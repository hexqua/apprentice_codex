package jp.aquafactory.apprenticecodex.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import jp.aquafactory.apprenticecodex.config.ApprenticeCodexClientConfig;
import jp.aquafactory.apprenticecodex.item.ManaForceBlade;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.fml.ModList;
import org.jetbrains.annotations.NotNull;

@OnlyIn(Dist.CLIENT)
public class ManaForceBladeSheathLayer<T extends LivingEntity, M extends EntityModel<T>> extends RenderLayer<T, M> {
    private static final String EPICFIGHT_MOD_ID = "epicfight";
    private static final int HOTBAR_SIZE = 9;
    private static final float PIXEL = 1.0F / 16.0F;
    private static final float HIP_OFFSET_X = 4.4F * PIXEL;
    private static final float HIP_OFFSET_Y = 8.25F * PIXEL;
    private static final float HIP_OFFSET_Z = -0.5F * PIXEL;
    private static final float ARMORED_OFFSET_X = 0.35F * PIXEL;
    private static final float SHEATH_SCALE = 1.1F;
    private static final float STORED_BLADE_OFFSET_Y = -0.4F;
    private static final ItemDisplayContext BODY_DISPLAY_CONTEXT = ItemDisplayContext.THIRD_PERSON_LEFT_HAND;
    private static final float BODY_ROTATION_X = 80.0F;
    private static final float BODY_ROTATION_Y = 180.0F;
    private static final float BODY_ROTATION_Z = 10.0F;

    private final ItemRenderer itemRenderer = Minecraft.getInstance().getItemRenderer();
    private final ItemStack sheathStack = new ItemStack(ItemRegistry.MANA_FORCE_BLADE_SHEATH.get());

    public ManaForceBladeSheathLayer(RenderLayerParent<T, M> renderer) {
        super(renderer);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void render(@NotNull PoseStack poseStack, @NotNull MultiBufferSource buffer, int packedLight, @NotNull T entity,
                       float limbSwing, float limbSwingAmount, float partialTicks,
                       float ageInTicks, float netHeadYaw, float headPitch) {
        if (!(entity instanceof AbstractClientPlayer player)) {
            return;
        }
        if (!(this.getParentModel() instanceof HumanoidModel<?> rawHumanoidModel)) {
            return;
        }
        var bladeStack = findHotbarBlade(player);
        if (bladeStack.isEmpty() || !shouldRender(player)) {
            return;
        }

        var humanoidModel = (HumanoidModel<LivingEntity>) rawHumanoidModel;
        var armorOffset = player.getItemBySlot(EquipmentSlot.CHEST).isEmpty() ? 0.0F : ARMORED_OFFSET_X;
        var renderStoredBlade = !isHoldingManaForceBlade(player);

        poseStack.pushPose();
        humanoidModel.body.translateAndRotate(poseStack);
        // 非 EpicFight では腰固定の見た目だけを補う。EpicFight 導入時は item_skins の sheath 表示へ任せる。
        poseStack.translate(HIP_OFFSET_X + armorOffset, HIP_OFFSET_Y, HIP_OFFSET_Z);
        poseStack.mulPose(Axis.XP.rotationDegrees(BODY_ROTATION_X));
        poseStack.mulPose(Axis.YP.rotationDegrees(BODY_ROTATION_Y));
        poseStack.mulPose(Axis.ZP.rotationDegrees(BODY_ROTATION_Z));
        poseStack.scale(SHEATH_SCALE, SHEATH_SCALE, SHEATH_SCALE);

        itemRenderer.renderStatic(this.sheathStack, BODY_DISPLAY_CONTEXT, packedLight, OverlayTexture.NO_OVERLAY,
                poseStack, buffer, player.level(), player.getId());
        if (renderStoredBlade) {
            renderStoredBlade(bladeStack, poseStack, buffer, packedLight, player);
        }
        poseStack.popPose();
    }

    private void renderStoredBlade(ItemStack bladeStack, PoseStack poseStack, MultiBufferSource buffer, int packedLight,
                                   AbstractClientPlayer player) {
        poseStack.pushPose();
        // 鞘の回転軸がベースになっているため、Yで差し具合、Zは浮かせ具合になる.
        poseStack.translate(0.0F, STORED_BLADE_OFFSET_Y, 0.0F);
        itemRenderer.renderStatic(bladeStack, BODY_DISPLAY_CONTEXT, packedLight, OverlayTexture.NO_OVERLAY,
                poseStack, buffer, player.level(), player.getId());
        poseStack.popPose();
    }

    private static boolean shouldRender(AbstractClientPlayer player) {
        var minecraft = Minecraft.getInstance();
        return minecraft.player == player
                && ApprenticeCodexClientConfig.enableManaForceBladeHotbarSheathRendering()
                && !player.isInvisible()
                && !minecraft.options.getCameraType().isFirstPerson()
                && !ModList.get().isLoaded(EPICFIGHT_MOD_ID);
    }

    private static ItemStack findHotbarBlade(AbstractClientPlayer player) {
        var inventory = player.getInventory();
        for (var slot = 0; slot < HOTBAR_SIZE && slot < inventory.items.size(); slot++) {
            var stack = inventory.items.get(slot);
            if (ManaForceBlade.isManaForceBlade(stack)) {
                return stack;
            }
        }

        return ItemStack.EMPTY;
    }

    private static boolean isHoldingManaForceBlade(AbstractClientPlayer player) {
        return ManaForceBlade.isManaForceBlade(player.getMainHandItem())
                || ManaForceBlade.isManaForceBlade(player.getOffhandItem());
    }
}
