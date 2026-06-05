package jp.aquafactory.apprenticecodex.item;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.spell.boundsword.BoundSwordClientTooltip;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.ItemAbilities;
import net.neoforged.neoforge.common.ItemAbility;
import net.neoforged.api.distmarker.Dist;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class BoundSwordItem extends SwordItem {
    public static final int DURABILITY = 1561;
    public static final double ATTACK_SPEED_MODIFIER_AMOUNT = -2.0D;
    private static final ItemStack SWORD_ENCHANTMENT_PROBE_STACK = new ItemStack(net.minecraft.world.item.Items.GOLDEN_SWORD);
    public static final String INSTANCE_ID_TAG = "apprenticecodex:bound_sword_instance_id";
    public static final String DISPLAY_DAMAGE_TAG = "apprenticecodex:bound_sword_display_damage";
    public static final String EQUIPMENT_SLOT_TAG = "apprenticecodex:bound_sword_equipment_slot";
    public static final String OFFHAND_SLOT_VALUE = "offhand";

    public BoundSwordItem() {
        super(Tiers.GOLD, new Item.Properties()
                .stacksTo(1)
                .durability(DURABILITY)
                .rarity(Rarity.RARE)
                .attributes(SwordItem.createAttributes(Tiers.GOLD, 0, (float) ATTACK_SPEED_MODIFIER_AMOUNT)));
    }

    public static ItemStack create(UUID instanceId, float displayDamage) {
        return create(instanceId, displayDamage, EquipmentSlot.MAINHAND);
    }

    public static ItemStack create(UUID instanceId, float displayDamage, EquipmentSlot equipmentSlot) {
        var stack = new ItemStack(jp.aquafactory.apprenticecodex.registry.ItemRegistry.BOUND_SWORD.get());
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
            tag.putUUID(INSTANCE_ID_TAG, instanceId);
            tag.putFloat(DISPLAY_DAMAGE_TAG, displayDamage);
            if (equipmentSlot == EquipmentSlot.OFFHAND) {
                tag.putString(EQUIPMENT_SLOT_TAG, OFFHAND_SLOT_VALUE);
            }
        });
        stack.set(DataComponents.ATTRIBUTE_MODIFIERS, createAttributeModifiers(displayDamage));
        return stack;
    }

    public static boolean isBoundSword(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() instanceof BoundSwordItem;
    }

    public static boolean isGeneratedBoundSword(ItemStack stack) {
        return isBoundSword(stack) && getInstanceId(stack).isPresent();
    }

    public static Optional<UUID> getInstanceId(ItemStack stack) {
        if (!isBoundSword(stack)) {
            return Optional.empty();
        }
        CompoundTag tag = getCustomDataTag(stack);
        return tag != null && tag.hasUUID(INSTANCE_ID_TAG)
                ? Optional.of(tag.getUUID(INSTANCE_ID_TAG))
                : Optional.empty();
    }

    public static boolean hasInstanceId(ItemStack stack, @Nullable UUID instanceId) {
        if (instanceId == null) {
            return false;
        }
        return getInstanceId(stack).map(instanceId::equals).orElse(false);
    }

    public static float getDisplayDamage(ItemStack stack) {
        var tag = getCustomDataTag(stack);
        return tag == null ? 0.0F : tag.getFloat(DISPLAY_DAMAGE_TAG);
    }

    public static boolean isGeneratedForOffhand(ItemStack stack) {
        var tag = getCustomDataTag(stack);
        return tag != null && OFFHAND_SLOT_VALUE.equals(tag.getString(EQUIPMENT_SLOT_TAG));
    }

    @Override
    public ItemAttributeModifiers getDefaultAttributeModifiers(ItemStack stack) {
        return createAttributeModifiers(getDisplayDamage(stack));
    }

    private static ItemAttributeModifiers createAttributeModifiers(float displayDamage) {
        var builder = ItemAttributeModifiers.builder();
        builder.add(
                Attributes.ATTACK_DAMAGE,
                new AttributeModifier(
                        Item.BASE_ATTACK_DAMAGE_ID,
                        Math.max(0.0F, displayDamage - 1.0F),
                        AttributeModifier.Operation.ADD_VALUE
                ),
                EquipmentSlotGroup.MAINHAND
        );
        builder.add(
                Attributes.ATTACK_SPEED,
                new AttributeModifier(
                        Item.BASE_ATTACK_SPEED_ID,
                        ATTACK_SPEED_MODIFIER_AMOUNT,
                        AttributeModifier.Operation.ADD_VALUE
                ),
                EquipmentSlotGroup.MAINHAND
        );
        return builder.build();
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, Item.@NotNull TooltipContext context,
                                @NotNull List<Component> lines, @NotNull TooltipFlag flag) {
        super.appendHoverText(stack, context, lines, flag);

        if (FMLEnvironment.dist == Dist.CLIENT) {
            BoundSwordClientTooltip.getStoredItemName(stack).ifPresent(storedItemName -> {
                lines.add(Component.translatable(
                        "item." + ApprenticeCodex.MODID + ".bound_weapon.contain_item.item",
                        storedItemName
                ).withStyle(ChatFormatting.GRAY));
                lines.add(Component.translatable(
                        "item." + ApprenticeCodex.MODID + ".bound_weapon.contain_item.hint"
                ).withStyle(ChatFormatting.DARK_GRAY));
            });
        }
    }

    @Override
    public boolean isEnchantable(@NotNull ItemStack stack) {
        return true;
    }

    @Override
    public boolean supportsEnchantment(ItemStack stack, Holder<Enchantment> enchantment) {
        return super.supportsEnchantment(stack, enchantment)
                || SWORD_ENCHANTMENT_PROBE_STACK.supportsEnchantment(enchantment);
    }

    @Override
    public boolean isPrimaryItemFor(ItemStack stack, Holder<Enchantment> enchantment) {
        return super.isPrimaryItemFor(stack, enchantment) || supportsEnchantment(stack, enchantment);
    }

    @Override
    public boolean isBookEnchantable(ItemStack stack, ItemStack book) {
        if (!super.isBookEnchantable(stack, book)) {
            return false;
        }

        var enchantments = EnchantmentHelper.getEnchantmentsForCrafting(book);
        return enchantments.isEmpty() || enchantments.keySet().stream()
                .allMatch(enchantment -> supportsEnchantment(stack, enchantment));
    }

    @Override
    public boolean isFoil(@NotNull ItemStack stack) {
        return true;
    }

    @Override
    public boolean canPerformAction(@NotNull ItemStack stack, @NotNull ItemAbility itemAbility) {
        return itemAbility == ItemAbilities.SWORD_SWEEP || super.canPerformAction(stack, itemAbility);
    }

    private static @Nullable CompoundTag getCustomDataTag(ItemStack stack) {
        var customData = stack.get(DataComponents.CUSTOM_DATA);
        return customData == null ? null : customData.copyTag();
    }
}
