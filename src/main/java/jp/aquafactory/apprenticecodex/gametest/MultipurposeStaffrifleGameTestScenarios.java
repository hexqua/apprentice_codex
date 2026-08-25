package jp.aquafactory.apprenticecodex.gametest;

import io.redspace.ironsspellbooks.api.events.SpellCooldownAddedEvent;
import io.redspace.ironsspellbooks.api.events.SpellOnCastEvent;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.CastSource;

import java.util.ArrayList;

import jp.aquafactory.apprenticecodex.item.curios.CuriosSlotConstants;
import jp.aquafactory.apprenticecodex.item.ItemManaBypassCastEvent;
import jp.aquafactory.apprenticecodex.item.ManaBypassSpellItem;
import jp.aquafactory.apprenticecodex.item.multipurposestaffrifle.MultipurposeStaffrifleCastContext;
import jp.aquafactory.apprenticecodex.item.multipurposestaffrifle.MultipurposeStaffrifleCastEvent;
import jp.aquafactory.apprenticecodex.item.multipurposestaffrifle.MultipurposeStaffrifleRateLimiter;
import jp.aquafactory.apprenticecodex.item.multipurposestaffrifle.MultipurposeStaffrifle;
import jp.aquafactory.apprenticecodex.item.spellgun.SpellgunCastContext;
import jp.aquafactory.apprenticecodex.item.SpellcasterRoundItem;
import jp.aquafactory.apprenticecodex.item.spellgun.SpellGunCastEvent;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraftforge.event.entity.player.PlayerEvent;

final class MultipurposeStaffrifleGameTestScenarios extends ApprenticeCodexGameTestScenarios {
    private static final String LEGACY_NEXT_SPECIAL_CAST_TICK_TAG =
            "ApprenticeCodexMultipurposeStaffrifleNextSpecialCastTick";

    private MultipurposeStaffrifleGameTestScenarios() {
    }

    static void multipurposeStaffrifleKeepsExpectedStats(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var stack = new ItemStack(ItemRegistry.MULTIPURPOSE_STAFFRIFLE.get());
            var item = (MultipurposeStaffrifle) stack.getItem();
            var modifiers = item.getAttributeModifiers(EquipmentSlot.MAINHAND, stack);

            helper.assertTrue(modifiers.get(Attributes.ATTACK_DAMAGE).isEmpty(),
                    "Multipurpose Staffrifle should not add attack damage modifiers");
            helper.assertTrue(modifiers.get(Attributes.ATTACK_SPEED).isEmpty(),
                    "Multipurpose Staffrifle should not add attack speed modifiers");
            assertSingleModifierAmount(
                    helper,
                    modifiers.get(io.redspace.ironsspellbooks.api.registry.AttributeRegistry.SPELL_POWER.get()),
                    AttributeModifier.Operation.MULTIPLY_BASE,
                    0.10D,
                    "Multipurpose Staffrifle spell power modifier changed"
            );

        });
    }
    static void multipurposeStaffrifleTooltipShowsControlsBeforeShiftHint(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var stack = new ItemStack(ItemRegistry.MULTIPURPOSE_STAFFRIFLE.get());
            var tooltipLines = new ArrayList<Component>();
            stack.getItem().appendHoverText(stack, helper.getLevel(), tooltipLines, TooltipFlag.Default.NORMAL);

            helper.assertTrue(tooltipLines.size() >= 4,
                    "Multipurpose Staffrifle tooltip should include controls, spacer, and shift hint");
            assertTranslatableKey(
                    helper,
                    tooltipLines.get(0),
                    "item.apprenticecodex.multipurpose_staffrifle.desc_1",
                    "Multipurpose Staffrifle should show left-click control first"
            );
            assertTranslatableKey(
                    helper,
                    tooltipLines.get(1),
                    "item.apprenticecodex.multipurpose_staffrifle.desc_2",
                    "Multipurpose Staffrifle should show right-click control second"
            );
            helper.assertTrue(tooltipLines.get(2).getString().isEmpty(),
                    "Multipurpose Staffrifle should separate controls from the shift hint with a blank line");
            assertTranslatableKey(
                    helper,
                    tooltipLines.get(3),
                    "item.apprenticecodex.spellgun.tooltip.hint",
                    "Multipurpose Staffrifle should show shift hint after controls"
            );
        });
    }
    static void multipurposeStaffrifleSpecialCooldownPolicyMatchesDefaults(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var item = (MultipurposeStaffrifle) ItemRegistry.MULTIPURPOSE_STAFFRIFLE.get();
            helper.assertTrue(item.resolveSpecialCooldownTicks(20 * 10) == 0,
                    "Multipurpose Staffrifle should remove cooldowns at the default bypass threshold");
            helper.assertTrue(item.resolveSpecialCooldownTicks(20 * 11) == 20 * 10,
                    "Multipurpose Staffrifle should not reduce longer cooldowns below the default minimum");
            helper.assertTrue(item.resolveSpecialCooldownTicks(20 * 60) == 20 * 30,
                    "Multipurpose Staffrifle should subtract the default 30 seconds from long cooldowns");
        });
    }

    static void multipurposeStaffrifleRateLimitIgnoresLegacyPersistentNbt(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "multipurpose_staffrifle_rate_limit_test");
            MultipurposeStaffrifleRateLimiter.clear(player);
            try {
                player.getPersistentData().putLong(LEGACY_NEXT_SPECIAL_CAST_TICK_TAG, Long.MAX_VALUE);

                helper.assertTrue(MultipurposeStaffrifleRateLimiter.canAttemptSpecialCast(player),
                        "Multipurpose Staffrifle should ignore legacy persistent next-cast NBT");
                helper.assertFalse(MultipurposeStaffrifleRateLimiter.canAttemptSpecialCast(player),
                        "Multipurpose Staffrifle should still rate-limit repeated same-tick attempts");

                MultipurposeStaffrifleCastEvent.onPlayerLoggedOut(new PlayerEvent.PlayerLoggedOutEvent(player));
                helper.assertTrue(MultipurposeStaffrifleRateLimiter.canAttemptSpecialCast(player),
                        "Multipurpose Staffrifle rate limit should be cleared on logout");
            } finally {
                MultipurposeStaffrifleRateLimiter.clear(player);
            }
        });
    }

    static void multipurposeStaffrifleUsesDedicatedAmmoAndCasingReturnPolicy(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var stack = new ItemStack(ItemRegistry.MULTIPURPOSE_STAFFRIFLE.get());
            var item = (MultipurposeStaffrifle) stack.getItem();
            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "multipurpose_staffrifle_ammo_policy_test");

            helper.assertTrue(item.getAmmoItem(stack) == ItemRegistry.MULTI_PURPOSE_SPELL_ROUND.get(),
                    "Multipurpose Staffrifle should use Multi-purpose Spell Round");
            helper.assertTrue(ItemRegistry.MULTI_PURPOSE_SPELL_ROUND.get() instanceof SpellcasterRoundItem,
                    "Multi-purpose Spell Round should be a SpellcasterRoundItem");
            var roundItem = (SpellcasterRoundItem) ItemRegistry.MULTI_PURPOSE_SPELL_ROUND.get();
            helper.assertTrue(roundItem.getEmptyCasingItem() == ItemRegistry.EMPTY_MULTI_PURPOSE_SPELL_CASING.get(),
                    "Multi-purpose Spell Round should return Empty Multi-purpose Spell Casing");
            helper.assertTrue(item.resolveEmptyCasingReturnChance(player) == 0.0F,
                    "Multipurpose Staffrifle should not return empty casings without Spellcaster Ammo Pouch");

            equipCurio(player, CuriosSlotConstants.BELT, new ItemStack(ItemRegistry.SPELLCASTER_AMMO_POUCH.get()));
            helper.assertTrue(item.resolveEmptyCasingReturnChance(player) == 0.2F,
                    "Multipurpose Staffrifle should use 20% empty casing return chance with Spellcaster Ammo Pouch");
        });
    }
    static void multipurposeStaffrifleRecastSkipsAmmoConsumption(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var stack = new ItemStack(ItemRegistry.MULTIPURPOSE_STAFFRIFLE.get());
            var item = (MultipurposeStaffrifle) stack.getItem();
            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "multipurpose_staffrifle_recast_ammo_test");
            player.setItemInHand(InteractionHand.MAIN_HAND, stack);
            var ammoStack = new ItemStack(ItemRegistry.MULTI_PURPOSE_SPELL_ROUND.get(), 1);
            player.getInventory().add(ammoStack);

            var spell = io.redspace.ironsspellbooks.api.registry.SpellRegistry.MAGIC_MISSILE_SPELL.get();
            var magicData = MagicData.getPlayerMagicData(player);
            magicData.setPlayerCastingItem(stack);
            try (var ignored = MultipurposeStaffrifleCastContext.open(player.getUUID(), stack, spell, true)) {
                MultipurposeStaffrifleCastEvent.onSpellCast(new SpellOnCastEvent(
                        player,
                        spell.getSpellId(),
                        1,
                        spell.getManaCost(1),
                        spell.getSchoolType(),
                        CastSource.SWORD
                ));
            } catch (Exception exception) {
                throw new IllegalStateException("Failed to close Multipurpose Staffrifle test context.", exception);
            }

            helper.assertTrue(SpellGunCastEvent.countAvailableAmmo(
                    player,
                    player.getInventory(),
                    item.getAmmoItem(stack)
            ) == 1, "Multipurpose Staffrifle recast should not consume Multi-purpose Spell Round");
        });
    }
    static void multipurposeStaffrifleKeepsNormalManaCost(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var stack = new ItemStack(ItemRegistry.MULTIPURPOSE_STAFFRIFLE.get());
            var item = (MultipurposeStaffrifle) stack.getItem();
            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "multipurpose_staffrifle_mana_policy_test");
            player.setItemInHand(InteractionHand.MAIN_HAND, stack);
            player.getInventory().add(new ItemStack(ItemRegistry.MULTI_PURPOSE_SPELL_ROUND.get(), 1));

            helper.assertFalse(stack.getItem() instanceof ManaBypassSpellItem,
                    "Multipurpose Staffrifle should not bypass mana consumption");

            var spell = io.redspace.ironsspellbooks.api.registry.SpellRegistry.MAGIC_MISSILE_SPELL.get();
            var manaCost = spell.getManaCost(1);
            var magicData = MagicData.getPlayerMagicData(player);
            magicData.setPlayerCastingItem(stack);
            magicData.setMana(0.0F);

            try (var ignored = MultipurposeStaffrifleCastContext.open(player.getUUID(), stack, spell, false)) {
                helper.assertFalse(SpellgunCastContext.shouldBypassManaCheck(spell, player),
                        "Multipurpose Staffrifle context must not enable Spellgun mana bypass");
                helper.assertFalse(spell.canBeCastedBy(1, CastSource.SWORD, magicData, player).isSuccess(),
                        "Multipurpose Staffrifle should remain mana-gated when mana is insufficient");
                helper.assertTrue(SpellGunCastEvent.countAvailableAmmo(
                                player,
                                player.getInventory(),
                                item.getAmmoItem(stack)
                        ) == 1,
                        "Rejected Multipurpose Staffrifle cast should not consume ammunition");

                var event = new SpellOnCastEvent(
                        player,
                        spell.getSpellId(),
                        1,
                        manaCost,
                        spell.getSchoolType(),
                        CastSource.SWORD
                );
                ItemManaBypassCastEvent.onSpellCast(event);
                helper.assertTrue(event.getManaCost() == manaCost,
                        "Multipurpose Staffrifle should keep normal mana cost: " + event.getManaCost());

                magicData.setMana(manaCost);
                helper.assertTrue(spell.canBeCastedBy(1, CastSource.SWORD, magicData, player).isSuccess(),
                        "Multipurpose Staffrifle should cast when normal mana requirements are met");
            } catch (Exception exception) {
                throw new IllegalStateException("Failed to close Multipurpose Staffrifle mana policy test context.", exception);
            }
        });
    }
    static void multipurposeStaffrifleInstantCastConsumesAmmoAndAppliesCooldownPolicy(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var stack = new ItemStack(ItemRegistry.MULTIPURPOSE_STAFFRIFLE.get());
            var item = (MultipurposeStaffrifle) stack.getItem();
            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "multipurpose_staffrifle_instant_policy_test");
            player.setItemInHand(InteractionHand.MAIN_HAND, stack);
            player.getInventory().add(new ItemStack(ItemRegistry.MULTI_PURPOSE_SPELL_ROUND.get(), 1));

            var spell = io.redspace.ironsspellbooks.api.registry.SpellRegistry.MAGIC_MISSILE_SPELL.get();
            var magicData = MagicData.getPlayerMagicData(player);
            magicData.setPlayerCastingItem(stack);
            MultipurposeStaffrifleCastContext.rememberPending(
                    player.getUUID(),
                    stack,
                    spell,
                    false,
                    helper.getLevel().getGameTime()
            );

            MultipurposeStaffrifleCastEvent.onSpellCast(new SpellOnCastEvent(
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
                    item.getAmmoItem(stack)
            ) == 0, "Multipurpose Staffrifle instant cast should consume Multi-purpose Spell Round");

            var cooldownEvent = new SpellCooldownAddedEvent.Pre(
                    20 * 10,
                    spell,
                    player,
                    CastSource.SWORD
            );
            MultipurposeStaffrifleCastEvent.onSpellCooldownAdded(cooldownEvent);
            helper.assertTrue(cooldownEvent.getEffectiveCooldown() == 0,
                    "Multipurpose Staffrifle instant cast should bypass cooldowns at the threshold: "
                            + cooldownEvent.getEffectiveCooldown());
        });
    }
}
