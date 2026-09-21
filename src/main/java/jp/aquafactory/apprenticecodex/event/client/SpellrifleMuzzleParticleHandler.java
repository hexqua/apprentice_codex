package jp.aquafactory.apprenticecodex.event.client;

import jp.aquafactory.apprenticecodex.item.fullautorapidcastspellrifle.FullautoRapidcastSpellrifle;
import jp.aquafactory.apprenticecodex.item.multipurposestaffrifle.MultipurposeStaffrifle;
import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

public final class SpellrifleMuzzleParticleHandler {
    private SpellrifleMuzzleParticleHandler() {
    }

    public static void handle(ClientboundLevelParticlesPacket particle) {
        var minecraft = Minecraft.getInstance();
        var connection = minecraft.getConnection();
        if (connection == null || minecraft.level == null || minecraft.player == null) {
            return;
        }
        var offset = firstPersonTranslationOffset(minecraft);
        connection.handleParticleEvent(new ClientboundLevelParticlesPacket(
                particle.getParticle(), particle.isOverrideLimiter(),
                particle.getX() + offset.x, particle.getY() + offset.y, particle.getZ() + offset.z,
                particle.getXDist(), particle.getYDist(), particle.getZDist(),
                particle.getMaxSpeed(), particle.getCount()));
    }

    private static Vec3 firstPersonTranslationOffset(Minecraft minecraft) {
        var player = minecraft.player;
        if (player == null || minecraft.getCameraEntity() != player
                || !minecraft.options.getCameraType().isFirstPerson()) {
            return Vec3.ZERO;
        }
        var stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof MultipurposeStaffrifle)
                && !(stack.getItem() instanceof FullautoRapidcastSpellrifle)) {
            return Vec3.ZERO;
        }
        boolean left = player.getMainArm() == HumanoidArm.LEFT;
        var display = left ? ItemDisplayContext.FIRST_PERSON_LEFT_HAND : ItemDisplayContext.FIRST_PERSON_RIGHT_HAND;
        // overrides解決後のモデルを参照し、ADS用JSONやresource reload後の座標も反映する。
        var model = minecraft.getItemRenderer().getModel(stack, minecraft.level, player, player.getId());
        var translation = model.getTransforms().getTransform(display).translation;
        // 従来の固定マズル位置はdisplay.translation=[0,-4.5,0]に対応した近似値。
        // translationはロード時点で1/16単位。scaleはtranslationに掛からない。
        // 今回は平行移動の差分のみ。銃口の回転・リコイル・FOVまで厳密に追うには描画アンカーが必要。
        var offset = new Vector3f((left ? -1 : 1) * translation.x(),
                translation.y() + 4.5F / 16.0F, translation.z());
        minecraft.gameRenderer.getMainCamera().rotation().transform(offset);
        return new Vec3(offset.x(), offset.y(), offset.z());
    }
}
