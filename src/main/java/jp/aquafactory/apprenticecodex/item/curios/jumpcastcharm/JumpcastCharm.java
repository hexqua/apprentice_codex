package jp.aquafactory.apprenticecodex.item.curios.jumpcastcharm;

import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastType;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import io.redspace.ironsspellbooks.api.spells.SpellData;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.item.ImbueTooltipHelper;
import jp.aquafactory.apprenticecodex.item.RestrictedSpellImbuableItem;
import jp.aquafactory.apprenticecodex.item.WeaponImbueCooldownPolicyItem;
import jp.aquafactory.apprenticecodex.item.curios.CuriosSlotConstants;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

import java.util.ArrayList;
import java.util.List;

public class JumpcastCharm extends Item implements ICurioItem, RestrictedSpellImbuableItem, WeaponImbueCooldownPolicyItem {
    final String slotIdentifier;

    public JumpcastCharm() {
        super(new Properties().stacksTo(1).rarity(Rarity.UNCOMMON));
        slotIdentifier = CuriosSlotConstants.FEET;
    }

    @Override
    public @NotNull ItemStack getDefaultInstance() {
        var stack = super.getDefaultInstance();
        initializeSpellContainer(stack);
        return stack;
    }

    @Override
    public void onCraftedBy(@NotNull ItemStack stack, @NotNull Level level, @NotNull Player player) {
        super.onCraftedBy(stack, level, player);
        initializeSpellContainer(stack);
    }

    @Override
    public void inventoryTick(@NotNull ItemStack stack, @NotNull Level level, @NotNull Entity entity, int slotId, boolean isSelected) {
        super.inventoryTick(stack, level, entity, slotId, isSelected);
        if (!level.isClientSide) {
            initializeSpellContainer(stack);
        }
    }

    @Override
    public void onEquip(SlotContext slotContext, ItemStack prevStack, ItemStack stack) {
    }

    @Override
    public void onUnequip(SlotContext slotContext, ItemStack newStack, ItemStack stack) {
    }

    @Override
    public void curioTick(SlotContext slotContext, ItemStack stack) {
    }

    @Override
    public List<Component> getSlotsTooltip(List<Component> tooltips, ItemStack stack) {
        if (slotIdentifier != null) {
            // Curiosっぽい共通ヘッダ.
            tooltips.add(Component.empty());
            tooltips.add(Component.translatable("curios.modifiers." + this.slotIdentifier).withStyle(ChatFormatting.GOLD));

            // 本体.
            tooltips.add(Component.literal(" ")
                    .append(Component.translatable(
                            getDescriptionId() + ".desc",
                            ImbueTooltipHelper.getJumpKeyName()
                    ))
                    .withStyle(Style.EMPTY.withColor(ChatFormatting.YELLOW)));
            tooltips.add(Component.empty());
        }

        return tooltips;
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @Nullable Level level, @NotNull List<Component> lines,
                                @NotNull TooltipFlag flag) {
        super.appendHoverText(stack, level, lines, flag);
        appendJumpcastTooltip(lines);
    }

    @Override
    public boolean canEquipFromUse(SlotContext slotContext, ItemStack stack) {
        return true;
    }

    @Override
    public boolean canImbueSpell(SpellData spellData) {
        return spellData != SpellData.EMPTY && canImbueSpell(spellData.getSpell(), spellData.getLevel());
    }

    @Override
    public boolean canImbueSpell(@Nullable AbstractSpell spell, int spellLevel) {
        return spell != null && spell != SpellRegistry.none() && spell.getCastType() != CastType.CONTINUOUS;
    }

    @Override
    public void normalizeImbuedSpellContainer(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return;
        }

        initializeSpellContainer(stack);
        var current = ISpellContainer.get(stack);
        if (current == null) {
            return;
        }

        var normalized = ISpellContainer.create(1, false, false).mutableCopy();
        var spellData = current.getSpellAtIndex(0);
        if (spellData != SpellData.EMPTY && canImbueSpell(spellData)) {
            normalized.addSpellAtIndex(
                    spellData.getSpell(),
                    spellData.getLevel(),
                    normalized.getActiveSpellCount(),
                    false
            );
        }

        ISpellContainer.set(stack, normalized.toImmutable());
    }

    public void initializeSpellContainer(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return;
        }
        if (ISpellContainer.isSpellContainer(stack)) {
            return;
        }

        ISpellContainer.set(stack, ISpellContainer.create(1, false, false));
    }

    @Override
    public boolean canRemoveWorkbenchSpell(ItemStack stack, ISpellContainer spellContainer, int spellIndex, SpellData spellData) {
        return spellData != SpellData.EMPTY;
    }

    @Override
    public List<Component> getImbueRestrictionTooltipLines() {
        return collectJumpcastRestrictTooltipSection();
    }

    private static void appendJumpcastTooltip(List<Component> lines) {
        ImbueTooltipHelper.appendBlankLineIfNeeded(lines);
        if (ImbueTooltipHelper.appendHintIfDetailsHidden(lines)) {
            return;
        }

        ImbueTooltipHelper.appendTooltipSection(
                lines,
                collectJumpcastAbilityTooltipSection(),
                "item." + ApprenticeCodex.MODID + ".spellgun.tooltip.ability_title",
                "item." + ApprenticeCodex.MODID + ".spellgun.tooltip.ability_none"
        );
        ImbueTooltipHelper.appendTooltipSection(
                lines,
                collectJumpcastRestrictTooltipSection(),
                "item." + ApprenticeCodex.MODID + ".spellgun.tooltip.restrict_title",
                "item." + ApprenticeCodex.MODID + ".spellgun.tooltip.restrict_none"
        );
    }

    private static List<Component> collectJumpcastAbilityTooltipSection() {
        var translatedLines = new ArrayList<Component>();
        translatedLines.add(ImbueTooltipHelper.translatableGray(
                "item." + ApprenticeCodex.MODID + ".spellgun.tooltip.ability_long_to_instant"
        ));
        translatedLines.add(ImbueTooltipHelper.translatableGray(
                "item." + ApprenticeCodex.MODID + ".spellgun.tooltip.ability_extend_cooldown"
        ));
        return translatedLines;
    }

    private static List<Component> collectJumpcastRestrictTooltipSection() {
        var translatedLines = new ArrayList<Component>();
        translatedLines.add(ImbueTooltipHelper.translatableGray(
                "item." + ApprenticeCodex.MODID + ".spellgun.tooltip.restrict_restrict_not_continuous"
        ));
        return translatedLines;
    }

    @Override
    public boolean ignoresWeaponImbueCooldownMultiplier(ItemStack stack, @Nullable AbstractSpell spell, io.redspace.ironsspellbooks.api.spells.CastSource castSource) {
        // 剣 Imbue 扱いの接着だけ借りるが、実時間 cooldown は武器 Imbue の短縮調整へ寄せない。
        return castSource == io.redspace.ironsspellbooks.api.spells.CastSource.SWORD;
    }
}
