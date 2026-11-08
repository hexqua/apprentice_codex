package jp.aquafactory.apprenticecodex.gametest.spell.targeting.multipart;

import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import io.redspace.ironsspellbooks.api.util.Utils;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.utility.CombatTools;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.Objects;
import java.util.UUID;

import static jp.aquafactory.apprenticecodex.gametest.spell.targeting.multipart.DragonTargetFixture.*;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class DragonDamageGameTests {
    private DragonDamageGameTests() {
    }

    @GameTest(template = TEMPLATE)
    public static void spellDamageUsesHeadDamageForEveryPartAndParent(GameTestHelper helper) {
        if (skipDragonTest(helper)) return;
        try (var scene = new DragonTargetFixture(helper)) {
            assertDragonDamage(helper, scene, true);
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void unscaledDamageUsesHeadDamageForEveryPartAndParent(GameTestHelper helper) {
        if (skipDragonTest(helper)) return;
        try (var scene = new DragonTargetFixture(helper)) {
            assertDragonDamage(helper, scene, false);
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void partDamageUsesParentResistanceAndProtection(GameTestHelper helper) {
        if (skipDragonTest(helper)) return;
        try (var scene = new DragonTargetFixture(helper)) {
            var dragon = scene.dragon();
            Objects.requireNonNull(dragon.getAttribute(AttributeRegistry.SPELL_RESIST)).setBaseValue(2);
            var source = helper.getLevel().damageSources().playerAttack(scene.owner);
            CombatTools.applyDamage(dragon.getSubEntities()[2], 20, source, null, CombatTools.KnockbackTypes.DEFAULT);
            assertDamage(helper, dragon, 20 * (2 - (float) Utils.softCapFormula(2)));

            var unscaled = scene.dragon();
            Objects.requireNonNull(unscaled.getAttribute(AttributeRegistry.SPELL_RESIST)).setBaseValue(2);
            CombatTools.applyUnscaledDamage(unscaled, 20, source, CombatTools.KnockbackTypes.DEFAULT);
            assertDamage(helper, unscaled, 20);

            var ally = scene.dragon();
            var board = helper.getLevel().getScoreboard();
            var team = board.addPlayerTeam("part_" + UUID.randomUUID().toString().substring(0, 8));
            team.setAllowFriendlyFire(false);
            try {
                board.addPlayerToTeam(scene.owner.getScoreboardName(), team);
                board.addPlayerToTeam(ally.getScoreboardName(), team);
                helper.assertFalse(CombatTools.applyDamage(ally.getSubEntities()[6], 20, source, null,
                        CombatTools.KnockbackTypes.DEFAULT), "Spell damage must protect the parent team");
                helper.assertFalse(CombatTools.applyUnscaledDamage(ally, 20, source,
                        CombatTools.KnockbackTypes.DEFAULT), "Unscaled damage must protect the parent team");
                assertDamage(helper, ally, 0);
            } finally {
                board.removePlayerTeam(team);
            }
            var selfSource = helper.getLevel().damageSources().mobAttack(ally);
            helper.assertFalse(CombatTools.applyDamage(ally.head, 20, selfSource, null,
                    CombatTools.KnockbackTypes.DEFAULT), "A parent must be protected from its own part hit");
            helper.assertFalse(CombatTools.applyUnscaledDamage(ally.head, 20, selfSource,
                    CombatTools.KnockbackTypes.DEFAULT), "Unscaled self damage must resolve the parent");
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void ordinaryLivingTargetsAndCrystalsStillTakeDamage(GameTestHelper helper) {
        try (var scene = new DragonTargetFixture(helper)) {
            var source = helper.getLevel().damageSources().playerAttack(scene.owner);
            for (boolean spell : new boolean[]{true, false}) {
                var target = Objects.requireNonNull(EntityType.HUSK.create(helper.getLevel()));
                target.setPos(scene.origin.add(3, 0, 3));
                target.setNoAi(true);
                Objects.requireNonNull(target.getAttribute(Attributes.ARMOR)).setBaseValue(0);
                scene.add(target);
                float before = target.getHealth();
                boolean applied = spell
                        ? CombatTools.applyDamage(target, 4, source, null, CombatTools.KnockbackTypes.NO_KNOCKBACK)
                        : CombatTools.applyUnscaledDamage(target, 4, source, CombatTools.KnockbackTypes.NO_KNOCKBACK);
                helper.assertTrue(applied && Math.abs(before - target.getHealth() - 4) < 0.01F,
                        "Ordinary living targets must retain damage behavior");
                var crystal = Objects.requireNonNull(EntityType.END_CRYSTAL.create(helper.getLevel()));
                crystal.setPos(scene.origin.add(0, 30, 0));
                scene.add(crystal);
                applied = spell
                        ? CombatTools.applyDamage(crystal, 4, source, null, CombatTools.KnockbackTypes.DEFAULT)
                        : CombatTools.applyUnscaledDamage(crystal, 4, source, CombatTools.KnockbackTypes.DEFAULT);
                helper.assertTrue(applied && crystal.isRemoved(), "End crystals must remain damageable");
            }
        }
        helper.succeed();
    }
}
