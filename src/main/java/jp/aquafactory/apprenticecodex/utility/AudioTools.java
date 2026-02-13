package jp.aquafactory.apprenticecodex.utility;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public final class AudioTools {
    private AudioTools() {}

    public static void playSoundFromEntity(Level level, Entity entity,SoundEvent soundEvent,SoundSource soundSource, float volume, float pitch, float pitchVariation) {
        level.playSound(
                null,
                entity.getX(), entity.getY(), entity.getZ(),
                soundEvent,
                soundSource,
                volume,
                pitch + pitchVariation * level.random.nextFloat() - pitchVariation / 2
        );
    }

    public static void playSoundFromEntity(Level level, Entity entity,SoundEvent soundEvent,SoundSource soundSource, float volume, float pitch) {
        playSoundFromEntity(level, entity, soundEvent, soundSource, volume, pitch, 0.2f);
    }

    public static void playSoundFromEntity(Level level, Entity entity,SoundEvent soundEvent,SoundSource soundSource, float volume) {
        playSoundFromEntity(level, entity, soundEvent, soundSource, volume, 1.0f);
    }

    public static void playSoundFromEntity(Level level, Entity entity,SoundEvent soundEvent,SoundSource soundSource) {
        playSoundFromEntity(level, entity, soundEvent, soundSource, 1.0f);
    }

    public static void playSoundFromBlock(Level level, Vec3 position, SoundEvent soundEvent, SoundSource soundSource) {
        level.playSound(
                null,
                position.x, position.y, position.z,
                soundEvent,
                soundSource,
                1.0f,
                1.0f
        );
    }
}
