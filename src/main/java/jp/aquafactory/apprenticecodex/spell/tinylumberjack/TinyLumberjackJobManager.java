package jp.aquafactory.apprenticecodex.spell.tinylumberjack;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

@Mod.EventBusSubscriber(modid = ApprenticeCodex.MODID)
public class TinyLumberjackJobManager {
    private static final Map<ServerLevel, List<TinyLumberjackJob>> JOBS = new WeakHashMap<>();

    private TinyLumberjackJobManager() {
        // do nothing.
    }

    public static void submit(ServerLevel level, TinyLumberjackJob job) {
        JOBS.computeIfAbsent(level, key -> new ArrayList<>()).add(job);
    }

    @SubscribeEvent
    public static void onLevelTick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        if (!(event.level instanceof ServerLevel serverLevel)) {
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
