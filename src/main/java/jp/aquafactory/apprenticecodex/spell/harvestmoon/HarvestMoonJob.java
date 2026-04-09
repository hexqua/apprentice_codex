package jp.aquafactory.apprenticecodex.spell.harvestmoon;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayDeque;

public final class HarvestMoonJob {
    private final ServerPlayer starter;
    private final ItemStack toolTemplate;
    private final Vec3 attractPos;
    private final ArrayDeque<HarvestMoonAction> actions = new ArrayDeque<>();
    private final int blockBudgetPerTick;
    private boolean complete;

    public HarvestMoonJob(ServerPlayer starter, ItemStack toolTemplate, Iterable<HarvestMoonAction> actions, Vec3 attractPos, int blockBudgetPerTick) {
        this.starter = starter;
        this.toolTemplate = toolTemplate.copy();
        this.attractPos = attractPos;
        this.blockBudgetPerTick = Math.max(1, blockBudgetPerTick);
        for (var action : actions) {
            this.actions.addLast(action);
        }
        complete = this.actions.isEmpty();
    }

    public boolean isComplete() {
        return complete;
    }

    public void tick(ServerLevel level) {
        if (complete) {
            return;
        }

        if (starter.isRemoved() || !starter.isAlive() || starter.level() != level) {
            complete = true;
            return;
        }

        var consumedBudget = 0;
        while (consumedBudget < blockBudgetPerTick && !actions.isEmpty()) {
            var action = actions.removeFirst();
            var processed = Math.max(0, action.execute(level, starter, toolTemplate, attractPos));
            consumedBudget += Math.max(1, processed);
        }

        if (actions.isEmpty()) {
            complete = true;
        }
    }
}
