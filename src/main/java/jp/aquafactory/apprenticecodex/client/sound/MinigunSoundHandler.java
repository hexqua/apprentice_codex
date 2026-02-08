package jp.aquafactory.apprenticecodex.client.sound;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.common.registry.SoundRegistry;
import jp.aquafactory.apprenticecodex.common.spells.bulletstream.BulletStreamMinigunEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

import java.util.HashMap;
import java.util.Map;

import static jp.aquafactory.apprenticecodex.common.spells.bulletstream.BulletStreamMinigunEntity.IS_SOUND_LOOP_MODE;

@EventBusSubscriber(modid = ApprenticeCodex.MODID, value = Dist.CLIENT)
public class MinigunSoundHandler {
    private static final Map<Integer, MinigunLoopSound> playing = new HashMap<>();

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        var minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
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
