package jp.aquafactory.apprenticecodex.spell.healingbloom;

import com.mojang.blaze3d.vertex.PoseStack;
import jp.aquafactory.apprenticecodex.particle.AdditiveGlowParticleOptions;
import jp.aquafactory.apprenticecodex.registry.ParticleRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

public class HealingBloomLightBlockEntityRenderer implements BlockEntityRenderer<HealingBloomLightBlockEntity> {
    private static final double PARTICLE_Y_OFFSET = 0.25;
    private static final BloomColor DEFAULT_COLOR = new BloomColor(1.0f, 0.82f, 0.24f);

    public HealingBloomLightBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(@NotNull HealingBloomLightBlockEntity blockEntity, float partialTick, @NotNull PoseStack poseStack,
                       @NotNull MultiBufferSource buffer, int packedLight, int packedOverlay) {
        var level = blockEntity.getLevel();
        if (level == null) {
            return;
        }

        var camera = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
        var center = Vec3.atCenterOf(blockEntity.getBlockPos());
        var distance = camera.distanceTo(center);
        if (distance > 24.0) {
            return;
        }

        var random = RandomSource.create(blockEntity.getBlockPos().asLong());
        var gameTime = level.getGameTime();
        var hash = blockEntity.getBlockPos().hashCode();
        var baseX = center.x;
        var baseY = center.y + PARTICLE_Y_OFFSET;
        var baseZ = center.z;
        var color = resolveColor(level, blockEntity);

        if ((gameTime + (hash & 3)) % 4L == 0L) {
            level.addParticle(
                    new AdditiveGlowParticleOptions(
                            ParticleRegistry.ADDITIVE_CIRCLE.get(),
                            0.42f,
                            color.red(),
                            color.green(),
                            color.blue(),
                            6,
                            18,
                            4,
                            0.85f,
                            1.15f,
                            0.5f,
                            0.85f,
                            0.10f,
                            0.70f,
                            0.72f,
                            false
                    ),
                    baseX,
                    baseY - 0.08,
                    baseZ,
                    0.0,
                    0.0,
                    0.0
            );
        }

        if ((gameTime + ((hash >> 4) & 1)) % 2L == 0L) {
            for (int i = 0; i < 2; ++i) {
                var angle = (gameTime * 0.14 + i * Math.PI + (hash & 31) * 0.03);
                var radius = 0.18 + random.nextDouble() * 0.10;
                level.addParticle(
                        new AdditiveGlowParticleOptions(
                                ParticleRegistry.ADDITIVE_SPARK.get(),
                                0.14f,
                                color.red(),
                                color.green(),
                                color.blue(),
                                5,
                                12,
                                4,
                                0.95f,
                                1.20f,
                                0.9f,
                                1.0f,
                                0.04f,
                                0.72f,
                                0.78f,
                                false
                        ),
                        baseX + Math.cos(angle) * radius,
                        baseY - 0.02 + random.nextDouble() * 0.12,
                        baseZ + Math.sin(angle) * radius,
                        0.0,
                        0.005 + random.nextDouble() * 0.01,
                        0.0
                );
            }
        }
    }

    private static BloomColor resolveColor(net.minecraft.world.level.Level level, HealingBloomLightBlockEntity blockEntity) {
        var bloom = level.getEntitiesOfClass(
                HealingBloomEntity.class,
                new net.minecraft.world.phys.AABB(blockEntity.getBlockPos().below())
                        .inflate(0.3, 0.4, 0.3),
                entity -> entity.isAlive() && entity.blockPosition().equals(blockEntity.getBlockPos().below())
        ).stream().findFirst().orElse(null);

        if (bloom == null) {
            return DEFAULT_COLOR;
        }

        return new BloomColor(
                bloom.getLightColorRed(),
                bloom.getLightColorGreen(),
                bloom.getLightColorBlue()
        );
    }

    private record BloomColor(float red, float green, float blue) {
    }
}
