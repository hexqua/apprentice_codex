package jp.aquafactory.apprenticecodex.gametest.support;

import com.google.common.collect.Multimap;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import jp.aquafactory.apprenticecodex.item.offhand.AbstractOffhandMagicItem;
import jp.aquafactory.apprenticecodex.utility.MagicTools;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

import java.util.Collection;
import java.util.UUID;
import java.util.stream.Collectors;

public final class AttributeTestSupport {
    private AttributeTestSupport() {}

    public static final UUID VANILLA_BASE_ATTACK_DAMAGE_MODIFIER_ID =
            UUID.fromString("CB3F55D3-645C-4F38-A497-9C13A33DB5CF");

    public static final UUID VANILLA_BASE_ATTACK_SPEED_MODIFIER_ID =
            UUID.fromString("FA233E1C-4180-4865-B01B-BCCE9785ACA3");

    public static void assertModifierAmount(
            GameTestHelper helper,
            AbstractOffhandMagicItem item,
            ItemStack stack,
            Attribute attribute,
            double expectedAmount,
            AttributeModifier.Operation operation,
            String message
    ) {
        var actualAmount = sumModifierAmount(
                item.getAttributeModifiers(EquipmentSlot.OFFHAND, stack).get(attribute),
                operation
        );
        helper.assertTrue(Math.abs(actualAmount - expectedAmount) < 1.0e-9D,
                message + ": expected stacked amount " + expectedAmount + " but got " + actualAmount
                        + " modifiers=" + describeModifiers(item.getAttributeModifiers(EquipmentSlot.OFFHAND, stack)));
    }

    public static void assertCurioModifierAmount(
            GameTestHelper helper,
            ICurioItem item,
            SlotContext slotContext,
            ItemStack stack,
            Attribute attribute,
            double expectedAmount,
            AttributeModifier.Operation operation,
            String message
    ) {
        var modifiers = item.getAttributeModifiers(slotContext, UUID.randomUUID(), stack);
        var actualAmount = sumModifierAmount(modifiers.get(attribute), operation);
        helper.assertTrue(Math.abs(actualAmount - expectedAmount) < 1.0e-9D,
                message + ": expected stacked amount " + expectedAmount + " but got " + actualAmount
                        + " modifiers=" + describeModifiers(modifiers));
    }

    public static double sumModifierAmount(
            Collection<AttributeModifier> modifiers,
            AttributeModifier.Operation operation
    ) {
        return modifiers.stream()
                .filter(modifier -> modifier.getOperation() == operation)
                .mapToDouble(AttributeModifier::getAmount)
                .sum();
    }

    public static void assertSingleModifierAmount(
            GameTestHelper helper,
            Collection<AttributeModifier> modifiers,
            AttributeModifier.Operation operation,
            double expectedAmount,
            String message
    ) {
        var matchingModifiers = modifiers.stream()
                .filter(modifier -> modifier.getOperation() == operation)
                .toList();
        helper.assertTrue(matchingModifiers.size() == 1,
                message + ": expected exactly one " + operation + " modifier but got " + matchingModifiers);
        var actualAmount = matchingModifiers.get(0).getAmount();
        helper.assertTrue(Math.abs(actualAmount - expectedAmount) < 1.0e-9D,
                message + ": expected " + expectedAmount + " but got " + actualAmount);
    }

    public static void assertModifierWithId(
            GameTestHelper helper,
            Collection<AttributeModifier> modifiers,
            UUID expectedId,
            AttributeModifier.Operation operation,
            double expectedAmount,
            String message
    ) {
        var matchingModifier = modifiers.stream()
                .filter(modifier -> expectedId.equals(modifier.getId()))
                .findFirst();
        helper.assertTrue(matchingModifier.isPresent(),
                message + ": missing modifier " + expectedId + " in " + modifiers);
        var modifier = matchingModifier.get();
        helper.assertTrue(modifier.getOperation() == operation
                        && Math.abs(modifier.getAmount() - expectedAmount) < 1.0e-9D,
                message + ": expected " + operation + " " + expectedAmount + " but got " + modifier);
    }

    public static void assertSchoolSpellPowerBonus(
            GameTestHelper helper,
            ItemStack stack,
            EquipmentSlot slot,
            AbstractSpell spell,
            double expectedAmount,
            String message
    ) {
        var attribute = MagicTools.resolveSchoolPowerAttribute(spell.getSchoolType());
        helper.assertTrue(attribute != null, "Could not resolve school spell power attribute for " + spell.getSpellId());
        var actualAmount = sumModifierAmount(
                stack.getAttributeModifiers(slot).get(attribute),
                AttributeModifier.Operation.MULTIPLY_BASE
        );
        helper.assertTrue(Math.abs(actualAmount - expectedAmount) < 1.0e-9D,
                message + ": expected=" + expectedAmount
                        + ", actual=" + actualAmount
                        + ", modifiers=" + describeModifiers(stack.getAttributeModifiers(slot)));
    }

    public static String describeModifiers(Multimap<Attribute, AttributeModifier> modifiers) {
        return modifiers.entries().stream()
                .map(entry -> ForgeRegistries.ATTRIBUTES.getKey(entry.getKey()) + "="
                        + entry.getValue().getAmount() + "@" + entry.getValue().getOperation())
                .collect(Collectors.joining(", "));
    }
}
