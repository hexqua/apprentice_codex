package jp.aquafactory.apprenticecodex.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import jp.aquafactory.apprenticecodex.enchantment.PlunderLootingEvent;
import net.minecraft.core.Holder;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.functions.EnchantedCountIncreaseFunction;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(EnchantedCountIncreaseFunction.class)
public abstract class EnchantedCountIncreasePlunderMixin {
    @Shadow @Final
    private Holder<Enchantment> enchantment;

    // 死亡とドロップのイベントは対にならないため、実物へ一時付与せず計算内だけを補正する。
    @ModifyExpressionValue(method = "run", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/storage/loot/LootContext;getParamOrNull(Lnet/minecraft/world/level/storage/loot/parameters/LootContextParam;)Ljava/lang/Object;"))
    private Object apprenticecodex$resolvePlunderAttacker(Object original, ItemStack stack, LootContext context) {
        return PlunderLootingEvent.resolveLootingAttacker(original, enchantment, context);
    }

    // 戻り値の補正にとどめ、Curios等による後続の加算とloot tableの上限・確率式を維持する。
    @ModifyExpressionValue(method = "run", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/item/enchantment/EnchantmentHelper;getEnchantmentLevel(Lnet/minecraft/core/Holder;Lnet/minecraft/world/entity/LivingEntity;)I"))
    private int apprenticecodex$applyPlunderLevel(int original, ItemStack stack, LootContext context) {
        return PlunderLootingEvent.applyLootingLevel(original, enchantment, context);
    }
}
