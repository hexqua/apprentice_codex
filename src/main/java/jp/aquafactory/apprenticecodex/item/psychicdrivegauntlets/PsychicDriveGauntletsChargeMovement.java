package jp.aquafactory.apprenticecodex.item.psychicdrivegauntlets;

import java.util.UUID;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;

public final class PsychicDriveGauntletsChargeMovement {
    private static final UUID MODIFIER_ID = UUID.fromString("449c539f-bf42-480e-96c3-58a6e8e1949f");

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
        if (speed.getModifier(MODIFIER_ID) == null) {
            speed.addTransientModifier(new AttributeModifier(MODIFIER_ID, "psychic_drive_gauntlets_charge_movement",
                    PsychicDriveGauntletsCharge.MOVEMENT_FACTOR - 1.0,
                    AttributeModifier.Operation.MULTIPLY_TOTAL));
        }
    }
}
