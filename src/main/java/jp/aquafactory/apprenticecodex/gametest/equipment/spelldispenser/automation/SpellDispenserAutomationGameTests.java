package jp.aquafactory.apprenticecodex.gametest.equipment.spelldispenser.automation;

import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.registries.PotionRegistry;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.block.spelldispenser.SpellDispenserBlockEntity;
import jp.aquafactory.apprenticecodex.registry.BlockRegistry;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import jp.aquafactory.apprenticecodex.utility.PotionContentsHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potions;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import static jp.aquafactory.apprenticecodex.gametest.equipment.spelldispenser.support.SpellDispenserManaTestSupport.createFilledSpellcastersFlask;
import static jp.aquafactory.apprenticecodex.gametest.equipment.spelldispenser.support.SpellDispenserManaTestSupport.createInstantManaPotion;
import static jp.aquafactory.apprenticecodex.gametest.equipment.spelldispenser.support.SpellDispenserTestSupport.createSpellScroll;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class SpellDispenserAutomationGameTests {
    private SpellDispenserAutomationGameTests() {}

    @GameTest(template = "gametest/basic_floor")
    public static void spellDispenserAutomationOnlyAcceptsManaContainers(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var blockEntity = new SpellDispenserBlockEntity(BlockPos.ZERO, BlockRegistry.SPELL_DISPENSER.get().defaultBlockState());
            var itemHandler = blockEntity.getItemHandler(Direction.UP);
            var manaFlask = createFilledSpellcastersFlask(
                    helper.getLevel().registryAccess(),
                    createInstantManaPotion(PotionRegistry.INSTANT_MANA_ONE.get()),
                    1,
                    0
            );
            var nonManaPotion = PotionContentsHelper.createPotionStack(Items.POTION, Potions.HEALING.value());
            var nonManaFlask = createFilledSpellcastersFlask(helper.getLevel().registryAccess(), nonManaPotion, 1, 0);

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
                            new ItemStack(Items.STICK)
                    ),
                    "Spell Dispenser flask slot accepted an unrelated item"
            );
        });
    }

    @GameTest(template = "gametest/basic_floor")
    public static void spellDispenserSidedAutomationProtectsScrollSlot(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var pos = new BlockPos(0, 1, 0);
            helper.setBlock(pos, BlockRegistry.SPELL_DISPENSER.get());
            var blockEntity = helper.getBlockEntity(pos);
            helper.assertTrue(blockEntity instanceof SpellDispenserBlockEntity, "Spell Dispenser block entity was not created");
            var spellDispenser = (SpellDispenserBlockEntity) blockEntity;
            var itemHandler = helper.getLevel().getCapability(
                    Capabilities.ItemHandler.BLOCK,
                    helper.absolutePos(pos),
                    Direction.UP
            );
            var scrollStack = createSpellScroll(SpellRegistry.HEAL_SPELL.get());
            spellDispenser.getInventory().setStackInSlot(SpellDispenserBlockEntity.SPELL_SLOT_INDEX, scrollStack.copy());

            helper.assertTrue(itemHandler != null, "Spell Dispenser item capability was not exposed");
            helper.assertTrue(itemHandler != null && !itemHandler.isItemValid(0, scrollStack),
                    "Spell Dispenser sided automation exposed the scroll slot as insertable");

            var remainder = itemHandler == null ? scrollStack.copy() : itemHandler.insertItem(0, scrollStack.copy(), false);
            helper.assertTrue(ItemStack.isSameItemSameComponents(remainder, scrollStack),
                    "Spell Dispenser sided automation consumed a scroll while rejecting the protected slot");
            helper.assertTrue(itemHandler != null && ItemStack.isSameItemSameComponents(itemHandler.getStackInSlot(0), scrollStack),
                    "Spell Dispenser sided automation inserted a scroll into the protected slot");
            helper.assertTrue(itemHandler != null && itemHandler.extractItem(0, 1, false).isEmpty(),
                    "Spell Dispenser sided automation extracted the protected scroll slot");
        });
    }

    @GameTest(template = "gametest/basic_floor")
    public static void spellDispenserSidedAutomationExtractsOnlyEmptyContainers(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var pos = new BlockPos(0, 1, 0);
            helper.setBlock(pos, BlockRegistry.SPELL_DISPENSER.get());
            var blockEntity = helper.getBlockEntity(pos);
            helper.assertTrue(blockEntity instanceof SpellDispenserBlockEntity, "Spell Dispenser block entity was not created");
            var spellDispenser = (SpellDispenserBlockEntity) blockEntity;
            var itemHandler = helper.getLevel().getCapability(
                    Capabilities.ItemHandler.BLOCK,
                    helper.absolutePos(pos),
                    Direction.UP
            );
            var filledFlask = createFilledSpellcastersFlask(
                    helper.getLevel().registryAccess(),
                    createInstantManaPotion(PotionRegistry.INSTANT_MANA_ONE.get()),
                    1,
                    0
            );
            spellDispenser.getInventory().setStackInSlot(SpellDispenserBlockEntity.FLASK_SLOT_START, filledFlask);
            spellDispenser.getInventory().setStackInSlot(SpellDispenserBlockEntity.FLASK_SLOT_START + 1, new ItemStack(ItemRegistry.SPELLCASTERS_FLASK.get()));
            spellDispenser.getInventory().setStackInSlot(SpellDispenserBlockEntity.FLASK_SLOT_START + 2, new ItemStack(Items.GLASS_BOTTLE));

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
