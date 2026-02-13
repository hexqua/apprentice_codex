package jp.aquafactory.apprenticecodex.sound;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;

public class MinigunLoopSound extends AbstractTickableSoundInstance {
    private final Entity owner;
    private final FiringStateProvider state;

    public interface FiringStateProvider {
        boolean isFiring();
    }

    public MinigunLoopSound(SoundEvent sound, Entity owner, RandomSource random, FiringStateProvider state) {
        super(sound, SoundSource.PLAYERS, random);
        this.owner = owner;
        this.state = state;

        looping = true;
        delay = 0;

        attenuation = Attenuation.LINEAR;
        volume = 0.7f;
        pitch = 1.0f;

        x = owner.getX();
        y = owner.getY();
        z = owner.getZ();
    }

    @Override
    public void tick() {
        if (owner.isRemoved() || !state.isFiring()) {
            this.stop();
            return;
        }

        x = owner.getX();
        y = owner.getY();
        z = owner.getZ();

    }

    public void stopSound(){
        super.stop();
    }
}
