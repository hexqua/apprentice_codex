package jp.aquafactory.apprenticecodex.spell.harvestmoon;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.animal.Sheep;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayDeque;
import java.util.List;

public final class HarvestMoonJob {
    private final ServerPlayer starter;
    private final ItemStack toolTemplate;
    private final Vec3 attractPos;
    private final ArrayDeque<HarvestMoonAction> actions = new ArrayDeque<>();
    private final ArrayDeque<Sheep> sheep = new ArrayDeque<>();
    private final int blockBudgetPerTick;
    private boolean complete;

    public HarvestMoonJob(ServerPlayer starter, ItemStack toolTemplate, Iterable<HarvestMoonAction> actions,
                          List<Sheep> sheep, Vec3 attractPos, int blockBudgetPerTick) {
        this.starter = starter;
        this.toolTemplate = toolTemplate.copy();
        this.attractPos = attractPos;
        this.blockBudgetPerTick = Math.max(1, blockBudgetPerTick);
        for (var action : actions) {
            this.actions.addLast(action);
        }
        this.sheep.addAll(sheep);
        complete = this.actions.isEmpty() && this.sheep.isEmpty();
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
        while (consumedBudget < blockBudgetPerTick && !sheep.isEmpty()) {
            shearSheep(level, sheep.removeFirst());
            ++consumedBudget;
        }
        while (consumedBudget < blockBudgetPerTick && !actions.isEmpty()) {
            var action = actions.removeFirst();
            var processed = Math.max(0, action.execute(level, starter, toolTemplate, attractPos));
            consumedBudget += Math.max(1, processed);
        }

        if (actions.isEmpty() && sheep.isEmpty()) {
            complete = true;
        }
    }

    private void shearSheep(ServerLevel level, Sheep target) {
        if (target.isRemoved() || target.level() != level) {
            return;
        }

        var shears = new ItemStack(Items.SHEARS);
        var pos = target.blockPosition();
        if (!target.isShearable(starter, shears, level, pos)) {
            return;
        }

        var dropBox = target.getBoundingBox().inflate(1.5);
        var beforeIds = HarvestMoonAction.HarvestMoonActionUtil.captureNearbyItemIds(level, dropBox);
        var originalItem = starter.getMainHandItem();
        try {
            // 通常のエンティティ操作イベントとハサミ使用経路へ通すが、本物の利き手と耐久は消費しない。
            starter.setItemInHand(InteractionHand.MAIN_HAND, shears);
            starter.interactOn(target, InteractionHand.MAIN_HAND);
        } finally {
            starter.setItemInHand(InteractionHand.MAIN_HAND, originalItem);
        }
        HarvestMoonAction.HarvestMoonActionUtil.moveNewDropsTo(level, dropBox, beforeIds, attractPos);
    }
}
