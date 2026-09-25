package jp.aquafactory.apprenticecodex.item.multipurposestaffrifle;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.config.ApprenticeCodexServerConfig;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.event.TickEvent;

@Mod.EventBusSubscriber(modid = ApprenticeCodex.MODID)
public final class MultipurposeStaffrifleAdsMovement {
    private static final UUID MODIFIER_ID = UUID.nameUUIDFromBytes((
            ApprenticeCodex.MODID + ":" + "multipurpose_staffrifle_ads_movement").getBytes(StandardCharsets.UTF_8));

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
        if (current == null || current.getAmount() != amount) {
            speed.removeModifier(MODIFIER_ID);
            speed.addTransientModifier(new AttributeModifier(MODIFIER_ID, "apprenticecodex.ads_movement", amount,
                    AttributeModifier.Operation.MULTIPLY_TOTAL));
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (event.player instanceof ServerPlayer player) {
            var speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
            // 持ち替え・死亡時は減速を解除し、ADS中のスプリント再開は抑止する。
            if (speed != null && speed.getModifier(MODIFIER_ID) != null) {
                update(player, true);
            }
        }
    }
}
