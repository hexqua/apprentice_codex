package jp.aquafactory.apprenticecodex.gametest;

import io.redspace.ironsspellbooks.api.events.SpellPreCastEvent;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.magic.SpellSelectionManager;
import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import io.redspace.ironsspellbooks.api.spells.SpellData;
import io.redspace.ironsspellbooks.api.util.Utils;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.block.spellcalibrationbench.SpellCalibrationBenchMenu;
import jp.aquafactory.apprenticecodex.config.ApprenticeCodexServerConfig;
import jp.aquafactory.apprenticecodex.enchantment.Enchantments;
import jp.aquafactory.apprenticecodex.item.ScrollSlotTooltipData;
import jp.aquafactory.apprenticecodex.item.curios.shootingstarmantle.MantleCalibration;
import jp.aquafactory.apprenticecodex.item.curios.shootingstarmantle.MantleEnergy;
import jp.aquafactory.apprenticecodex.item.curios.shootingstarmantle.MantleMovement;
import jp.aquafactory.apprenticecodex.item.curios.shootingstarmantle.ShootingStarMantle;
import jp.aquafactory.apprenticecodex.item.curios.shootingstarmantle.ShootingStarMantleRuntime;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import jp.aquafactory.apprenticecodex.utility.SpellSelectionStackResolver;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotContext;

import java.util.List;
import java.util.function.Consumer;

import static io.redspace.ironsspellbooks.registries.ItemRegistry.COOLDOWN_RUNE;
import static io.redspace.ironsspellbooks.registries.ItemRegistry.ICE_RUNE;
import static io.redspace.ironsspellbooks.registries.ItemRegistry.MANA_RUNE;
import static io.redspace.ironsspellbooks.registries.ItemRegistry.PROTECTION_RUNE;
import static io.redspace.ironsspellbooks.registries.ItemRegistry.SCROLL;
import static jp.aquafactory.apprenticecodex.registry.SpellRegistry.WAVERING_STAR;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class MantleCalibrationGameTests {
    private static final String TEMPLATE = "gametest/basic_floor";

    private MantleCalibrationGameTests() { }

    @GameTest(template = TEMPLATE)
    public static void benchExpandsFromZeroAndPreservesInactiveScrolls(GameTestHelper helper) {
        var player = player(helper, "mantle_bench");
        var lookup = player.registryAccess();
        var stack = new ItemStack(ItemRegistry.SHOOTING_STAR_MANTLE.get());
        var menu = new SpellCalibrationBenchMenu(0, player.getInventory());
        helper.assertTrue(menu.getSlot(0).mayPlace(stack), "Bench must accept a new mantle with zero scroll slots");
        menu.getSlot(0).set(stack);
        helper.assertTrue(menu.getEnabledScrollSlotCount() == 0, "New mantles must have zero scroll slots");
        var scroll = scroll(SpellRegistry.MAGIC_MISSILE_SPELL.get());
        scroll.set(DataComponents.CUSTOM_NAME, Component.literal("Owned scroll"));
        helper.assertFalse(menu.getSlot(4).mayPlace(scroll), "Unexpanded mantle must reject scroll insertion");
        for (int slot = 0; slot < 3; slot++) {
            var parchment = new ItemStack(ItemRegistry.SCROLLWOVEN_PARCHMENT.get());
            helper.assertTrue(menu.getSlot(slot + 1).mayPlace(parchment), "Parchment must be repeatable");
            menu.getSlot(slot + 1).set(parchment);
            helper.assertTrue(menu.getEnabledScrollSlotCount() == slot + 1, "Each parchment must add one slot");
            menu.getSlot(slot + 4).set(scroll.copy());
        }
        for (var spell : SpellRegistry.getEnabledSpells()) {
            helper.assertTrue(menu.getSlot(4).mayPlace(scroll(spell)), "All valid spells must be accepted: " + spell.getSpellResource());
        }
        helper.assertFalse(menu.getSlot(4).mayPlace(new ItemStack(SCROLL.get())), "Blank scrolls must be rejected");
        helper.assertFalse(menu.getSlot(4).mayPlace(new ItemStack(Items.STONE)), "Non-scroll items must be rejected");
        helper.assertFalse(menu.getSlot(7).mayPlace(scroll), "Fourth scroll slot must remain unavailable");
        new MantleEnergy(42, false, 7).save(stack);
        for (int slot = 1; slot <= 3; slot++) menu.getSlot(slot).set(ItemStack.EMPTY);
        var restored = ItemStack.parse(lookup, stack.save(lookup)).orElseThrow();
        helper.assertTrue(MantleEnergy.read(restored).energy() == 42, "Calibration must preserve mantle energy");
        var tooltip = MantleCalibration.tooltipData(restored, lookup);
        helper.assertTrue(tooltip.entries().size() == 3 && tooltip.entries().stream().noneMatch(ScrollSlotTooltipData.Entry::usable)
                        && tooltip.selectedSlot() == -1 && tooltip.selectedSpell() == SpellData.EMPTY,
                "Inactive scrolls must remain visible without a selected spell");
        menu.getSlot(0).set(restored);
        for (int slot = 1; slot <= 3; slot++) menu.getSlot(slot).set(new ItemStack(ItemRegistry.SCROLLWOVEN_PARCHMENT.get()));
        helper.assertTrue(MantleCalibration.tooltipData(restored, lookup).entries().stream().allMatch(ScrollSlotTooltipData.Entry::usable),
                "Re-expansion must reactivate every stored scroll");
        for (int slot = 1; slot <= 3; slot++) menu.getSlot(slot).set(ItemStack.EMPTY);
        for (int slot = 0; slot < 3; slot++) {
            var inventorySlot = menu.getSlot(slot + 4);
            helper.assertTrue(inventorySlot.mayPickup(player), "Inactive scrolls must remain extractable");
            helper.assertTrue(ItemStack.isSameItemSameComponents(scroll, inventorySlot.remove(1)), "Extraction must preserve scroll components");
            helper.assertTrue(MantleCalibration.getScroll(restored, slot, lookup).isEmpty(), "Extraction must not duplicate scrolls");
        }
        helper.assertFalse(ISpellContainer.isSpellContainer(restored), "Mantle must never gain a projected spell container");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void runesAreUniqueAndProtectionUsesCurioAttributes(GameTestHelper helper) {
        var player = player(helper, "mantle_runes");
        var stack = new ItemStack(ItemRegistry.SHOOTING_STAR_MANTLE.get());
        var mantle = (ShootingStarMantle) stack.getItem();
        for (var rune : List.of(PROTECTION_RUNE.get(), COOLDOWN_RUNE.get(), ICE_RUNE.get())) {
            helper.assertTrue(mantle.trySetCalibrationAdjustment(stack, 0, new ItemStack(rune)), "First rune must be accepted");
            helper.assertFalse(mantle.trySetCalibrationAdjustment(stack, 1, new ItemStack(rune)), "Duplicate rune must be rejected");
            mantle.trySetCalibrationAdjustment(stack, 0, ItemStack.EMPTY);
        }
        var context = new SlotContext("back", player, 0, false, true);
        var id = ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "gametest/mantle_resist");
        mantle.trySetCalibrationAdjustment(stack, 0, new ItemStack(PROTECTION_RUNE.get()));
        var modifiers = mantle.getAttributeModifiers(context, id, stack);
        var resist = modifiers.get(AttributeRegistry.SPELL_RESIST);
        helper.assertTrue(resist.size() == 1 && resist.iterator().next().amount() == 0.1
                        && resist.iterator().next().operation() == AttributeModifier.Operation.ADD_MULTIPLIED_BASE,
                "Protection rune must supply one ten-percent base resistance modifier");
        var original = player.getAttributeValue(AttributeRegistry.SPELL_RESIST);
        player.getAttributes().addTransientAttributeModifiers(modifiers);
        helper.assertTrue(Math.abs(player.getAttributeValue(AttributeRegistry.SPELL_RESIST) - original * 1.1) < 1.0e-6,
                "Equipping the modifier must increase spell resistance by ten percent");
        player.getAttributes().removeAttributeModifiers(modifiers);
        helper.assertTrue(player.getAttributeValue(AttributeRegistry.SPELL_RESIST) == original, "Unequipping must restore resistance");
        mantle.trySetCalibrationAdjustment(stack, 0, ItemStack.EMPTY);
        helper.assertTrue(mantle.getAttributeModifiers(context, id, stack).get(AttributeRegistry.SPELL_RESIST).isEmpty(),
                "Removing the rune must remove its modifier");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void manaRuneChangesCapacityWithoutRestoringEnergy(GameTestHelper helper) {
        var stack = new ItemStack(ItemRegistry.SHOOTING_STAR_MANTLE.get());
        var mantle = (ShootingStarMantle) stack.getItem();
        helper.assertTrue(mantle.trySetCalibrationAdjustment(stack, 0, new ItemStack(MANA_RUNE.get())),
                "Mana rune must be accepted");
        var energy = MantleEnergy.read(stack);
        helper.assertTrue(energy.energy() == 100 && energy.maxEnergy() == 200 && mantle.getBarWidth(stack) == 7,
                "Inserting a rune into a new mantle must keep one hundred energy and show half capacity");
        helper.assertFalse(mantle.trySetCalibrationAdjustment(stack, 1, new ItemStack(MANA_RUNE.get())),
                "Duplicate mana rune must be rejected");
        helper.assertTrue(mantle.trySetCalibrationAdjustment(stack, 1, new ItemStack(PROTECTION_RUNE.get())),
                "Mana rune must coexist with other runes");
        mantle.trySetCalibrationAdjustment(stack, 0, ItemStack.EMPTY);
        new MantleEnergy(60, false, 7).save(stack);
        mantle.trySetCalibrationAdjustment(stack, 0, new ItemStack(MANA_RUNE.get()));
        helper.assertTrue(MantleEnergy.read(stack).energy() == 60 && MantleEnergy.read(stack).maxEnergy() == 200,
                "Rune insertion must preserve partially spent energy");
        new MantleEnergy(160, false, 7, 200).save(stack);
        mantle.trySetCalibrationAdjustment(stack, 0, ItemStack.EMPTY);
        helper.assertTrue(MantleEnergy.read(stack).energy() == 100 && MantleEnergy.read(stack).maxEnergy() == 100,
                "Removing the rune must clamp surplus energy");
        mantle.trySetCalibrationAdjustment(stack, 0, new ItemStack(MANA_RUNE.get()));
        helper.assertTrue(MantleEnergy.read(stack).energy() == 100,
                "Reinserting the rune must not restore discarded energy");
        var restored = ItemStack.parse(helper.getLevel().registryAccess(), stack.save(helper.getLevel().registryAccess())).orElseThrow();
        helper.assertTrue(MantleEnergy.read(restored).energy() == 100 && MantleEnergy.read(restored).maxEnergy() == 200,
                "Capacity and current energy must survive serialization");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void manaRuneKeepsRecoveryAmountAndCost(GameTestHelper helper) {
        var player = player(helper, "mantle_capacity_recovery");
        var stack = equip(player);
        var mantle = (ShootingStarMantle) stack.getItem();
        mantle.trySetCalibrationAdjustment(stack, 0, new ItemStack(MANA_RUNE.get()));
        var magic = MagicData.getPlayerMagicData(player);
        float cost = ApprenticeCodexServerConfig.shootingStarMantleRecoveryCost();
        try {
            new MantleEnergy(198, false, 0, 200).save(stack);
            magic.setMana(cost);
            for (int tick = 0; tick < 9; tick++) ShootingStarMantleRuntime.tick(player);
            helper.assertTrue(MantleEnergy.read(stack).energy() == 198 && magic.getMana() == cost,
                    "Capacity rune must retain the ten-tick recovery interval");
            ShootingStarMantleRuntime.tick(player);
            helper.assertTrue(MantleEnergy.read(stack).energy() == 200 && magic.getMana() == 0,
                    "Capacity rune must restore two energy at the original mana cost");
            magic.setMana(cost);
            for (int tick = 0; tick < 10; tick++) ShootingStarMantleRuntime.tick(player);
            helper.assertTrue(MantleEnergy.read(stack).energy() == 200 && magic.getMana() == cost,
                    "Full capacity must not consume mana");
        } finally {
            ShootingStarMantleRuntime.clear(player);
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void recoveryRuneUsesFastCostWithoutLockingUsableEnergy(GameTestHelper helper) {
        var player = player(helper, "mantle_fast_recovery");
        var stack = equip(player);
        var mantle = (ShootingStarMantle) stack.getItem();
        mantle.trySetCalibrationAdjustment(stack, 0, new ItemStack(COOLDOWN_RUNE.get()));
        var magic = MagicData.getPlayerMagicData(player);
        float cost = ApprenticeCodexServerConfig.shootingStarMantleRecoveryCost();
        try {
            for (boolean recovering : new boolean[]{false, true}) {
                new MantleEnergy(50, recovering, 7).save(stack);
                magic.setMana(cost);
                for (int tick = 0; tick < 9; tick++) ShootingStarMantleRuntime.tick(player);
                helper.assertTrue(MantleEnergy.read(stack).energy() == 50, "Fast recovery must still wait ten idle ticks");
                ShootingStarMantleRuntime.tick(player);
                helper.assertTrue(MantleEnergy.read(stack).energy() == 65 && magic.getMana() == 0,
                        "Rune must restore fifteen energy at the shared cost");
                helper.assertTrue(MantleEnergy.read(stack).recovering() == recovering,
                        "Recovery rune must not change the depletion lock");
                if (cost > 0) {
                    magic.setMana(cost - 1);
                    for (int tick = 0; tick < 10; tick++) ShootingStarMantleRuntime.tick(player);
                    helper.assertTrue(MantleEnergy.read(stack).energy() == 65 && magic.getMana() == cost - 1,
                            "Insufficient fast-recovery mana must change neither resource");
                }
            }
            new MantleEnergy(99, true, 7).save(stack);
            magic.setMana(cost);
            for (int tick = 0; tick < 10; tick++) ShootingStarMantleRuntime.tick(player);
            helper.assertTrue(MantleEnergy.read(stack).usable() && magic.getMana() == 0,
                    "Final partial refill must pay full cost and unlock the mantle");
            new MantleEnergy(50, false, 7).save(stack);
            helper.assertTrue(ShootingStarMantleRuntime.toggle(player), "Rune must not lock usable mantle energy");
            magic.setMana(cost);
            for (int tick = 0; tick < 10; tick++) ShootingStarMantleRuntime.tick(player);
            helper.assertTrue(MantleEnergy.read(stack).energy() == 50 && magic.getMana() == cost,
                    "Hovering must not recharge even with the rune");
        } finally {
            ShootingStarMantleRuntime.clear(player);
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void iceRuneRetainsMomentumAfterTravelAndDepletedServerImpulse(GameTestHelper helper) {
        var player = player(helper, "mantle_drift");
        var stack = equip(player);
        var mantle = (ShootingStarMantle) stack.getItem();
        mantle.trySetCalibrationAdjustment(stack, 0, new ItemStack(ICE_RUNE.get()));
        var start = helper.absoluteVec(new Vec3(0.5, 2, 0.5));
        for (int x = 0; x <= 7; x++) {
            for (int y = 1; y <= 4; y++) helper.setBlock(new BlockPos(x, y, 0), Blocks.AIR);
        }
        try {
            player.setPos(start);
            player.setDeltaMovement(Vec3.ZERO);
            var state = ShootingStarMantleRuntime.state(player);
            state.dashDirection = new Vec3(1, 0, 0);
            state.dashTicks = 5;
            for (int tick = 0; tick < 5; tick++) MantleMovement.travel(player, Vec3.ZERO, state);
            helper.assertTrue(Math.abs(player.getX() - start.x - 5) < 0.001 && state.dashTicks == 0,
                    "Ice rune must preserve the five-tick propulsion window");
            helper.assertTrue(player.getDeltaMovement().x > 0.9, "Ice rune must preserve post-dash momentum");
            MantleMovement.travel(player, Vec3.ZERO, state);
            helper.assertTrue(player.getX() > start.x + 5.8 && player.getDeltaMovement().x < 0.91,
                    "Post-dash travel must keep normal drag");
            player.setPos(start);
            player.setDeltaMovement(Vec3.ZERO);
            state.lastPosition = null;
            state.dashTicks = 5;
            for (int y = 1; y <= 4; y++) helper.setBlock(new BlockPos(2, y, 0), Blocks.STONE);
            for (int tick = 0; tick < 6; tick++) MantleMovement.travel(player, Vec3.ZERO, state);
            helper.assertTrue(player.getX() < start.x + 2, "Ice rune must not bypass solid collisions");
            // 枯渇終了のserver経路でも、利用制限は残して水平速度だけを維持する。
            new MantleEnergy(5, false, 0).save(stack);
            helper.assertTrue(ShootingStarMantleRuntime.toggle(player), "Mantle must enter hover before depletion");
            helper.assertTrue(ShootingStarMantleRuntime.impulse(player, 0, 1, 0), "Final impulse must be accepted");
            helper.assertTrue(MantleEnergy.read(stack).energy() == 0, "Ice rune impulse must cost five energy");
            for (int tick = 0; tick < 5; tick++) ShootingStarMantleRuntime.tick(player);
            helper.assertTrue(MantleEnergy.read(stack).recovering() && !state.hovering && state.dashTicks == 0,
                    "Ice rune must not bypass depletion or extend propulsion");
            helper.assertTrue(player.getDeltaMovement().horizontalDistanceSqr() > 0.9,
                    "Server must retain momentum after the final depleted impulse");
            helper.assertFalse(ShootingStarMantleRuntime.impulse(player, 1, 1, 0), "Depleted mantle must reject another impulse");
            mantle.trySetCalibrationAdjustment(stack, 0, ItemStack.EMPTY);
            MantleMovement.finishImpulse(player);
            helper.assertTrue(player.getDeltaMovement().horizontalDistanceSqr() == 0, "Removing ice rune must restore stopping");
        } finally {
            ShootingStarMantleRuntime.clear(player);
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void scrollWheelUsesAllSlotsAndTranscendenceOnlyForStoredSpells(GameTestHelper helper) {
        var player = player(helper, "mantle_scroll_wheel");
        var stack = equip(player);
        var mantle = (ShootingStarMantle) stack.getItem();
        var lookup = player.registryAccess();
        var enchantment = lookup.lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.TRANSCENDENCE);
        for (int slot = 0; slot < 3; slot++) mantle.trySetCalibrationAdjustment(stack, slot, new ItemStack(ItemRegistry.SCROLLWOVEN_PARCHMENT.get()));
        var spell = SpellRegistry.MAGIC_MISSILE_SPELL.get();
        var scroll = scroll(spell);
        MantleCalibration.setScroll(stack, 0, scroll, lookup);
        MantleCalibration.setScroll(stack, 2, scroll, lookup);
        try {
            for (int level : new int[]{0, 1, 3, 10}) {
                if (level > 0) {
                    var beforeEnchant = stack.copy();
                    stack.enchant(enchantment, level);
                    helper.assertFalse(MantleCalibration.hasSameScrollSelection(beforeEnchant, stack, lookup),
                            "Enchantment changes must invalidate wheel contents");
                }
                int expected = spell.getMaxLevel() + (level > 0 ? 1 : 0);
                var snapshot = stack.copy();
                var manager = new SpellSelectionManager(player);
                var options = manager.getSpellsForSlot(ShootingStarMantleRuntime.SCROLL_SPELL_SLOT);
                helper.assertTrue(options.size() == 2 && options.get(0).slotIndex == 0 && options.get(1).slotIndex == 1,
                        "Sparse duplicate scrolls must be independently published with consecutive indices");
                helper.assertTrue(options.stream().allMatch(option -> option.spellData.getLevel() == expected),
                        "Transcendence must apply a fixed bonus exactly once");
                var innate = manager.getSpellsForSlot(ShootingStarMantleRuntime.SPELL_SLOT);
                helper.assertTrue(innate.size() == 1 && innate.getFirst().spellData.getSpell() == WAVERING_STAR.get()
                                && innate.getFirst().spellData.getLevel() == 1, "Innate Wavering Star must remain unchanged");
                helper.assertTrue(MantleCalibration.tooltipData(stack, lookup).entries().stream()
                        .allMatch(entry -> entry.spell().getLevel() == expected), "Tooltip and wheel levels must match");
                helper.assertTrue(SpellSelectionStackResolver.resolveSelectionStack(player, options.getFirst().slot) == stack,
                        "Scroll selection must resolve its equipped mantle");
                int[] observed = {-1};
                Consumer<SpellPreCastEvent> listener = event -> {
                    if (event.getEntity() == player) {
                        observed[0] = event.getSpellLevel();
                        event.setCanceled(true);
                    }
                };
                NeoForge.EVENT_BUS.addListener(listener);
                try {
                    MagicData.getPlayerMagicData(player).setMana(10000);
                    Utils.serverSideInitiateQuickCast(player, options.getFirst().globalIndex);
                    helper.assertTrue(observed[0] == expected, "Wheel casting must receive the resolved level");
                } finally {
                    NeoForge.EVENT_BUS.unregister(listener);
                }
                helper.assertTrue(ItemStack.isSameItemSameComponents(snapshot, stack)
                                && ItemStack.isSameItemSameComponents(scroll, MantleCalibration.getScroll(stack, 0, lookup)),
                        "Wheel, tooltip and casting must preserve the original scroll and mantle energy");
            }
            var snapshot = stack.copy();
            new MantleEnergy(0, true, 0).save(stack);
            player.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.ELYTRA));
            helper.assertTrue(MantleCalibration.hasSameScrollSelection(snapshot, stack, lookup), "Energy changes must not invalidate wheel contents");
            helper.assertTrue(new SpellSelectionManager(player).getSpellsForSlot(ShootingStarMantleRuntime.SCROLL_SPELL_SLOT).size() == 2,
                    "Depletion and flight conflict must not hide scrolls");
            mantle.trySetCalibrationAdjustment(stack, 2, ItemStack.EMPTY);
            helper.assertFalse(MantleCalibration.hasSameScrollSelection(snapshot, stack, lookup), "Slot changes must invalidate wheel contents");
            helper.assertTrue(new SpellSelectionManager(player).getSpellsForSlot(ShootingStarMantleRuntime.SCROLL_SPELL_SLOT).size() == 1,
                    "Only disabled scrolls must disappear");
            snapshot = stack.copy();
            MantleCalibration.setScroll(stack, 0, scroll(SpellRegistry.FIRE_ARROW_SPELL.get()), lookup);
            helper.assertFalse(MantleCalibration.hasSameScrollSelection(snapshot, stack, lookup), "Replacing a scroll must invalidate wheel contents");
            helper.assertTrue(new SpellSelectionManager(player).getSpellsForSlot(ShootingStarMantleRuntime.SCROLL_SPELL_SLOT)
                            .getFirst().spellData.getSpell() == SpellRegistry.FIRE_ARROW_SPELL.get(),
                    "Wheel must publish the replacement scroll");
            var back = CuriosApi.getCuriosInventory(player).orElseThrow().getCurios().get("back");
            back.getStacks().setStackInSlot(0, ItemStack.EMPTY);
            back.getCosmeticStacks().setStackInSlot(0, stack);
            helper.assertTrue(new SpellSelectionManager(player).getSpellsForSlot(ShootingStarMantleRuntime.SCROLL_SPELL_SLOT).isEmpty(),
                    "Cosmetic mantles must not supply scrolls");
        } finally {
            ShootingStarMantleRuntime.clear(player);
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void additionalBackSlotsPublishOnlyTheFirstFunctionalMantle(GameTestHelper helper) {
        var player = player(helper, "mantle_multiple_scrolls");
        var first = equip(player);
        var second = new ItemStack(ItemRegistry.SHOOTING_STAR_MANTLE.get());
        var mantle = (ShootingStarMantle) first.getItem();
        var lookup = player.registryAccess();
        for (var stack : List.of(first, second)) {
            mantle.trySetCalibrationAdjustment(stack, 0, new ItemStack(ItemRegistry.SCROLLWOVEN_PARCHMENT.get()));
        }
        MantleCalibration.setScroll(first, 0, scroll(SpellRegistry.MAGIC_MISSILE_SPELL.get()), lookup);
        MantleCalibration.setScroll(second, 0, scroll(SpellRegistry.FIRE_ARROW_SPELL.get()), lookup);
        var inventory = CuriosApi.getCuriosInventory(player).orElseThrow();
        inventory.addTransientSlotModifier("back", ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID,
                "gametest/mantle_scroll_back"), 1, AttributeModifier.Operation.ADD_VALUE);
        inventory.setEquippedCurio("back", 1, second);
        try {
            var options = new SpellSelectionManager(player).getSpellsForSlot(ShootingStarMantleRuntime.SCROLL_SPELL_SLOT);
            helper.assertTrue(options.size() == 1 && options.getFirst().spellData.getSpell() == SpellRegistry.MAGIC_MISSILE_SPELL.get(),
                    "Only the first functional mantle must supply wheel scrolls");
            inventory.setEquippedCurio("back", 0, ItemStack.EMPTY);
            options = new SpellSelectionManager(player).getSpellsForSlot(ShootingStarMantleRuntime.SCROLL_SPELL_SLOT);
            helper.assertTrue(options.size() == 1 && options.getFirst().spellData.getSpell() == SpellRegistry.FIRE_ARROW_SPELL.get(),
                    "Removing the first mantle must expose the next functional mantle");
        } finally {
            ShootingStarMantleRuntime.clear(player);
        }
        helper.succeed();
    }

    private static ItemStack scroll(AbstractSpell spell) {
        var stack = new ItemStack(SCROLL.get());
        ISpellContainer.createScrollContainer(spell, spell.getMaxLevel(), stack);
        return stack;
    }

    private static FakePlayer player(GameTestHelper helper, String name) {
        return ApprenticeCodexGameTestScenarios.createEquipmentTestPlayer(helper, new BlockPos(1, 3, 1), name);
    }

    private static ItemStack equip(FakePlayer player) {
        var stack = new ItemStack(ItemRegistry.SHOOTING_STAR_MANTLE.get());
        CuriosApi.getCuriosInventory(player).orElseThrow().setEquippedCurio("back", 0, stack);
        ShootingStarMantleRuntime.refreshEquipment(player);
        player.setOnGround(false);
        return stack;
    }
}
