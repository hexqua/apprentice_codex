package jp.aquafactory.apprenticecodex.item.spellgun;

import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.CastType;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.config.ServerConfigs;
import io.redspace.ironsspellbooks.util.TooltipsUtils;
import jp.aquafactory.apprenticecodex.item.ScrollSlotTooltipClientHelper;
import jp.aquafactory.apprenticecodex.item.SpellCalibrationImbueTarget;
import jp.aquafactory.apprenticecodex.item.WeaponImbueCooldownHelper;
import jp.aquafactory.apprenticecodex.item.fullautorapidcastspellrifle.FullautoEchoCasting;
import jp.aquafactory.apprenticecodex.item.fullautorapidcastspellrifle.FullautoCooldownPolicy;
import jp.aquafactory.apprenticecodex.item.fullautorapidcastspellrifle.FullautoRapidcastSpellrifle;
import jp.aquafactory.apprenticecodex.item.fullautorapidcastspellrifle.FullautoEchoConfigState;
import jp.aquafactory.apprenticecodex.item.multicastechostaff.MulticastEchoStaffAttackProfileManager;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public final class RifleSpellTooltipClientHelper {
    private RifleSpellTooltipClientHelper() {}

    public static void append(ItemStack stack, List<Component> lines) {
        var player = Minecraft.getInstance().player;
        if (player == null) return;
        var data = RifleSpellTooltipData.read(stack, player, player.level().registryAccess());
        lines.add(Component.empty());
        if (Screen.hasControlDown() && !data.slots().isEmpty()) {
            ScrollSlotTooltipClientHelper.appendLabel(lines);
            for (var slot : data.slots()) {
                ScrollSlotTooltipClientHelper.appendSpell(lines, slot.spell(), slot.scroll().getHoverName(),
                        slot.usable(), slot.index() == data.selectedSlot(), player);
            }
            return;
        }
        if (SpellCalibrationImbueTarget.isValidCalibrationSpell(data.selectedSpell())) {
            var spellData = data.selectedSpell();
            var spell = spellData.getSpell();
            // マナは魔法の標準情報として残し、選択元の補正はクールダウンだけへ適用する。
            var details = TooltipsUtils.formatActiveSpellTooltip(stack, spellData, CastSource.SPELLBOOK, player);
            if (!details.isEmpty()) details.remove(0);
            boolean echo = FullautoEchoConfigState.enabled() && FullautoEchoCasting.hasStaff(stack, player.level().registryAccess());
            if (echo) {
                int mana = FullautoEchoCasting.scaleMana(spell.getManaCost(spell.getLevelFor(spellData.getLevel(), player)),
                        FullautoEchoConfigState.manaMultiplier());
                details.replaceAll(line -> line.getContents() instanceof TranslatableContents text
                        && (text.getKey().equals("tooltip.irons_spellbooks.mana_cost")
                        || text.getKey().equals("tooltip.irons_spellbooks.mana_cost_per_second"))
                        ? TooltipsUtils.getManaCostComponent(spell.getCastType(), mana).withStyle(ChatFormatting.BLUE) : line);
                if (!details.isEmpty() && MulticastEchoStaffAttackProfileManager.hasClientSyncedProfile(spell)) {
                    details.get(0).append(Component.translatable("item.apprenticecodex.common.scroll_slots.echo_supported")
                            .withStyle(ChatFormatting.GRAY));
                }
            }
            if (stack.getItem() instanceof FullautoRapidcastSpellrifle) {
                details.removeIf(line -> containsTranslation(line, "tooltip.irons_spellbooks.cast_long")
                        || containsTranslation(line, "tooltip.irons_spellbooks.cast_continuous")
                        || containsTranslation(line, "ui.irons_spellbooks.cast_instant")
                        || containsTranslation(line, "tooltip.irons_spellbooks.cooldown_length_seconds"));
                int castTime = spell.getCastType() == CastType.LONG
                        ? spell.getEffectiveCastTime(spell.getLevelFor(spellData.getLevel(), player), player) : 0;
                int cooldown = FullautoCooldownPolicy.resolveClient(spell.getSpellCooldown(),
                        WeaponImbueCooldownHelper.getEffectiveSpellCooldown(spell, player, CastSource.SWORD), castTime);
                // 上流がCD行を省略する0秒の魔法でも、最終値を必ず表示する。
                details.add(Component.translatable("item.apprenticecodex.fullauto_rapidcast_spellrifle.tooltip.cooldown",
                        Utils.timeFromTicks(cooldown, 2)).withStyle(ChatFormatting.BLUE));
            } else {
                boolean showCooldown = data.castSource() != CastSource.SWORD || ServerConfigs.SWORDS_CD_MULTIPLIER.get() > 0;
                details.removeIf(line -> !showCooldown && line.getContents() instanceof TranslatableContents text
                        && text.getKey().equals("tooltip.irons_spellbooks.cooldown_length_seconds"));
                details.replaceAll(line -> line.getContents() instanceof TranslatableContents text
                        && text.getKey().equals("tooltip.irons_spellbooks.cooldown_length_seconds")
                        ? Component.translatable(text.getKey(), Utils.timeFromTicks(
                                WeaponImbueCooldownHelper.getEffectiveSpellCooldown(spell, player, data.castSource()), 2))
                                 .withStyle(ChatFormatting.BLUE) : line);
            }
            lines.addAll(details);
        } else {
            lines.add(Component.translatable("item.apprenticecodex.common.scroll_slots.no_selected_spell")
                    .withStyle(ChatFormatting.GRAY));
        }
        ScrollSlotTooltipClientHelper.appendHint(lines, data.slots().size());
    }

    private static boolean containsTranslation(Component line, String key) {
        return line.getContents() instanceof TranslatableContents text && text.getKey().equals(key)
                || line.getSiblings().stream().anyMatch(sibling -> containsTranslation(sibling, key));
    }
}
