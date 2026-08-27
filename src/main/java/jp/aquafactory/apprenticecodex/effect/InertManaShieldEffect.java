package jp.aquafactory.apprenticecodex.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public final class InertManaShieldEffect extends MobEffect {
    public InertManaShieldEffect() {
        super(MobEffectCategory.HARMFUL, 0x2B0B4F);
    }

    @Override
    public List<ItemStack> getCurativeItems() {
        // Mana Shieldへの対抗状態を牛乳で即座に解除できないよう、標準のcureを意図的に登録しない.
        return List.of();
    }
}
