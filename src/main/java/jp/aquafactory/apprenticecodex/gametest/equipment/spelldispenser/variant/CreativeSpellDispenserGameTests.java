package jp.aquafactory.apprenticecodex.gametest.equipment.spelldispenser.variant;

import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.block.spelldispenser.SpellDispenser;
import jp.aquafactory.apprenticecodex.block.spelldispenser.SpellDispenserBlockEntity;
import jp.aquafactory.apprenticecodex.block.spelldispenser.SpellDispenserManaHelper;
import jp.aquafactory.apprenticecodex.block.spelldispenser.SpellDispenserVariant;
import jp.aquafactory.apprenticecodex.config.ApprenticeCodexServerConfig;
import jp.aquafactory.apprenticecodex.gametest.equipment.spelldispenser.support.DispenserTestScope;
import jp.aquafactory.apprenticecodex.registry.BlockRegistry;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import static jp.aquafactory.apprenticecodex.gametest.equipment.spelldispenser.support.SpellDispenserTestSupport.createSpellDispenserOwnerProfile;
import static jp.aquafactory.apprenticecodex.gametest.equipment.spelldispenser.support.SpellDispenserTestSupport.createSpellDispenserPlacer;
import static jp.aquafactory.apprenticecodex.gametest.equipment.spelldispenser.support.SpellDispenserTestSupport.createSpellScroll;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class CreativeSpellDispenserGameTests {
    private CreativeSpellDispenserGameTests() {}

    @GameTest(template = "gametest/basic_floor")
    public static void creativeSpellDispenserPlacementStartsAtZeroManaAndSkipsOwnerProfile(GameTestHelper helper) {
        helper.succeedIf(() -> {
            try (var scope = new DispenserTestScope(helper)) {
                var level = (ServerLevel) helper.getLevel();
                var pos = new BlockPos(0, 1, 0);
                var absolutePos = helper.absolutePos(pos);
                helper.setBlock(pos, BlockRegistry.CREATIVE_SPELL_DISPENSER.get());

                var blockEntity = level.getBlockEntity(absolutePos);
                helper.assertTrue(blockEntity instanceof SpellDispenserBlockEntity, "Creative Spell Dispenser block entity was not created");
                var spellDispenser = scope.block((SpellDispenserBlockEntity) blockEntity);

                var player = createSpellDispenserPlacer(helper, new BlockPos(0, 2, 0), "creative_spell_dispenser_place_test");
                var state = level.getBlockState(absolutePos);
                ((SpellDispenser) state.getBlock()).setPlacedBy(level, absolutePos, state, player, new ItemStack(ItemRegistry.CREATIVE_SPELL_DISPENSER.get()));

                helper.assertTrue(spellDispenser.getCurrentMana() == 0,
                        "Creative Spell Dispenser placement should start at zero mana: " + spellDispenser.getCurrentMana());
                helper.assertFalse(spellDispenser.hasOwnerProfile(), "Creative Spell Dispenser placement stored an owner profile");
            }
        });
    }

    @GameTest(template = "gametest/basic_floor")
    public static void creativeSpellDispenserIgnoresOwnerProfileNbt(GameTestHelper helper) {
        helper.succeedIf(() -> {
            try (var scope = new DispenserTestScope(helper)) {
                var ownerProfile = createSpellDispenserOwnerProfile("creative_spell_dispenser_owner_nbt_test");
                var originalTag = new CompoundTag();
                SpellDispenserBlockEntity.saveOwnerProfile(originalTag, ownerProfile);

                var creative = new SpellDispenserBlockEntity(BlockPos.ZERO, BlockRegistry.CREATIVE_SPELL_DISPENSER.get().defaultBlockState());
                creative.loadWithComponents(originalTag, helper.getLevel().registryAccess());
                helper.assertFalse(creative.hasOwnerProfile(), "Creative Spell Dispenser restored owner profile from NBT");

                creative.setOwnerProfile(ownerProfile);
                helper.assertFalse(creative.getUpdateTag(helper.getLevel().registryAccess()).hasUUID("OwnerUuid"),
                        "Creative Spell Dispenser saved owner UUID into update NBT");
            }
        });
    }

    @GameTest(template = "gametest/basic_floor", batch = "creative_spell_dispenser_config")
    public static void creativeSpellDispenserUsesCreativeManaConfig(GameTestHelper helper) {
        helper.succeedIf(() -> {
            try (var scope = new DispenserTestScope(helper)) {
                var pos = new BlockPos(0, 1, 0);
                helper.setBlock(pos, BlockRegistry.CREATIVE_SPELL_DISPENSER.get());
                var blockEntity = helper.getBlockEntity(pos);
                helper.assertTrue(blockEntity instanceof SpellDispenserBlockEntity, "Creative Spell Dispenser block entity was not created");
                var spellDispenser = scope.block((SpellDispenserBlockEntity) blockEntity);
                var spell = SpellRegistry.MAGIC_MISSILE_SPELL.get();
                var scrollStack = createSpellScroll(spell);
                spellDispenser.getInventory().setStackInSlot(0, scrollStack.copy());
                spellDispenser.setCurrentMana(0);

                try (var ignored = ApprenticeCodexServerConfig.useCreativeSpellDispenserConfigOverrideForGameTest(false, 0.1D)) {
                    var castResult = scope.capture(() -> spellDispenser.tryActivate());
                    helper.assertTrue(castResult.succeeded(), "Creative Spell Dispenser consumed mana while creative mana cost was disabled");
                    helper.assertTrue(spellDispenser.getCurrentMana() == 0,
                            "Creative Spell Dispenser changed mana while creative mana cost was disabled: " + spellDispenser.getCurrentMana());
                }

                spellDispenser.clearCooldown();
                try (var ignored = ApprenticeCodexServerConfig.useCreativeSpellDispenserConfigOverrideForGameTest(true, 0.1D)) {
                    var castResult = scope.capture(() -> spellDispenser.tryActivate());
                    helper.assertTrue(!castResult.succeeded(), "Creative Spell Dispenser cast without mana while creative mana cost was enabled");
                    helper.assertTrue(castResult.insufficientMana(), "Creative Spell Dispenser returned the wrong failure with creative mana cost enabled");
                }

                scope.assertReleased(helper, "Creative Spell Dispenser mana config test left proxy state behind");
            }
        });
    }

    @GameTest(template = "gametest/basic_floor", batch = "creative_spell_dispenser_config")
    public static void creativeSpellDispenserAllowsZeroCooldownMultiplier(GameTestHelper helper) {
        helper.succeedIf(() -> {
            try (var scope = new DispenserTestScope(helper)) {
                var pos = new BlockPos(0, 1, 0);
                helper.setBlock(pos, BlockRegistry.CREATIVE_SPELL_DISPENSER.get());
                var blockEntity = helper.getBlockEntity(pos);
                helper.assertTrue(blockEntity instanceof SpellDispenserBlockEntity, "Creative Spell Dispenser block entity was not created");
                var spellDispenser = scope.block((SpellDispenserBlockEntity) blockEntity);
                var spell = SpellRegistry.MAGIC_MISSILE_SPELL.get();
                var scrollStack = createSpellScroll(spell);
                spellDispenser.getInventory().setStackInSlot(0, scrollStack.copy());
                spellDispenser.setCurrentMana(SpellDispenserManaHelper.MAX_MANA);

                try (var ignored = ApprenticeCodexServerConfig.useCreativeSpellDispenserConfigOverrideForGameTest(true, 0.0D)) {
                    var castResult = scope.capture(() -> spellDispenser.tryActivate());
                    helper.assertTrue(castResult.succeeded(), "Creative Spell Dispenser failed to cast with zero cooldown multiplier");
                    helper.assertTrue(castResult.cooldownTicks() == 0,
                            "Creative Spell Dispenser did not allow zero cooldown multiplier: " + castResult.cooldownTicks());
                    helper.assertFalse(spellDispenser.isCoolingDown(), "Creative Spell Dispenser entered cooldown with zero cooldown multiplier");
                }

                scope.assertReleased(helper, "Creative Spell Dispenser cooldown config test left proxy state behind");
            }
        });
    }

    @GameTest(template = "gametest/basic_floor")
    public static void creativeSpellDispenserDropsNoStoredItems(GameTestHelper helper) {
        helper.succeedIf(() -> {
            try (var scope = new DispenserTestScope(helper)) {
                var level = (ServerLevel) helper.getLevel();
                var pos = new BlockPos(0, 1, 0);
                var absolutePos = helper.absolutePos(pos);
                helper.setBlock(pos, BlockRegistry.CREATIVE_SPELL_DISPENSER.get());

                var blockEntity = helper.getBlockEntity(pos);
                helper.assertTrue(blockEntity instanceof SpellDispenserBlockEntity, "Creative Spell Dispenser block entity was not created");
                var spellDispenser = scope.block((SpellDispenserBlockEntity) blockEntity);
                var scrollStack = createSpellScroll(SpellRegistry.MAGIC_MISSILE_SPELL.get());
                spellDispenser.getInventory().setStackInSlot(SpellDispenserBlockEntity.SPELL_SLOT_INDEX, scrollStack);
                spellDispenser.getInventory().setStackInSlot(SpellDispenserBlockEntity.FLASK_SLOT_START, new ItemStack(Items.GLASS_BOTTLE));

                scope.run(() -> level.setBlock(absolutePos, Blocks.AIR.defaultBlockState(), 3));
                var drops = scope.entities(ItemEntity.class);
                helper.assertTrue(drops.isEmpty(), "Creative Spell Dispenser dropped stored items: " + drops.size());
            }
        });
    }

    @GameTest(template = "gametest/basic_floor")
    public static void creativeSpellDispenserRestrictsMenuAccess(GameTestHelper helper) {
        helper.succeedIf(() -> {
            try (var scope = new DispenserTestScope(helper)) {
                var survivalPlayer = createSpellDispenserPlacer(helper, new BlockPos(0, 2, 0), "creative_spell_dispenser_menu_survival");
                var creativePlayer = createSpellDispenserPlacer(helper, new BlockPos(0, 2, 0), "creative_spell_dispenser_menu_creative");
                creativePlayer.gameMode.changeGameModeForPlayer(GameType.CREATIVE);

                helper.assertFalse(
                        SpellDispenserVariant.canUseCreativeVariant(survivalPlayer),
                        "Creative Spell Dispenser menu allowed a survival player without permissions"
                );
                helper.assertTrue(
                        SpellDispenserVariant.canUseCreativeVariant(creativePlayer),
                        "Creative Spell Dispenser menu rejected a creative player"
                );
            }
        });
    }


}
