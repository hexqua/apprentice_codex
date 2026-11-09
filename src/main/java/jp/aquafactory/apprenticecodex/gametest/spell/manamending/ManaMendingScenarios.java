package jp.aquafactory.apprenticecodex.gametest.spell.manamending;

import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.CastType;
import jp.aquafactory.apprenticecodex.mixin.MagicDataAccessor;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import jp.aquafactory.apprenticecodex.registry.SpellRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import static jp.aquafactory.apprenticecodex.gametest.support.CurioTestSupport.equipRingCurio;
import static jp.aquafactory.apprenticecodex.gametest.support.PlayerTestSupport.createEquipmentTestPlayer;

public final class ManaMendingScenarios {
    private ManaMendingScenarios() {}

    static void manaMendingRequiresDamagedHeldItem(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var spell = SpellRegistry.MANA_MENDING.get();
            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "mana_mending_empty_test");
            var magicData = MagicData.getPlayerMagicData(player);

            helper.assertFalse(spell.checkPreCastConditions(helper.getLevel(), 1, player, magicData),
                    "Mana Mending should reject empty hands");

            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.DIAMOND_SWORD));
            magicData.setAdditionalCastData(null);
            helper.assertFalse(spell.checkPreCastConditions(helper.getLevel(), 1, player, magicData),
                    "Mana Mending should reject undamaged damageable items");
        });
    }

    static void manaMendingRepairsMainHandDamagedItem(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var spell = SpellRegistry.MANA_MENDING.get();
            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "mana_mending_mainhand_test");
            var sword = new ItemStack(Items.DIAMOND_SWORD);
            sword.setDamageValue(5);
            player.setItemInHand(InteractionHand.MAIN_HAND, sword);

            var magicData = MagicData.getPlayerMagicData(player);
            helper.assertTrue(spell.checkPreCastConditions(helper.getLevel(), 1, player, magicData),
                    "Mana Mending should accept a damaged main-hand item");

            prepareManaMendingProcessTick(magicData);
            spell.onServerCastTick(helper.getLevel(), 1, player, magicData);

            helper.assertTrue(sword.getDamageValue() == 4,
                    "Mana Mending should repair 1 durability at level 1 but damage is " + sword.getDamageValue());
        });
    }

    static void manaMendingPrefersOffhandDamagedItem(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var spell = SpellRegistry.MANA_MENDING.get();
            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "mana_mending_offhand_test");
            var mainHandSword = new ItemStack(Items.DIAMOND_SWORD);
            var offhandPickaxe = new ItemStack(Items.IRON_PICKAXE);
            mainHandSword.setDamageValue(5);
            offhandPickaxe.setDamageValue(5);
            player.setItemInHand(InteractionHand.MAIN_HAND, mainHandSword);
            player.setItemInHand(InteractionHand.OFF_HAND, offhandPickaxe);

            var magicData = MagicData.getPlayerMagicData(player);
            helper.assertTrue(spell.checkPreCastConditions(helper.getLevel(), 1, player, magicData),
                    "Mana Mending should accept damaged items in both hands");

            prepareManaMendingProcessTick(magicData);
            spell.onServerCastTick(helper.getLevel(), 1, player, magicData);

            helper.assertTrue(offhandPickaxe.getDamageValue() == 4,
                    "Mana Mending should repair the offhand item first but offhand damage is "
                            + offhandPickaxe.getDamageValue());
            helper.assertTrue(mainHandSword.getDamageValue() == 5,
                    "Mana Mending should not repair the main-hand item while offhand is the locked target");
        });
    }

    static void manaMendingCraftsmansDelightBoostsRepairAndPreservesRepairCost(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var spell = SpellRegistry.MANA_MENDING.get();
            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "mana_mending_craftsmans_test");
            equipRingCurio(player, new ItemStack(ItemRegistry.CRAFTSMANS_DELIGHT.get()));

            var pickaxe = new ItemStack(Items.DIAMOND_PICKAXE);
            pickaxe.setDamageValue(3);
            pickaxe.setRepairCost(7);
            player.setItemInHand(InteractionHand.MAIN_HAND, pickaxe);

            var magicData = MagicData.getPlayerMagicData(player);
            helper.assertTrue(spell.checkPreCastConditions(helper.getLevel(), 1, player, magicData),
                    "Mana Mending should accept a damaged item while CraftsmansDelight is equipped");

            prepareManaMendingProcessTick(magicData);
            spell.onServerCastTick(helper.getLevel(), 1, player, magicData);
            helper.assertTrue(pickaxe.getDamageValue() == 2,
                    "First CraftsmansDelight-boosted repair tick should repair 1 durability and carry 0.5");

            prepareManaMendingProcessTick(magicData);
            spell.onServerCastTick(helper.getLevel(), 1, player, magicData);

            helper.assertTrue(pickaxe.getDamageValue() == 0,
                    "Second CraftsmansDelight-boosted repair tick should spend the carried fraction and finish repair");
            helper.assertTrue(pickaxe.getBaseRepairCost() == 7,
                    "CraftsmansDelight Mana Mending completion should preserve repair cost");
        });
    }

    public static void prepareManaMendingProcessTick(MagicData magicData) {
        var accessor = (MagicDataAccessor) magicData;
        accessor.apprenticecodex$setCastingSpellLevel(1);
        accessor.apprenticecodex$setCastDuration(200);
        accessor.apprenticecodex$setCastDurationRemaining(190);
        accessor.apprenticecodex$setCastSource(CastSource.SPELLBOOK);
        accessor.apprenticecodex$setCastType(CastType.CONTINUOUS);
    }
}
