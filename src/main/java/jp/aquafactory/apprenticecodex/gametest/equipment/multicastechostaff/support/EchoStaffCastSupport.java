package jp.aquafactory.apprenticecodex.gametest.equipment.multicastechostaff.support;

import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.magic.SpellSelectionManager;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.capabilities.magic.MagicManager;
import jp.aquafactory.apprenticecodex.config.ApprenticeCodexServerConfig;
import jp.aquafactory.apprenticecodex.registry.EffectRegistry;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.util.FakePlayer;

public final class EchoStaffCastSupport {
    private EchoStaffCastSupport() {}

    static void prepareMulticastEchoStaffCast(
            FakePlayer player,
            ItemStack staffStack,
            AbstractSpell spell,
            int spellLevel,
            int amplifier,
            float mana
    ) {
        var magicData = MagicData.getPlayerMagicData(player);
        magicData.getPlayerCooldowns().removeCooldown(spell.getSpellId());
        magicData.setPlayerCastingItem(staffStack);
        magicData.setMana(mana);
        player.addEffect(new MobEffectInstance(EffectRegistry.ECHO_SPELL.get(), 200, amplifier));
    }

    public static void completeMulticastEchoStaffCast(
            ServerLevel level,
            FakePlayer player,
            ItemStack staffStack,
            AbstractSpell spell,
            int spellLevel,
            int amplifier,
            float mana
    ) {
        beginMulticastEchoStaffCast(level, player, staffStack, spell, spellLevel, amplifier, mana);
        finishStartedSpellCast(level, player, spell, spellLevel);
    }

    public static void beginMulticastEchoStaffCast(
            ServerLevel level,
            FakePlayer player,
            ItemStack staffStack,
            AbstractSpell spell,
            int spellLevel,
            int amplifier,
            float mana
    ) {
        prepareMulticastEchoStaffCast(player, staffStack, spell, spellLevel, amplifier, mana);
        var magicData = MagicData.getPlayerMagicData(player);
        magicData.getSyncedData();
        magicData.initiateCast(
                spell,
                spellLevel,
                spell.getEffectiveCastTime(spellLevel, player),
                CastSource.SPELLBOOK,
                SpellSelectionManager.MAINHAND
        );
        magicData.setPlayerCastingItem(staffStack);
        spell.onServerPreCast(level, spellLevel, player, magicData);
    }

    public static void finishStartedSpellCast(
            ServerLevel level,
            FakePlayer player,
            AbstractSpell spell,
            int spellLevel
    ) {
        var magicData = MagicData.getPlayerMagicData(player);
        spell.castSpell(level, spellLevel, player, CastSource.SPELLBOOK, true);
        spell.onServerCastComplete(level, spellLevel, player, magicData, false);
    }

    public static int expectedMulticastEchoStaffCooldown(
            AbstractSpell spell,
            ServerPlayer player,
            CastSource castSource,
            int amplifier
    ) {
        var baseCooldown = MagicManager.getEffectiveSpellCooldown(spell, player, castSource);
        var cooldownCapTicks = ApprenticeCodexServerConfig.multicastEchoStaffCooldownCapTicks();
        if (baseCooldown > cooldownCapTicks) {
            return baseCooldown;
        }

        var cooldownComponent = (amplifier + 2)
                * ApprenticeCodexServerConfig.multicastEchoStaffCooldownMultiplier()
                * baseCooldown;
        var castTimeComponent = (amplifier + 1)
                * ApprenticeCodexServerConfig.multicastEchoStaffCastTimeCooldownMultiplier()
                * spell.getEffectiveCastTime(1, player);
        return Math.min(cooldownCapTicks, (int) Math.ceil(cooldownComponent + castTimeComponent));
    }
}
