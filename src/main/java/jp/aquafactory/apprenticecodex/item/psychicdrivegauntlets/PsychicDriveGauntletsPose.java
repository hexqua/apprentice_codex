package jp.aquafactory.apprenticecodex.item.psychicdrivegauntlets;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

public final class PsychicDriveGauntletsPose {
    private PsychicDriveGauntletsPose() {}

    public static Vec3 offset(float drawnAmount, boolean right) {
        var side = right ? 1.0F : -1.0F;
        return new Vec3(side * Mth.lerp(drawnAmount, 0.35F, 0.8F),
                Mth.lerp(drawnAmount, -0.65F, -0.5F),
                Mth.lerp(drawnAmount, 0.65F, -0.1F));
    }

    public static Vec3 position(Player player, float drawnAmount, boolean right) {
        var yaw = Mth.rotLerp(drawnAmount, player.yBodyRot, player.getYRot());
        return player.getEyePosition().add(offset(drawnAmount, right).yRot((180.0F - yaw) * Mth.DEG_TO_RAD));
    }
}
