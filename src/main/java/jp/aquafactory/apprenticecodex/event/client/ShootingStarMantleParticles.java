package jp.aquafactory.apprenticecodex.event.client;

import jp.aquafactory.apprenticecodex.item.curios.shootingstarmantle.ShootingStarMantle;
import jp.aquafactory.apprenticecodex.item.curios.shootingstarmantle.ShootingStarMantleRuntime;
import jp.aquafactory.apprenticecodex.particle.AdditiveGlowParticleOptions;
import jp.aquafactory.apprenticecodex.registry.ParticleRegistry;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import top.theillusivec4.curios.api.CuriosApi;

final class ShootingStarMantleParticles {
    private static final DustParticleOptions NIGHT_DUST =
            new DustParticleOptions(new Vector3f(0.12F, 0.18F, 0.43F), 0.8F);
    private static final AdditiveGlowParticleOptions BLUE_SPARK =
            new AdditiveGlowParticleOptions(ParticleRegistry.ADDITIVE_SPARK.get(), 0.12F,
                    0.27F, 0.42F, 0.82F, 2, 12, 4,
                    0.85F, 1.25F, 0.65F, 0.9F, 0.05F, 0.65F, 0.65F, true);
    private static final AdditiveGlowParticleOptions GOLD_SPARK =
            new AdditiveGlowParticleOptions(ParticleRegistry.ADDITIVE_SPARK.get(), 0.13F,
                    1.0F, 0.86F, 0.34F, 2, 12, 4,
                    0.85F, 1.25F, 0.7F, 0.95F, 0.05F, 0.65F, 0.65F, true);
    private static final AdditiveGlowParticleOptions GOLD_RHOMBUS =
            new AdditiveGlowParticleOptions(ParticleRegistry.ADDITIVE_RHOMBUS.get(), 0.15F,
                    1.0F, 0.88F, 0.4F, 2, 15, 4,
                    0.85F, 1.2F, 0.6F, 0.85F, 0.05F, 0.7F, 0.6F, false);
    private static Vec3 previousPosition;

    private ShootingStarMantleParticles() { }

    static void tick(LocalPlayer player) {
        if (ShootingStarMantleImpulseParticles.tick(player)) {
            previousPosition = null;
            return;
        }
        if (player == null || !canRender(player) || !player.isFallFlying() && !ShootingStarMantleRuntime.isHovering(player)) {
            reset();
            return;
        }
        var random = player.getRandom();
        var current = player.position();
        boolean flying = player.isFallFlying();
        var travel = previousPosition == null ? Vec3.ZERO : current.subtract(previousPosition);
        // テレポートや再ログインで離れた位置を軌跡で結ばない。
        if (travel.lengthSqr() > 64.0D) travel = Vec3.ZERO;
        int count = flying ? Mth.clamp((int) Math.ceil(travel.length() / 0.75D), 1, 4) : 2;
        var forward = Vec3.directionFromRotation(0, player.getYRot());
        var right = new Vec3(-forward.z, 0, forward.x);
        for (int i = 0; i < count; i++) {
            var base = flying ? current.subtract(travel.scale(random.nextDouble())) : current;
            double side = (random.nextBoolean() ? 1 : -1) * (0.45D + random.nextDouble() * (flying ? 0.3D : 0.5D));
            double behind = flying ? 0.2D + random.nextDouble() * 0.35D : 0.2D + random.nextDouble() * 0.25D;
            double height = flying ? 0.65D + random.nextDouble() * 0.7D : 0.75D + random.nextDouble() * 0.85D;
            var position = base.add(right.scale(side)).subtract(forward.scale(behind)).add(0, height, 0);
            // 暗い粒を面として残し、黄色い星は少数の光点にする。
            double choice = random.nextDouble();
            ParticleOptions particle = choice < 0.55D ? NIGHT_DUST
                    : choice < 0.8D ? BLUE_SPARK
                    : choice < 0.96D ? GOLD_SPARK : GOLD_RHOMBUS;
            double outward = (side > 0 ? 1 : -1) * (flying ? 0.002D : 0.012D);
            var velocity = right.scale(outward).add(
                    (random.nextDouble() - 0.5D) * 0.008D,
                    flying ? 0.002D : 0.01D + random.nextDouble() * 0.01D,
                    (random.nextDouble() - 0.5D) * 0.008D);
            player.level().addParticle(particle, position.x, position.y, position.z,
                    velocity.x, velocity.y, velocity.z);
        }
        // 一人称では背後の軌跡が視界に入りにくいため、時々だけ肩の外側にも星を置く。
        if (random.nextInt(8) == 0) {
            var position = current.add(forward.scale(0.55D))
                    .add(right.scale(random.nextBoolean() ? 0.7D : -0.7D))
                    .add(0, player.getEyeHeight() - 0.4D, 0);
            player.level().addParticle(random.nextInt(4) == 0 ? GOLD_SPARK : BLUE_SPARK,
                    position.x, position.y, position.z, 0, 0.005D, 0);
        }
        previousPosition = current;
    }

    private static boolean canRender(LocalPlayer player) {
        return CuriosApi.getCuriosInventory(player).map(inventory -> {
            var back = inventory.getCurios().get("back");
            if (back == null) return false;
            // Curiosはコスメを実装備より優先してrendererへ渡すため、同じ枠の上書き状態を見る。
            for (int i = 0; i < back.getStacks().getSlots(); i++) {
                if (back.getRenders().size() <= i || !back.getRenders().get(i)
                        || !(back.getStacks().getStackInSlot(i).getItem() instanceof ShootingStarMantle)) continue;
                var cosmetic = back.getCosmeticStacks().getStackInSlot(i);
                if (cosmetic.isEmpty() || cosmetic.getItem() instanceof ShootingStarMantle) return true;
            }
            return false;
        }).orElse(false);
    }

    static void reset() {
        previousPosition = null;
        ShootingStarMantleImpulseParticles.reset();
    }
}
