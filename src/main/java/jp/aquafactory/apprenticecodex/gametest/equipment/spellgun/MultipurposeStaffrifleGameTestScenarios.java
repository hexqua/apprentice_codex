package jp.aquafactory.apprenticecodex.gametest.equipment.spellgun;

import io.redspace.ironsspellbooks.api.events.SpellCooldownAddedEvent;
import io.redspace.ironsspellbooks.api.events.SpellOnCastEvent;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.SpellData;
import io.redspace.ironsspellbooks.capabilities.magic.SyncedSpellData;
import jp.aquafactory.apprenticecodex.item.ItemManaBypassCastEvent;
import jp.aquafactory.apprenticecodex.item.ManaBypassSpellItem;
import jp.aquafactory.apprenticecodex.item.multipurposestaffrifle.*;
import jp.aquafactory.apprenticecodex.item.spellgun.SpellGunCastEvent;
import jp.aquafactory.apprenticecodex.item.spellgun.SpellgunCastContext;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import jp.aquafactory.apprenticecodex.utility.SpellCalibrationImbueHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.player.PlayerEvent;

import static jp.aquafactory.apprenticecodex.gametest.support.PlayerTestSupport.createEquipmentTestPlayer;

final class MultipurposeStaffrifleGameTestScenarios {
    private MultipurposeStaffrifleGameTestScenarios() {}

    static final String LEGACY_NEXT_SPECIAL_CAST_TICK_TAG =
            "ApprenticeCodexMultipurposeStaffrifleNextSpecialCastTick";





    static void multipurposeStaffriflePreservesNormalCooldown(GameTestHelper helper) {
        var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "multipurpose_cooldown");
        var stack = new ItemStack(ItemRegistry.MULTIPURPOSE_STAFFRIFLE.get());
        MagicData.getPlayerMagicData(player).setPlayerCastingItem(stack);
        var spell = SpellRegistry.MAGIC_MISSILE_SPELL.get();
        for (var ticks : new int[]{0, 20, 200, 220, 1200}) {
            var event = new SpellCooldownAddedEvent.Pre(ticks, spell, player, CastSource.SWORD);
            try (var ignored = MultipurposeStaffrifleCastContext.open(player.getUUID(), stack, spell, false)) {
                MultipurposeStaffrifleCastEvent.onSpellCooldownAdded(event);
            } catch (Exception exception) {
                throw new IllegalStateException("Failed to close cooldown test context", exception);
            }
            helper.assertTrue(event.getEffectiveCooldown() == ticks, "INSTANT cooldown must remain unchanged");
        }
        helper.succeed();
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

    static void multipurposeStaffrifleRecastSkipsAmmoConsumption(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var stack = new ItemStack(ItemRegistry.MULTIPURPOSE_STAFFRIFLE.get());
            var item = (MultipurposeStaffrifle) stack.getItem();
            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "multipurpose_staffrifle_recast_ammo_test");
            player.setItemInHand(InteractionHand.MAIN_HAND, stack);
            var ammoStack = new ItemStack(ItemRegistry.MULTI_PURPOSE_SPELL_ROUND.get(), 1);
            player.getInventory().add(ammoStack);

            var spell = SpellRegistry.MAGIC_MISSILE_SPELL.get();
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

            var spell = SpellRegistry.MAGIC_MISSILE_SPELL.get();
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

    static void multipurposeStaffrifleCastsAtZeroMana(GameTestHelper helper) {
        var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "multipurpose_mana");
        var stack = new ItemStack(ItemRegistry.MULTIPURPOSE_STAFFRIFLE.get());
        var rifle = (MultipurposeStaffrifle) stack.getItem();
        var lookup = helper.getLevel().registryAccess();
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        var magic = MagicData.getPlayerMagicData(player);
        magic.setSyncedData(new SyncedSpellData(player));
        magic.setMana(0);
        var spell = SpellRegistry.MAGIC_MISSILE_SPELL.get();
        MultipurposeStaffrifleScrollStorage.set(stack, 0,
                SpellCalibrationImbueHelper.createScroll(new SpellData(spell, 1)), lookup);
        helper.assertFalse(rifle.tryTriggerSelectedSpell(player, false), "Missing ammo must reject the cast");
        player.getInventory().add(new ItemStack(ItemRegistry.MULTI_PURPOSE_SPELL_ROUND.get(), 2));
        helper.assertTrue(rifle.tryTriggerSelectedSpell(player, false), "INSTANT must initiate at zero mana");
        // FakePlayerは通常のplayer tickを受けないため、INSTANTの完了だけ明示的に進める。
        spell.castSpell(helper.getLevel(), magic.getCastingSpellLevel(), player, CastSource.SWORD, true);
        helper.assertTrue(magic.getMana() == 0, "Successful cast must not consume or add mana");
        helper.assertTrue(SpellGunCastEvent.countAvailableAmmo(player, player.getInventory(), rifle.getAmmoItem(stack)) == 1,
                "Successful cast must consume exactly one round");
        helper.assertFalse(SpellgunCastContext.shouldBypassManaCheck(spell, player), "Bypass must not escape the cast scope");
        helper.assertTrue(magic.getPlayerCooldowns().isOnCooldown(spell), "Normal cooldown must be applied");
        MultipurposeStaffrifleRateLimiter.clear(player);
        helper.assertFalse(rifle.tryTriggerSelectedSpell(player, true), "ADS must not bypass spell cooldown");
        helper.assertTrue(SpellGunCastEvent.countAvailableAmmo(player, player.getInventory(), rifle.getAmmoItem(stack)) == 1,
                "Rejected cast must preserve ammunition");
        helper.succeed();
    }

    static void multipurposeStaffrifleInstantCastConsumesAmmoAndAppliesCooldownPolicy(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var stack = new ItemStack(ItemRegistry.MULTIPURPOSE_STAFFRIFLE.get());
            var item = (MultipurposeStaffrifle) stack.getItem();
            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "multipurpose_staffrifle_instant_policy_test");
            player.setItemInHand(InteractionHand.MAIN_HAND, stack);
            player.getInventory().add(new ItemStack(ItemRegistry.MULTI_PURPOSE_SPELL_ROUND.get(), 1));

            var spell = SpellRegistry.MAGIC_MISSILE_SPELL.get();
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
            helper.assertTrue(cooldownEvent.getEffectiveCooldown() == 200,
                    "Multipurpose Staffrifle instant cast must preserve normal cooldown: "
                            + cooldownEvent.getEffectiveCooldown());
        });
    }
}
