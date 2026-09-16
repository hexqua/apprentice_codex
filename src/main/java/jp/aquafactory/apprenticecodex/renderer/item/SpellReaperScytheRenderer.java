package jp.aquafactory.apprenticecodex.renderer.item;

import com.mojang.blaze3d.vertex.PoseStack;
import jp.aquafactory.apprenticecodex.item.spellreaperscythe.ScytheThrowManager;
import jp.aquafactory.apprenticecodex.item.spellreaperscythe.SpellReaperScythe;
import jp.aquafactory.apprenticecodex.model.SpellReaperScytheModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import software.bernie.geckolib.renderer.GeoItemRenderer;

public final class SpellReaperScytheRenderer extends GeoItemRenderer<SpellReaperScythe> {
    public SpellReaperScytheRenderer() {
        super(new SpellReaperScytheModel<>());
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext context,
                             PoseStack pose,
                             MultiBufferSource buffers, int light, int overlay) {
        if ((context.firstPerson() || context == ItemDisplayContext.THIRD_PERSON_LEFT_HAND
                || context == ItemDisplayContext.THIRD_PERSON_RIGHT_HAND)
                && ScytheThrowManager.isThrown(stack)) return;
        super.renderByItem(stack, context, pose, buffers, light, overlay);
    }
}
