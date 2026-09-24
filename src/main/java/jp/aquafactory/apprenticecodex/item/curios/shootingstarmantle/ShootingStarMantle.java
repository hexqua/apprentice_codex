package jp.aquafactory.apprenticecodex.item.curios.shootingstarmantle;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import io.redspace.ironsspellbooks.api.spells.SpellData;
import io.redspace.ironsspellbooks.registries.ItemRegistry;
import jp.aquafactory.apprenticecodex.enchantment.TranscendenceTarget;
import jp.aquafactory.apprenticecodex.item.ArcaneAnvilScrollImbueBlockItem;
import jp.aquafactory.apprenticecodex.item.CalibrationAdjustmentProfile;
import jp.aquafactory.apprenticecodex.item.SpellCalibrationAdjustmentTarget;
import jp.aquafactory.apprenticecodex.item.SpellCalibrationImbueTarget;
import jp.aquafactory.apprenticecodex.item.StoredSpellCalibrationImbueTarget;
import jp.aquafactory.apprenticecodex.item.curios.CuriosSlotConstants;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import org.jetbrains.annotations.NotNull;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ShootingStarMantle extends Item implements ICurioItem, SpellCalibrationAdjustmentTarget,
        StoredSpellCalibrationImbueTarget, ArcaneAnvilScrollImbueBlockItem, TranscendenceTarget {
    final String slotIdentifier;

    public ShootingStarMantle() {
        super(new Properties().stacksTo(1).rarity(Rarity.RARE));
        slotIdentifier = CuriosSlotConstants.BACK;
    }

    @Override
    public boolean isEnchantable(@NotNull ItemStack stack) {
        return true;
    }

    @Override
    public boolean isBarVisible(@NotNull ItemStack stack) {
        var energy = MantleEnergy.read(stack);
        return energy.energy() < energy.maxEnergy();
    }

    @Override
    public int getBarWidth(@NotNull ItemStack stack) {
        var energy = MantleEnergy.read(stack);
        return Math.round(13F * energy.energy() / energy.maxEnergy());
    }

    @Override
    public int getBarColor(@NotNull ItemStack stack) { return MantleEnergy.read(stack).recovering() ? 0xFF4400 : 0xFFEEDD; }

    @Override
    public List<Component> getSlotsTooltip(List<Component> tooltips, TooltipContext context, ItemStack stack) {
        var result = new ArrayList<>(tooltips);
        if (slotIdentifier != null) {
            // Curiosっぽい共通ヘッダ.
            result.add(Component.empty());
            result.add(Component.translatable("curios.modifiers." + this.slotIdentifier).withStyle(ChatFormatting.GOLD));

            // 本体.
            result.add(Component.literal(" ")
                    .append(Component.translatable(getDescriptionId() + ".desc_1"))
                    .withStyle(Style.EMPTY.withColor(ChatFormatting.YELLOW)));
            result.add(Component.literal(" ")
                    .append(Component.translatable(getDescriptionId() + ".desc_2"))
                    .withStyle(Style.EMPTY.withColor(ChatFormatting.YELLOW)));
            result.add(Component.literal(" ")
                    .append(Component.translatable(getDescriptionId() + ".desc_3"))
                    .withStyle(Style.EMPTY.withColor(ChatFormatting.YELLOW)));
        }

        return result;
    }

    @Override
    public boolean canEquipFromUse(SlotContext slotContext, ItemStack stack) {
        return true;
    }

    @Override
    public int getCalibrationAdjustmentSlotCount(@NotNull ItemStack stack) {
        return MantleCalibration.ADJUSTMENT_SLOTS;
    }

    @Override
    public @NotNull CalibrationAdjustmentProfile getCalibrationAdjustmentProfile(@NotNull ItemStack stack) {
        return MantleCalibration.PROFILE;
    }

    @Override
    public void onCalibrationAdjustmentsChanged(@NotNull ItemStack stack, @NotNull HolderLookup.Provider lookup) {
        // 最大値が下がった瞬間に保存値も切り詰め、再挿入による余剰魔力の復活を防ぐ。
        MantleEnergy.read(stack).save(stack);
    }

    @Override
    public boolean acceptsCalibrationSpell(@NotNull SpellData spellData) {
        return SpellCalibrationImbueTarget.isValidCalibrationSpell(spellData);
    }

    @Override
    public boolean isCalibrationSlotAvailable(@NotNull ItemStack stack, int slot) {
        return isCalibrationSlotAvailable(stack, slot, MantleCalibration.serializationLookup());
    }

    @Override
    public boolean isCalibrationSlotAvailable(@NotNull ItemStack stack, int slot, @NotNull HolderLookup.Provider lookup) {
        return slot >= 0 && slot < MantleCalibration.enabledSlots(stack, lookup);
    }

    @Override
    public boolean hasAnyStoredCalibrationScroll(@NotNull ItemStack stack) {
        var lookup = MantleCalibration.serializationLookup();
        for (int slot = 0; slot < MantleCalibration.SCROLL_SLOTS; slot++) {
            if (!MantleCalibration.getScroll(stack, slot, lookup).isEmpty()) return true;
        }
        return false;
    }

    @Override
    public Multimap<Holder<Attribute>, AttributeModifier> getAttributeModifiers(
            SlotContext slotContext, ResourceLocation id, ItemStack stack) {
        var builder = ImmutableMultimap.<Holder<Attribute>, AttributeModifier>builder();
        builder.putAll(ICurioItem.super.getAttributeModifiers(slotContext, id, stack));
        if (MantleCalibration.hasAdjustment(stack, ItemRegistry.PROTECTION_RUNE.get())) {
            // Curiosの枠ごとのIDに従い、付け外しと調整変更で属性が確実に更新されるようにする。
            builder.put(AttributeRegistry.SPELL_RESIST, new AttributeModifier(
                    id.withSuffix("/mantle_spell_resist"), 0.1, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
        }
        return builder.build();
    }

    @Override
    public @NotNull Optional<TooltipComponent> getTooltipImage(@NotNull ItemStack stack) {
        return createCalibrationAdjustmentTooltip(stack);
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @NotNull TooltipContext context,
                                @NotNull List<Component> lines, @NotNull TooltipFlag flag) {
        super.appendHoverText(stack, context, lines, flag);
        // 魔法レベルの表示はLocalPlayerを使うため、専用サーバーでclient helperを読み込まない。
        if (FMLEnvironment.dist == Dist.CLIENT) MantleClientTooltip.append(stack, lines);
    }
}
