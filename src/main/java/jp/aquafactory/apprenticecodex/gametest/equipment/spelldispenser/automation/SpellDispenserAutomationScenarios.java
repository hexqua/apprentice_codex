package jp.aquafactory.apprenticecodex.gametest.equipment.spelldispenser.automation;

import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.registries.PotionRegistry;
import jp.aquafactory.apprenticecodex.block.spelldispenser.SpellDispenserBlockEntity;
import jp.aquafactory.apprenticecodex.registry.BlockRegistry;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraftforge.common.capabilities.ForgeCapabilities;

import static jp.aquafactory.apprenticecodex.gametest.equipment.bow.SpellScrollTestSupport.createSpellScroll;
import static jp.aquafactory.apprenticecodex.gametest.equipment.flask.FlaskDoseTestSupport.createFilledAlchemistsFlask;
import static jp.aquafactory.apprenticecodex.gametest.equipment.flask.FlaskScenarios.createFilledSpellcastersFlask;
import static jp.aquafactory.apprenticecodex.gametest.equipment.luminousdevice.ManaPotionTestSupport.createInstantManaPotion;

final class SpellDispenserAutomationScenarios {
    private SpellDispenserAutomationScenarios() {}

    static void spellDispenserAutomationOnlyAcceptsManaContainers(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var blockEntity = new SpellDispenserBlockEntity(BlockPos.ZERO, BlockRegistry.SPELL_DISPENSER.get().defaultBlockState());
            var itemHandler = blockEntity.getCapability(ForgeCapabilities.ITEM_HANDLER, Direction.UP).resolve().orElse(null);
            var manaFlask = createFilledSpellcastersFlask(
                    createInstantManaPotion(PotionRegistry.INSTANT_MANA_ONE.get()),
                    1,
                    0
            );
            var alchemistsManaFlask = createFilledAlchemistsFlask(
                    PotionUtils.setPotion(
                            new ItemStack(Items.SPLASH_POTION),
                            PotionRegistry.INSTANT_MANA_ONE.get()
                    ),
                    1,
                    0
            );
            var nonManaPotion = PotionUtils.setPotion(new ItemStack(Items.POTION), Potions.HEALING);
            var nonManaFlask = createFilledSpellcastersFlask(nonManaPotion, 1, 0);

            helper.assertTrue(itemHandler != null, "Spell Dispenser item capability was not exposed for flask slot validation");
            helper.assertTrue(
                    itemHandler != null && itemHandler.isItemValid(
                            SpellDispenserBlockEntity.FLASK_SLOT_START,
                            createInstantManaPotion(PotionRegistry.INSTANT_MANA_ONE.get())
                    ),
                    "Spell Dispenser flask slot rejected a mana potion"
            );
            helper.assertTrue(
                    itemHandler != null && itemHandler.isItemValid(
                            SpellDispenserBlockEntity.FLASK_SLOT_START,
                            manaFlask
                    ),
                    "Spell Dispenser flask slot rejected a mana flask"
            );
            helper.assertTrue(
                    itemHandler != null && !itemHandler.isItemValid(
                            SpellDispenserBlockEntity.FLASK_SLOT_START,
                            new ItemStack(Items.GLASS_BOTTLE)
                    ),
                    "Spell Dispenser flask slot accepted a glass bottle from automation"
            );
            helper.assertTrue(
                    itemHandler != null && !itemHandler.isItemValid(
                            SpellDispenserBlockEntity.FLASK_SLOT_START,
                            nonManaPotion
                    ),
                    "Spell Dispenser flask slot accepted a non-mana potion"
            );
            helper.assertTrue(
                    itemHandler != null && !itemHandler.isItemValid(
                            SpellDispenserBlockEntity.FLASK_SLOT_START,
                            nonManaFlask
                    ),
                    "Spell Dispenser flask slot accepted a non-mana flask"
            );
            helper.assertTrue(
                    itemHandler != null && !itemHandler.isItemValid(
                            SpellDispenserBlockEntity.FLASK_SLOT_START,
                            alchemistsManaFlask
                    ),
                    "Spell Dispenser flask slot accepted an Alchemist's Flask"
            );
            helper.assertTrue(
                    itemHandler != null && !itemHandler.isItemValid(
                            SpellDispenserBlockEntity.FLASK_SLOT_START,
                            new ItemStack(Items.STICK)
                    ),
                    "Spell Dispenser flask slot accepted an unrelated item"
            );
        });
    }

    static void spellDispenserSidedAutomationProtectsScrollSlot(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var blockEntity = new SpellDispenserBlockEntity(BlockPos.ZERO, BlockRegistry.SPELL_DISPENSER.get().defaultBlockState());
            var itemHandler = blockEntity.getCapability(ForgeCapabilities.ITEM_HANDLER, Direction.UP).resolve().orElse(null);
            var scrollStack = createSpellScroll(SpellRegistry.HEAL_SPELL.get());
            blockEntity.getInventory().setStackInSlot(SpellDispenserBlockEntity.SPELL_SLOT_INDEX, scrollStack.copy());

            helper.assertTrue(itemHandler != null, "Spell Dispenser item capability was not exposed");
            helper.assertTrue(itemHandler != null && !itemHandler.isItemValid(0, scrollStack),
                    "Spell Dispenser sided automation exposed the scroll slot as insertable");

            var remainder = itemHandler == null ? scrollStack.copy() : itemHandler.insertItem(0, scrollStack.copy(), false);
            helper.assertTrue(ItemStack.isSameItemSameTags(remainder, scrollStack),
                    "Spell Dispenser sided automation inserted a scroll into the protected slot");
            helper.assertTrue(itemHandler != null && ItemStack.isSameItemSameTags(itemHandler.getStackInSlot(0), scrollStack),
                    "Spell Dispenser sided automation did not expose the protected scroll contents");
            helper.assertTrue(itemHandler != null && itemHandler.extractItem(0, 1, false).isEmpty(),
                    "Spell Dispenser sided automation extracted the protected scroll slot");
        });
    }

    static void spellDispenserSidedAutomationExtractsOnlyEmptyContainers(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var blockEntity = new SpellDispenserBlockEntity(BlockPos.ZERO, BlockRegistry.SPELL_DISPENSER.get().defaultBlockState());
            var itemHandler = blockEntity.getCapability(ForgeCapabilities.ITEM_HANDLER, Direction.UP).resolve().orElse(null);
            var filledFlask = createFilledSpellcastersFlask(
                    createInstantManaPotion(PotionRegistry.INSTANT_MANA_ONE.get()),
                    1,
                    0
            );
            blockEntity.getInventory().setStackInSlot(SpellDispenserBlockEntity.FLASK_SLOT_START, filledFlask);
            blockEntity.getInventory().setStackInSlot(SpellDispenserBlockEntity.FLASK_SLOT_START + 1, new ItemStack(ItemRegistry.SPELLCASTERS_FLASK.get()));
            blockEntity.getInventory().setStackInSlot(SpellDispenserBlockEntity.FLASK_SLOT_START + 2, new ItemStack(Items.GLASS_BOTTLE));

            helper.assertTrue(itemHandler != null, "Spell Dispenser sided automation capability was not exposed");
            helper.assertTrue(itemHandler != null && itemHandler.extractItem(SpellDispenserBlockEntity.FLASK_SLOT_START, 1, false).isEmpty(),
                    "Spell Dispenser sided automation extracted a filled mana container");
            helper.assertTrue(itemHandler != null
                            && itemHandler.extractItem(SpellDispenserBlockEntity.FLASK_SLOT_START + 1, 1, false).is(ItemRegistry.SPELLCASTERS_FLASK.get()),
                    "Spell Dispenser sided automation failed to extract an empty flask");
            helper.assertTrue(itemHandler != null
                            && itemHandler.extractItem(SpellDispenserBlockEntity.FLASK_SLOT_START + 2, 1, false).is(Items.GLASS_BOTTLE),
                    "Spell Dispenser sided automation failed to extract a glass bottle");
        });
    }
}
