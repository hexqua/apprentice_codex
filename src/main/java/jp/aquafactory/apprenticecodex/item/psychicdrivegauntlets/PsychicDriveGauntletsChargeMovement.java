package jp.aquafactory.apprenticecodex.item.psychicdrivegauntlets;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;

public final class PsychicDriveGauntletsChargeMovement {
    private static final ResourceLocation MODIFIER_ID = ResourceLocation.fromNamespaceAndPath(
            ApprenticeCodex.MODID, "psychic_drive_gauntlets_charge_movement");

    private PsychicDriveGauntletsChargeMovement() {}

    public static void update(Player player, boolean charging) {
        var speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed == null) return;
        if (!charging || !player.isAlive() || player.isSpectator()) {
            speed.removeModifier(MODIFIER_ID);
            return;
        }
        player.setSprinting(false);
        // バニラの使用中入力減衰を使わず、ADSと同様にserverの属性を同期する。
        if (!speed.hasModifier(MODIFIER_ID)) {
            speed.addTransientModifier(new AttributeModifier(MODIFIER_ID,
                    PsychicDriveGauntletsCharge.MOVEMENT_FACTOR - 1.0,
                    AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        }
    }
}
