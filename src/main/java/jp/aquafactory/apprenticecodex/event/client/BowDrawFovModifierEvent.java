package jp.aquafactory.apprenticecodex.event.client;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.item.BoundBowItem;
import jp.aquafactory.apprenticecodex.item.ElementalBow;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.Item;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ComputeFovModifierEvent;

@EventBusSubscriber(modid = ApprenticeCodex.MODID, value = Dist.CLIENT)
public final class BowDrawFovModifierEvent {
    private BowDrawFovModifierEvent() {
    }

    @SubscribeEvent
    public static void onComputeFovModifier(ComputeFovModifierEvent event) {
        var player = event.getPlayer();
        if (!player.isUsingItem() || !shouldApplyVanillaBowDrawFov(player.getUseItem().getItem())) {
            return;
        }

        // 1.20.1 の弓 FOV 縮小は BowItem 継承ではなく Items.BOW 固定判定なので、
        // バニラ弓相当のカスタム弓だけ client event で同じ補正式を補う。
        float adjustedFovModifier = event.getFovModifier() * resolveBowDrawFovModifier(player.getTicksUsingItem());
        float fovEffectScale = Minecraft.getInstance().options.fovEffectScale().get().floatValue();
        event.setNewFovModifier(Mth.lerp(fovEffectScale, 1.0F, adjustedFovModifier));
    }

    private static boolean shouldApplyVanillaBowDrawFov(Item item) {
        return item instanceof ElementalBow || item instanceof BoundBowItem;
    }

    private static float resolveBowDrawFovModifier(int ticksUsingItem) {
        float progress = (float) ticksUsingItem / BowItem.MAX_DRAW_DURATION;
        if (progress > 1.0F) {
            progress = 1.0F;
        } else {
            progress *= progress;
        }

        return 1.0F - progress * 0.15F;
    }
}
