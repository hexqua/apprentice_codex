package jp.aquafactory.apprenticecodex.gametest.support;

import jp.aquafactory.apprenticecodex.config.ApprenticeCodexServerConfig;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.CombatRules;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;

public final class DamageTestSupport {
    private DamageTestSupport() {}

    public static void setMaxHealthForDamageTest(LivingEntity target, float health) {
        target.getAttribute(Attributes.MAX_HEALTH).setBaseValue(health);
        target.setHealth(health);
    }

    public static float getEquippedAttributeTotal(Player player, Attribute attribute) {
        var total = 0.0F;
        for (var slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            var stack = player.getItemBySlot(slot);
            if (stack.isEmpty()) {
                continue;
            }

            total += (float) stack.getAttributeModifiers(slot).get(attribute).stream()
                    .filter(modifier -> modifier.getOperation() == AttributeModifier.Operation.ADDITION)
                    .mapToDouble(AttributeModifier::getAmount)
                    .sum();
        }
        return total;
    }

    static void equipProtectionIvIronArmor(ServerPlayer player) {
        for (var slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            var armorStack = switch (slot) {
                case HEAD -> new ItemStack(Items.IRON_HELMET);
                case CHEST -> new ItemStack(Items.IRON_CHESTPLATE);
                case LEGS -> new ItemStack(Items.IRON_LEGGINGS);
                case FEET -> new ItemStack(Items.IRON_BOOTS);
                default -> ItemStack.EMPTY;
            };
            armorStack.enchant(Enchantments.ALL_DAMAGE_PROTECTION, 4);
            player.setItemSlot(slot, armorStack);
        }
    }

    static float findDamageForArmorReducedTarget(float armor, float toughness, float targetReducedDamage) {
        var low = 0.0F;
        var high = Math.max(targetReducedDamage * 2.0F, 1.0F);
        while (CombatRules.getDamageAfterAbsorb(high, armor, toughness) < targetReducedDamage) {
            high *= 2.0F;
        }

        for (var iteration = 0; iteration < 40; ++iteration) {
            var mid = (low + high) * 0.5F;
            var reducedDamage = CombatRules.getDamageAfterAbsorb(mid, armor, toughness);
            if (reducedDamage < targetReducedDamage) {
                low = mid;
            } else {
                high = mid;
            }
        }
        return high;
    }

    static float findDamageForMagicReducedTarget(int protection, float targetReducedDamage) {
        var low = 0.0F;
        var high = Math.max(targetReducedDamage * 2.0F, 1.0F);
        while (CombatRules.getDamageAfterMagicAbsorb(high, protection) < targetReducedDamage) {
            high *= 2.0F;
        }

        for (var iteration = 0; iteration < 40; ++iteration) {
            var mid = (low + high) * 0.5F;
            var reducedDamage = CombatRules.getDamageAfterMagicAbsorb(mid, protection);
            if (reducedDamage < targetReducedDamage) {
                low = mid;
            } else {
                high = mid;
            }
        }
        return high;
    }

    public static float resolveExpectedBarrierManaAfterHitForGameTest(float incomingDamage, float availableMana) {
        var cost = (float) Math.ceil(Math.max(incomingDamage, 0.0F))
                * ApprenticeCodexServerConfig.manaShieldCharmManaPerDamage();
        return Math.max(availableMana - cost, 0.0F);
    }
}
