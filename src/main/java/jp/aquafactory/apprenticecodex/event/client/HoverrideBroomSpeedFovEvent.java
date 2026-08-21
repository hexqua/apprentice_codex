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
        var currentFovModifier = event.getNewFovModifier();
        var adjusted = currentFovModifier * speedMultiplier;
        var fovEffectScale = Minecraft.getInstance().options.fovEffectScale().get().floatValue();
        // 設定補間と先行イベントの結果を維持し、その値へ箒の速度演出だけを合成する。
        event.setNewFovModifier(Mth.lerp(fovEffectScale, currentFovModifier, adjusted));
    }
}
