package jp.aquafactory.apprenticecodex.spell.inscribeice;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

@EventBusSubscriber(modid = ApprenticeCodex.MODID)
public final class InscribeIceDaggerThrowJobManager {
    private static final Map<ServerLevel, List<InscribeIceDaggerThrowJob>> JOBS = new WeakHashMap<>();

    private InscribeIceDaggerThrowJobManager() {
    }

    public static void submit(ServerLevel level, InscribeIceDaggerThrowJob job) {
        if (job.isComplete()) {
            return;
        }
        JOBS.computeIfAbsent(level, key -> new ArrayList<>()).add(job);
    }

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel serverLevel)) {
            return;
        }

        var jobs = JOBS.get(serverLevel);
        if (jobs == null || jobs.isEmpty()) {
            return;
        }

        var iterator = jobs.iterator();
        while (iterator.hasNext()) {
            var job = iterator.next();
            job.tick(serverLevel);
            if (job.isComplete()) {
                iterator.remove();
            }
        }

        if (jobs.isEmpty()) {
            JOBS.remove(serverLevel);
        }
    }
}
