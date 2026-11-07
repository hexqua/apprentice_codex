package jp.aquafactory.apprenticecodex.gametest.equipment.bow.elemental;

import io.redspace.ironsspellbooks.api.events.SpellOnCastEvent;
import io.redspace.ironsspellbooks.api.item.curios.AffinityData;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.enchantment.Enchantments;
import jp.aquafactory.apprenticecodex.item.elementalbow.ElementalBow;
import jp.aquafactory.apprenticecodex.item.elementalbow.ElementalBowPendingCast;
import jp.aquafactory.apprenticecodex.item.elementalbow.ElementalBowRunes;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import jp.aquafactory.apprenticecodex.registry.SpellRegistry;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static jp.aquafactory.apprenticecodex.gametest.equipment.bow.elemental.ElementalBowLongCastFixture.player;
import static jp.aquafactory.apprenticecodex.gametest.equipment.bow.elemental.ElementalBowLongCastFixture.useDamageTestConfig;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class ElementalBowLongPowerGameTests {
    private ElementalBowLongPowerGameTests() {}
    private static final String TEMPLATE = "gametest/bow_casting";

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void transcendenceAndAffinityApplyOnceToInstantAndLongCasts(GameTestHelper h) {
        var players = new ArrayList<FakePlayer>();
        try (var config = useDamageTestConfig(h)) {
            for (var spell : List.of(SpellRegistry.LUNAR_AIM.get(),
                    io.redspace.ironsspellbooks.api.registry.SpellRegistry.MAGIC_MISSILE_SPELL.get())) {
                var player = player(h, InteractionHand.MAIN_HAND, spell);
                players.add(player);
                var stack = player.getMainHandItem();
                var scroll = ElementalBowTestSupport.createSpellScroll(spell);
                ISpellContainer.createScrollContainer(spell, spell.getMaxLevel(), scroll);
                ElementalBow.setCalibrationScroll(stack, 0, scroll, h.getLevel().registryAccess());
                stack.enchant(h.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT)
                        .getOrThrow(Enchantments.TRANSCENDENCE), 3);
                var ring = new ItemStack(ItemRegistry.ENCHANTED_CIRCLET.get());
                AffinityData.setAffinityData(ring, spell, 2);
                ElementalBowTestSupport.equipCurio(player, "head", ring);
                h.assertTrue(ElementalBow.getDisplayedSpellProfile(stack).spellLevel() == spell.getMaxLevel() + 1,
                        "Stored spell preview must apply only the fixed internal bonus");
                var selectedView = ElementalBow.getAvailableSelectionViews(player, stack).stream()
                        .filter(ElementalBow.ModeSelectionView::currentSelection).findFirst().orElseThrow();
                var expectedDisplayLevel = Integer.toString(spell.getMaxLevel() + 3);
                h.assertTrue(expectedDisplayLevel.equals(selectedView.badgeText())
                                && selectedView.displayName().getString().endsWith(" " + expectedDisplayLevel),
                        "Selection name and badge must include Transcendence and Affinity once");
                h.assertTrue(stack.getItem().use(h.getLevel(), player, InteractionHand.MAIN_HAND).getResult().consumesAction(),
                        "Enchanted bow must begin drawing with Affinity equipped");
            }
        } catch (RuntimeException | Error failure) {
            players.forEach(player -> { ElementalBowPendingCast.cancel(player); player.discard(); });
            throw failure;
        }
        h.runAfterDelay(30, () -> {
            try (var config = useDamageTestConfig(h)) {
                for (var player : players) {
                    var stack = player.getMainHandItem();
                    var profile = ElementalBow.getDisplayedSpellProfile(stack);
                    int expectedLevel = profile.spell().getMaxLevel() + 3;
                    int[] castLevels = {0, 0};
                    Consumer<SpellOnCastEvent> listener = event -> {
                        if (event.getEntity() == player) {
                            castLevels[0]++;
                            castLevels[1] = event.getOriginalSpellLevel();
                        }
                    };
                    NeoForge.EVENT_BUS.addListener(listener);
                    try {
                        float manaBefore = MagicData.getPlayerMagicData(player).getMana();
                        float expectedMana = ElementalBowRunes.baseManaCost(stack, player, profile.spell().getManaCost(expectedLevel));
                        stack.getItem().releaseUsing(stack, h.getLevel(), player, stack.getUseDuration(player) - 30);
                        h.assertTrue(castLevels[0] == 1 && castLevels[1] == expectedLevel,
                                "Bow must cast once at original level plus one Transcendence and two Affinity levels");
                        h.assertTrue(Math.abs(manaBefore - MagicData.getPlayerMagicData(player).getMana() - expectedMana) < 0.01F,
                                "Mana consumption must use the same effective level as casting");
                        h.assertTrue(stack.getEnchantmentLevel(h.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT)
                                        .getOrThrow(Enchantments.TRANSCENDENCE)) == 3,
                                "Casting must preserve legacy enchantment levels");
                    } finally {
                        NeoForge.EVENT_BUS.unregister(listener);
                    }
                }
                h.succeed();
            } finally {
                players.forEach(player -> { ElementalBowPendingCast.cancel(player); player.discard(); });
            }
        });
    }
}
