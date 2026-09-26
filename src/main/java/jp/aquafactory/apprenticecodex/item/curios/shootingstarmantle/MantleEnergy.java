package jp.aquafactory.apprenticecodex.item.curios.shootingstarmantle;

import io.redspace.ironsspellbooks.registries.ItemRegistry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

public record MantleEnergy(int energy, boolean recovering, int spentTicks, int maxEnergy) {
    public static final int MAX = 100;
    public static final int RUNE_MAX = 200;
    private static final String KEY = "apprenticecodex_shooting_star_mantle";

    public MantleEnergy(int energy, boolean recovering, int spentTicks) {
        this(energy, recovering, spentTicks, MAX);
    }

    public MantleEnergy {
        maxEnergy = maxEnergy == RUNE_MAX ? RUNE_MAX : MAX;
        energy = Mth.clamp(energy, 0, maxEnergy);
        recovering = energy == 0 || energy < maxEnergy && recovering;
        spentTicks = Mth.clamp(spentTicks, 0, 39);
    }

    public static int maxEnergy(ItemStack stack) {
        return MantleCalibration.hasAdjustment(stack, ItemRegistry.MANA_RUNE.get())
                ? RUNE_MAX : MAX;
    }

    public static MantleEnergy read(ItemStack stack) {
        var root = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        int maxEnergy = maxEnergy(stack);
        // 未使用品にルーンを入れても、保存されていなかった100を現在量として引き継ぐ。
        if (!root.contains(KEY, Tag.TAG_COMPOUND)) return new MantleEnergy(MAX, false, 0, maxEnergy);
        var tag = root.getCompound(KEY);
        return new MantleEnergy(tag.getInt("energy"), tag.getBoolean("recovering"), tag.getInt("spent_ticks"), maxEnergy);
    }

    public void save(ItemStack stack) {
        var bounded = new MantleEnergy(energy, recovering, spentTicks, maxEnergy(stack));
        CustomData.update(DataComponents.CUSTOM_DATA, stack, root -> {
            var tag = new CompoundTag();
            tag.putInt("energy", bounded.energy);
            tag.putBoolean("recovering", bounded.recovering);
            tag.putInt("spent_ticks", bounded.spentTicks);
            root.put(KEY, tag);
        });
    }

    public boolean usable() { return energy > 0 && !recovering; }

    public MantleEnergy tickUse() {
        return tickUse(2);
    }

    public MantleEnergy tickUse(int rate) {
        if (!usable()) return this;
        int spent = spentTicks + rate;
        return new MantleEnergy(energy - spent / 40, false, spent % 40, maxEnergy);
    }

    public boolean canImpulse() { return usable(); }

    public MantleEnergy impulse() {
        return canImpulse() ? spend(10) : this;
    }

    public MantleEnergy spend(int cost) {
        return new MantleEnergy(energy - Math.max(0, cost), recovering, spentTicks, maxEnergy);
    }

    public MantleEnergy recharge() {
        return recharge(false);
    }

    public MantleEnergy recharge(boolean fastRecovery) {
        // 枯渇ロックは回復量と独立させ、満充電まで再使用を禁止する。
        return new MantleEnergy(energy + (fastRecovery ? 15 : 10), recovering, spentTicks, maxEnergy);
    }
}
