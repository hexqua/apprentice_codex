package jp.aquafactory.apprenticecodex.gametest.equipment.spelldispenser.storage;

import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.registries.PotionRegistry;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.block.spelldispenser.SpellDispenser;
import jp.aquafactory.apprenticecodex.block.spelldispenser.SpellDispenserBlockEntity;
import jp.aquafactory.apprenticecodex.block.spelldispenser.SpellDispenserManaFluidHelper;
import jp.aquafactory.apprenticecodex.block.spelldispenser.SpellDispenserManaHelper;
import jp.aquafactory.apprenticecodex.gametest.equipment.spelldispenser.support.DispenserTestScope;
import jp.aquafactory.apprenticecodex.registry.BlockRegistry;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import jp.aquafactory.apprenticecodex.registry.SpellRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import static jp.aquafactory.apprenticecodex.gametest.equipment.spelldispenser.support.SpellDispenserManaTestSupport.createFilledSpellcastersFlask;
import static jp.aquafactory.apprenticecodex.gametest.equipment.spelldispenser.support.SpellDispenserManaTestSupport.createInstantManaPotion;
import static jp.aquafactory.apprenticecodex.gametest.equipment.spelldispenser.support.SpellDispenserManaTestSupport.createIronsManaPotionFluid;
import static jp.aquafactory.apprenticecodex.gametest.equipment.spelldispenser.support.SpellDispenserTestSupport.createSpellDispenserOwnerProfile;
import static jp.aquafactory.apprenticecodex.gametest.equipment.spelldispenser.support.SpellDispenserTestSupport.createSpellDispenserPlacer;
import static jp.aquafactory.apprenticecodex.gametest.equipment.spelldispenser.support.SpellDispenserTestSupport.createSpellScroll;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class SpellDispenserStorageGameTests {
    private SpellDispenserStorageGameTests() {}

    @GameTest(template = "gametest/basic_floor")
    public static void spellDispenserPlacementStartsAtZeroManaAndStoresOwnerProfile(GameTestHelper helper) {
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

    @GameTest(template = "gametest/basic_floor")
    public static void spellDispenserSneakPlacementStartsAtZeroMana(GameTestHelper helper) {
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

    @GameTest(template = "gametest/basic_floor")
    public static void spellDispenserOwnerProfileCanBeReadFromSavedTag(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var ownerProfile = createSpellDispenserOwnerProfile("spell_dispenser_owner_tag_test");
            var blockEntity = new SpellDispenserBlockEntity(BlockPos.ZERO, BlockRegistry.SPELL_DISPENSER.get().defaultBlockState());
            blockEntity.setOwnerProfile(ownerProfile);

            var restoredOwner = SpellDispenserBlockEntity.readOwnerProfile(blockEntity.getUpdateTag(helper.getLevel().registryAccess()));
            helper.assertTrue(restoredOwner != null, "Spell Dispenser owner profile helper returned null");
            helper.assertTrue(restoredOwner != null && ownerProfile.getId().equals(restoredOwner.getId()),
                    "Spell Dispenser owner UUID helper returned the wrong value");
            helper.assertTrue(restoredOwner != null && ownerProfile.getName().equals(restoredOwner.getName()),
                    "Spell Dispenser owner name helper returned the wrong value");
        });
    }

    @GameTest(template = "gametest/basic_floor")
    public static void storedAssetsAndOwnerPersistTogetherThroughNbt(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var lookup = helper.getLevel().registryAccess();
            var owner = createSpellDispenserOwnerProfile("dispenser_assets");
            var original = new SpellDispenserBlockEntity(BlockPos.ZERO, BlockRegistry.SPELL_DISPENSER.get().defaultBlockState());
            original.setOwnerProfile(owner);
            var scroll = createSpellScroll(SpellRegistry.MAGE_LIGHT.get());
            scroll.set(DataComponents.CUSTOM_NAME, Component.literal("Saved dispenser scroll"));
            var flask = createFilledSpellcastersFlask(lookup,
                    createInstantManaPotion(PotionRegistry.INSTANT_MANA_ONE.get()), 2, 1);
            flask.set(DataComponents.CUSTOM_NAME, Component.literal("Saved dispenser fuel"));
            original.getInventory().setStackInSlot(SpellDispenserBlockEntity.SPELL_SLOT_INDEX, scroll.copy());
            original.getInventory().setStackInSlot(SpellDispenserBlockEntity.FLASK_SLOT_START, flask.copy());
            // 回復量のバランス値に依存せず未消費の液体を保存するため、マナを満杯にする。
            var storedMana = SpellDispenserManaHelper.MAX_MANA;
            original.setCurrentMana(storedMana);
            var fluid = createIronsManaPotionFluid(PotionRegistry.INSTANT_MANA_ONE.get(), 250);
            var handler = original.getFluidHandler(Direction.SOUTH);
            helper.assertTrue(handler != null && handler.fill(fluid, IFluidHandler.FluidAction.EXECUTE) == 250,
                    "Stored fluid fixture should accept a full dose without room to consume it");
            helper.assertTrue(ItemStack.matches(scroll, original.getInventory().getStackInSlot(0))
                            && ItemStack.matches(flask, original.getInventory().getStackInSlot(SpellDispenserBlockEntity.FLASK_SLOT_START))
                            && original.getCurrentMana() == storedMana && original.getStoredManaPotionFluid().getAmount() == 250,
                    "Assets should already be intact before serialization");
            var restored = new SpellDispenserBlockEntity(BlockPos.ZERO, BlockRegistry.SPELL_DISPENSER.get().defaultBlockState());
            restored.loadWithComponents(original.getUpdateTag(lookup), lookup);
            helper.assertTrue(restored.getOwnerProfile() != null && owner.getId().equals(restored.getOwnerProfile().getId())
                            && owner.getName().equals(restored.getOwnerProfile().getName()),
                    "Owner UUID and name should survive together with stored assets");
            for (var slot = 0; slot < original.getInventory().getSlots(); ++slot) {
                helper.assertTrue(ItemStack.matches(original.getInventory().getStackInSlot(slot),
                                restored.getInventory().getStackInSlot(slot)),
                        "Every stored slot should retain count and full components: slot=" + slot);
            }
            helper.assertTrue(restored.getCurrentMana() == storedMana && restored.getStoredManaPotionFluid().getAmount() == 250
                            && SpellDispenserManaFluidHelper.isSameFluidAndTags(restored.getStoredManaPotionFluid(), fluid),
                    "Mana and unconsumed potion fluid should survive the same save/load");
        });
    }

    @GameTest(template = "gametest/basic_floor")
    public static void normalBlockRemovalReturnsEveryStoredItemExactlyOnce(GameTestHelper helper) {
        helper.succeedIf(() -> {
            try (var scope = new DispenserTestScope(helper)) {
                var pos = new BlockPos(2, 1, 2);
                helper.setBlock(pos, BlockRegistry.SPELL_DISPENSER.get());
                var block = (SpellDispenserBlockEntity) helper.getBlockEntity(pos);
                var scroll = createSpellScroll(SpellRegistry.MAGE_LIGHT.get());
                scroll.set(DataComponents.CUSTOM_NAME, Component.literal("Returned dispenser scroll"));
                var bottles = new ItemStack(Items.GLASS_BOTTLE, 3);
                block.getInventory().setStackInSlot(0, scroll.copy());
                block.getInventory().setStackInSlot(SpellDispenserBlockEntity.FLASK_SLOT_START, bottles.copy());
                scope.run(() -> helper.getLevel().setBlock(helper.absolutePos(pos), Blocks.AIR.defaultBlockState(), 3));
                var drops = scope.entities(ItemEntity.class);
                helper.assertTrue(drops.size() == 2 && drops.stream().anyMatch(e -> ItemStack.matches(e.getItem(), scroll))
                                && drops.stream().anyMatch(e -> ItemStack.matches(e.getItem(), bottles)),
                        "Normal removal should return both complete stored stacks once: drops=" + drops.stream().map(ItemEntity::getItem).toList());
            }
        });
    }
}
