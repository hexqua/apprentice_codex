package jp.aquafactory.apprenticecodex.item;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.spell.boundsword.BoundSwordClientTooltip;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.ToolAction;
import net.minecraftforge.common.ToolActions;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class BoundSwordItem extends SwordItem {
    public static final int DURABILITY = 1561;
    public static final double ATTACK_SPEED_MODIFIER_AMOUNT = -2.0D;
    public static final String INSTANCE_ID_TAG = "apprenticecodex:bound_sword_instance_id";
    public static final String DISPLAY_DAMAGE_TAG = "apprenticecodex:bound_sword_display_damage";
    public static final String EQUIPMENT_SLOT_TAG = "apprenticecodex:bound_sword_equipment_slot";
    public static final String OFFHAND_SLOT_VALUE = "offhand";

    public BoundSwordItem() {
        super(Tiers.GOLD, 0, (float) ATTACK_SPEED_MODIFIER_AMOUNT,
                new Item.Properties().stacksTo(1).durability(DURABILITY).rarity(Rarity.RARE));
    }

    public static ItemStack create(UUID instanceId, float displayDamage) {
        return create(instanceId, displayDamage, EquipmentSlot.MAINHAND);
    }

    public static ItemStack create(UUID instanceId, float displayDamage, EquipmentSlot equipmentSlot) {
        var stack = new ItemStack(jp.aquafactory.apprenticecodex.registry.ItemRegistry.BOUND_SWORD.get());
        stack.getOrCreateTag().putUUID(INSTANCE_ID_TAG, instanceId);
        stack.getOrCreateTag().putFloat(DISPLAY_DAMAGE_TAG, displayDamage);
        if (equipmentSlot == EquipmentSlot.OFFHAND) {
            stack.getOrCreateTag().putString(EQUIPMENT_SLOT_TAG, OFFHAND_SLOT_VALUE);
        }
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
        CompoundTag tag = stack.getTag();
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
        var tag = stack.getTag();
        return tag == null ? 0.0F : tag.getFloat(DISPLAY_DAMAGE_TAG);
    }

    public static boolean isGeneratedForOffhand(ItemStack stack) {
        var tag = stack.getTag();
        return tag != null && OFFHAND_SLOT_VALUE.equals(tag.getString(EQUIPMENT_SLOT_TAG));
    }

    @Override
    public Multimap<Attribute, AttributeModifier> getAttributeModifiers(EquipmentSlot slot, ItemStack stack) {
        if (slot != EquipmentSlot.MAINHAND) {
            return super.getAttributeModifiers(slot, stack);
        }

        var builder = ImmutableMultimap.<Attribute, AttributeModifier>builder();
        builder.put(
                Attributes.ATTACK_DAMAGE,
                new AttributeModifier(
                        Item.BASE_ATTACK_DAMAGE_UUID,
                        "Weapon modifier",
                        Math.max(0.0F, getDisplayDamage(stack) - 1.0F),
                        AttributeModifier.Operation.ADDITION
                )
        );
        builder.put(
                Attributes.ATTACK_SPEED,
                new AttributeModifier(
                        Item.BASE_ATTACK_SPEED_UUID,
                        "Weapon modifier",
                        ATTACK_SPEED_MODIFIER_AMOUNT,
                        AttributeModifier.Operation.ADDITION
                )
        );
        return builder.build();
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @Nullable Level level, @NotNull List<Component> lines,
                                @NotNull TooltipFlag flag) {
        super.appendHoverText(stack, level, lines, flag);

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

    @Override
    public boolean isEnchantable(@NotNull ItemStack stack) {
        return true;
    }

    @Override
    public boolean canApplyAtEnchantingTable(ItemStack stack, Enchantment enchantment) {
        return enchantment.canApplyAtEnchantingTable(new ItemStack(net.minecraft.world.item.Items.GOLDEN_SWORD));
    }

    @Override
    public boolean isFoil(@NotNull ItemStack stack) {
        return true;
    }

    @Override
    public boolean canPerformAction(@NotNull ItemStack stack, @NotNull ToolAction toolAction) {
        return ToolActions.SWORD_SWEEP == toolAction || super.canPerformAction(stack, toolAction);
    }
}
