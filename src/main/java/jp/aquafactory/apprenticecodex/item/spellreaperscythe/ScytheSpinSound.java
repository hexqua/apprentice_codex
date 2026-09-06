package jp.aquafactory.apprenticecodex.item.spellreaperscythe;

import jp.aquafactory.apprenticecodex.registry.SoundRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;

/** 停滞中だけ存在する音。サーバーから反復packetを送り続けず、終了時には再生途中でも止める。 */
final class ScytheSpinSound extends AbstractTickableSoundInstance {
    private final ScytheThrowEntity entity;

    private ScytheSpinSound(ScytheThrowEntity entity) {
        super(SoundRegistry.SCYTHE_SPIN.get(), SoundSource.PLAYERS, RandomSource.create());
        this.entity = entity;
        volume = 0.35f;
        pitch = 1f;
        looping = true;
        // 素材は約0.93秒。再生間に短い間隔を置き、約1秒周期で旋回音を繰り返す。
        delay = 2;
        updatePosition();
    }

    static void play(ScytheThrowEntity entity) {
        Minecraft.getInstance().getSoundManager().play(new ScytheSpinSound(entity));
    }

    @Override
    public void tick() {
        if (entity.isRemoved() || !entity.isHovering() || Minecraft.getInstance().level != entity.level()) {
            stop();
            return;
        }
        updatePosition();
    }

    @Override
    public boolean canPlaySound() {
        return !entity.isRemoved() && entity.isHovering() && Minecraft.getInstance().level == entity.level();
    }

    private void updatePosition() {
        x = entity.getX();
        y = entity.getY();
        z = entity.getZ();
    }
}
