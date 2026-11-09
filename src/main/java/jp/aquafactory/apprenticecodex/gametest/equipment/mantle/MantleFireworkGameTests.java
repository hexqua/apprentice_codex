package jp.aquafactory.apprenticecodex.gametest.equipment.mantle;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

final class MantleFireworkGameTests {
    private MantleFireworkGameTests() {}

    static ItemStack rocket(int flight, int count, boolean explosion) {
        var stack = new ItemStack(Items.FIREWORK_ROCKET, count);
        var fireworks = new CompoundTag();
        fireworks.putByte("Flight", (byte) flight);
        if (explosion) {
            var explosions = new ListTag();
            explosions.add(new CompoundTag());
            fireworks.put("Explosions", explosions);
        }
        stack.getOrCreateTag().put("Fireworks", fireworks);
        return stack;
    }
}
