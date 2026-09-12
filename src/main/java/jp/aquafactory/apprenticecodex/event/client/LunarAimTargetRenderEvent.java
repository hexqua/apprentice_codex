package jp.aquafactory.apprenticecodex.event.client;

import com.mojang.math.Axis;
import io.redspace.ironsspellbooks.player.ClientMagicData;
import io.redspace.ironsspellbooks.render.SpellTargetingLayer;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.registry.SpellRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

@EventBusSubscriber(modid = ApprenticeCodex.MODID, value = Dist.CLIENT)
public final class LunarAimTargetRenderEvent {
    private LunarAimTargetRenderEvent() {}

    @SubscribeEvent
    public static void onRenderLevelStage(net.neoforged.neoforge.client.event.RenderLevelStageEvent event) {
        if (event.getStage() != net.neoforged.neoforge.client.event.RenderLevelStageEvent.Stage.AFTER_ENTITIES) return;
        var minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null || !ClientMagicData.isCasting()) return;
        var spell = SpellRegistry.LUNAR_AIM.get();
        var targeting = ClientMagicData.getTargetingData();
        if (!spell.getSpellId().equals(ClientMagicData.getCastingSpellId()) || !spell.getSpellId().equals(targeting.spellId)) return;

        // Iron'sの標準レイヤーはLivingEntity専用。クリスタルだけ同じ同期UUID・テクスチャ・色で補完する。
        var renderType = RenderType.energySwirl(SpellTargetingLayer.TEXTURE, 0, 0);
        var buffers = minecraft.renderBuffers().bufferSource();
        var pose = event.getPoseStack();
        var camera = event.getCamera().getPosition();
        var color = new org.joml.Vector3f(spell.getTargetingColor()).mul(0.4f);
        boolean rendered = false;
        for (var entity : minecraft.level.entitiesForRendering()) {
            if (!(entity instanceof EndCrystal crystal) || !crystal.isAlive() || !targeting.isTargeted(crystal.getUUID())) continue;
            var position = crystal.getPosition(event.getPartialTick().getGameTimeDeltaPartialTick(false));
            float halfWidth = crystal.getBbWidth() * 0.55f;
            float height = crystal.getBbHeight();
            pose.pushPose();
            pose.translate(position.x - camera.x, position.y - camera.y, position.z - camera.z);
            var consumer = buffers.getBuffer(renderType);
            for (int i = 0; i < 4; i++) {
                var matrix = pose.last().pose();
                consumer.addVertex(matrix, halfWidth, 0, halfWidth).setColor(color.x, color.y, color.z, 1).setUv(0, 1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT).setNormal(0, 1, 0);
                consumer.addVertex(matrix, halfWidth, height, halfWidth).setColor(color.x, color.y, color.z, 1).setUv(0, 0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT).setNormal(0, 1, 0);
                consumer.addVertex(matrix, -halfWidth, height, halfWidth).setColor(color.x, color.y, color.z, 1).setUv(1, 0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT).setNormal(0, 1, 0);
                consumer.addVertex(matrix, -halfWidth, 0, halfWidth).setColor(color.x, color.y, color.z, 1).setUv(1, 1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT).setNormal(0, 1, 0);
                pose.mulPose(Axis.YP.rotationDegrees(90));
            }
            pose.popPose();
            rendered = true;
        }
        if (rendered) buffers.endBatch(renderType);
    }
}
