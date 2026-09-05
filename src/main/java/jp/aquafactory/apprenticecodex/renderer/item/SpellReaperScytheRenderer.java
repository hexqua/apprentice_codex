package jp.aquafactory.apprenticecodex.renderer.item;

import jp.aquafactory.apprenticecodex.item.spellreaperscythe.SpellReaperScythe;
import jp.aquafactory.apprenticecodex.model.SpellReaperScytheModel;
import software.bernie.geckolib.renderer.GeoItemRenderer;

public final class SpellReaperScytheRenderer extends GeoItemRenderer<SpellReaperScythe> {
    public SpellReaperScytheRenderer() {
        super(new SpellReaperScytheModel<>());
    }

    @Override
    public void renderByItem(net.minecraft.world.item.ItemStack stack, net.minecraft.world.item.ItemDisplayContext context,
                             com.mojang.blaze3d.vertex.PoseStack pose,
                             net.minecraft.client.renderer.MultiBufferSource buffers, int light, int overlay) {
        if ((context.firstPerson() || context == net.minecraft.world.item.ItemDisplayContext.THIRD_PERSON_LEFT_HAND
                || context == net.minecraft.world.item.ItemDisplayContext.THIRD_PERSON_RIGHT_HAND)
                && jp.aquafactory.apprenticecodex.item.spellreaperscythe.ScytheThrowManager.isThrown(stack)) return;
        super.renderByItem(stack, context, pose, buffers, light, overlay);
    }
}
