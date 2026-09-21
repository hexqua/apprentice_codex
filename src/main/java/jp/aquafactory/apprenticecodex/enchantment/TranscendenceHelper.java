package jp.aquafactory.apprenticecodex.enchantment;

import net.minecraft.world.item.ItemStack;
import jp.aquafactory.apprenticecodex.registry.EnchantmentRegistry;

public final class TranscendenceHelper {
    private TranscendenceHelper() {
    }

    /** 補正済みの値ではなく格納スクロールの元レベルを渡し、保存データは変更しない。 */
    public static int resolveScrollSpellLevel(ItemStack source, int originalLevel) {
        if (!TranscendenceTarget.supportsDirectApplication(source.getItem())
                || source.getEnchantmentLevel(EnchantmentRegistry.TRANSCENDENCE.get()) <= 0) {
            return originalLevel;
        }
        // 旧 III や外部 MOD の高レベル付与も固定 +1 とし、魔法の通常上限は突破する。
        return originalLevel + 1;
    }
}
