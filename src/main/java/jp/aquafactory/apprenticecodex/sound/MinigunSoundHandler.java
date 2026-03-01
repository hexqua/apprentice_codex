package jp.aquafactory.apprenticecodex.sound;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.registry.SoundRegistry;
import jp.aquafactory.apprenticecodex.spell.bulletstream.BulletStreamMinigunEntity;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

import java.util.HashMap;
import java.util.Map;

import static jp.aquafactory.apprenticecodex.spell.bulletstream.BulletStreamMinigunEntity.IS_SOUND_LOOP_MODE;

@EventBusSubscriber(modid = ApprenticeCodex.MODID, value = Dist.CLIENT)
public class MinigunSoundHandler {
    private static final Map<Integer, MinigunLoopSound> playing = new HashMap<>();

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post ignored) {
        var minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            // ワールド切り替え/切断直後に static Map に参照が残らないよう即時解放する.
            if (!playing.isEmpty()) {
                playing.values().forEach(MinigunLoopSound::stopSound);
                playing.clear();
            }
            return;
        }

        playing.entrySet().removeIf(entry -> {
            var sound = entry.getValue();
            if (sound.isStopped()) {
                return true;
            }
            var entity = minecraft.level.getEntity(entry.getKey());
            if (!(entity instanceof BulletStreamMinigunEntity gun) || !gun.getEntityData().get(IS_SOUND_LOOP_MODE)) {
                sound.stopSound();
                return true;
            }
            return false;
        });

        for (var entity : minecraft.level.entitiesForRendering()) {
            if (entity instanceof BulletStreamMinigunEntity gun) {
                var id = gun.getId();
                if (gun.getEntityData().get(IS_SOUND_LOOP_MODE) && !playing.containsKey(id)) {
                    var sound = new MinigunLoopSound(
                            SoundRegistry.MINIGUN_LOOP.get(), gun, minecraft.level.random,
                            () -> gun.getEntityData().get(IS_SOUND_LOOP_MODE)
                    );
                    playing.put(id, sound);
                    minecraft.getSoundManager().play(sound);
                }
            }
        }
    }
}

