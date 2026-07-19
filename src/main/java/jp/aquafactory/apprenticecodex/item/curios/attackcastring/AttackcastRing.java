package jp.aquafactory.apprenticecodex.item.curios.attackcastring;

import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastType;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import io.redspace.ironsspellbooks.api.spells.SpellData;
import io.redspace.ironsspellbooks.api.util.AnimationHolder;
import io.redspace.ironsspellbooks.compat.Curios;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.item.CastAnimationOverrideItem;
import jp.aquafactory.apprenticecodex.item.ImbueTooltipHelper;
import jp.aquafactory.apprenticecodex.item.RestrictedSpellImbuableItem;
import jp.aquafactory.apprenticecodex.item.swingstaff.SwingcastStaffCastContext;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerPlayer;
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

public class AttackcastRing extends Item implements ICurioItem, RestrictedSpellImbuableItem, CastAnimationOverrideItem {
    final String slotIdentifier;

    public AttackcastRing() {
        super(new Properties().stacksTo(1).rarity(Rarity.UNCOMMON));
        slotIdentifier = Curios.RING_SLOT;
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
                    .append(Component.translatable(getDescriptionId() + ".desc"))
                    .withStyle(Style.EMPTY.withColor(ChatFormatting.YELLOW)));
            tooltips.add(Component.empty());
        }

        return tooltips;
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @Nullable Level level, @NotNull List<Component> lines,
                                @NotNull TooltipFlag flag) {
        super.appendHoverText(stack, level, lines, flag);
        appendAttackcastTooltip(lines);
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
        return spell != null && spell != SpellRegistry.none() && spell.getCastType() == CastType.INSTANT;
    }

    public boolean tryTriggerImbuedSpell(ServerPlayer player, ItemStack stack, String castingSlot) {
        initializeSpellContainer(stack);
        var spellContainer = ISpellContainer.get(stack);
        if (spellContainer == null || spellContainer.getActiveSpellCount() <= 0) {
            return false;
        }

        var spellData = spellContainer.getSpellAtIndex(0);
        if (!canImbueSpell(spellData)) {
            return false;
        }

        var spell = spellData.getSpell();
        var magicData = MagicData.getPlayerMagicData(player);
        if (magicData == null || magicData.getPlayerCooldowns().isOnCooldown(spell)) {
            // 攻撃のたびに同じ警告が出ないよう、クールダウンだけは Iron's Spells の通知前に除外する。
            return false;
        }

        var spellLevel = spell.getLevelFor(spellData.getLevel(), player);
        try (var ignored = SwingcastStaffCastContext.open(player.getUUID(), stack, spell)) {
            if (!spell.attemptInitiateCast(
                    stack,
                    spellLevel,
                    player.level(),
                    player,
                    io.redspace.ironsspellbooks.api.spells.CastSource.SWORD,
                    true,
                    castingSlot
            )) {
                return false;
            }

            // MagicData は同時に 1 詠唱しか保持できないため、Instant を通常 tick と同じ順序で完了させて次の指輪へ進む。
            // 1.21.1 側では Iron's Spells の詠唱完了 API と packet 順序を再確認する。
            spell.castSpell(player.level(), spellLevel, player, magicData.getCastSource(), true);
            spell.onServerCastComplete(player.level(), spellLevel, player, magicData, false);
            return true;
        }
    }

    @Override
    public boolean shouldSuppressCastStartAnimation(ItemStack stack, @Nullable AbstractSpell spell) {
        return matchesImbuedSpell(stack, spell);
    }

    @Override
    public boolean shouldOverrideCastStartAnimation(ItemStack stack, @Nullable AbstractSpell spell) {
        return matchesImbuedSpell(stack, spell);
    }

    @Override
    public AnimationHolder getCastStartAnimation(ItemStack stack, AbstractSpell spell, int spellLevel) {
        return AnimationHolder.pass();
    }

    @Override
    public boolean shouldSuppressCastFinishAnimation(ItemStack stack, @Nullable AbstractSpell spell) {
        return matchesImbuedSpell(stack, spell);
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
        return collectAttackcastRestrictTooltipSection();
    }

    private static void appendAttackcastTooltip(List<Component> lines) {
        ImbueTooltipHelper.appendBlankLineIfNeeded(lines);
        if (ImbueTooltipHelper.appendHintIfDetailsHidden(lines)) {
            return;
        }

        ImbueTooltipHelper.appendTooltipSection(
                lines,
                collectAttackcastAbilityTooltipSection(),
                "item." + ApprenticeCodex.MODID + ".spellgun.tooltip.ability_title",
                "item." + ApprenticeCodex.MODID + ".spellgun.tooltip.ability_none"
        );
        ImbueTooltipHelper.appendTooltipSection(
                lines,
                collectAttackcastRestrictTooltipSection(),
                "item." + ApprenticeCodex.MODID + ".spellgun.tooltip.restrict_title",
                "item." + ApprenticeCodex.MODID + ".spellgun.tooltip.restrict_none"
        );
    }

    private static List<Component> collectAttackcastAbilityTooltipSection() {
        return List.of();
    }

    private static List<Component> collectAttackcastRestrictTooltipSection() {
        var translatedLines = new ArrayList<Component>();
        translatedLines.add(ImbueTooltipHelper.translatableGray(
                "item." + ApprenticeCodex.MODID + ".spellgun.tooltip.restrict_restrict_instant_only"
        ));
        return translatedLines;
    }

    private static boolean matchesImbuedSpell(ItemStack stack, @Nullable AbstractSpell spell) {
        if (spell == null || !ISpellContainer.isSpellContainer(stack)) {
            return false;
        }

        var spellContainer = ISpellContainer.get(stack);
        if (spellContainer == null || spellContainer.getActiveSpellCount() <= 0) {
            return false;
        }

        var spellData = spellContainer.getSpellAtIndex(0);
        return spellData != SpellData.EMPTY && spell.equals(spellData.getSpell());
    }

}
