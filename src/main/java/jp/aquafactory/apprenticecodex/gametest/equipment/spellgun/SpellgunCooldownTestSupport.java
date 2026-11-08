package jp.aquafactory.apprenticecodex.gametest.equipment.spellgun;

import io.redspace.ironsspellbooks.api.events.SpellCooldownAddedEvent;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import jp.aquafactory.apprenticecodex.item.spellgun.SpellGunCastEvent;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

final class SpellgunCooldownTestSupport {
    private SpellgunCooldownTestSupport() {}

    static void assertSpellgunCooldownAdjustment(
            GameTestHelper helper,
            ServerPlayer player,
            ItemStack stack,
            AbstractSpell spell,
            int originalCooldownTicks,
            int expectedCooldownTicks,
            String message
    ) {
        var magicData = MagicData.getPlayerMagicData(player);
        helper.assertTrue(magicData != null, "Spellgun cooldown test could not resolve player mana data");
        magicData.setPlayerCastingItem(stack);

        var cooldownEvent = new SpellCooldownAddedEvent.Pre(
                originalCooldownTicks,
                spell,
                player,
                CastSource.SWORD
        );
        SpellGunCastEvent.onSpellCooldownAdded(cooldownEvent);
        SpellGunCastEvent.onInstantCastTimeCooldownAdded(cooldownEvent);
        helper.assertTrue(cooldownEvent.getEffectiveCooldown() == expectedCooldownTicks,
                message + ": expected " + expectedCooldownTicks + " but got " + cooldownEvent.getEffectiveCooldown());
    }
}
