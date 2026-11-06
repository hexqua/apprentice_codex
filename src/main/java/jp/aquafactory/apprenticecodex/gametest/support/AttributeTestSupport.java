package jp.aquafactory.apprenticecodex.gametest.support;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import java.util.Collection;
import java.util.stream.Collectors;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.utility.MagicTools;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

public final class AttributeTestSupport {
    private AttributeTestSupport() {}

    public static final ResourceLocation VANILLA_BASE_ATTACK_DAMAGE_MODIFIER_ID = Item.BASE_ATTACK_DAMAGE_ID;

    public static final ResourceLocation VANILLA_BASE_ATTACK_SPEED_MODIFIER_ID = Item.BASE_ATTACK_SPEED_ID;

    public static void assertBaseAttackModifier(
            GameTestHelper helper,
            ResourceLocation itemId,
            Item item,
            Holder<Attribute> attribute,
            ResourceLocation expectedModifierId
    ) {
        var stack = new ItemStack(item);
        var modifiers = item.getDefaultAttributeModifiers(stack).modifiers().stream()
                .filter(entry -> entry.attribute().equals(attribute) && entry.slot().equals(EquipmentSlotGroup.MAINHAND))
                .toList();
        helper.assertTrue(modifiers.size() == 1,
                "Expected exactly one " + attribute.unwrapKey().map(key -> key.location()).orElse(ResourceLocation.withDefaultNamespace("unknown"))
                        + " modifier on " + itemId + " but got " + modifiers.size());
        helper.assertTrue(modifiers.getFirst().modifier().id().equals(expectedModifierId),
                "Unexpected modifier id for " + itemId + " / "
                        + attribute.unwrapKey().map(key -> key.location()).orElse(ResourceLocation.withDefaultNamespace("unknown"))
                        + ": " + modifiers.getFirst().modifier().id());
    }

    public static void assertModifierAmount(
            GameTestHelper helper,
            ItemAttributeModifiers modifiers,
            Attribute attribute,
            double expectedAmount,
            AttributeModifier.Operation operation,
            String message
    ) {
        assertModifierAmount(helper, modifiers, attribute, EquipmentSlotGroup.OFFHAND, expectedAmount, operation, message);
    }

    public static void assertModifierAmount(
            GameTestHelper helper,
            ItemAttributeModifiers modifiers,
            Attribute attribute,
            EquipmentSlotGroup slotGroup,
            double expectedAmount,
            AttributeModifier.Operation operation,
            String message
    ) {
        var actualAmount = modifiers.modifiers().stream()
                .filter(entry -> entry.slot().equals(slotGroup))
                .filter(entry -> entry.attribute().value() == attribute)
                .filter(entry -> entry.modifier().operation() == operation)
                .mapToDouble(entry -> entry.modifier().amount())
                .sum();
        helper.assertTrue(Math.abs(actualAmount - expectedAmount) < 1.0e-9D,
                message + ": expected " + expectedAmount + " but got " + actualAmount);
    }

    public static void assertCurioModifierAmount(
            GameTestHelper helper,
            ICurioItem item,
            SlotContext slotContext,
            ItemStack stack,
            Holder<Attribute> attribute,
            double expectedAmount,
            AttributeModifier.Operation operation,
            String message
    ) {
        var slotId = ResourceLocation.fromNamespaceAndPath(
                ApprenticeCodex.MODID,
                "gametest/curio/%s_%d".formatted(slotContext.identifier(), slotContext.index())
        );
        var modifiers = item.getAttributeModifiers(slotContext, slotId, stack);
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
                .filter(modifier -> modifier.operation() == operation)
                .mapToDouble(AttributeModifier::amount)
                .sum();
    }

    public static String describeModifiers(Multimap<Holder<Attribute>, AttributeModifier> modifiers) {
        return modifiers.entries().stream()
                .map(entry -> BuiltInRegistries.ATTRIBUTE.getKey(entry.getKey().value()) + "="
                        + entry.getValue().amount() + "@" + entry.getValue().operation())
                .collect(Collectors.joining(", "));
    }

    public static Multimap<Holder<Attribute>, AttributeModifier> toModifierMultimap(
            ItemAttributeModifiers modifiers
    ) {
        var builder = ImmutableMultimap.<Holder<Attribute>, AttributeModifier>builder();
        for (var entry : modifiers.modifiers()) {
            builder.put(entry.attribute(), entry.modifier());
        }
        return builder.build();
    }

    public static void assertSingleModifierAmount(
            GameTestHelper helper,
            Collection<AttributeModifier> modifiers,
            AttributeModifier.Operation operation,
            double expectedAmount,
            String message
    ) {
        var matchingModifiers = modifiers.stream()
                .filter(modifier -> modifier.operation() == operation)
                .toList();
        helper.assertTrue(matchingModifiers.size() == 1,
                message + ": expected exactly one " + operation + " modifier but got " + matchingModifiers);
        var actualAmount = matchingModifiers.get(0).amount();
        helper.assertTrue(Math.abs(actualAmount - expectedAmount) < 1.0e-9D,
                message + ": expected " + expectedAmount + " but got " + actualAmount);
    }

    public static void assertModifierWithId(
            GameTestHelper helper,
            Collection<AttributeModifier> modifiers,
            ResourceLocation expectedId,
            AttributeModifier.Operation operation,
            double expectedAmount,
            String message
    ) {
        var matchingModifier = modifiers.stream()
                .filter(modifier -> expectedId.equals(modifier.id()))
                .findFirst();
        helper.assertTrue(matchingModifier.isPresent(),
                message + ": missing modifier " + expectedId + " in " + modifiers);
        var modifier = matchingModifier.get();
        helper.assertTrue(modifier.operation() == operation
                        && Math.abs(modifier.amount() - expectedAmount) < 1.0e-9D,
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
        var modifiers = toModifierMultimap(stack.getItem().getDefaultAttributeModifiers(stack));
        var actualAmount = sumModifierAmount(
                modifiers.get(BuiltInRegistries.ATTRIBUTE.wrapAsHolder(attribute)),
                AttributeModifier.Operation.ADD_MULTIPLIED_BASE
        );
        helper.assertTrue(Math.abs(actualAmount - expectedAmount) < 1.0e-9D,
                message + ": expected=" + expectedAmount
                        + ", actual=" + actualAmount
                        + ", modifiers=" + describeModifiers(modifiers));
    }
}
