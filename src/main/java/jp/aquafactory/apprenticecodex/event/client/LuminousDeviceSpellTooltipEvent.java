package jp.aquafactory.apprenticecodex.event.client;

import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.util.TooltipsUtils;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.item.luminousdevice.LuminousDevice;
import jp.aquafactory.apprenticecodex.item.luminousdevice.LuminousDeviceConfigState;
import jp.aquafactory.apprenticecodex.registry.SpellRegistry;
import jp.aquafactory.apprenticecodex.spell.magelight.MageLight;
import net.minecraft.ChatFormatting;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = ApprenticeCodex.MODID, value = Dist.CLIENT)
public final class LuminousDeviceSpellTooltipEvent {
    private LuminousDeviceSpellTooltipEvent() {
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onItemTooltip(ItemTooltipEvent event) {
        var stack = event.getItemStack();
        if (!(stack.getItem() instanceof LuminousDevice)
                || LuminousDevice.getMode(stack) != LuminousDevice.Mode.SPELL
                || !(event.getEntity() instanceof LocalPlayer player)) {
            return;
        }

        var spellData = LuminousDevice.getSelectedSpellData(stack);
        if (spellData == io.redspace.ironsspellbooks.api.spells.SpellData.EMPTY || spellData.getSpell() == null) {
            return;
        }

        var spell = spellData.getSpell();
        var spellLevel = spell.getLevelFor(spellData.getLevel(), player);
        var spellLines = TooltipsUtils.formatActiveSpellTooltip(
                ItemStack.EMPTY,
                spellData,
                CastSource.SWORD,
                player
        );
        if (!spellLines.isEmpty()) {
            spellLines.remove(0);
        }

        if (spell == SpellRegistry.MAGE_LIGHT.get()) {
            var mageLight = (MageLight) spell;
            var profile = mageLight.createCastProfile(
                    spellLevel,
                    player,
                    LuminousDeviceConfigState.mageLightExtendedRange()
            );
            if (profile.extendsRange()) {
                var insertIndex = Math.min(2, spellLines.size());
                spellLines.add(insertIndex, Component.translatable(
                        "item.apprenticecodex.luminous_device.spell.max_range",
                        io.redspace.ironsspellbooks.api.util.Utils.stringTruncation(profile.effectiveRange(), 1)
                ).withStyle(TooltipsUtils.getStyleFor(player, spell)));
                spellLines.add(insertIndex + 1, Component.translatable(
                        "item.apprenticecodex.luminous_device.spell.range_mana"
                ).withStyle(ChatFormatting.GRAY));
                replaceManaLine(spellLines, profile.baseManaCost(), profile.maximumManaCost());
            } else {
                ensureManaLine(spellLines, spell.getManaCost(spellLevel));
            }
        } else {
            ensureManaLine(spellLines, spell.getManaCost(spellLevel));
        }

        event.getToolTip().add(Component.empty());
        event.getToolTip().add(
                Component.translatable("tooltip.irons_spellbooks.imbued_tooltip")
                        .withStyle(ChatFormatting.GRAY)
        );
        event.getToolTip().addAll(spellLines);
    }

    private static void replaceManaLine(java.util.List<? extends Component> lines, int minimum, int maximum) {
        @SuppressWarnings("unchecked")
        var mutableLines = (java.util.List<Component>) lines;
        var manaIndex = TooltipsUtils.indexOfComponent(mutableLines, "tooltip.irons_spellbooks.mana_cost");
        var rangeLine = Component.translatable(
                "item.apprenticecodex.luminous_device.spell.mana_range",
                minimum,
                maximum
        ).withStyle(ChatFormatting.BLUE);
        if (manaIndex >= 0) {
            mutableLines.set(manaIndex, rangeLine);
        } else {
            mutableLines.add(rangeLine);
        }
    }

    private static void ensureManaLine(java.util.List<? extends Component> lines, int manaCost) {
        if (manaCost <= 0) {
            return;
        }
        @SuppressWarnings("unchecked")
        var mutableLines = (java.util.List<Component>) lines;
        if (TooltipsUtils.indexOfComponent(mutableLines, "tooltip.irons_spellbooks.mana_cost") < 0) {
            mutableLines.add(
                    TooltipsUtils.getManaCostComponent(
                            io.redspace.ironsspellbooks.api.spells.CastType.INSTANT,
                            manaCost
                    ).withStyle(ChatFormatting.BLUE)
            );
        }
    }
}
