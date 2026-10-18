package jp.aquafactory.apprenticecodex.item.manaforcereleasedevice;

import jp.aquafactory.apprenticecodex.registry.ParticleRegistry;
import jp.aquafactory.apprenticecodex.spell.shock.ShockBoltEntity;
import jp.aquafactory.apprenticecodex.spell.shock.ShockImpactParticles;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

public final class ManaForceReleaseBolt extends ShockBoltEntity {
    public ManaForceReleaseBolt(EntityType<? extends ManaForceReleaseBolt> type, Level level) {
        super(type, level);
    }

    @Override
    protected void spawnImpactParticles() {
        var end = getEndPosition();
        var incoming = end.subtract(position());
        for (int i = 0; i < 3; i++) {
            ShockImpactParticles.spawn(level(), end, incoming, new ShockImpactParticles.Palette(0.3F, 1.0F, 0.4F));
        }
        for (int i = 0; i < 28; i++) {
            var velocity = incoming.normalize().scale(-0.06).add(
                    (random.nextDouble() - 0.5) * 0.4, 0.1 + random.nextDouble() * 0.3,
                    (random.nextDouble() - 0.5) * 0.4);
            level().addParticle(ParticleRegistry.MANA_FORCE_RELEASE_SPARK.get(), end.x, end.y, end.z,
                    velocity.x, velocity.y, velocity.z);
        }
    }
}
