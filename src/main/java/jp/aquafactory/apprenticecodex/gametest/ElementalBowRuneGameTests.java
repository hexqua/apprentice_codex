package jp.aquafactory.apprenticecodex.gametest;

import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.block.spellcalibrationbench.SpellCalibrationBenchMenu;
import jp.aquafactory.apprenticecodex.config.ApprenticeCodexServerConfig;
import jp.aquafactory.apprenticecodex.item.elementalbow.*;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class ElementalBowRuneGameTests {
    private ElementalBowRuneGameTests() {}

    private static ItemStack rune(String name) {
        return new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("irons_spellbooks", name)));
    }

    private static ItemStack bow(GameTestHelper helper, boolean school, boolean recovery) {
        var stack = new ItemStack(ItemRegistry.ELEMENTAL_BOW.get());
        var item = (ElementalBow) stack.getItem();
        var lookup = helper.getLevel().registryAccess();
        item.trySetCalibrationAdjustment(stack, 0, rune("lesser_spell_slot_upgrade"), lookup);
        if (school) item.trySetCalibrationAdjustment(stack, 1, rune("fire_rune"), lookup);
        if (recovery) item.trySetCalibrationAdjustment(stack, 2,
                new ItemStack(io.redspace.ironsspellbooks.registries.ItemRegistry.COOLDOWN_RUNE.get()), lookup);
        for (int slot = 0; slot < 2; slot++) ElementalBow.setCalibrationScroll(stack, slot,
                BowGameTestSupport.createSpellScroll(SpellRegistry.FIRE_ARROW_SPELL.get()), lookup);
        select(stack, 0);
        return stack;
    }

    private static void select(ItemStack stack, int slot) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
            tag.putString("ElementalBowShotMode", "magic");
            tag.putString("ElementalBowMode", ElementalBow.selectionIdForSlot(slot).toString());
        });
    }

    @GameTest(template = "gametest/basic_floor")
    public static void elementalBowRuneConfigSyncUpdatesExistingJeiRecipe(GameTestHelper helper) {
        double previous = ElementalBowClientConfigState.schoolRuneManaCostMultiplier();
        var buffer = new net.minecraft.network.FriendlyByteBuf(io.netty.buffer.Unpooled.buffer());
        try (var config = ApprenticeCodexServerConfig.overrideElementalBowSchoolRuneManaCostMultiplierForGameTest(2.5)) {
            var packet = new jp.aquafactory.apprenticecodex.network.packet.SyncElementalBowConfigPacket(java.util.List.of());
            jp.aquafactory.apprenticecodex.network.packet.SyncElementalBowConfigPacket.encode(packet, buffer);
            var decoded = jp.aquafactory.apprenticecodex.network.packet.SyncElementalBowConfigPacket.decode(buffer);
            helper.assertTrue(decoded.schoolRuneManaCostMultiplier() == 2.5, "Config codec must preserve fractional rune multipliers");
            var stack = bow(helper, true, false);
            var recipe = new jp.aquafactory.apprenticecodex.compat.jei.SpellCalibrationAdjustmentJeiRecipe(
                    ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "rune_sync_test"),
                    stack, java.util.List.of(rune("fire_rune")), java.util.List.of(stack),
                    () -> jp.aquafactory.apprenticecodex.item.CalibrationAdjustmentEffects.forceSpellSchool(
                            ElementalBowRunes.configuredManaMultiplier(true)),
                    jp.aquafactory.apprenticecodex.item.CalibrationConstraintDisplay.none());
            ElementalBowClientConfigState.setSchoolRuneManaCostMultiplier(2);
            var before = (net.minecraft.network.chat.contents.TranslatableContents) recipe.effectLines().get(1).getContents();
            helper.assertTrue(((Number) before.getArgs()[0]).longValue() == 100,
                    "Double mana must be shown as a 100 percent JEI increase");
            ElementalBowClientConfigState.setSchoolRuneManaCostMultiplier(decoded.schoolRuneManaCostMultiplier());
            var after = (net.minecraft.network.chat.contents.TranslatableContents) recipe.effectLines().get(1).getContents();
            helper.assertTrue(((Number) after.getArgs()[0]).longValue() == 150,
                    "An existing JEI recipe must use newly synchronized config");
            ElementalBowClientConfigState.reset();
            helper.assertTrue(ElementalBowClientConfigState.schoolRuneManaCostMultiplier() == 2,
                    "Disconnect must restore the default multiplier");
        } finally {
            buffer.release();
            ElementalBowClientConfigState.setSchoolRuneManaCostMultiplier(previous);
        }
        helper.succeed();
    }

    @GameTest(template = "gametest/basic_floor")
    public static void elementalBowRunesShareThreeAdjustmentSlots(GameTestHelper helper) {
        var player = BowGameTestSupport.createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "bow_rune_bench");
        var stack = bow(helper, true, true);
        var menu = new SpellCalibrationBenchMenu(0, player.getInventory());
        menu.getSlot(0).set(stack);
        helper.assertTrue(ElementalBow.getEnabledCalibrationScrollSlotCount(stack) == 2,
                "Both runes must leave room for only one slot upgrade");
        helper.assertTrue(ElementalBowRunes.school(stack) == SchoolRegistry.FIRE.get()
                && ElementalBowRunes.separatesOverheat(stack), "Both runes must apply together");
        helper.assertFalse(menu.getSlot(1).mayPlace(rune("ice_rune")), "Different school runes must still conflict");
        helper.assertFalse(menu.getSlot(1).mayPlace(new ItemStack(
                io.redspace.ironsspellbooks.registries.ItemRegistry.COOLDOWN_RUNE.get())), "Recovery runes must be unique");
        var secondScroll = ElementalBow.getCalibrationScroll(stack, 1, helper.getLevel().registryAccess()).copy();
        menu.getSlot(1).set(ItemStack.EMPTY);
        helper.assertTrue(ElementalBow.getEnabledCalibrationScrollSlotCount(stack) == 1,
                "Removing the upgrade must disable the second scroll slot");
        helper.assertTrue(ItemStack.isSameItemSameComponents(secondScroll,
                ElementalBow.getCalibrationScroll(stack, 1, helper.getLevel().registryAccess())),
                "Adjusting runes must preserve inactive owned scrolls");
        helper.succeed();
    }

    @GameTest(template = "gametest/basic_floor")
    public static void elementalBowRuneManaIncludesHeatAndHonorsConfig(GameTestHelper helper) {
        helper.succeedIf(() -> {
            try (var spells = BowGameTestSupport.useElementalBowSpellConfig(helper);
                 var heat = BowGameTestSupport.useElementalBowConfigOverrideForGameTest(1, .2, .08, 1, 0, 0)) {
                for (double multiplier : new double[]{1, 1.25, 2, 10}) {
                    try (var config = ApprenticeCodexServerConfig.overrideElementalBowSchoolRuneManaCostMultiplierForGameTest(multiplier)) {
                        var player = BowGameTestSupport.createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "bow_rune_mana");
                        player.getAttribute(AttributeRegistry.MAX_MANA).setBaseValue(20000);
                        var stack = bow(helper, true, true);
                        if (multiplier == 1.25 || multiplier == 10) {
                            ElementalBow.setCalibrationScroll(stack, 0,
                                    BowGameTestSupport.createSpellScroll(SpellRegistry.MAGIC_ARROW_SPELL.get()),
                                    helper.getLevel().registryAccess());
                        }
                        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
                        player.getInventory().setItem(1, new ItemStack(Items.ARROW, 5));
                        var magic = MagicData.getPlayerMagicData(player);
                        var profile = ElementalBow.getDisplayedSpellProfile(stack);
                        int base = profile.spell().getManaCost(profile.spellLevel());
                        int scaled = (int) Math.ceil(base * multiplier);
                        // 同系統のルーンでも通常分・過熱分の両方にペナルティを課す。
                        ElementalBowOverheatManager.applyOverheatAfterCast(player, 200, 0, true);
                        float expected = scaled + (float) (base * .28 * multiplier);
                        magic.setMana(expected - 1);
                        helper.assertFalse(stack.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND)
                                .getResult().consumesAction(), "Insufficient total rune mana must reject drawing");
                        helper.assertTrue(stack.getDamageValue() == 0
                                && ElementalBowOverheatManager.getState(player, 0, true).chainDepth() == 1,
                                "Failed drawing must not consume durability or deepen heat");
                        magic.setMana(10000);
                        stack.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
                        stack.getItem().releaseUsing(stack, helper.getLevel(), player, stack.getUseDuration(player) - 20);
                        player.stopUsingItem();
                        helper.assertTrue(stack.getDamageValue() == 1, "The rune bow must really fire");
                        helper.assertTrue(Math.abs(magic.getMana() - (10000 - expected)) < .01,
                                "Rune multiplier must apply once to normal and additional mana: " + magic.getMana());
                        helper.assertFalse(ElementalBowCasting.isActive(player, profile.spell()),
                                "Casting scope must be cleared after firing");
                        helper.assertTrue(profile.spell().getManaCost(profile.spellLevel()) == base,
                                "Rune mana must not alter the shared spell definition");
                    }
                }
            }
        });
    }

    @GameTest(template = "gametest/basic_floor")
    public static void elementalBowSchoolPowerScopeIsIsolated(GameTestHelper helper) {
        var player = BowGameTestSupport.createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "bow_rune_power");
        var other = BowGameTestSupport.createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "bow_rune_other");
        var stack = bow(helper, true, false);
        var spell = SpellRegistry.MAGIC_ARROW_SPELL.get();
        player.getAttribute(AttributeRegistry.FIRE_SPELL_POWER).setBaseValue(3);
        var basePower = spell.getSpellPower(1, player);
        var originalSchool = spell.getSchoolType();
        var otherPower = spell.getSpellPower(1, other);
        try {
            try (var ignored = ElementalBowSpellPowerContext.open(player, spell, stack)) {
                helper.assertTrue(Math.abs(spell.getSpellPower(1, player) - basePower * 3) < .001,
                        "A different-school spell must use fire rune power");
                helper.assertTrue(spell.getSpellPower(1, other) == otherPower, "Rune scope must isolate the caster");
                helper.assertTrue(ElementalBowSpellPowerContext.school(SpellRegistry.POISON_ARROW_SPELL.get(), player) == null,
                        "Rune scope must isolate the spell");
                helper.assertFalse(ElementalBowCasting.isActive(player, spell), "Power preview must not bypass cooldowns");
                try (var nested = ElementalBowSpellPowerContext.open(player, spell, bow(helper, false, false))) {
                    helper.assertTrue(spell.getSpellPower(1, player) == basePower, "Nested unruned scope must use normal power");
                }
                helper.assertTrue(ElementalBowSpellPowerContext.school(spell, player) == SchoolRegistry.FIRE.get(),
                        "Nested scopes must restore the previous rune");
                throw new IllegalStateException("Intentional scope cleanup probe");
            }
        } catch (IllegalStateException expected) {
            helper.assertTrue(expected.getMessage().equals("Intentional scope cleanup probe"), "Unexpected exception");
        }
        helper.assertTrue(spell.getSpellPower(1, player) == basePower, "Exceptions must not leak rune power");
        helper.assertTrue(spell.getSchoolType() == originalSchool, "The spell's actual school must not change");
        helper.succeed();
    }

    @GameTest(template = "gametest/basic_floor")
    public static void elementalBowRecoveryUsesPlayerSlotNumbers(GameTestHelper helper) {
        helper.succeedIf(() -> {
            try (var spells = BowGameTestSupport.useElementalBowSpellConfig(helper)) {
                var player = BowGameTestSupport.createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "bow_recovery_slots");
                player.getAttribute(AttributeRegistry.MAX_MANA).setBaseValue(20000);
                var first = bow(helper, false, true);
                var second = bow(helper, false, true);
                player.setItemInHand(InteractionHand.MAIN_HAND, first);
                player.getInventory().setItem(1, new ItemStack(Items.ARROW, 8));
                MagicData.getPlayerMagicData(player).setMana(10000);
                first.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
                first.getItem().releaseUsing(first, helper.getLevel(), player, first.getUseDuration(player) - 20);
                player.stopUsingItem();
                helper.assertTrue(ElementalBowOverheatManager.getState(player, 0, true).chainDepth() == 1
                        && !ElementalBowOverheatManager.getState(player, 1, true).active(),
                        "Recovery must heat only the fired slot");
                var views = ElementalBow.getAvailableSelectionViews(player, second);
                helper.assertTrue(views.stream().anyMatch(v -> ElementalBow.selectionIdForSlot(0).equals(v.selection().selectionId()) && v.overheatActive()),
                        "A second bow must show heat for the same slot number");
                helper.assertTrue(views.stream().anyMatch(v -> ElementalBow.selectionIdForSlot(1).equals(v.selection().selectionId()) && !v.overheatActive()),
                        "A duplicate spell in another slot must show no heat");
                select(second, 1);
                player.setItemInHand(InteractionHand.MAIN_HAND, second);
                second.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
                second.getItem().releaseUsing(second, helper.getLevel(), player, second.getUseDuration(player) - 20);
                player.stopUsingItem();
                helper.assertTrue(ElementalBowOverheatManager.getState(player, 0, true).chainDepth() == 1
                        && ElementalBowOverheatManager.getState(player, 1, true).chainDepth() == 1,
                        "Duplicate spells must heat their own slots across bows");
                ((ElementalBow) second.getItem()).trySetCalibrationAdjustment(second, 2, ItemStack.EMPTY, helper.getLevel().registryAccess());
                second.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
                second.getItem().releaseUsing(second, helper.getLevel(), player, second.getUseDuration(player) - 20);
                player.stopUsingItem();
                for (int slot = 0; slot < 4; slot++) helper.assertTrue(
                        ElementalBowOverheatManager.getState(player, slot, true).chainDepth() == 2,
                        "Removing Recovery must synchronize even empty and disabled slots");
            }
        });
    }

    @GameTest(template = "gametest/basic_floor")
    public static void elementalBowUnrunedHeatSynchronizesWorstState(GameTestHelper helper) {
        try (var config = BowGameTestSupport.useElementalBowConfigOverrideForGameTest(1, .2, .08, 1, 0, 0)) {
            var player = BowGameTestSupport.createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "bow_worst_heat");
            for (int i = 0; i < 3; i++) ElementalBowOverheatManager.applyOverheatAfterCast(player, 200, 0, true);
            ElementalBowOverheatManager.applyOverheatAfterCast(player, 400, 1, true);
            long longest = ElementalBowOverheatManager.getState(player, 1, true).expireGameTime();
            helper.assertTrue(Math.abs(ElementalBowOverheatManager.getAdditionalManaCost(player, 100) - 132) < .001,
                    "Shared cost must use maximum depth independently of maximum expiry");
            ElementalBowOverheatManager.applyOverheatAfterCast(player, 0, 2, false);
            for (int slot = 0; slot < 4; slot++) {
                var state = ElementalBowOverheatManager.getState(player, slot, true);
                helper.assertTrue(state.chainDepth() == 4 && state.expireGameTime() == longest,
                        "Depths 3,1,0,0 must synchronize to 4 without shortening the longest expiry");
            }
            ElementalBowOverheatManager.clear(player);
            ElementalBowOverheatManager.applyOverheatAfterCast(player, 0, 0, false);
            helper.assertFalse(ElementalBowOverheatManager.getState(player).active(), "Cold zero-duration casts must stay cold");
            var legacy = new CompoundTag();
            legacy.putInt("ChainDepth", 3);
            legacy.putLong("ExpireGameTime", longest);
            legacy.putInt("LastAppliedCooldownTicks", 400);
            ElementalBowOverheatManager.applySyncedState(player, legacy);
            for (int slot = 0; slot < 4; slot++) helper.assertTrue(
                    ElementalBowOverheatManager.getState(player, slot, true).chainDepth() == 3,
                    "Legacy single heat must migrate to every slot");
            var migrated = ElementalBowOverheatManager.createSyncTag(player);
            helper.assertFalse(migrated.contains("ChainDepth"), "Migration must remove the single-state representation");
            migrated.getCompound("Slot0").putLong("ExpireGameTime", player.getServer().overworld().getGameTime());
            ElementalBowOverheatManager.applySyncedState(player, migrated);
            helper.assertFalse(ElementalBowOverheatManager.getState(player, 0, true).active(), "Expired slots must clear independently");
            helper.assertTrue(ElementalBowOverheatManager.getState(player, 1, true).active(), "Expiring one slot must preserve other slots");
        }
        helper.succeed();
    }
}
