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
        if (!usable()) return this;
        return spentTicks == 19 ? new MantleEnergy(energy - 1, false, 0)
                : new MantleEnergy(energy, false, spentTicks + 1);
    }

    public boolean canImpulse() { return usable(); }

    public MantleEnergy impulse() {
        return canImpulse() ? new MantleEnergy(energy - 10, false, spentTicks) : this;
    }

    public MantleEnergy recharge() {
        return new MantleEnergy(energy + (recovering ? 10 : 2), recovering, spentTicks);
    }
}
