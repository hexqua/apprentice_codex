package jp.aquafactory.apprenticecodex.gametest.equipment.craftsmansdelight;

import io.redspace.ironsspellbooks.api.events.SpellCooldownAddedEvent;
import io.redspace.ironsspellbooks.api.events.SpellOnCastEvent;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.capabilities.magic.MagicManager;
import io.redspace.ironsspellbooks.spells.nature.TouchDigSpell;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.item.WeaponImbueCooldownHelper;
import jp.aquafactory.apprenticecodex.item.curios.craftsmansdelight.CraftsmansDelight;
import jp.aquafactory.apprenticecodex.item.curios.craftsmansdelight.CraftsmansDelightCooldownReductionEvent;
import jp.aquafactory.apprenticecodex.item.curios.craftsmansdelight.CraftsmansDelightManaCostDiscountEvent;
import jp.aquafactory.apprenticecodex.item.curios.craftsmansdelight.CraftsmansDelightSpellSupport;
import jp.aquafactory.apprenticecodex.item.scrollcastergauntlet.ScrollcasterGauntlet;
import jp.aquafactory.apprenticecodex.item.scrollcastergauntlet.ScrollcasterGauntletCastEvent;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import jp.aquafactory.apprenticecodex.registry.SpellRegistry;
import jp.aquafactory.apprenticecodex.spell.ICraftsmansDelightAffectedSpell;
import jp.aquafactory.apprenticecodex.spell.tinylumberjack.TinyLumberjack;
import jp.aquafactory.apprenticecodex.spell.worldflatter.WorldFlatter;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.List;

import static jp.aquafactory.apprenticecodex.gametest.equipment.EquipmentTestBatches.TEMPLATE;
import static jp.aquafactory.apprenticecodex.gametest.equipment.spelldispenser.support.SpellDispenserTestSupport.createSpellScroll;
import static jp.aquafactory.apprenticecodex.gametest.spell.heavenlyfist.HeavenlyFistHarvestGameTests.equipRingCurio;
import static jp.aquafactory.apprenticecodex.gametest.support.IsolationTestSupport.prepareMiningSpellIsolationArea;
import static jp.aquafactory.apprenticecodex.gametest.support.PlayerTestSupport.createEquipmentTestPlayer;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class CraftsmansDelightLegacyScenarios {
    private CraftsmansDelightLegacyScenarios() {}

    @GameTest(template = TEMPLATE)
    public static void craftsmansDelightAppliesToExternalSpellManaAndCooldown(GameTestHelper helper) {
        CraftsmansDelightLegacyScenariosSupport.craftsmansDelightAppliesToExternalSpellManaAndCooldown(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void craftsmansDelightAppliesToHarvestMoonAndEarthForgeManaAndCooldown(GameTestHelper helper) {
        CraftsmansDelightLegacyScenariosSupport.craftsmansDelightAppliesToHarvestMoonAndEarthForgeManaAndCooldown(helper);
    }

    static void assertCraftsmansDelightBasicDiscounts(
            GameTestHelper helper,
            FakePlayer player,
            AbstractSpell spell,
            int baseManaCost,
            String spellName,
            boolean expectedCastingMobility
    ) {
        if (!(spell instanceof ICraftsmansDelightAffectedSpell affectedSpell)) {
            helper.fail(spellName + " should opt into CraftsmansDelight support");
            return;
        }

        helper.assertFalse(affectedSpell.isCraftsmansDelightBreakSpeedBonusEnabled(),
                spellName + " should not receive CraftsmansDelight break speed bonuses");
        helper.assertFalse(affectedSpell.isCraftsmansDelightProcessSpeedBonusEnabled(),
                spellName + " should not receive CraftsmansDelight process speed bonuses");
        helper.assertTrue(affectedSpell.isCraftsmansDelightCastingMobilityEnabled() == expectedCastingMobility,
                spellName + " casting mobility should be " + expectedCastingMobility);
        helper.assertTrue(CraftsmansDelightSpellSupport.isManaCostDiscountTarget(spell.getSpellId()),
                spellName + " should be a CraftsmansDelight mana discount target");
        helper.assertTrue(CraftsmansDelightSpellSupport.isCooldownReductionTarget(spell),
                spellName + " should be a CraftsmansDelight cooldown reduction target");

        var manaEvent = new SpellOnCastEvent(
                player,
                spell.getSpellId(),
                1,
                baseManaCost,
                spell.getSchoolType(),
                CastSource.SPELLBOOK
        );
        CraftsmansDelightManaCostDiscountEvent.onSpellCast(manaEvent);
        var expectedManaCost = Math.max(1, Math.round(baseManaCost * 0.5f));
        helper.assertTrue(manaEvent.getManaCost() == expectedManaCost,
                spellName + " mana cost should be reduced to " + expectedManaCost + " but got " + manaEvent.getManaCost());

        var cooldownEvent = new SpellCooldownAddedEvent.Pre(
                spell.getSpellCooldown(),
                spell,
                player,
                CastSource.SPELLBOOK
        );
        CraftsmansDelightCooldownReductionEvent.onSpellCooldownAdded(cooldownEvent);
        helper.assertTrue(cooldownEvent.getEffectiveCooldown()
                        == CraftsmansDelight.getReducedEffectiveCooldown(spell, player, CastSource.SPELLBOOK),
                spellName + " cooldown should route through the reduced cooldown helper");
    }

    public static void craftsmansDelightScrollcasterGauntletCooldownUsesSwordMultiplier(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0),
                    "craftsmans_scrollcaster_sword_cooldown_test");
            equipRingCurio(player, new ItemStack(ItemRegistry.CRAFTSMANS_DELIGHT.get()));
            var spell = SpellRegistry.HARVEST_MOON.get();
            var gauntlet = new ItemStack(ItemRegistry.SCROLLCASTER_GAUNTLET.get());
            ScrollcasterGauntlet.setCalibrationScroll(gauntlet, 0, createSpellScroll(spell));
            ScrollcasterGauntlet.setSelectedScrollIndex(gauntlet, 0);
            var magicData = MagicData.getPlayerMagicData(player);
            helper.assertTrue(magicData != null,
                    "CraftsmansDelight Scrollcaster Gauntlet cooldown test could not resolve player magic data");
            magicData.setPlayerCastingItem(gauntlet.copy());

            var expectedCooldown = WeaponImbueCooldownHelper.getEffectiveSpellCooldown(
                    spell,
                    player,
                    CastSource.SWORD
            );
            helper.assertTrue(expectedCooldown < MagicManager.getEffectiveSpellCooldown(
                            spell,
                            player,
                            CastSource.SWORD
                    ),
                    "CraftsmansDelight Scrollcaster Gauntlet cooldown should be reduced from the normal sword cooldown");

            var craftsmansFirstEvent = new SpellCooldownAddedEvent.Pre(
                    MagicManager.getEffectiveSpellCooldown(spell, player, CastSource.SWORD),
                    spell,
                    player,
                    CastSource.SWORD
            );
            CraftsmansDelightCooldownReductionEvent.onSpellCooldownAdded(craftsmansFirstEvent);
            ScrollcasterGauntletCastEvent.onSpellCooldownAdded(craftsmansFirstEvent);
            helper.assertTrue(craftsmansFirstEvent.getEffectiveCooldown() == expectedCooldown,
                    "CraftsmansDelight -> Scrollcaster Gauntlet cooldown order should keep the reduced sword cooldown but got "
                            + craftsmansFirstEvent.getEffectiveCooldown() + " / expected " + expectedCooldown);

            var scrollcasterFirstEvent = new SpellCooldownAddedEvent.Pre(
                    MagicManager.getEffectiveSpellCooldown(spell, player, CastSource.SWORD),
                    spell,
                    player,
                    CastSource.SWORD
            );
            ScrollcasterGauntletCastEvent.onSpellCooldownAdded(scrollcasterFirstEvent);
            CraftsmansDelightCooldownReductionEvent.onSpellCooldownAdded(scrollcasterFirstEvent);
            helper.assertTrue(scrollcasterFirstEvent.getEffectiveCooldown() == expectedCooldown,
                    "Scrollcaster Gauntlet -> CraftsmansDelight cooldown order should keep the reduced sword cooldown but got "
                            + scrollcasterFirstEvent.getEffectiveCooldown() + " / expected " + expectedCooldown);
        });
    }

    public static void craftsmansDelightExtendsTouchDigRange(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var spell = new TouchDigSpell();
            var playerPos = new BlockPos(0, 12, 0);
            prepareMiningSpellIsolationArea(helper, playerPos);
            var player = createEquipmentTestPlayer(helper, playerPos, "touch_dig_range_test");
            var magicData = MagicData.getPlayerMagicData(player);
            var targetPos = helper.absolutePos(new BlockPos(0, 23, 0));

            helper.assertTrue(magicData != null, "Touch Dig range test could not resolve player mana data");
            player.setYRot(0.0f);
            player.setXRot(-90.0f);
            helper.getLevel().setBlock(targetPos, Blocks.STONE.defaultBlockState(), 3);

            helper.assertFalse(spell.checkPreCastConditions(helper.getLevel(), 1, player, magicData),
                    "Touch Dig should keep the default 8 block range without CraftsmansDelight");

            equipRingCurio(player, new ItemStack(ItemRegistry.CRAFTSMANS_DELIGHT.get()));
            helper.assertTrue(spell.checkPreCastConditions(helper.getLevel(), 1, player, magicData),
                    "Touch Dig should reach a target 12 blocks away when CraftsmansDelight is equipped");
            helper.assertTrue(spell.getUniqueInfo(1, player).stream().anyMatch(component -> component.getString().contains("16")),
                    "Touch Dig unique info should display 16 block range while CraftsmansDelight is equipped");

            spell.onCast(helper.getLevel(), 1, player, CastSource.SPELLBOOK, magicData);
            helper.assertTrue(helper.getLevel().getBlockState(targetPos).isAir(),
                    "Touch Dig should destroy the targeted block inside the extended range");
        });
    }

    static void craftsmansDelightDummyToolsCopyAllCurrentMainHandEnchantments(GameTestHelper helper) {
        helper.runAtTickTime(1, () -> {
            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 12, 0), "craftsmans_main_hand_enchant_copy_test");
            equipRingCurio(player, new ItemStack(ItemRegistry.CRAFTSMANS_DELIGHT.get()));

            var heldSword = new ItemStack(Items.DIAMOND_SWORD);
            heldSword.enchant(Enchantments.SHARPNESS, 4);
            heldSword.enchant(Enchantments.UNBREAKING, 3);
            player.setItemInHand(InteractionHand.MAIN_HAND, heldSword);

            var lumberjackTool = TinyLumberjack.createDummyTool(player);
            var worldFlatterTool = WorldFlatter.createDummyTool(player);
            var heavenlyFistTool = CraftsmansDelight.createHeavenlyFistCrystalHarvestTool(player);
            helper.assertTrue(lumberjackTool.is(Items.IRON_AXE),
                    "Tiny Lumberjack should keep its iron axe dummy tool");
            helper.assertTrue(worldFlatterTool.is(Items.NETHERITE_PICKAXE),
                    "World Flatter should keep its CraftsmansDelight netherite pickaxe dummy tool");
            helper.assertTrue(heavenlyFistTool.is(Items.DIAMOND_PICKAXE),
                    "Heavenly Fist should keep its diamond pickaxe dummy tool");
            for (var dummyTool : List.of(lumberjackTool, worldFlatterTool, heavenlyFistTool)) {
                helper.assertTrue(dummyTool.getEnchantmentLevel(Enchantments.SHARPNESS) == 4,
                        "CraftsmansDelight should copy even non-mining main-hand enchantments without filtering");
                helper.assertTrue(dummyTool.getEnchantmentLevel(Enchantments.UNBREAKING) == 3,
                        "CraftsmansDelight should copy every main-hand enchantment");
            }

            var heldPickaxe = new ItemStack(Items.DIAMOND_PICKAXE);
            heldPickaxe.enchant(Enchantments.SILK_TOUCH, 1);
            player.setItemInHand(InteractionHand.MAIN_HAND, heldPickaxe);
            var updatedLumberjackTool = TinyLumberjack.createDummyTool(player);
            helper.assertTrue(updatedLumberjackTool.getEnchantmentLevel(Enchantments.SILK_TOUCH) == 1,
                    "CraftsmansDelight should follow the current main-hand enchantments at destruction time");
            helper.assertTrue(updatedLumberjackTool.getEnchantmentLevel(Enchantments.SHARPNESS) == 0,
                    "CraftsmansDelight should not retain enchantments from the previous main-hand item");
            helper.succeed();
        });
    }
}
