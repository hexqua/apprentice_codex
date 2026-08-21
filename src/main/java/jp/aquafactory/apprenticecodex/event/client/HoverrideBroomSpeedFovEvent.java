package jp.aquafactory.apprenticecodex.event.client;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.entity.broom.HoverrideBroomEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ComputeFovModifierEvent;

@EventBusSubscriber(modid = ApprenticeCodex.MODID, value = Dist.CLIENT)
public final class HoverrideBroomSpeedFovEvent {
    private static final float MAXIMUM_FOV_MULTIPLIER = 1.10F;

    private HoverrideBroomSpeedFovEvent() {
    }

    @SubscribeEvent
    public static void onComputeFovModifier(ComputeFovModifierEvent event) {
        if (!(event.getPlayer().getVehicle() instanceof HoverrideBroomEntity broom)) {
            return;
        }

        var speedMultiplier = Mth.lerp(broom.getSpeedEffectIntensity(), 1.0F, MAXIMUM_FOV_MULTIPLIER);
        var adjusted = event.getFovModifier() * speedMultiplier;
        var fovEffectScale = Minecraft.getInstance().options.fovEffectScale().get().floatValue();
        // バニラの「FOV の変化」設定を尊重し、無効時はカメラ演出を追加しない。
        event.setNewFovModifier(Mth.lerp(fovEffectScale, event.getFovModifier(), adjusted));
    }
}
