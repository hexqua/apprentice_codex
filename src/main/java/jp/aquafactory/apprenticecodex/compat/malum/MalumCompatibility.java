package jp.aquafactory.apprenticecodex.compat.malum;

import jp.aquafactory.apprenticecodex.item.AbstractRightClickMagicWeaponItem;
import jp.aquafactory.apprenticecodex.item.ChargedTwinBladeStaff;
import jp.aquafactory.apprenticecodex.item.CircuitHeatStaff;
import jp.aquafactory.apprenticecodex.item.CrystalBladedStaff;
import jp.aquafactory.apprenticecodex.item.ManaForceBlade;
import jp.aquafactory.apprenticecodex.item.PastelStaff;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

public final class MalumCompatibility {
    public static final String MOD_ID = "malum";
    public static final ResourceLocation SPIRIT_PLUNDER =
            ResourceLocation.fromNamespaceAndPath(MOD_ID, "spirit_plunder");
    public static final ResourceLocation HAUNTED =
            ResourceLocation.fromNamespaceAndPath(MOD_ID, "haunted");
    public static final ResourceLocation ANIMATED =
            ResourceLocation.fromNamespaceAndPath(MOD_ID, "animated");
    public static final TagKey<Item> SOUL_HUNTER_WEAPON = TagKey.create(
            Registries.ITEM,
            ResourceLocation.fromNamespaceAndPath(MOD_ID, "soul_hunter_weapon")
    );
    public static final TagKey<Item> MAGIC_CAPABLE_WEAPON = TagKey.create(
            Registries.ITEM,
            ResourceLocation.fromNamespaceAndPath(MOD_ID, "magic_capable_weapon")
    );

    private static final Set<ResourceLocation> MAGIC_CAPABLE_WEAPON_ENCHANTMENTS = Set.of(HAUNTED, ANIMATED);

    private MalumCompatibility() {
    }

    public static boolean isSpiritPlunderSupported(ItemStack stack, @Nullable ResourceLocation enchantmentId) {
        return enchantmentId != null && SPIRIT_PLUNDER.equals(enchantmentId) && stack.is(SOUL_HUNTER_WEAPON);
    }

    public static boolean isMagicCapableWeaponEnchantment(ItemStack stack, @Nullable ResourceLocation enchantmentId) {
        return enchantmentId != null
                && MAGIC_CAPABLE_WEAPON_ENCHANTMENTS.contains(enchantmentId)
                && stack.is(MAGIC_CAPABLE_WEAPON);
    }

    public static boolean isHauntedCompatibleWeapon(ItemStack stack) {
        return !stack.isEmpty() && isHauntedCompatibleWeapon(stack.getItem());
    }

    public static boolean isHauntedCompatibleWeapon(Item item) {
        return item instanceof AbstractRightClickMagicWeaponItem
                || item instanceof PastelStaff
                || item instanceof CrystalBladedStaff
                || item instanceof CircuitHeatStaff
                || item instanceof ChargedTwinBladeStaff
                || item instanceof ManaForceBlade;
    }

    public static int getHauntedLevel(ItemStack stack) {
        if (!isHauntedCompatibleWeapon(stack)) {
            return 0;
        }

        return getEnchantmentLevel(stack, HAUNTED);
    }

    public static int getEnchantmentLevel(ItemStack stack, ResourceLocation enchantmentId) {
        if (stack == null || stack.isEmpty()) {
            return 0;
        }

        var enchantments = EnchantmentHelper.getEnchantmentsForCrafting(stack);
        if (enchantments.isEmpty()) {
            return 0;
        }

        for (var enchantment : enchantments.keySet()) {
            var enchantmentKey = enchantment.unwrapKey().orElse(null);
            if (enchantmentKey != null && enchantmentId.equals(enchantmentKey.location())) {
                return enchantments.getLevel(enchantment);
            }
        }

        return 0;
    }
}
