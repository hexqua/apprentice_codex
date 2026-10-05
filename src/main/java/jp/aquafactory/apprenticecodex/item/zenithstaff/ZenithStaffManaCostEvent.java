package jp.aquafactory.apprenticecodex.item.zenithstaff;

import io.redspace.ironsspellbooks.api.events.SpellOnCastEvent;
import io.redspace.ironsspellbooks.api.events.SpellPreCastEvent;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.config.ServerConfigs;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.config.ApprenticeCodexServerConfig;
import jp.aquafactory.apprenticecodex.item.SpellManaCostDiscounts;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = ApprenticeCodex.MODID)
public final class ZenithStaffManaCostEvent {
    private ZenithStaffManaCostEvent() {
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onSpellPreCast(SpellPreCastEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        var magicData = MagicData.getPlayerMagicData(player);
        if (magicData == null || !requiresManaGate(player, event, magicData)) {
            return;
        }

        if (!ZenithStaffPowerHelper.shouldIncreaseManaCost(player, event.getSchoolType())) {
            return;
        }

        var spell = SpellRegistry.getSpell(event.getSpellId());
        var baseManaCost = Math.max(0, spell.getManaCost(event.getSpellLevel()));
        if (baseManaCost <= 0) {
            return;
        }

        var requiredManaCost = resolvePreCastManaCost(player, event.getSpellId(), baseManaCost);
        if (magicData.getMana() >= requiredManaCost) {
            return;
        }

        player.displayClientMessage(Component.translatable(
                "ui.apprenticecodex.zenith_staff.insufficient_mana",
                spell.getDisplayName(player),
                requiredManaCost
        ).withStyle(ChatFormatting.RED), true);
        event.setCanceled(true);
    }

    private static boolean requiresManaGate(ServerPlayer player, SpellPreCastEvent event, MagicData magicData) {
        return event.getCastSource().consumesMana()
                && !magicData.getPlayerRecasts().hasRecastForSpell(event.getSpellId())
                && !(player.isCreative() && !ServerConfigs.CREATIVE_MANA_COST.get());
    }

    private static int resolvePreCastManaCost(ServerPlayer player, String spellId, int baseManaCost) {
        return applyZenithManaCostMultiplier(SpellManaCostDiscounts.applyKnownDiscounts(baseManaCost, player, spellId));
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onSpellCast(SpellOnCastEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || event.getManaCost() <= 0) {
            return;
        }

        if (!ZenithStaffPowerHelper.shouldIncreaseManaCost(player, event.getSchoolType())) {
            return;
        }

        event.setManaCost(applyZenithManaCostMultiplier(event.getManaCost()));
    }

    public static int applyZenithManaCostMultiplier(int manaCost) {
        if (manaCost <= 0) {
            return 0;
        }

        var multiplier = Math.max(1.0F, ApprenticeCodexServerConfig.zenithStaffManaCostMultiplier());
        return Math.max(1, Math.round(manaCost * multiplier));
    }
}
