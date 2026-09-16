package jp.aquafactory.apprenticecodex.gametest;

import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.CastType;
import io.redspace.ironsspellbooks.capabilities.magic.SyncedSpellData;
import io.redspace.ironsspellbooks.compat.Curios;
import jp.aquafactory.apprenticecodex.capability.Capabilities;
import jp.aquafactory.apprenticecodex.capability.codexspelldata.CodexSpellStateTypeRegister;
import jp.aquafactory.apprenticecodex.config.ApprenticeCodexServerConfig;
import jp.aquafactory.apprenticecodex.damage.DamageTypes;
import jp.aquafactory.apprenticecodex.item.ammo.BowCastAmmoResolver;
import jp.aquafactory.apprenticecodex.item.chargecastcatalystbook.ChargecastCatalystbook;
import jp.aquafactory.apprenticecodex.mixin.MagicDataAccessor;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import jp.aquafactory.apprenticecodex.registry.SpellRegistry;
import jp.aquafactory.apprenticecodex.utility.CombatTools;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.damagesource.DamageContainer;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

final class SpellCastParryingRingGameTestScenarios {
    private SpellCastParryingRingGameTestScenarios() {
    }

    static void spellCastParryingRingBlocksFrontLongCastWithinWindow(GameTestHelper helper) {
        helper.succeedIf(() -> {
            try (var ignored = ApprenticeCodexServerConfig.useSpellCastParryingRingParryWindowTicksOverrideForGameTest(10)) {
                var player = createRingTestPlayer(helper, "spell_cast_parry_front_long");
                equipRing(player);
                startNormalCast(player, CastType.LONG, 20, 15);
                var attacker = createAttacker(helper, player, 0.0D, 0.0D, 3.0D, "spell_cast_parry_front_attacker");

                var event = postAttack(player, helper.getLevel().damageSources().mobAttack(attacker));

                helper.assertTrue(event.isCanceled(), "Spell Cast Parrying Ring should block front LONG cast hits inside the configured window");
            }
        });
    }

    static void spellCastParryingRingWithoutRingDoesNotBlock(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = createRingTestPlayer(helper, "spell_cast_parry_no_ring");
            startNormalCast(player, CastType.LONG, 20, 15);
            var attacker = createAttacker(helper, player, 0.0D, 0.0D, 3.0D, "spell_cast_parry_no_ring_attacker");

            var event = postAttack(player, helper.getLevel().damageSources().mobAttack(attacker));

            helper.assertFalse(event.isCanceled(), "Spell Cast Parrying Ring should not block when the ring is not equipped");
        });
    }

    static void spellCastParryingRingAfterWindowDoesNotBlock(GameTestHelper helper) {
        helper.succeedIf(() -> {
            try (var ignored = ApprenticeCodexServerConfig.useSpellCastParryingRingParryWindowTicksOverrideForGameTest(4)) {
                var player = createRingTestPlayer(helper, "spell_cast_parry_late_long");
                equipRing(player);
                startNormalCast(player, CastType.LONG, 20, 5);
                var attacker = createAttacker(helper, player, 0.0D, 0.0D, 3.0D, "spell_cast_parry_late_attacker");

                var event = postAttack(player, helper.getLevel().damageSources().mobAttack(attacker));

                helper.assertFalse(event.isCanceled(), "Spell Cast Parrying Ring should not block after the configured parry window");
            }
        });
    }

    static void spellCastParryingRingBackAttackDoesNotBlock(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = createRingTestPlayer(helper, "spell_cast_parry_back");
            equipRing(player);
            startNormalCast(player, CastType.LONG, 20, 15);
            var attacker = createAttacker(helper, player, 0.0D, 0.0D, -3.0D, "spell_cast_parry_back_attacker");

            var event = postAttack(player, helper.getLevel().damageSources().mobAttack(attacker));

            helper.assertFalse(event.isCanceled(), "Spell Cast Parrying Ring should not block attacks from behind");
        });
    }

    static void spellCastParryingRingNormalInstantAndContinuousDoNotBlock(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var instantPlayer = createRingTestPlayer(helper, "spell_cast_parry_normal_instant");
            equipRing(instantPlayer);
            startNormalCast(instantPlayer, CastType.INSTANT, 20, 15);
            var instantAttacker = createAttacker(helper, instantPlayer, 0.0D, 0.0D, 3.0D, "spell_cast_parry_instant_attacker");
            var instantEvent = postAttack(instantPlayer, helper.getLevel().damageSources().mobAttack(instantAttacker));

            var continuousPlayer = createRingTestPlayer(helper, "spell_cast_parry_normal_continuous");
            equipRing(continuousPlayer);
            startNormalCast(continuousPlayer, CastType.CONTINUOUS, 20, 15);
            var continuousAttacker = createAttacker(helper, continuousPlayer, 0.0D, 0.0D, 3.0D, "spell_cast_parry_continuous_attacker");
            var continuousEvent = postAttack(continuousPlayer, helper.getLevel().damageSources().mobAttack(continuousAttacker));

            helper.assertFalse(instantEvent.isCanceled(), "Spell Cast Parrying Ring should not block normal INSTANT casts");
            helper.assertFalse(continuousEvent.isCanceled(), "Spell Cast Parrying Ring should not block normal CONTINUOUS casts");
        });
    }

    static void spellCastParryingRingStoppedLongCastDoesNotBlock(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = createRingTestPlayer(helper, "spell_cast_parry_stopped_long");
            equipRing(player);
            startNormalCast(player, CastType.LONG, 20, 15);
            MagicData.getPlayerMagicData(player).resetCastingState();
            var attacker = createAttacker(helper, player, 0.0D, 0.0D, 3.0D, "spell_cast_parry_stopped_attacker");

            var event = postAttack(player, helper.getLevel().damageSources().mobAttack(attacker));

            helper.assertFalse(event.isCanceled(), "Spell Cast Parrying Ring should not block after LONG casting state has stopped");
        });
    }

    static void spellCastParryingRingFocusStaffbowInstantPendingBlocks(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = createRingTestPlayer(helper, "spell_cast_parry_focus_instant");
            equipRing(player);
            startFocusStaffbowPending(player, CastType.INSTANT, 0L);
            var attacker = createAttacker(helper, player, 0.0D, 0.0D, 3.0D, "spell_cast_parry_focus_instant_attacker");

            var event = postAttack(player, helper.getLevel().damageSources().mobAttack(attacker));

            helper.assertTrue(event.isCanceled(), "Spell Cast Parrying Ring should block FocusStaffbow INSTANT pending casts");
        });
    }

    static void spellCastParryingRingFocusStaffbowLongPendingBlocks(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = createRingTestPlayer(helper, "spell_cast_parry_focus_long");
            equipRing(player);
            startFocusStaffbowPending(player, CastType.LONG, 0L);
            var attacker = createAttacker(helper, player, 0.0D, 0.0D, 3.0D, "spell_cast_parry_focus_long_attacker");
            helper.assertTrue(postAttack(player, helper.getLevel().damageSources().mobAttack(attacker)).isCanceled(),
                    "Spell Cast Parrying Ring should block FocusStaffbow LONG pending casts");
        });
    }

    static void spellCastParryingRingChargecastRespectsWindowAndCancellation(GameTestHelper helper) {
        helper.succeedIf(() -> {
            try (var ignored = ApprenticeCodexServerConfig.useSpellCastParryingRingParryWindowTicksOverrideForGameTest(4)) {
                for (var hand : InteractionHand.values()) {
                    var player = createRingTestPlayer(helper, "spell_cast_parry_book_" + hand.name());
                    // Iron's の毒被弾時刻は初期値0なので、生成直後の tick を通常被弾として扱わない。
                    player.tickCount = 10;
                    equipRing(player);
                    var book = ItemRegistry.CHARGECAST_CATALYSTBOOK.get().getDefaultInstance();
                    var spell = io.redspace.ironsspellbooks.api.registry.SpellRegistry.MAGIC_MISSILE_SPELL.get();
                    ChargecastCatalystbook.setCalibrationScroll(book, 0,
                            ApprenticeCodexGameTestScenarios.createSpellScroll(spell));
                    player.setItemInHand(hand, book);
                    var magicData = MagicData.getPlayerMagicData(player);
                    magicData.setMana(1000.0F);
                    book.getItem().use(helper.getLevel(), player, hand);
                    helper.assertTrue(magicData.isCasting() && magicData.getCastType() == CastType.INSTANT,
                            "Book use should start a timed INSTANT cast");
                    helper.assertTrue(magicData.getCastDuration() > 5, "Chargecast should outlast the parry window");
                    var attacker = createAttacker(helper, player, 0.0D, 0.0D, 3.0D, "spell_cast_parry_book_attacker_" + hand.name());
                    var source = helper.getLevel().damageSources().mobAttack(attacker);
                    helper.assertTrue(postAttack(player, source).isCanceled(), "Chargecast should parry at cast start");
                    // 実際の残り時間更新を使い、境界内の被弾では LOWEST の詠唱中断が起きないことも確認する。
                    for (var tick = 0; tick < 4; ++tick) {
                        magicData.handleCastDuration();
                    }
                    helper.assertTrue(postAttack(player, source).isCanceled(), "Chargecast should parry at the window boundary");
                    helper.assertTrue(magicData.isCasting(), "Parried damage should preserve chargecast");
                    magicData.handleCastDuration();
                    helper.assertFalse(postAttack(player, source).isCanceled(), "Chargecast should not parry after the window");
                    helper.assertFalse(magicData.isCasting(), "Unblocked damage should cancel chargecast");
                    helper.assertFalse(postAttack(player, source).isCanceled(), "Canceled chargecast should not parry");
                }
            }
        });
    }

    static void spellCastParryingRingFocusStaffbowContinuousPendingDoesNotBlock(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = createRingTestPlayer(helper, "spell_cast_parry_focus_continuous");
            equipRing(player);
            startFocusStaffbowPending(player, CastType.CONTINUOUS, 0L);
            var attacker = createAttacker(helper, player, 0.0D, 0.0D, 3.0D, "spell_cast_parry_focus_continuous_attacker");

            var event = postAttack(player, helper.getLevel().damageSources().mobAttack(attacker));

            helper.assertFalse(event.isCanceled(), "Spell Cast Parrying Ring should not block FocusStaffbow CONTINUOUS states");
        });
    }

    static void spellCastParryingRingBypassShieldDoesNotBlock(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = createRingTestPlayer(helper, "spell_cast_parry_bypass_shield");
            equipRing(player);
            startNormalCast(player, CastType.LONG, 20, 15);
            var attacker = createAttacker(helper, player, 0.0D, 0.0D, 3.0D, "spell_cast_parry_bypass_attacker");

            var event = postAttack(player, CombatTools.getDamageSource(helper.getLevel(), attacker, DamageTypes.UNITE_LUNA));

            helper.assertFalse(event.isCanceled(), "Spell Cast Parrying Ring should not block DamageTypeTags.BYPASSES_SHIELD damage");
        });
    }

    static void spellCastParryingRingDiscardsBlockedProjectile(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = createRingTestPlayer(helper, "spell_cast_parry_projectile");
            equipRing(player);
            startNormalCast(player, CastType.LONG, 20, 15);
            var arrow = new Arrow(EntityType.ARROW, helper.getLevel());
            arrow.setOwner(player);
            arrow.setPos(player.getX(), player.getY() + 1.0D, player.getZ() + 3.0D);
            helper.getLevel().addFreshEntity(arrow);

            var event = postAttack(player, helper.getLevel().damageSources().arrow(arrow, player));

            helper.assertTrue(event.isCanceled(), "Spell Cast Parrying Ring should block frontal projectile damage");
            helper.assertTrue(arrow.isRemoved(), "Spell Cast Parrying Ring should discard blocked direct projectiles");
        });
    }

    private static FakePlayer createRingTestPlayer(GameTestHelper helper, String profileName) {
        var player = BowGameTestSupport.createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), profileName);
        helper.getLevel().addFreshEntity(player);
        player.setYRot(0.0F);
        player.setXRot(0.0F);
        return player;
    }

    private static void equipRing(FakePlayer player) {
        BowGameTestSupport.equipCurio(player, Curios.RING_SLOT,
                new ItemStack(ItemRegistry.SPELL_CAST_PARRYING_RING.get()));
    }

    private static FakePlayer createAttacker(GameTestHelper helper, LivingEntity defender, double xOffset, double yOffset,
                                             double zOffset, String profileName) {
        var attacker = BowGameTestSupport.createEquipmentTestPlayer(helper.getLevel(), defender.blockPosition(), profileName);
        attacker.setPos(defender.getX() + xOffset, defender.getY() + yOffset, defender.getZ() + zOffset);
        helper.getLevel().addFreshEntity(attacker);
        return attacker;
    }

    private static void startNormalCast(FakePlayer player, CastType castType, int castDuration, int castDurationRemaining) {
        var spell = SpellRegistry.SHOCK.get();
        var magicData = MagicData.getPlayerMagicData(player);
        var syncedData = new SyncedSpellData(player);
        syncedData.setIsCasting(true, spell.getSpellId(), 1, "gametest");
        magicData.setSyncedData(syncedData);
        var accessor = (MagicDataAccessor) magicData;
        accessor.apprenticecodex$setCastingSpellLevel(1);
        accessor.apprenticecodex$setCastDuration(castDuration);
        accessor.apprenticecodex$setCastDurationRemaining(castDurationRemaining);
        accessor.apprenticecodex$setCastSource(CastSource.SWORD);
        accessor.apprenticecodex$setCastType(castType);
    }

    private static void startFocusStaffbowPending(FakePlayer player, CastType castType, long startedGameTimeOffset) {
        var spell = switch (castType) {
            case LONG -> SpellRegistry.SLASH_BLADE.get();
            case CONTINUOUS -> SpellRegistry.FORCE_FIELD.get();
            case INSTANT -> SpellRegistry.MANA_SLASH.get();
            default -> SpellRegistry.SHOCK.get();
        };
        var spellData = Capabilities.getSpellDataOrNull(player);
        if (spellData == null) {
            throw new IllegalStateException("Missing spell data for FocusStaffbow pending parry test");
        }

        spellData.edit(CodexSpellStateTypeRegister.FOCUS_STAFFBOW_CAST_STATE, state -> state.startPending(
                spell,
                1,
                CastSource.SWORD,
                "gametest",
                player.level().getGameTime() - startedGameTimeOffset,
                20,
                20,
                player.level().dimension().location().toString(),
                player.getInventory().selected,
                BowCastAmmoResolver.FocusStaffbowAmmoRoute.ARROW_CATALYST
        ));
    }

    private static LivingIncomingDamageEvent postAttack(LivingEntity target, DamageSource source) {
        var event = new LivingIncomingDamageEvent(target, new DamageContainer(source, 4.0F));
        NeoForge.EVENT_BUS.post(event);
        return event;
    }
}
