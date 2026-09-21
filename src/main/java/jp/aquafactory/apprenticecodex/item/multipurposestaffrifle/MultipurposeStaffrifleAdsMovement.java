package jp.aquafactory.apprenticecodex.item.multipurposestaffrifle;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.config.ApprenticeCodexServerConfig;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber(modid = ApprenticeCodex.MODID)
public final class MultipurposeStaffrifleAdsMovement {
    private static final ResourceLocation MODIFIER_ID = ResourceLocation.fromNamespaceAndPath(
            ApprenticeCodex.MODID, "multipurpose_staffrifle_ads_movement");

    private MultipurposeStaffrifleAdsMovement() {
    }

    public static void update(ServerPlayer player, boolean aiming) {
        var speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed == null) {
            return;
        }
        if (!aiming || !player.isAlive() || player.isSpectator()
                || !(player.getMainHandItem().getItem() instanceof MultipurposeStaffrifle)) {
            speed.removeModifier(MODIFIER_ID);
            return;
        }

        // ADS開始とスプリント通知の到着順に依存せず、照準入力を優先する。
        player.setSprinting(false);
        // 射撃でバニラの使用状態が解除されても減速を維持し、倍率は必ずサーバー設定から取得する。
        double amount = ApprenticeCodexServerConfig.multipurposeStaffrifleAdsMovementSpeedMultiplier() - 1.0D;
        var current = speed.getModifier(MODIFIER_ID);
        if (current == null || current.amount() != amount) {
            speed.removeModifier(MODIFIER_ID);
            speed.addTransientModifier(new AttributeModifier(MODIFIER_ID, amount,
                    AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            var speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
            // 持ち替え・死亡時は減速を解除し、ADS中のスプリント再開は抑止する。
            if (speed != null && speed.hasModifier(MODIFIER_ID)) {
                update(player, true);
            }
        }
    }
}
