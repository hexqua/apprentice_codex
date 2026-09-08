package jp.aquafactory.apprenticecodex.item.curios.protectionspellsupporter;

import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import jp.aquafactory.apprenticecodex.compat.jei.IJeiInfoItem;
import jp.aquafactory.apprenticecodex.item.ImbueTooltipHelper;
import jp.aquafactory.apprenticecodex.item.curios.CuriosSlotConstants;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import jp.aquafactory.apprenticecodex.registry.SpellRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import org.jetbrains.annotations.Nullable;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class ProtectionSpellSupporter extends Item implements ICurioItem, IJeiInfoItem {
    private static final float MANA_COST_DISCOUNT_MULTIPLIER = 0.5f;
    private static final String JEI_INFO_KEY_PREFIX = "jei.apprenticecodex.protection_spell_supporter.desc_";
    private static final String SPELL_HINT_KEY = "item.apprenticecodex.common.desc.spell_hint";
    private static final String SPELL_HINT_OPEN_KEY = "item.apprenticecodex.common.desc.spell_hint_open";
    private static final List<Supplier<AbstractSpell>> TARGET_SPELLS = List.of(
            SpellRegistry.FORCE_FIELD,
            SpellRegistry.PHALANX_CHARGE,
            SpellRegistry.MYSTIC_SHIELD,
            io.redspace.ironsspellbooks.api.registry.SpellRegistry.SHIELD_SPELL,
            io.redspace.ironsspellbooks.api.registry.SpellRegistry.ICE_TOMB_SPELL,
            io.redspace.ironsspellbooks.api.registry.SpellRegistry.EVASION_SPELL,
            io.redspace.ironsspellbooks.api.registry.SpellRegistry.HEARTSTOP_SPELL,
            io.redspace.ironsspellbooks.api.registry.SpellRegistry.ABYSSAL_SHROUD_SPELL
    );

    private final String slotIdentifier;

    public ProtectionSpellSupporter() {
        super(new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON));
        slotIdentifier = CuriosSlotConstants.BELT;
    }

    @Override
    public void onEquip(SlotContext slotContext, ItemStack prevStack, ItemStack stack) {
    }

    @Override
    public void onUnequip(SlotContext slotContext, ItemStack newStack, ItemStack stack) {
    }

    @Override
    public List<Component> getSlotsTooltip(List<Component> tooltips, Item.TooltipContext context, ItemStack stack) {
        var result = new ArrayList<>(tooltips);
        if (slotIdentifier != null) {
            result.add(Component.empty());
            result.add(Component.translatable("curios.modifiers." + slotIdentifier).withStyle(ChatFormatting.GOLD));
            result.add(Component.literal(" ")
                    .append(Component.translatable(getDescriptionId() + ".desc"))
                    .withStyle(Style.EMPTY.withColor(ChatFormatting.YELLOW)));
            appendTargetSpellHintOrTooltips(result);
        }

        return result;
    }

    private static void appendTargetSpellHintOrTooltips(List<Component> tooltips) {
        if (!ImbueTooltipHelper.hasDetailsKeyDown()) {
            tooltips.add(Component.translatable(SPELL_HINT_KEY).withStyle(ChatFormatting.DARK_GRAY));
            return;
        }

        tooltips.add(Component.translatable(SPELL_HINT_OPEN_KEY).withStyle(ChatFormatting.GRAY));
        appendTargetSpellTooltips(tooltips);
    }

    private static void appendTargetSpellTooltips(List<Component> tooltips) {
        for (var spellEntry : TARGET_SPELLS) {
            var spell = spellEntry.get();
            if (!spell.isEnabled()) {
                continue;
            }
            tooltips.add(Component.literal("- ")
                    .append(spell.getDisplayName(null))
                    .withStyle(ChatFormatting.GRAY));
        }
    }

    @Override
    public boolean canEquipFromUse(SlotContext slotContext, ItemStack stack) {
        return true;
    }

    @Override
    public String getJeiInfoTranslationKeyPrefix() {
        return JEI_INFO_KEY_PREFIX;
    }

    public static boolean isEquippedBy(@Nullable LivingEntity entity) {
        if (entity == null) {
            return false;
        }

        return CuriosApi.getCuriosInventory(entity)
                .map(inventory -> inventory.findFirstCurio(stack -> stack.is(ItemRegistry.PROTECTION_SPELL_SUPPORTER.get())).isPresent())
                .orElse(false);
    }

    public static boolean isManaCostDiscountTargetSpell(String spellId) {
        for (var spellEntry : TARGET_SPELLS) {
            if (spellEntry.get().getSpellId().equals(spellId)) {
                return true;
            }
        }
        return false;
    }

    public static int applyManaCostDiscount(int manaCost, @Nullable LivingEntity entity) {
        if (manaCost <= 0 || !isEquippedBy(entity)) {
            return manaCost;
        }

        return Math.max(1, Math.round(manaCost * MANA_COST_DISCOUNT_MULTIPLIER));
    }

    public static int applyEvasionAmplifierBonus(int amplifier, @Nullable LivingEntity entity) {
        // Iron's は Amp + 1 回を回避するため、最終回数を倍増してから Amp に戻す。
        return isEquippedBy(entity) ? 2 * amplifier + 1 : amplifier;
    }

    public static float applyHeartstopAccumulationDiscount(float addedDamage, @Nullable LivingEntity entity) {
        // 通常の50%蓄積をさらに半減する。累積値には触れず、過去の被弾を付け替えで再計算しない。
        return isEquippedBy(entity) ? addedDamage * 0.5f : addedDamage;
    }
}
