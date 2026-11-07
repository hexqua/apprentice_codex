package jp.aquafactory.apprenticecodex.gametest.enchantment.application;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.enchantment.Enchantments;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;

import static jp.aquafactory.apprenticecodex.gametest.enchantment.support.EnchantmentTestSupport.createEnchantedBook;
import static jp.aquafactory.apprenticecodex.gametest.support.ConnectedPlayerTestSupport.createPlayer;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class EnchantmentAnvilGameTests {
    private EnchantmentAnvilGameTests() {}

    @GameTest(template = "gametest/basic_floor")
    public static void exclusivePairsRejectAnvilMerge(GameTestHelper helper) {
        var amplifier = ItemRegistry.COPPER_SPELL_AMPLIFIER.get();
        var charm = ItemRegistry.MANA_SHIELD_CHARM.get();
        var pairs = List.of(
                new ExclusivePair(amplifier, Enchantments.REFLUX, Enchantments.RESERVOIR),
                new ExclusivePair(amplifier, Enchantments.ALACRITY, Enchantments.TENSE),
                new ExclusivePair(amplifier, Enchantments.SURGE, Enchantments.ATTUNEMENT),
                new ExclusivePair(ItemRegistry.SPELLCASTERS_FLASK.get(), Enchantments.RED_ENERGY, Enchantments.GLOW_ENERGY),
                new ExclusivePair(ItemRegistry.ELEMENTAL_BOW.get(), Enchantments.SYNTHESIS,
                        net.minecraft.world.item.enchantment.Enchantments.INFINITY),
                new ExclusivePair(ItemRegistry.ELEMENTAL_BOW.get(), Enchantments.SYNTHESIS,
                        net.minecraft.world.item.enchantment.Enchantments.MENDING),
                new ExclusivePair(charm, Enchantments.SHELL, Enchantments.SYNCHRONIZATION),
                new ExclusivePair(charm, Enchantments.SHELL, Enchantments.NEUTRALIZATION),
                new ExclusivePair(charm, Enchantments.SYNCHRONIZATION, Enchantments.NEUTRALIZATION)
        );
        var lookup = helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        try (var fixture = createPlayer(helper)) {
            var player = fixture.player();
            for (var pair : pairs) {
                for (boolean reverse : new boolean[]{false, true}) {
                    var existing = lookup.getOrThrow(reverse ? pair.second() : pair.first());
                    var added = lookup.getOrThrow(reverse ? pair.first() : pair.second());
                    var stack = new ItemStack(pair.item());
                    var book = createEnchantedBook(added);
                    var accepted = result(player, stack.copy(), book.copy());
                    helper.assertTrue(!accepted.isEmpty() && accepted.getEnchantmentLevel(added) == 1,
                            "The anvil control must accept an isolated enchantment: " + added + " on " + pair.item());
                    stack.enchant(existing, 1);
                    helper.assertTrue(result(player, stack, book).isEmpty(),
                            "The anvil must reject an exclusive pair: " + existing + " / " + added);
                }
            }
            for (var item : List.of(ItemRegistry.SPELLCASTERS_FLASK.get(), ItemRegistry.ALCHEMISTS_FLASK.get())) {
                assertApplication(helper, player, item, Enchantments.LARGE_MUG, true);
                assertApplication(helper, player, item, Enchantments.GUZZLE,
                        item == ItemRegistry.SPELLCASTERS_FLASK.get());
            }
            assertApplication(helper, player, ItemRegistry.FOCUS_STAFFBOW.get(), Enchantments.WISDOM, true);
            assertApplication(helper, player, ItemRegistry.FOCUS_STAFFBOW.get(), Enchantments.PLUNDER, true);
            assertApplication(helper, player, ItemRegistry.IRON_SPELLCASTER_GUN.get(), Enchantments.LARGE_MUG, false);
            var compatible = new ItemStack(amplifier);
            compatible.enchant(lookup.getOrThrow(Enchantments.RESERVOIR), 1);
            var surge = lookup.getOrThrow(Enchantments.SURGE);
            var combined = result(player, compatible, createEnchantedBook(surge));
            helper.assertTrue(!combined.isEmpty() && combined.getEnchantmentLevel(surge) == 1
                            && combined.getEnchantmentLevel(lookup.getOrThrow(Enchantments.RESERVOIR)) == 1,
                    "Independent enchantments must combine through the anvil");
        }
        helper.succeed();
    }

    private static void assertApplication(GameTestHelper helper, ServerPlayer player, Item item,
                                          ResourceKey<Enchantment> key, boolean allowed) {
        var enchantment = helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(key);
        var output = result(player, new ItemStack(item), createEnchantedBook(enchantment));
        helper.assertTrue(allowed ? !output.isEmpty() && output.getEnchantmentLevel(enchantment) == 1 : output.isEmpty(),
                "Anvil application changed: item=" + item + " enchantment=" + key.location() + " allowed=" + allowed);
    }

    private static ItemStack result(ServerPlayer player, ItemStack left, ItemStack right) {
        var menu = new AnvilMenu(0, player.getInventory());
        menu.getSlot(0).set(left);
        menu.getSlot(1).set(right);
        menu.createResult();
        return menu.getSlot(2).getItem().copy();
    }

    private record ExclusivePair(Item item, ResourceKey<Enchantment> first, ResourceKey<Enchantment> second) {}
}
