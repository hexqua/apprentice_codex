package jp.aquafactory.apprenticecodex.gametest.equipment.spellgun;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.item.SpellcasterRoundItem;
import jp.aquafactory.apprenticecodex.item.curios.CuriosSlotConstants;
import jp.aquafactory.apprenticecodex.item.curios.spellcasterammopouch.SpellcasterAmmoPouch;
import jp.aquafactory.apprenticecodex.item.fullautorapidcastspellrifle.FullautoRapidcastSpellrifle;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import jp.aquafactory.apprenticecodex.registry.TagRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import static jp.aquafactory.apprenticecodex.gametest.equipment.EquipmentTestBatches.TEMPLATE;
import static jp.aquafactory.apprenticecodex.gametest.support.CurioTestSupport.equipCurio;
import static jp.aquafactory.apprenticecodex.gametest.support.PlayerTestSupport.createEquipmentTestPlayer;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class RifleAmmunitionGameTests {
    private RifleAmmunitionGameTests() {}

    @GameTest(template = TEMPLATE)
    public static void multipurposeStaffrifleUsesDedicatedAmmoAndCasingReturnPolicy(GameTestHelper helper) {
        RifleAmmunitionScenarios.multipurposeStaffrifleUsesDedicatedAmmoAndCasingReturnPolicy(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void fullautoRapidcastSpellrifleUsesDedicatedAmmoAndCasingReturnPolicy(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var stack = new ItemStack(ItemRegistry.FULLAUTO_RAPIDCAST_SPELLRIFLE.get());
            var item = (FullautoRapidcastSpellrifle) stack.getItem();
            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "fullauto_rapidcast_spellrifle_ammo_policy_test");

            helper.assertTrue(item.getAmmoItem(stack) == ItemRegistry.FULLAUTO_SPELL_CASTING_ROUND.get(),
                    "Fullauto Rapidcast Spellrifle should use Full-auto Spell Casting Round");
            helper.assertTrue(ItemRegistry.FULLAUTO_SPELL_CASTING_ROUND.get() instanceof SpellcasterRoundItem,
                    "Full-auto Spell Casting Round should be a SpellcasterRoundItem");
            var roundItem = (SpellcasterRoundItem) ItemRegistry.FULLAUTO_SPELL_CASTING_ROUND.get();
            helper.assertTrue(roundItem.getEmptyCasingItem() == ItemRegistry.EMPTY_FULLAUTO_SPELL_CASTING_CASING.get(),
                    "Full-auto Spell Casting Round should return Empty Full-auto Spell Casting Casing");
            helper.assertTrue(item.resolveEmptyCasingReturnChance(player) == 0.2F,
                    "Fullauto Rapidcast Spellrifle should use 20% empty casing return chance without Spellcaster Ammo Pouch");

            equipCurio(player, CuriosSlotConstants.BELT, new ItemStack(ItemRegistry.SPELLCASTER_AMMO_POUCH.get()));
            helper.assertTrue(item.resolveEmptyCasingReturnChance(player) == 0.9F,
                    "Fullauto Rapidcast Spellrifle should use 90% empty casing return chance with Spellcaster Ammo Pouch");
            var ammo = new ItemStack(roundItem, 2);
            var casing = new ItemStack(ItemRegistry.EMPTY_FULLAUTO_SPELL_CASTING_CASING.get());
            helper.assertTrue(SpellcasterAmmoPouch.storeInEquippedPouches(player, ammo) == 2,
                    "Ammo pouch should store Full-auto Spell Casting Rounds");
            helper.assertTrue(SpellcasterAmmoPouch.consumeAmmoFromAccessiblePouches(player, roundItem),
                    "Full-auto ammunition should be consumable from an equipped pouch");
            helper.assertTrue(SpellcasterAmmoPouch.countAmmoInAccessiblePouches(player, roundItem) == 1,
                    "Pouch consumption should remove exactly one Full-auto round");
            helper.assertTrue(casing.is(TagRegistry.Items.SPELLCASTER_EMPTY_CASINGS),
                    "Full-auto casing should be classified as an empty casing");
            helper.assertTrue(SpellcasterAmmoPouch.storeInEquippedPouches(player, casing) == 1,
                    "Ammo pouch should store Empty Full-auto Spell Casting Casings");
        });
    }
}
