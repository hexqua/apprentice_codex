package jp.aquafactory.apprenticecodex.particle;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.config.ApprenticeCodexClientConfig;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.model.data.ModelData;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

@Mod.EventBusSubscriber(modid = ApprenticeCodex.MODID, value = Dist.CLIENT)
public class SmashcastTremorBlockParticle extends TextureSheetParticle {
    private static final List<RenderableBlock> BLOCKS_TO_RENDER = new ArrayList<>();

    private final BlockState state;
    private final BlockPos originalPos;

    protected SmashcastTremorBlockParticle(ClientLevel level, double x, double y, double z,
                                           SmashcastTremorBlockParticleOptions options) {
        super(level, x, y, z, 0.0D, 0.0D, 0.0D);
        this.state = options.state();
        this.originalPos = BlockPos.containing(x, y, z);
        this.xd = options.motion().x;
        this.yd = options.motion().y;
        this.zd = options.motion().z;
        this.gravity = 0.08F;
        this.lifetime = 80;
        this.hasPhysics = true;
    }

    @SubscribeEvent
    public static void renderBlocks(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES) {
            return;
        }

        var minecraft = Minecraft.getInstance();
        var level = minecraft.level;
        if (level == null || BLOCKS_TO_RENDER.isEmpty()) {
            BLOCKS_TO_RENDER.clear();
            return;
        }

        var dispatcher = minecraft.getBlockRenderer();
        var bufferSource = minecraft.renderBuffers().bufferSource();
        synchronized (BLOCKS_TO_RENDER) {
            for (var block : BLOCKS_TO_RENDER) {
                PoseStack poseStack = event.getPoseStack();
                poseStack.pushPose();
                poseStack.translate(block.relativePos().x, block.relativePos().y, block.relativePos().z);
                poseStack.translate(-0.5D, 0.0D, -0.5D);

                var model = dispatcher.getBlockModel(block.state());
                for (var renderType : model.getRenderTypes(block.state(), RandomSource.create(0L), ModelData.EMPTY)) {
                    dispatcher.getModelRenderer().tesselateBlock(
                            level,
                            model,
                            block.state(),
                            block.worldPos().above(),
                            poseStack,
                            bufferSource.getBuffer(renderType),
                            false,
                            RandomSource.create(),
                            block.state().getSeed(block.originalPos()),
                            OverlayTexture.NO_OVERLAY,
                            ModelData.EMPTY,
                            renderType
                    );
                }

                poseStack.popPose();
            }
            BLOCKS_TO_RENDER.clear();
        }
    }

    @Override
    public void tick() {
        var wasOnGround = onGround;
        age++;
        xo = x;
        yo = y;
        zo = z;
        move(xd, yd, zd);
        yd -= gravity;
        if (state.isAir() || wasOnGround || age > lifetime) {
            remove();
        }
    }

    @Override
    public void render(@NotNull VertexConsumer buffer, @NotNull Camera camera, float partialTick) {
        if (state.getRenderShape() != RenderShape.MODEL) {
            return;
        }

        var cameraPos = camera.getPosition();
        var renderX = Mth.lerp(partialTick, xo, x) - cameraPos.x();
        var renderY = Mth.lerp(partialTick, yo, y) - cameraPos.y();
        var renderZ = Mth.lerp(partialTick, zo, z) - cameraPos.z();
        synchronized (BLOCKS_TO_RENDER) {
            BLOCKS_TO_RENDER.add(new RenderableBlock(
                    BlockPos.containing(x, y, z),
                    originalPos,
                    new Vec3(renderX, renderY, renderZ),
                    state
            ));
        }
    }

    @Override
    public @NotNull ParticleRenderType getRenderType() {
        return ParticleRenderType.TERRAIN_SHEET;
    }

    @Override
    protected int getLightColor(float partialTick) {
        return LightTexture.FULL_BRIGHT;
    }

    private record RenderableBlock(BlockPos worldPos, BlockPos originalPos, Vec3 relativePos, BlockState state) {
    }

    public static class Provider implements ParticleProvider<SmashcastTremorBlockParticleOptions> {
        @Override
        public @Nullable Particle createParticle(@NotNull SmashcastTremorBlockParticleOptions options,
                                                @NotNull ClientLevel level,
                                                double x,
                                                double y,
                                                double z,
                                                double xd,
                                                double yd,
                                                double zd) {
            if (!ApprenticeCodexClientConfig.enableSmashcastScepterTremorBlockRendering()) {
                return null;
            }
            return new SmashcastTremorBlockParticle(level, x, y, z, options);
        }
    }
}
