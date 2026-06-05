package jp.aquafactory.apprenticecodex.gametest;

import io.redspace.ironsspellbooks.api.events.SpellOnCastEvent;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import io.redspace.ironsspellbooks.capabilities.magic.RecastInstance;
import jp.aquafactory.apprenticecodex.config.ApprenticeCodexServerConfig;
import jp.aquafactory.apprenticecodex.config.item.SpellgunServerConfig;
import jp.aquafactory.apprenticecodex.item.AbstractSpellGunItem;
import jp.aquafactory.apprenticecodex.item.SpellGunCastEvent;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import jp.aquafactory.apprenticecodex.registry.SpellRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;

final class EquipmentSpellGunGameTestScenarios extends ApprenticeCodexGameTestScenarios {
    private EquipmentSpellGunGameTestScenarios() {
    }

    static void goldSpellcasterGunImbuedSpellStaysRemovableAfterNormalization(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var item = (AbstractSpellGunItem) ItemRegistry.GOLD_SPELLCASTER_GUN.get();
            var stack = createInitializedPresetStack(item);
            var replacementSpell = io.redspace.ironsspellbooks.api.registry.SpellRegistry.MAGIC_MISSILE_SPELL.get();

            applyRestrictedImbueNormalization(helper, stack, item, replacementSpell, 1);

            var spellContainer = ISpellContainer.get(stack);
            helper.assertTrue(spellContainer != null, "Gold Spellcaster Gun normalized spell container is null");
            assertSpellData(helper, spellContainer, 0, replacementSpell, 1, false,
                    "Gold Spellcaster Gun imbued spell should be removable");
            helper.assertTrue(spellContainer.getSpellAtIndex(0).canRemove(),
                    "Gold Spellcaster Gun imbued spell should remain extractable in Spellcaster Workbench");
        });
    }
    static void spellcasterGunRecastImbueRestrictionsMatchTier(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var iron = (AbstractSpellGunItem) ItemRegistry.IRON_SPELLCASTER_GUN.get();
            var gold = (AbstractSpellGunItem) ItemRegistry.GOLD_SPELLCASTER_GUN.get();
            var diamond = (AbstractSpellGunItem) ItemRegistry.DIAMOND_SPELLCASTER_GUN.get();
            var instantRecastSpell = SpellRegistry.HIGANBANA.get();
            var longRecastSpell = SpellRegistry.ARCHER_MULTIPLE.get();
            var continuousSpell = io.redspace.ironsspellbooks.api.registry.SpellRegistry.FIRE_BREATH_SPELL.get();

            helper.assertFalse(iron.canImbueSpell(instantRecastSpell, 1),
                    "Iron Spellcaster Gun should continue rejecting recast spells");
            helper.assertTrue(gold.canImbueSpell(instantRecastSpell, 1),
                    "Gold Spellcaster Gun should allow instant recast spell imbuing");
            helper.assertTrue(diamond.canImbueSpell(instantRecastSpell, 1),
                    "Diamond Spellcaster Gun should allow instant recast spell imbuing");
            helper.assertTrue(diamond.canImbueSpell(longRecastSpell, 1),
                    "Diamond Spellcaster Gun should allow long recast spell imbuing");
            helper.assertFalse(diamond.canImbueSpell(continuousSpell, 1),
                    "Diamond Spellcaster Gun should continue rejecting continuous spells");
        });
    }
    static void spellgunServerConfigDefaultsMatchCurrentHardcodedValues(GameTestHelper helper) {
        helper.succeedIf(() -> {
            helper.assertTrue(ApprenticeCodexServerConfig.ironSpellgunMaxInstantImbueCooldownTicks() == 20 * 5,
                    "Iron Spellcaster Gun imbue cooldown limit default changed");
            helper.assertTrue(ApprenticeCodexServerConfig.ironSpellgunOverriddenSpellCooldownTicks() == 10,
                    "Iron Spellcaster Gun cast cooldown default changed");
            helper.assertTrue(ApprenticeCodexServerConfig.copperSpellgunMaxInstantImbueCooldownTicks() == 20 * 10,
                    "Copper Spellcaster Gun imbue cooldown limit default changed");
            helper.assertTrue(ApprenticeCodexServerConfig.copperSpellgunOverriddenSpellCooldownTicks() == 20,
                    "Copper Spellcaster Gun cast cooldown default changed");
            helper.assertTrue(ApprenticeCodexServerConfig.goldSpellgunMaxInstantImbueCooldownTicks() == 20 * 20,
                    "Gold Spellcaster Gun imbue cooldown limit default changed");
            helper.assertTrue(ApprenticeCodexServerConfig.goldSpellgunOverriddenSpellCooldownTicks() == 40,
                    "Gold Spellcaster Gun cast cooldown default changed");
            helper.assertTrue(ApprenticeCodexServerConfig.diamondSpellgunMaxInstantImbueCooldownTicks() == 20 * 30,
                    "Diamond Spellcaster Gun imbue cooldown limit default changed");
            helper.assertTrue(ApprenticeCodexServerConfig.diamondSpellgunOverriddenSpellCooldownTicks() == 80,
                    "Diamond Spellcaster Gun cast cooldown default changed");

            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "spellgun_default_config_test");
            var spell = io.redspace.ironsspellbooks.api.registry.SpellRegistry.MAGIC_MISSILE_SPELL.get();
            assertSpellgunCooldownOverride(helper, player, new ItemStack(ItemRegistry.IRON_SPELLCASTER_GUN.get()), spell, 200, 10,
                    "Iron Spellcaster Gun should keep its default cast cooldown");
            assertSpellgunCooldownOverride(helper, player, new ItemStack(ItemRegistry.COPPER_SPELLCASTER_GUN.get()), spell, 200, 20,
                    "Copper Spellcaster Gun should keep its default cast cooldown");
            assertSpellgunCooldownOverride(helper, player, new ItemStack(ItemRegistry.GOLD_SPELLCASTER_GUN.get()), spell, 200, 40,
                    "Gold Spellcaster Gun should keep its default cast cooldown");
            assertSpellgunCooldownOverride(helper, player, new ItemStack(ItemRegistry.DIAMOND_SPELLCASTER_GUN.get()), spell, 200, 80,
                    "Diamond Spellcaster Gun should keep its default cast cooldown");
        });
    }
    static void spellgunZeroImbueCooldownLimitDisablesOnlyCooldownLimit(GameTestHelper helper) {
        helper.succeedIf(() -> {
            try (var ignored = ApprenticeCodexServerConfig.useSpellgunConfigOverrideForGameTest(new SpellgunServerConfig.Values(
                    0,
                    10,
                    20 * 10,
                    20,
                    20 * 20,
                    40,
                    20 * 30,
                    80
            ))) {
                var iron = (AbstractSpellGunItem) ItemRegistry.IRON_SPELLCASTER_GUN.get();
                var cooldownLimitedSpell = SpellRegistry.SEARCH_BEACON.get();
                helper.assertTrue(cooldownLimitedSpell.getSpellCooldown() > 20 * 5,
                        "Search Beacon should remain above Iron Spellcaster Gun's default cooldown limit");
                helper.assertTrue(iron.canImbueSpell(cooldownLimitedSpell, 1),
                        "Iron Spellcaster Gun maxInstantImbueCooldownTicks=0 should disable only the cooldown limit");
                helper.assertFalse(iron.canImbueSpell(SpellRegistry.HIGANBANA.get(), 1),
                        "Iron Spellcaster Gun should still reject recast spells when only the cooldown limit is disabled");
            }
        });
    }
    static void spellgunZeroCastCooldownConfigForcesZeroCooldown(GameTestHelper helper) {
        helper.succeedIf(() -> {
            try (var ignored = ApprenticeCodexServerConfig.useSpellgunConfigOverrideForGameTest(new SpellgunServerConfig.Values(
                    20 * 5,
                    0,
                    20 * 10,
                    0,
                    20 * 20,
                    0,
                    20 * 30,
                    0
            ))) {
                var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "spellgun_zero_cooldown_config_test");
                var spell = io.redspace.ironsspellbooks.api.registry.SpellRegistry.MAGIC_MISSILE_SPELL.get();
                assertSpellgunCooldownOverride(helper, player, new ItemStack(ItemRegistry.IRON_SPELLCASTER_GUN.get()), spell, 200, 0,
                        "Iron Spellcaster Gun overriddenSpellCooldownTicks=0 should force a 0-tick cooldown");
                assertSpellgunCooldownOverride(helper, player, new ItemStack(ItemRegistry.DIAMOND_SPELLCASTER_GUN.get()), spell, 200, 0,
                        "Diamond Spellcaster Gun overriddenSpellCooldownTicks=0 should force a 0-tick cooldown");
            }
        });
    }
    static void spellcasterGunRecastCastBypassesAmmoRequirement(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var item = (AbstractSpellGunItem) ItemRegistry.DIAMOND_SPELLCASTER_GUN.get();
            var stack = createInitializedPresetStack(item);
            var spell = SpellRegistry.ARCHER_MULTIPLE.get();
            applyRestrictedImbueNormalization(helper, stack, item, spell, 1);

            var player = createArcherMultiplePlayer(helper, new BlockPos(0, 12, 0), "spellgun_recast_ammo_bypass_test");
            player.setItemInHand(InteractionHand.MAIN_HAND, stack);

            var firstUse = stack.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
            helper.assertFalse(firstUse.getResult().consumesAction(),
                    "Diamond Spellcaster Gun should reject initial Archer Multiple cast without ammo");

            var magicData = MagicData.getPlayerMagicData(player);
            magicData.getPlayerRecasts().addRecast(new RecastInstance(
                    spell.getSpellId(),
                    1,
                    2,
                    100,
                    CastSource.SWORD,
                    null
            ), magicData);

            var recastUse = stack.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
            helper.assertTrue(recastUse.getResult().consumesAction(),
                    "Diamond Spellcaster Gun should allow recast without ammo");

            var ammoStack = new ItemStack(ItemRegistry.ADVANCED_SPELLCASTER_ROUND.get(), 1);
            player.getInventory().add(ammoStack);
            magicData.getPlayerRecasts().addRecast(new RecastInstance(
                    spell.getSpellId(),
                    1,
                    2,
                    100,
                    CastSource.SWORD,
                    null
            ), magicData);
            magicData.setPlayerCastingItem(stack);
            MinecraftForge.EVENT_BUS.post(new SpellOnCastEvent(
                    player,
                    spell.getSpellId(),
                    1,
                    spell.getManaCost(1),
                    spell.getSchoolType(),
                    CastSource.SWORD
            ));
            helper.assertTrue(SpellGunCastEvent.countAvailableAmmo(
                    player,
                    player.getInventory(),
                    ItemRegistry.ADVANCED_SPELLCASTER_ROUND.get()
            ) == 1, "Recast Spellcaster Gun cast should not consume ammo from the cast event");
        });
    }
    static void goldSpellcasterGunImbuedSpellStaysRemovableAfterSaveLoad(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var item = (AbstractSpellGunItem) ItemRegistry.GOLD_SPELLCASTER_GUN.get();
            var stack = createInitializedPresetStack(item);
            var replacementSpell = io.redspace.ironsspellbooks.api.registry.SpellRegistry.MAGIC_MISSILE_SPELL.get();

            applyRestrictedImbueNormalization(helper, stack, item, replacementSpell, 1);

            var restored = roundTripItemStack(stack);
            repairPresetSpellContainerStateIfNeeded(restored);
            var spellContainer = ISpellContainer.get(restored);
            helper.assertTrue(spellContainer != null, "Gold Spellcaster Gun save/load spell container is null");
            assertSpellData(helper, spellContainer, 0, replacementSpell, 1, false,
                    "Gold Spellcaster Gun imbued spell should remain removable after save/load");
            helper.assertTrue(spellContainer.getSpellAtIndex(0).canRemove(),
                    "Gold Spellcaster Gun imbued spell should remain extractable after save/load");
        });
    }
    static void ironSpellcasterGunExtractedSpellStaysClearedAfterSaveLoad(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var item = (AbstractSpellGunItem) ItemRegistry.IRON_SPELLCASTER_GUN.get();
            var stack = createInitializedPresetStack(item);

            applyPresetSpellExtraction(helper, stack);

            var restored = roundTripItemStack(stack);
            repairPresetSpellContainerStateIfNeeded(restored);
            assertClearedSpellContainer(helper, restored, "Iron Spellcaster Gun should stay cleared after save/load");
        });
    }
    static void goldSpellcasterGunLegacyLockedReplacementIsRecoveredAfterSaveLoad(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var item = (AbstractSpellGunItem) ItemRegistry.GOLD_SPELLCASTER_GUN.get();
            var stack = createInitializedPresetStack(item);
            var replacementSpell = io.redspace.ironsspellbooks.api.registry.SpellRegistry.MAGIC_MISSILE_SPELL.get();

            applyLegacyLockedReplacement(helper, stack, replacementSpell, 1);

            var restored = roundTripItemStack(stack);
            repairPresetSpellContainerStateIfNeeded(restored);
            var spellContainer = ISpellContainer.get(restored);
            helper.assertTrue(spellContainer != null, "Gold Spellcaster Gun recovered spell container is null");
            assertSpellData(helper, spellContainer, 0, replacementSpell, 1, false,
                    "Gold Spellcaster Gun legacy locked replacement should be recovered after save/load");
        });
    }
}
