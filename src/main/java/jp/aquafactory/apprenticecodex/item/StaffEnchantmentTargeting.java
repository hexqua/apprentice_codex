package jp.aquafactory.apprenticecodex.item;

import jp.aquafactory.apprenticecodex.compat.malum.MalumHauntedCompat;
import jp.aquafactory.apprenticecodex.enchantment.WisdomPolicy;
import jp.aquafactory.apprenticecodex.registry.EnchantmentRegistry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraftforge.registries.ForgeRegistries;

public final class StaffEnchantmentTargeting {
    private static final String MALUM_NAMESPACE = "malum";
    private static final ResourceLocation MALUM_REPLENISHING =
            ResourceLocation.fromNamespaceAndPath(MALUM_NAMESPACE, "replenishing");
    private static final ItemStack DURABILITY_ENCHANTMENT_PROBE_STACK = new ItemStack(Items.ELYTRA);
    private static final ItemStack SWORD_ENCHANTMENT_PROBE_STACK = new ItemStack(Items.DIAMOND_SWORD);

    private StaffEnchantmentTargeting() {
    }

    public static boolean canApplyAtEnchantingTable(ItemStack stack, Enchantment enchantment) {
        var enchantmentId = ForgeRegistries.ENCHANTMENTS.getKey(enchantment);
        if (enchantmentId == null) {
            return false;
        }

        if (isDurabilityTargetEnchantment(enchantment)) {
            return false;
        }

        // Malum 1.20.1 の Animated は大鎌系前提のため staff では拒否する。
        // 1.21.1 では大鎌限定ではなくなるため、forward-port 時は許容へ戻すこと。
        if (MalumHauntedCompat.isAnimatedEnchantment(enchantmentId) || MALUM_REPLENISHING.equals(enchantmentId)) {
            return false;
        }

        if (MalumHauntedCompat.isHauntedEnchantment(enchantmentId)) {
            return MalumHauntedCompat.isSupportedHauntedMainhandItem(stack);
        }

        if (EnchantmentRegistry.WISDOM.isPresent() && enchantment == EnchantmentRegistry.WISDOM.get()) {
            return WisdomPolicy.supportsDirectApplication(stack.getItem());
        }

        // Forge の canApplyAtEnchantingTable(stack) はアイテム側へ戻って再帰するため、
        // Spirit Plunder など、タグで staff を対象に加える外部エンチャントはカテゴリ判定を直接尊重する。
        if (enchantment.category.canEnchant(stack.getItem())) {
            return true;
        }

        return enchantment.canApplyAtEnchantingTable(SWORD_ENCHANTMENT_PROBE_STACK);
    }

    private static boolean isDurabilityTargetEnchantment(Enchantment enchantment) {
        // エリトラは耐久値を持つが武器/ツール系カテゴリではないため、
        // ここに付くエンチャントを「耐久値持ちアイテム向け」とみなして除外する。
        return enchantment.canApplyAtEnchantingTable(DURABILITY_ENCHANTMENT_PROBE_STACK);
    }
}
