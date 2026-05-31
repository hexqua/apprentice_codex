package jp.aquafactory.apprenticecodex.item.zenithstaff;

import io.redspace.ironsspellbooks.api.events.SpellOnCastEvent;
import io.redspace.ironsspellbooks.api.events.SpellPreCastEvent;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.config.ServerConfigs;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.config.ApprenticeCodexServerConfig;
import jp.aquafactory.apprenticecodex.item.curios.craftsmansdelight.CraftsmansDelight;
import jp.aquafactory.apprenticecodex.item.curios.craftsmansdelight.CraftsmansDelightSpellSupport;
import jp.aquafactory.apprenticecodex.item.curios.protectionspellsupporter.ProtectionSpellSupporter;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

@EventBusSubscriber(modid = ApprenticeCodex.MODID)
public final class ZenithStaffManaCostEvent {
    private ZenithStaffManaCostEvent() {
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onSpellPreCast(SpellPreCastEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !requiresManaGate(player, event)) {
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
        var magicData = MagicData.getPlayerMagicData(player);
        if (magicData == null || magicData.getMana() >= requiredManaCost) {
            return;
        }

        player.displayClientMessage(Component.translatable(
                "ui.apprenticecodex.zenith_staff.insufficient_mana",
                spell.getDisplayName(player),
                requiredManaCost
        ).withStyle(ChatFormatting.RED), true);
        event.setCanceled(true);
    }

    private static boolean requiresManaGate(ServerPlayer player, SpellPreCastEvent event) {
        return event.getCastSource().consumesMana() && !(player.isCreative() && !ServerConfigs.CREATIVE_MANA_COST.get());
    }

    private static int resolvePreCastManaCost(ServerPlayer player, String spellId, int baseManaCost) {
        return applyZenithManaCostMultiplier(applyKnownManaCostDiscounts(baseManaCost, player, spellId));
    }

    private static int applyKnownManaCostDiscounts(int manaCost, ServerPlayer player, String spellId) {
        // SpellOnCastEvent を事前判定で疑似発火すると、弾消費などマナ以外の副作用まで走る。
        // Zenith gate では、この MOD 内の副作用なし helper で表現できる既知の割引だけを明示的に再現する。
        var discountedManaCost = manaCost;
        if (CraftsmansDelightSpellSupport.isManaCostDiscountTarget(spellId)) {
            discountedManaCost = CraftsmansDelight.applyManaCostDiscount(discountedManaCost, player);
        }
        if (ProtectionSpellSupporter.isManaCostDiscountTargetSpell(spellId)) {
            discountedManaCost = ProtectionSpellSupporter.applyManaCostDiscount(discountedManaCost, player);
        }
        return Math.max(0, discountedManaCost);
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
