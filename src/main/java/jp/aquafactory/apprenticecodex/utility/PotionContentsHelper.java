package jp.aquafactory.apprenticecodex.utility;

import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionContents;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public final class PotionContentsHelper {
    private PotionContentsHelper() {
    }

    @Nullable
    public static Potion getPotion(@Nullable ItemStack stack) {
        return getPotionHolder(stack).map(Holder::value).orElse(null);
    }

    public static List<MobEffectInstance> getMobEffects(@Nullable ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return List.of();
        }

        var contents = stack.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY);
        if (!contents.hasEffects()) {
            return List.of();
        }

        var effects = new ArrayList<MobEffectInstance>();
        contents.forEachEffect(effects::add);
        return effects;
    }

    public static void setPotion(ItemStack stack, Potion potion) {
        stack.set(DataComponents.POTION_CONTENTS, new PotionContents(wrap(potion)));
    }

    public static ItemStack createPotionStack(Item item, Potion potion) {
        return PotionContents.createItemStack(item, wrap(potion));
    }

    private static java.util.Optional<Holder<Potion>> getPotionHolder(@Nullable ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return java.util.Optional.empty();
        }

        return stack.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY).potion();
    }

    private static Holder<Potion> wrap(Potion potion) {
        return BuiltInRegistries.POTION.wrapAsHolder(potion);
    }
}
