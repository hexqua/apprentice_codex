package jp.aquafactory.apprenticecodex.mixin;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.utility.ApprenticeEnchantmentAvailability;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.EnchantedBookItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.functions.EnchantRandomlyFunction;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(EnchantRandomlyFunction.class)
public abstract class EnchantRandomlyFunctionMixin {
    @Shadow
    @Final
    private List<Enchantment> enchantments;

    @Inject(method = "run", at = @At("HEAD"), cancellable = true)
    private void apprenticecodex$excludeFlaskEnchantmentsFromRandomBooks(
            ItemStack stack,
            LootContext lootContext,
            CallbackInfoReturnable<ItemStack> cir
    ) {
        if (!enchantments.isEmpty() || !stack.is(Items.BOOK)) {
            return;
        }

        // isDiscoverable を落とすとエンチャント台候補まで消えるため、
        // loot のランダム本経路だけで除外対象エンチャントを弾く。
        var availableEnchantments = BuiltInRegistries.ENCHANTMENT.stream()
                .filter(Enchantment::isDiscoverable)
                .filter(enchantment -> !ApprenticeEnchantmentAvailability.isExcludedFromRandomBookLoot(enchantment))
                .toList();
        if (availableEnchantments.isEmpty()) {
            ApprenticeCodex.LOGGER.warn("Couldn't find a compatible enchantment for {}", stack);
            cir.setReturnValue(stack);
            return;
        }

        var random = lootContext.getRandom();
        var enchantment = availableEnchantments.get(random.nextInt(availableEnchantments.size()));
        cir.setReturnValue(apprenticecodex$createRandomEnchantedBook(enchantment, random));
    }

    @Unique
    private static ItemStack apprenticecodex$createRandomEnchantedBook(Enchantment enchantment, RandomSource random) {
        var level = Mth.nextInt(random, enchantment.getMinLevel(), enchantment.getMaxLevel());
        var result = new ItemStack(Items.ENCHANTED_BOOK);
        EnchantedBookItem.addEnchantment(result, new EnchantmentInstance(enchantment, level));
        return result;
    }
}
