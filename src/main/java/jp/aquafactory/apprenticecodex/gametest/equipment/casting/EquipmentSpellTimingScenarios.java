package jp.aquafactory.apprenticecodex.gametest.equipment.casting;

import io.redspace.ironsspellbooks.api.events.SpellCooldownAddedEvent;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.capabilities.magic.MagicManager;
import jp.aquafactory.apprenticecodex.config.ApprenticeCodexServerConfig;
import jp.aquafactory.apprenticecodex.item.WeaponImbueCooldownHelper;
import jp.aquafactory.apprenticecodex.item.armor.MagiAgentSuitCooldownEvent;
import jp.aquafactory.apprenticecodex.item.armor.MagiAgentSuitEffects;
import jp.aquafactory.apprenticecodex.item.curios.craftsmansdelight.CraftsmansDelight;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import jp.aquafactory.apprenticecodex.registry.SpellRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;

import static jp.aquafactory.apprenticecodex.gametest.spell.heavenlyfist.HeavenlyFistHarvestGameTests.equipRingCurio;
import static jp.aquafactory.apprenticecodex.gametest.support.PlayerTestSupport.createEquipmentTestPlayer;

final class EquipmentSpellTimingScenarios {
    private EquipmentSpellTimingScenarios() {}

    static void magiAgentSuitBootsCooldownReducesTargetSpell(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0),
                    "magi_agent_boots_cooldown_test");
            var spell = SpellRegistry.COMMENCE_FIRE.get();
            var baseCooldown = MagicManager.getEffectiveSpellCooldown(
                    spell,
                    player,
                    CastSource.SPELLBOOK
            );

            var controlEvent = new SpellCooldownAddedEvent.Pre(baseCooldown, spell, player, CastSource.SPELLBOOK);
            MinecraftForge.EVENT_BUS.post(controlEvent);
            helper.assertTrue(controlEvent.getEffectiveCooldown() == baseCooldown,
                    "Magi Agent Suit Boots should not reduce cooldown while unequipped");

            player.setItemSlot(EquipmentSlot.FEET, new ItemStack(ItemRegistry.MAGI_AGENT_SUIT_BOOTS.get()));
            var bootsEvent = new SpellCooldownAddedEvent.Pre(baseCooldown, spell, player, CastSource.SPELLBOOK);
            MinecraftForge.EVENT_BUS.post(bootsEvent);

            var expectedCooldown = WeaponImbueCooldownHelper.getEffectiveSpellCooldown(
                    spell,
                    player,
                    CastSource.SPELLBOOK
            );
            helper.assertTrue(expectedCooldown < baseCooldown,
                    "Magi Agent Suit Boots expected cooldown should be shorter than base cooldown");
            helper.assertTrue(bootsEvent.getEffectiveCooldown() == expectedCooldown,
                    "Magi Agent Suit Boots should reduce target spell cooldown but got "
                            + bootsEvent.getEffectiveCooldown() + " / expected " + expectedCooldown);
        });
    }

    static void equipmentCooldownReductionsDoNotStack(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0),
                    "magi_agent_boots_craftsmans_cooldown_test");
            player.setItemSlot(EquipmentSlot.FEET, new ItemStack(ItemRegistry.MAGI_AGENT_SUIT_BOOTS.get()));
            equipRingCurio(player, new ItemStack(ItemRegistry.CRAFTSMANS_DELIGHT.get()));

            var spell = SpellRegistry.THERMAL_PROCESS.get();
            var baseCooldown = MagicManager.getEffectiveSpellCooldown(
                    spell,
                    player,
                    CastSource.SPELLBOOK
            );
            var cooldownEvent = new SpellCooldownAddedEvent.Pre(baseCooldown, spell, player, CastSource.SPELLBOOK);
            MinecraftForge.EVENT_BUS.post(cooldownEvent);

            var expectedCooldown = WeaponImbueCooldownHelper.getEffectiveSpellCooldown(
                    spell,
                    player,
                    CastSource.SPELLBOOK
            );
            var singleReductionCooldown = Math.max(1, (int) (spell.getSpellCooldown() * 0.5D));
            helper.assertTrue(expectedCooldown == singleReductionCooldown,
                    "Equal equipment cooldown multipliers should apply once instead of stacking");
            helper.assertTrue(cooldownEvent.getEffectiveCooldown() == expectedCooldown,
                    "Magi Agent Suit Boots and CraftsmansDelight should keep the strongest cooldown but got "
                            + cooldownEvent.getEffectiveCooldown() + " / expected " + expectedCooldown);
        });
    }

    static void equipmentSpellTimingMultipliersFollowServerConfigAndKeepOneTickMinimum(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0),
                    "equipment_spell_timing_multiplier_config_test");
            equipRingCurio(player, new ItemStack(ItemRegistry.CRAFTSMANS_DELIGHT.get()));
            player.setItemSlot(EquipmentSlot.FEET, new ItemStack(ItemRegistry.MAGI_AGENT_SUIT_BOOTS.get()));
            var craftsmansSpell = SpellRegistry.HARVEST_MOON.get();
            var magiAgentSpell = SpellRegistry.COMMENCE_FIRE.get();

            var effectiveCastTime = 101;

            try (var ignored = ApprenticeCodexServerConfig.useEquipmentSpellTimingMultipliersOverrideForGameTest(
                    0.25D,
                    0.75D,
                    0.25D
            )) {
                helper.assertTrue(
                        CraftsmansDelight.applyCooldownDiscount(craftsmansSpell.getSpellCooldown(), player)
                                == Math.max(1, (int) (craftsmansSpell.getSpellCooldown() * 0.25D)),
                        "CraftsmansDelight should follow its server cooldown multiplier"
                );
                helper.assertTrue(
                        MagiAgentSuitEffects.applyBootsCooldownDiscount(
                                magiAgentSpell.getSpellCooldown(),
                                magiAgentSpell,
                                player
                        ) == Math.max(1, (int) (magiAgentSpell.getSpellCooldown() * 0.75D)),
                        "Magi Agent Suit Boots should follow their server cooldown multiplier"
                );
                helper.assertTrue(
                        MagiAgentSuitEffects.applyBootsCastTimeReduction(
                                magiAgentSpell,
                                effectiveCastTime,
                                player
                        ) == Math.max(1, (int) Math.round(effectiveCastTime * 0.25D)),
                        "Magi Agent Suit Boots should follow their server cast time multiplier"
                );
            }

            try (var ignored = ApprenticeCodexServerConfig.useEquipmentSpellTimingMultipliersOverrideForGameTest(
                    0.0D,
                    0.0D,
                    0.0D
            )) {
                helper.assertTrue(
                        CraftsmansDelight.applyCooldownDiscount(craftsmansSpell.getSpellCooldown(), player) == 1,
                        "CraftsmansDelight should keep a 1 tick minimum at multiplier 0.0"
                );
                helper.assertTrue(
                        MagiAgentSuitEffects.applyBootsCooldownDiscount(
                                magiAgentSpell.getSpellCooldown(),
                                magiAgentSpell,
                                player
                        ) == 1,
                        "Magi Agent Suit Boots should keep a 1 tick minimum at multiplier 0.0"
                );
                helper.assertTrue(
                        MagiAgentSuitEffects.applyBootsCastTimeReduction(
                                magiAgentSpell,
                                effectiveCastTime,
                                player
                        ) == 1,
                        "Magi Agent Suit Boots should keep a 1 tick cast time minimum at multiplier 0.0"
                );
            }
        });
    }

    static void magiAgentSuitBootsCooldownPreservesExistingAdditiveCooldown(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0),
                    "magi_agent_boots_additive_cooldown_test");
            player.setItemSlot(EquipmentSlot.FEET, new ItemStack(ItemRegistry.MAGI_AGENT_SUIT_BOOTS.get()));

            var spell = SpellRegistry.COMMENCE_FIRE.get();
            var baseCooldown = MagicManager.getEffectiveSpellCooldown(
                    spell,
                    player,
                    CastSource.SPELLBOOK
            );
            var bootsCooldown = WeaponImbueCooldownHelper.getEffectiveSpellCooldown(
                    spell,
                    player,
                    CastSource.SPELLBOOK
            );
            var extraCooldown = 37;
            var cooldownEvent = new SpellCooldownAddedEvent.Pre(
                    baseCooldown + extraCooldown,
                    spell,
                    player,
                    CastSource.SPELLBOOK
            );

            MagiAgentSuitCooldownEvent.onSpellCooldownAdded(cooldownEvent);

            var expectedCooldown = bootsCooldown + extraCooldown;
            helper.assertTrue(cooldownEvent.getEffectiveCooldown() == expectedCooldown,
                    "Magi Agent Suit Boots should keep additive cooldown components but got "
                            + cooldownEvent.getEffectiveCooldown() + " / expected " + expectedCooldown);
        });
    }

    static void strongestLimitedBaseCooldownSelectionIgnoresStacking(GameTestHelper helper) {
        helper.succeedIf(() -> {
            helper.assertTrue(WeaponImbueCooldownHelper.selectStrongestLimitedBaseCooldown(90) == 90,
                    "No limited cooldown candidate should keep the base cooldown");
            helper.assertTrue(WeaponImbueCooldownHelper.selectStrongestLimitedBaseCooldown(90, 45, 30) == 30,
                    "Limited cooldown candidates should choose only the strongest reduction");
            helper.assertTrue(WeaponImbueCooldownHelper.selectStrongestLimitedBaseCooldown(90, 0, -1, 60) == 60,
                    "Invalid limited cooldown candidates should be ignored");
            helper.assertTrue(WeaponImbueCooldownHelper.selectStrongestLimitedBaseCooldown(0, 1) == 0,
                    "Zero base cooldown should stay zero");
        });
    }
}
