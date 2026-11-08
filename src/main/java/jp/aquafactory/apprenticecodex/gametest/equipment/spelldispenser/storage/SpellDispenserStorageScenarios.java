package jp.aquafactory.apprenticecodex.gametest.equipment.spelldispenser.storage;

import io.redspace.ironsspellbooks.api.magic.MagicData;
import jp.aquafactory.apprenticecodex.block.spelldispenser.SpellDispenser;
import jp.aquafactory.apprenticecodex.block.spelldispenser.SpellDispenserBlockEntity;
import jp.aquafactory.apprenticecodex.registry.BlockRegistry;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;

import static jp.aquafactory.apprenticecodex.gametest.equipment.spelldispenser.support.SpellDispenserTestSupport.createSpellDispenserPlacer;
import static jp.aquafactory.apprenticecodex.gametest.integration.create.CreateDispenserTestSupport.createSpellDispenserOwnerProfile;

final class SpellDispenserStorageScenarios {
    private SpellDispenserStorageScenarios() {}

    static void spellDispenserPlacementStartsAtZeroManaAndStoresOwnerProfile(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var level = (ServerLevel) helper.getLevel();
            var pos = new BlockPos(0, 1, 0);
            var absolutePos = helper.absolutePos(pos);
            helper.setBlock(pos, BlockRegistry.SPELL_DISPENSER.get());

            var blockEntity = level.getBlockEntity(absolutePos);
            helper.assertTrue(blockEntity instanceof SpellDispenserBlockEntity, "Spell Dispenser block entity was not created");
            var spellDispenser = (SpellDispenserBlockEntity) blockEntity;

            var player = createSpellDispenserPlacer(helper, new BlockPos(0, 2, 0), "spell_dispenser_place_mana_test");
            var magicData = MagicData.getPlayerMagicData(player);
            helper.assertTrue(magicData != null, "Spell Dispenser placement test could not resolve player mana data");
            magicData.setMana(1350.75F);
            var initialMana = magicData.getMana();

            var state = level.getBlockState(absolutePos);
            ((SpellDispenser) state.getBlock()).setPlacedBy(level, absolutePos, state, player, new ItemStack(ItemRegistry.SPELL_DISPENSER.get()));

            helper.assertTrue(spellDispenser.getCurrentMana() == 0,
                    "Spell Dispenser placement should now start at zero mana: " + spellDispenser.getCurrentMana());
            helper.assertTrue(Math.abs(magicData.getMana() - initialMana) < 1.0e-4F,
                    "Spell Dispenser placement should no longer drain player mana: " + magicData.getMana());
            helper.assertTrue(spellDispenser.hasOwnerProfile(), "Spell Dispenser placement did not store the owner profile");
        });
    }

    static void spellDispenserSneakPlacementStartsAtZeroMana(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var level = (ServerLevel) helper.getLevel();
            var pos = new BlockPos(0, 1, 0);
            var absolutePos = helper.absolutePos(pos);
            helper.setBlock(pos, BlockRegistry.SPELL_DISPENSER.get());

            var blockEntity = level.getBlockEntity(absolutePos);
            helper.assertTrue(blockEntity instanceof SpellDispenserBlockEntity, "Spell Dispenser block entity was not created");
            var spellDispenser = (SpellDispenserBlockEntity) blockEntity;

            var player = createSpellDispenserPlacer(helper, new BlockPos(0, 2, 0), "spell_dispenser_place_sneak_test");
            player.setShiftKeyDown(true);
            var magicData = MagicData.getPlayerMagicData(player);
            helper.assertTrue(magicData != null, "Spell Dispenser sneak placement test could not resolve player mana data");
            magicData.setMana(640.5F);
            var initialMana = magicData.getMana();

            var state = level.getBlockState(absolutePos);
            ((SpellDispenser) state.getBlock()).setPlacedBy(level, absolutePos, state, player, new ItemStack(ItemRegistry.SPELL_DISPENSER.get()));

            helper.assertTrue(spellDispenser.getCurrentMana() == 0,
                    "Spell Dispenser sneak placement should start at zero mana: " + spellDispenser.getCurrentMana());
            helper.assertTrue(Math.abs(magicData.getMana() - initialMana) < 1.0e-4F,
                    "Spell Dispenser sneak placement should not drain player mana: " + magicData.getMana());
            helper.assertTrue(spellDispenser.hasOwnerProfile(), "Spell Dispenser sneak placement did not store the owner profile");
        });
    }

    static void spellDispenserOwnerProfileCanBeReadFromSavedTag(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var ownerProfile = createSpellDispenserOwnerProfile("spell_dispenser_owner_tag_test");
            var blockEntity = new SpellDispenserBlockEntity(BlockPos.ZERO, BlockRegistry.SPELL_DISPENSER.get().defaultBlockState());
            blockEntity.setOwnerProfile(ownerProfile);

            var restoredOwner = SpellDispenserBlockEntity.readOwnerProfile(blockEntity.getUpdateTag());
            helper.assertTrue(restoredOwner != null, "Spell Dispenser owner profile helper returned null");
            helper.assertTrue(restoredOwner != null && ownerProfile.getId().equals(restoredOwner.getId()),
                    "Spell Dispenser owner UUID helper returned the wrong value");
            helper.assertTrue(restoredOwner != null && ownerProfile.getName().equals(restoredOwner.getName()),
                    "Spell Dispenser owner name helper returned the wrong value");
        });
    }
}
