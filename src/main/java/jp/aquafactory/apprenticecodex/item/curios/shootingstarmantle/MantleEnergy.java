package jp.aquafactory.apprenticecodex.item.curios.shootingstarmantle;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

public record MantleEnergy(int energy, boolean recovering, int spentTicks) {
    public static final int MAX = 100;
    private static final String KEY = "apprenticecodex_shooting_star_mantle";

    public MantleEnergy {
        energy = Mth.clamp(energy, 0, MAX);
        recovering = energy == 0 || energy < MAX && recovering;
        spentTicks = Mth.clamp(spentTicks, 0, 19);
    }

    public static MantleEnergy read(ItemStack stack) {
        var root = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (!root.contains(KEY, Tag.TAG_COMPOUND)) return new MantleEnergy(MAX, false, 0);
        var tag = root.getCompound(KEY);
        return new MantleEnergy(tag.getInt("energy"), tag.getBoolean("recovering"), tag.getInt("spent_ticks"));
    }

    public void save(ItemStack stack) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, root -> {
            var tag = new CompoundTag();
            tag.putInt("energy", energy);
            tag.putBoolean("recovering", recovering);
            tag.putInt("spent_ticks", spentTicks);
            root.put(KEY, tag);
        });
    }

    public boolean usable() { return energy > 0 && !recovering; }

    public MantleEnergy tickUse() {
        return tickUse(1);
    }

    public MantleEnergy tickUse(int rate) {
        if (!usable()) return this;
        int spent = spentTicks + rate;
        return new MantleEnergy(energy - spent / 20, false, spent % 20);
    }

    public boolean canImpulse() { return usable(); }

    public MantleEnergy impulse() {
        return canImpulse() ? spend(10) : this;
    }

    public MantleEnergy spend(int cost) {
        return new MantleEnergy(energy - Math.max(0, cost), recovering, spentTicks);
    }

    public MantleEnergy recharge() {
        return recharge(false);
    }

    public MantleEnergy recharge(boolean fastRecovery) {
        // 回復量の切り替えは枯渇ロックと独立させ、ルーンで飛行を禁止しない。
        return new MantleEnergy(energy + (recovering || fastRecovery ? 10 : 2), recovering, spentTicks);
    }
}
