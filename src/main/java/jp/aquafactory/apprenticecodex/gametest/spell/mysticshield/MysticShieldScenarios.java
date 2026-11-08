package jp.aquafactory.apprenticecodex.gametest.spell.mysticshield;

import io.redspace.ironsspellbooks.api.events.SpellOnCastEvent;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.magic.SpellSelectionManager;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import jp.aquafactory.apprenticecodex.damage.DamageTypes;
import jp.aquafactory.apprenticecodex.item.curios.CuriosSlotConstants;
import jp.aquafactory.apprenticecodex.item.curios.protectionspellsupporter.ProtectionSpellSupporterManaCostDiscountEvent;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import jp.aquafactory.apprenticecodex.registry.SpellRegistry;
import jp.aquafactory.apprenticecodex.spell.mysticshield.MysticShield;
import jp.aquafactory.apprenticecodex.spell.mysticshield.MysticShieldProjectileEntity;
import jp.aquafactory.apprenticecodex.utility.CombatTools;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.common.util.FakePlayer;

import static jp.aquafactory.apprenticecodex.gametest.support.CurioTestSupport.equipCurio;
import static jp.aquafactory.apprenticecodex.gametest.support.CurioTestSupport.postLivingAttackEventForGameTest;
import static jp.aquafactory.apprenticecodex.gametest.support.PlayerTestSupport.createEquipmentTestPlayer;
import static jp.aquafactory.apprenticecodex.gametest.support.PlayerTestSupport.createTrackedEquipmentTestPlayer;

public final class MysticShieldScenarios {
    private MysticShieldScenarios() {}

    static void mysticShieldBlocksFrontDamageAndLimitsSameSourceAccumulation(GameTestHelper helper) {
        helper.runAtTickTime(1, () -> {
            var level = helper.getLevel();
            var player = createTrackedEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "mystic_shield_front_test");
            player.setYRot(0.0f);
            player.setXRot(0.0f);
            var attacker = helper.spawn(EntityType.ZOMBIE, new BlockPos(0, 2, 3));
            var source = CombatTools.getDamageSource(level, attacker, DamageTypes.SHOCK);
            var spell = beginMysticShieldCast(level, player, 1);

            var firstAttack = postLivingAttackEventForGameTest(player, source, 8.0f);
            var duplicateAttack = postLivingAttackEventForGameTest(player, source, 8.0f);
            helper.assertTrue(firstAttack.isCanceled(), "Mystic Shield should block a front attack");
            helper.assertTrue(duplicateAttack.isCanceled(), "Mystic Shield should still block a repeated front attack");

            spell.onCast(level, 1, player, CastSource.SPELLBOOK, MagicData.getPlayerMagicData(player));
            completeMysticShieldCast(level, player, 1, false);
            var projectiles = level.getEntitiesOfClass(MysticShieldProjectileEntity.class, player.getBoundingBox().inflate(4.0));
            helper.assertTrue(projectiles.size() == 1,
                    "Mystic Shield should release exactly one stored projectile but got " + projectiles.size());
            var expectedDamage = 8.0f * spell.getReflectDamageMultiplier(1, player);
            helper.assertTrue(Math.abs(projectiles.get(0).getDamageForGameTest() - expectedDamage) < 1.0e-4f,
                    "Mystic Shield should ignore duplicate same-source accumulation: expected="
                            + expectedDamage + ", actual=" + projectiles.get(0).getDamageForGameTest());
            helper.succeed();
        });
    }

    static void mysticShieldReflectsStoredDamageAfterNonFrontCancel(GameTestHelper helper) {
        helper.runAtTickTime(1, () -> {
            var level = helper.getLevel();
            var player = createTrackedEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "mystic_shield_side_test");
            player.setYRot(0.0f);
            player.setXRot(0.0f);
            var frontAttacker = helper.spawn(EntityType.ZOMBIE, new BlockPos(0, 2, 3));
            var sideAttacker = helper.spawn(EntityType.ZOMBIE, new BlockPos(3, 2, 0));
            beginMysticShieldCast(level, player, 1);

            var frontAttack = postLivingAttackEventForGameTest(
                    player,
                    CombatTools.getDamageSource(level, frontAttacker, DamageTypes.SHOCK),
                    6.0f
            );
            helper.assertTrue(frontAttack.isCanceled(), "Mystic Shield should store front damage before a later cancel");

            var sideAttack = postLivingAttackEventForGameTest(
                    player,
                    CombatTools.getDamageSource(level, sideAttacker, DamageTypes.SHOCK),
                    4.0f
            );
            helper.assertFalse(sideAttack.isCanceled(), "Mystic Shield should not block a side attack");

            completeMysticShieldCast(level, player, 1, true);
            var projectiles = level.getEntitiesOfClass(MysticShieldProjectileEntity.class, player.getBoundingBox().inflate(4.0));
            helper.assertTrue(projectiles.size() == 1,
                    "Mystic Shield should release stored reflection even after a non-front cancel but got " + projectiles.size());
            helper.succeed();
        });
    }

    static void mysticShieldUsesYawWhenLookPitchIsVertical(GameTestHelper helper) {
        helper.runAtTickTime(1, () -> {
            var level = helper.getLevel();
            var player = createTrackedEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "mystic_shield_vertical_pitch_test");
            player.setYRot(0.0f);
            player.setYBodyRot(0.0f);
            player.setYHeadRot(0.0f);
            player.setXRot(-90.0f);
            var frontAttacker = helper.spawn(EntityType.ZOMBIE, new BlockPos(0, 2, 3));
            var sideAttacker = helper.spawn(EntityType.ZOMBIE, new BlockPos(3, 2, 0));
            beginMysticShieldCast(level, player, 1);

            var frontAttack = postLivingAttackEventForGameTest(
                    player,
                    CombatTools.getDamageSource(level, frontAttacker, DamageTypes.SHOCK),
                    6.0f
            );
            var sideAttack = postLivingAttackEventForGameTest(
                    player,
                    CombatTools.getDamageSource(level, sideAttacker, DamageTypes.SHOCK),
                    6.0f
            );

            helper.assertTrue(frontAttack.isCanceled(), "Mystic Shield should use yaw to block front attacks at vertical pitch");
            helper.assertFalse(sideAttack.isCanceled(), "Mystic Shield should still reject side attacks at vertical pitch");
            helper.succeed();
        });
    }

    static void mysticShieldReceivesProtectionSpellSupporterBenefits(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "mystic_shield_supporter_test");
            var spell = (MysticShield) SpellRegistry.MYSTIC_SHIELD.get();
            var baseMultiplier = spell.getReflectDamageMultiplier(1, player);
            equipCurio(player, CuriosSlotConstants.BELT, new ItemStack(ItemRegistry.PROTECTION_SPELL_SUPPORTER.get()));
            helper.assertTrue(Math.abs(spell.getReflectDamageMultiplier(1, player) - baseMultiplier * 2.0f) < 1.0e-6f,
                    "Protection Spell Supporter should double Mystic Shield reflection damage");

            var manaEvent = new SpellOnCastEvent(
                    player,
                    spell.getSpellId(),
                    1,
                    spell.getManaCost(1),
                    spell.getSchoolType(),
                    CastSource.SPELLBOOK
            );
            ProtectionSpellSupporterManaCostDiscountEvent.onSpellCast(manaEvent);
            var expectedManaCost = Math.max(1, Math.round(spell.getManaCost(1) * 0.5f));
            helper.assertTrue(manaEvent.getManaCost() == expectedManaCost,
                    "Protection Spell Supporter should halve Mystic Shield mana cost to "
                            + expectedManaCost + " but got " + manaEvent.getManaCost());
        });
    }

    public static MysticShield beginMysticShieldCast(ServerLevel level, FakePlayer player, int spellLevel) {
        var spell = (MysticShield) SpellRegistry.MYSTIC_SHIELD.get();
        var magicData = MagicData.getPlayerMagicData(player);
        magicData.setMana(1000.0f);
        magicData.getPlayerCooldowns().removeCooldown(spell.getSpellId());
        magicData.getSyncedData();
        magicData.initiateCast(
                spell,
                spellLevel,
                spell.getEffectiveCastTime(spellLevel, player),
                CastSource.SPELLBOOK,
                SpellSelectionManager.MAINHAND
        );
        magicData.setPlayerCastingItem(new ItemStack(Items.STICK));
        spell.onCast(level, spellLevel, player, CastSource.SPELLBOOK, magicData);
        return spell;
    }

    public static void completeMysticShieldCast(ServerLevel level, FakePlayer player, int spellLevel, boolean cancelled) {
        var spell = (MysticShield) SpellRegistry.MYSTIC_SHIELD.get();
        var magicData = MagicData.getPlayerMagicData(player);
        spell.onServerCastComplete(level, spellLevel, player, magicData, cancelled);
    }
}
