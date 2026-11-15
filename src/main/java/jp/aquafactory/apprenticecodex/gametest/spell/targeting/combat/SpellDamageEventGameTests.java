package jp.aquafactory.apprenticecodex.gametest.spell.targeting.combat;

import io.redspace.ironsspellbooks.api.events.SpellDamageEvent;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.damage.DamageSources;
import io.redspace.ironsspellbooks.damage.SpellDamageSource;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.damage.DamageTypes;
import jp.aquafactory.apprenticecodex.gametest.spell.targeting.support.TargetingTestFixture;
import jp.aquafactory.apprenticecodex.registry.EntityRegistry;
import jp.aquafactory.apprenticecodex.registry.SpellRegistry;
import jp.aquafactory.apprenticecodex.spell.manaslash.ManaSlashProjectileEntity;
import jp.aquafactory.apprenticecodex.utility.CombatTools;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.function.Consumer;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class SpellDamageEventGameTests {
    private static final String TEMPLATE = "gametest/combat_queries";

    private SpellDamageEventGameTests() {}

    @GameTest(template = TEMPLATE)
    public static void spellEventChangesDamageBeforeResistanceAndPreservesSource(GameTestHelper h) {
        try (var scope = TargetingTestFixture.of(h)) {
            var caster = target(h, scope, 5);
            var victim = target(h, scope, 8);
            var spell = SpellRegistry.CATCH_FLAME.get();
            var original = CombatTools.getDamageSource(h.getLevel(), caster, DamageTypes.CATCH_FLAME_PENETRATE);
            victim.getAttribute(AttributeRegistry.SPELL_RESIST).setBaseValue(2);
            var observed = new ArrayList<SpellDamageEvent>();
            var incoming = new ArrayList<DamageSource>();
            Consumer<SpellDamageEvent> listener = event -> {
                if (event.getEntity() == victim) {
                    observed.add(event);
                    event.setAmount(6);
                    event.getSpellDamageSource().indirect();
                }
            };
            Consumer<LivingIncomingDamageEvent> damageListener = event -> {
                if (event.getEntity() == victim) incoming.add(event.getSource());
            };
            NeoForge.EVENT_BUS.addListener(listener);
            NeoForge.EVENT_BUS.addListener(damageListener);
            try {
                h.assertTrue(CombatTools.applySpellDamage(victim, 10, original, spell, CombatTools.KnockbackTypes.NO_KNOCKBACK),
                        "Spell damage must be accepted");
                h.assertTrue(observed.size() == 1 && observed.getFirst().getOriginalAmount() == 10,
                        "Exactly one event must expose damage before resistance");
                var source = observed.getFirst().getSpellDamageSource();
                h.assertTrue(source.spell() == spell && source.getEntity() == caster && source.getDirectEntity() == caster,
                        "Spell and attacker identity must be preserved");
                h.assertTrue(!source.isDirect() && original.isDirect(), "Event source modifiers must not mutate the original source");
                h.assertTrue(source.typeHolder() == original.typeHolder() && source.type() == original.type()
                                && source.is(DamageTypes.CATCH_FLAME_PENETRATE)
                                && source.is(DamageTypeTags.BYPASSES_ARMOR) == original.is(DamageTypeTags.BYPASSES_ARMOR)
                                && source.getLocalizedDeathMessage(victim).equals(original.getLocalizedDeathMessage(victim)),
                        "Custom damage type, tags and death message must be preserved");
                h.assertTrue(incoming.size() == 1 && incoming.getFirst() == source,
                        "The actual hit must use the same spell source as the event");
                var expected = 6 * DamageSources.getResist(victim, spell.getSchoolType());
                h.assertTrue(Math.abs(100 - victim.getHealth() - expected) < 0.001,
                        "Modified event amount must pass through resistance exactly once");
            } finally {
                NeoForge.EVENT_BUS.unregister(listener);
                NeoForge.EVENT_BUS.unregister(damageListener);
            }
        }
        h.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void cancellationAndItemDamageStaySeparate(GameTestHelper h) {
        try (var scope = TargetingTestFixture.of(h)) {
            var caster = target(h, scope, 5);
            var victim = target(h, scope, 8);
            var source = CombatTools.getDamageSource(h.getLevel(), caster, DamageTypes.MANA_FORCE_BLADE);
            var events = new ArrayList<SpellDamageEvent>();
            Consumer<SpellDamageEvent> listener = event -> {
                if (event.getEntity() == victim || event.getEntity() == caster) {
                    events.add(event);
                    event.setCanceled(true);
                }
            };
            NeoForge.EVENT_BUS.addListener(listener);
            try {
                h.assertFalse(CombatTools.applySpellDamage(victim, 8, source, SpellRegistry.SHOCK.get(), CombatTools.KnockbackTypes.NO_KNOCKBACK),
                        "Canceled spell damage must fail");
                h.assertTrue(victim.getHealth() == 100 && events.size() == 1, "Canceled spell must leave health unchanged");
                h.assertTrue(CombatTools.applyDamage(victim, 8, source, SpellRegistry.SHOCK.get().getSchoolType(), CombatTools.KnockbackTypes.NO_KNOCKBACK),
                        "Item damage must remain independent of spell cancellation");
                h.assertTrue(victim.getHealth() < 100 && events.size() == 1, "Item damage with a school must not emit a spell event");
                h.assertFalse(victim.getLastDamageSource() instanceof SpellDamageSource, "Item hit must retain its ordinary damage source");
                CombatTools.applySpellDamage(caster, 8, source, SpellRegistry.SHOCK.get(), CombatTools.KnockbackTypes.DEFAULT);
                h.assertTrue(caster.getHealth() == 100 && events.size() == 1, "Protected self damage must be rejected");
                for (float amount : new float[]{0, -1, Float.NaN, Float.POSITIVE_INFINITY}) {
                    h.assertFalse(CombatTools.applySpellDamage(victim, amount, source, SpellRegistry.SHOCK.get(), CombatTools.KnockbackTypes.DEFAULT),
                            "Invalid initial spell damage must be rejected");
                }
                h.assertTrue(events.size() == 1, "Rejected damage must not emit a spell event");
            } finally {
                NeoForge.EVENT_BUS.unregister(listener);
            }
            victim.setDeltaMovement(Vec3.ZERO);
            victim.knockback(0.5, 1, 0);
            h.assertTrue(victim.getDeltaMovement().horizontalDistanceSqr() > 0, "Canceled damage must not leak knockback suppression");
        }
        h.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void directCastAndDelayedProjectileExposeTheirSpells(GameTestHelper h) {
        try (var scope = TargetingTestFixture.of(h)) {
            var caster = target(h, scope, 5);
            var victim = target(h, scope, 8);
            caster.setYRot(-90);
            caster.setYHeadRot(-90);
            var events = new ArrayList<SpellDamageEvent>();
            Consumer<SpellDamageEvent> listener = event -> {
                if (event.getEntity() == victim) events.add(event);
            };
            NeoForge.EVENT_BUS.addListener(listener);
            try {
                scope.run(() -> SpellRegistry.SHOCK.get().onCast(h.getLevel(), 1, caster, CastSource.SPELLBOOK, MagicData.getPlayerMagicData(caster)));
                h.assertTrue(events.size() == 1 && events.getFirst().getSpellDamageSource().spell() == SpellRegistry.SHOCK.get(),
                        "An actual direct cast must emit its spell event");
                victim.invulnerableTime = 0;
                var projectile = scope.track(new ManaSlashProjectileEntity(EntityRegistry.MANA_SLASH_PROJECTILE.get(), h.getLevel(), caster));
                projectile.setPos(victim.position().add(0, 0.5, 0));
                projectile.setDamage(4);
                scope.add(projectile);
                scope.step(projectile, false);
                h.assertTrue(events.size() == 2 && events.getLast().getSpellDamageSource().spell() == SpellRegistry.MANA_SLASH.get()
                                && events.getLast().getSpellDamageSource().getDirectEntity() == projectile
                                && events.getLast().getSpellDamageSource().getEntity() == caster,
                        "A delayed projectile must retain spell, projectile and caster identity");
            } finally {
                NeoForge.EVENT_BUS.unregister(listener);
            }
        }
        h.succeed();
    }

    private static Zombie target(GameTestHelper h, TargetingTestFixture scope, int x) {
        var target = scope.track(h.spawn(EntityType.ZOMBIE, new BlockPos(x, 4, 8)));
        target.setNoAi(true);
        target.setNoGravity(true);
        target.getAttribute(Attributes.MAX_HEALTH).setBaseValue(100);
        target.getAttribute(Attributes.ARMOR).setBaseValue(0);
        target.setHealth(100);
        scope.assertVisible(target);
        return target;
    }
}
