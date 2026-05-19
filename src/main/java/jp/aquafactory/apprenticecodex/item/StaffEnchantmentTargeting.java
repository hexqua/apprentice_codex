package jp.aquafactory.apprenticecodex.item;

import jp.aquafactory.apprenticecodex.compat.malum.MalumHauntedCompat;
import jp.aquafactory.apprenticecodex.registry.EnchantmentRegistry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Set;

final class StaffEnchantmentTargeting {
    private static final String VANILLA_NAMESPACE = "minecraft";
    private static final String MALUM_NAMESPACE = "malum";
    private static final ResourceLocation MALUM_SPIRIT_PLUNDER =
            ResourceLocation.fromNamespaceAndPath(MALUM_NAMESPACE, "spirit_plunder");
    private static final ResourceLocation MALUM_REPLENISHING =
            ResourceLocation.fromNamespaceAndPath(MALUM_NAMESPACE, "replenishing");
    private static final TagKey<Item> MALUM_SOUL_HUNTER_WEAPON = TagKey.create(
            net.minecraft.core.registries.Registries.ITEM,
            ResourceLocation.fromNamespaceAndPath(MALUM_NAMESPACE, "soul_hunter_weapon")
    );
    private static final ItemStack DURABILITY_ENCHANTMENT_PROBE_STACK = new ItemStack(Items.ELYTRA);
    private static final ItemStack SWORD_ENCHANTMENT_PROBE_STACK = new ItemStack(Items.DIAMOND_SWORD);
    private static final Set<ResourceLocation> ALLOWED_VANILLA_WEAPON_ENCHANTMENTS = Set.of(
            ResourceLocation.withDefaultNamespace("looting"),
            ResourceLocation.withDefaultNamespace("knockback"),
            ResourceLocation.withDefaultNamespace("fortune"),
            ResourceLocation.withDefaultNamespace("silk_touch")
    );

    private StaffEnchantmentTargeting() {
    }

    static boolean canApplyAtEnchantingTable(ItemStack stack, Enchantment enchantment) {
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

        if (MalumHauntedCompat.isHauntedEnchantment(enchantmentId)
                && MalumHauntedCompat.isSupportedHauntedMainhandItem(stack)) {
            return true;
        }

        if (MALUM_SPIRIT_PLUNDER.equals(enchantmentId) && stack.is(MALUM_SOUL_HUNTER_WEAPON)) {
            return true;
        }

        if (EnchantmentRegistry.WISDOM.isPresent() && enchantment == EnchantmentRegistry.WISDOM.get()) {
            return true;
        }

        if (VANILLA_NAMESPACE.equals(enchantmentId.getNamespace())) {
            return ALLOWED_VANILLA_WEAPON_ENCHANTMENTS.contains(enchantmentId);
        }

        return enchantment.canApplyAtEnchantingTable(SWORD_ENCHANTMENT_PROBE_STACK);
    }

    static boolean isDurabilityTargetEnchantment(Enchantment enchantment) {
        // エリトラは耐久値を持つが武器/ツール系カテゴリではないため、
        // ここに付くエンチャントを「耐久値持ちアイテム向け」とみなして除外する。
        return enchantment.canApplyAtEnchantingTable(DURABILITY_ENCHANTMENT_PROBE_STACK);
    }
}
