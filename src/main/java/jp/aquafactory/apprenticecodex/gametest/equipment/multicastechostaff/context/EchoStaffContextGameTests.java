package jp.aquafactory.apprenticecodex.gametest.equipment.multicastechostaff.context;

import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.capabilities.magic.MagicManager;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.gametest.spell.targeting.support.TargetingTestFixture;
import jp.aquafactory.apprenticecodex.registry.EffectRegistry;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import static jp.aquafactory.apprenticecodex.gametest.equipment.multicastechostaff.support.EchoStaffCastSupport.expectedMulticastEchoStaffCooldown;
import static jp.aquafactory.apprenticecodex.gametest.equipment.multicastechostaff.support.EchoStaffFixture.*;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class EchoStaffContextGameTests {
    private EchoStaffContextGameTests() {
    }

    @GameTest(template = TEMPLATE, batch = "apprenticecodex.multicast_echo_staff_isolated", timeoutTicks = 80)
    public static void multicastEchoStaffInvalidInstantCastKeepsEchoSpell(GameTestHelper helper) {
        var player = manualPlayer(helper, "multicast_echo_staff_invalid_instant_test");
        var staffStack = new ItemStack(ItemRegistry.MULTICAST_ECHO_STAFF.get());
        var spell = SpellRegistry.ACUPUNCTURE_SPELL.get();
        var spellLevel = 1;
        var amplifier = 0;
        var manaCost = spell.getManaCost(spellLevel);
        player.setItemInHand(InteractionHand.MAIN_HAND, staffStack);
        player.setYRot(-90.0F);

        helper.runAtTickTime(1, () -> {
            beginCast(helper, player, staffStack, spell, spellLevel, amplifier, manaCost * 3.0F);
            var target = target(helper, player, new Vec3(2, 1, 0));
            helper.assertTrue(spell.checkPreCastConditions(helper.getLevel(), spellLevel, player, MagicData.getPlayerMagicData(player)),
                    "Acupuncture should find the target before it is removed");
            target.discard();
            player.setYRot(90.0F);

            finishCast(helper, player, spell, spellLevel);

            var magicData = MagicData.getPlayerMagicData(player);
            helper.assertTrue(player.hasEffect(EffectRegistry.ECHO_SPELL),
                    "Invalid instant casts should not consume EchoSpell");
            helper.assertTrue(Math.abs(magicData.getMana() - (manaCost * 2.0F)) < 1.0e-4F,
                    "Invalid instant cast should only pay the normal cast mana cost: " + magicData.getMana());
        });

        helper.runAtTickTime(4, () -> {
            tickEcho(helper, player);
            var magicData = MagicData.getPlayerMagicData(player);
            helper.assertTrue(Math.abs(magicData.getMana() - (manaCost * 2.0F)) < 1.0e-4F,
                    "Invalid instant cast should not start delayed multicast: " + magicData.getMana());
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, batch = "apprenticecodex.multicast_echo_staff_isolated", timeoutTicks = 80)
    public static void multicastEchoStaffInvalidLongCastIgnoresStaleEchoContext(GameTestHelper helper) {
        var player = manualPlayer(helper, "multicast_echo_staff_invalid_long_test");
        var staffStack = new ItemStack(ItemRegistry.MULTICAST_ECHO_STAFF.get());
        var spell = SpellRegistry.SLOW_SPELL.get();
        var spellLevel = 1;
        var amplifier = 0;
        var manaCost = spell.getManaCost(spellLevel);
        var initialMana = manaCost * 3.0F;
        player.setItemInHand(InteractionHand.MAIN_HAND, staffStack);
        var maxMana = player.getAttribute(AttributeRegistry.MAX_MANA);
        if (maxMana != null) {
            maxMana.setBaseValue(initialMana);
        }
        player.setYRot(-90.0F);
        player.setYHeadRot(-90.0F);
        player.setYBodyRot(-90.0F);
        player.setXRot(0.0F);

        helper.runAtTickTime(1, () -> {
            beginCast(helper, player, staffStack, spell, spellLevel, amplifier, initialMana);
            var target = target(helper, player, new Vec3(2, 1, 0));
            helper.assertTrue(spell.checkPreCastConditions(helper.getLevel(), spellLevel, player, MagicData.getPlayerMagicData(player)),
                    "Slow should find the elevated target before it is removed");
            player.removeEffect(EffectRegistry.ECHO_SPELL);
            helper.assertFalse(player.hasEffect(EffectRegistry.ECHO_SPELL),
                    "Long cast test should remove EchoSpell before the normal cast completes");
            target.moveTo(player.position().add(-13, 1, 0));
            TargetingTestFixture.of(helper).assertVisible(target);
            target.discard();
            player.setYRot(90.0F);
            player.setYHeadRot(90.0F);
            player.setYBodyRot(90.0F);
        });

        helper.runAtTickTime(3, () -> {
            finishCast(helper, player, spell, spellLevel);

            var magicData = MagicData.getPlayerMagicData(player);
            helper.assertTrue(Math.abs(magicData.getMana() - (initialMana - manaCost)) < 1.0e-4F,
                    "Invalid long cast should only pay the normal cast mana cost before delayed ticks: "
                            + magicData.getMana());
        });

        helper.runAtTickTime(6, () -> {
            tickEcho(helper, player);
            var magicData = MagicData.getPlayerMagicData(player);
            var cooldown = magicData.getPlayerCooldowns().getSpellCooldowns().get(spell.getSpellId());
            var expectedCooldown = MagicManager.getEffectiveSpellCooldown(spell, player, CastSource.SPELLBOOK);

            helper.assertTrue(Math.abs(magicData.getMana() - (initialMana - manaCost)) < 1.0e-4F,
                    "Invalid long cast should not spend mana on delayed multicast: " + magicData.getMana());
            helper.assertTrue(cooldown != null && cooldown.getSpellCooldown() == expectedCooldown,
                    "Invalid long cast should keep the normal cooldown instead of the multicast penalty: "
                            + (cooldown == null ? "null" : cooldown.getSpellCooldown())
                            + " / expected " + expectedCooldown);
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, batch = "apprenticecodex.multicast_echo_staff_isolated", timeoutTicks = 80)
    public static void multicastEchoStaffLongCastUsesStartEchoContextAfterEffectRemoved(GameTestHelper helper) {
        var player = manualPlayer(helper, "multicast_echo_staff_long_context_test");
        var staffStack = new ItemStack(ItemRegistry.MULTICAST_ECHO_STAFF.get());
        var spell = SpellRegistry.SLOW_SPELL.get();
        var spellLevel = 1;
        var amplifier = 0;
        var manaCost = spell.getManaCost(spellLevel);
        var initialMana = manaCost * 3.0F;
        player.setItemInHand(InteractionHand.MAIN_HAND, staffStack);
        var maxMana = player.getAttribute(AttributeRegistry.MAX_MANA);
        if (maxMana != null) {
            maxMana.setBaseValue(initialMana);
        }
        player.setYRot(-90.0F);
        player.setYHeadRot(-90.0F);
        player.setYBodyRot(-90.0F);
        player.setXRot(0.0F);

        helper.runAtTickTime(1, () -> {
            beginCast(helper, player, staffStack, spell, spellLevel, amplifier, initialMana);
            target(helper, player, new Vec3(2, 1, 0));
            helper.assertTrue(spell.checkPreCastConditions(helper.getLevel(), spellLevel, player, MagicData.getPlayerMagicData(player)),
                    "Slow should store a valid target before EchoSpell is removed");
            player.removeEffect(EffectRegistry.ECHO_SPELL);
            helper.assertFalse(player.hasEffect(EffectRegistry.ECHO_SPELL),
                    "Long cast context test should remove EchoSpell before the normal cast completes");

            finishCast(helper, player, spell, spellLevel);

            var magicData = MagicData.getPlayerMagicData(player);
            helper.assertTrue(Math.abs(magicData.getMana() - (initialMana - manaCost)) < 1.0e-4F,
                    "Valid long cast should only pay the normal cast mana cost before delayed ticks: "
                            + magicData.getMana());
            magicData.setAdditionalCastData(null);
        });

        helper.succeedWhen(() -> {
            tickEcho(helper, player);
            var magicData = MagicData.getPlayerMagicData(player);
            var cooldown = magicData.getPlayerCooldowns().getSpellCooldowns().get(spell.getSpellId());
            var expectedCooldown = expectedMulticastEchoStaffCooldown(spell, player, CastSource.SPELLBOOK, amplifier);

            helper.assertTrue(Math.abs(magicData.getMana() - (initialMana - manaCost * 2.0F)) < 1.0e-4F,
                    "Valid long cast should use the start EchoSpell context for delayed multicast: " + magicData.getMana());
            helper.assertTrue(cooldown != null && cooldown.getSpellCooldown() == expectedCooldown,
                    "Valid long cast should apply the multicast penalty cooldown from the start context: "
                            + (cooldown == null ? "null" : cooldown.getSpellCooldown())
                            + " / expected " + expectedCooldown);
            helper.assertTrue(magicData.getAdditionalCastData() == null,
                    "Delayed multicast pre-cast data should be restored after the repeated cast");
        });
    }
}
