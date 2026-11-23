package jp.aquafactory.apprenticecodex.gametest.spell.targeting.combat;

import io.redspace.ironsspellbooks.api.events.SpellDamageEvent;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.damage.DamageSources;
import io.redspace.ironsspellbooks.damage.SpellDamageSource;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.damage.DamageTypes;
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
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.function.Consumer;

import static jp.aquafactory.apprenticecodex.gametest.spell.SpellTestBatches.TEMPLATE;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class SpellDamageEventGameTests {
    private SpellDamageEventGameTests() {}

    @GameTest(template = TEMPLATE)
    public static void spellEventChangesDamageBeforeResistanceAndPreservesSource(GameTestHelper h) {
        var caster = target(h, 0);
        var victim = target(h, 2);
        try {
            var spell = SpellRegistry.CATCH_FLAME.get();
            var damageType = CombatTools.getDamageSource(h.getLevel(), caster, DamageTypes.CATCH_FLAME_PENETRATE).typeHolder();
            var original = new DamageSource(damageType, caster, caster, caster.position().add(0, 1, 0));
            victim.getAttribute(AttributeRegistry.SPELL_RESIST.get()).setBaseValue(2);
            var observed = new ArrayList<SpellDamageEvent>();
            var incoming = new ArrayList<DamageSource>();
            Consumer<SpellDamageEvent> listener = event -> {
                if (event.getEntity() == victim) {
                    observed.add(event);
                    event.setAmount(6);
                    event.getSpellDamageSource().indirect();
                }
            };
            Consumer<LivingAttackEvent> damageListener = event -> {
                if (event.getEntity() == victim) incoming.add(event.getSource());
            };
            MinecraftForge.EVENT_BUS.addListener(listener);
            MinecraftForge.EVENT_BUS.addListener(damageListener);
            try {
                h.assertTrue(CombatTools.applySpellDamage(victim, 10, original, spell, CombatTools.KnockbackTypes.NO_KNOCKBACK),
                        "Spell damage must be accepted");
                h.assertTrue(observed.size() == 1 && observed.get(0).getOriginalAmount() == 10,
                        "Exactly one event must expose damage before resistance");
                var source = observed.get(0).getSpellDamageSource();
                h.assertTrue(source.spell() == spell && source.getEntity() == caster && source.getDirectEntity() == caster,
                        "Spell and attacker identity must be preserved");
                h.assertTrue(source.isIndirect() && !original.isIndirect(), "Event source modifiers must not mutate the original source");
                h.assertTrue(source.typeHolder() == original.typeHolder() && source.type() == original.type()
                                && source.is(DamageTypes.CATCH_FLAME_PENETRATE)
                                && source.is(DamageTypeTags.BYPASSES_ARMOR) == original.is(DamageTypeTags.BYPASSES_ARMOR)
                                && original.getSourcePosition().equals(source.getSourcePosition())
                                && original.sourcePositionRaw().equals(source.sourcePositionRaw())
                                && source.getLocalizedDeathMessage(victim).equals(original.getLocalizedDeathMessage(victim)),
                        "Custom damage type, tags and death message must be preserved");
                h.assertTrue(incoming.size() == 1 && incoming.get(0) == source,
                        "The actual hit must use the same spell source as the event");
                var expected = 6 * DamageSources.getResist(victim, spell.getSchoolType());
                h.assertTrue(Math.abs(100 - victim.getHealth() - expected) < 0.001,
                        "Modified event amount must pass through resistance exactly once");
            } finally {
                MinecraftForge.EVENT_BUS.unregister(listener);
                MinecraftForge.EVENT_BUS.unregister(damageListener);
            }
        } finally {
            victim.discard();
            caster.discard();
        }
        h.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void cancellationAndItemDamageStaySeparate(GameTestHelper h) {
        var caster = target(h, 0);
        var victim = target(h, 2);
        try {
            var source = CombatTools.getDamageSource(h.getLevel(), caster, DamageTypes.MANA_FORCE_BLADE);
            var events = new ArrayList<SpellDamageEvent>();
            Consumer<SpellDamageEvent> listener = event -> {
                if (event.getEntity() == victim || event.getEntity() == caster) {
                    events.add(event);
                    if (event.getAmount() == 4) {
                        event.setAmount(Float.NaN);
                    } else {
                        event.setCanceled(true);
                    }
                }
            };
            MinecraftForge.EVENT_BUS.addListener(listener);
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
                h.assertFalse(CombatTools.applySpellDamage(victim, 4, source, SpellRegistry.SHOCK.get(), CombatTools.KnockbackTypes.DEFAULT),
                        "Invalid modified spell damage must be rejected");
                h.assertTrue(events.size() == 2, "The event must allow listeners to modify valid initial damage");
            } finally {
                MinecraftForge.EVENT_BUS.unregister(listener);
            }
            victim.setDeltaMovement(Vec3.ZERO);
            victim.knockback(0.5, 1, 0);
            h.assertTrue(victim.getDeltaMovement().horizontalDistanceSqr() > 0, "Canceled damage must not leak knockback suppression");
        } finally {
            victim.discard();
            caster.discard();
        }
        h.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void nonLivingSpellDamageDoesNotEmitSpellEvent(GameTestHelper h) {
        var boat = h.spawn(EntityType.BOAT, new BlockPos(2, 2, 1));
        var source = h.getLevel().damageSources().magic();
        int[] eventCount = {0};
        Consumer<SpellDamageEvent> listener = event -> eventCount[0]++;
        MinecraftForge.EVENT_BUS.addListener(listener);
        try {
            h.assertTrue(CombatTools.applySpellDamage(boat, 2, source, SpellRegistry.SHOCK.get(), CombatTools.KnockbackTypes.DEFAULT),
                    "Spell damage should still hit a non-living target with no attacker");
            h.assertTrue(boat.getDamage() > 0 && eventCount[0] == 0,
                    "The non-living target must take damage without a spell event");
        } finally {
            MinecraftForge.EVENT_BUS.unregister(listener);
            boat.discard();
        }
        h.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void directCastAndDelayedProjectileExposeTheirSpells(GameTestHelper h) {
        var caster = target(h, 0);
        var victim = target(h, 2);
        try {
            caster.setYRot(-90);
            caster.setYHeadRot(-90);
            var events = new ArrayList<SpellDamageEvent>();
            Consumer<SpellDamageEvent> listener = event -> {
                if (event.getEntity() == victim) events.add(event);
            };
            MinecraftForge.EVENT_BUS.addListener(listener);
            try {
                SpellRegistry.SHOCK.get().onCast(h.getLevel(), 1, caster, CastSource.SPELLBOOK, MagicData.getPlayerMagicData(caster));
                h.assertTrue(events.size() == 1 && events.get(0).getSpellDamageSource().spell() == SpellRegistry.SHOCK.get(),
                        "An actual direct cast must emit its spell event");
                victim.invulnerableTime = 0;
                var projectile = new ManaSlashProjectileEntity(EntityRegistry.MANA_SLASH_PROJECTILE.get(), h.getLevel(), caster);
                projectile.setPos(victim.position().add(0, 0.5, 0));
                projectile.setDamage(4);
                h.getLevel().addFreshEntity(projectile);
                try {
                    projectile.tick();
                } finally {
                    projectile.discard();
                }
                h.assertTrue(events.size() == 2 && events.get(1).getSpellDamageSource().spell() == SpellRegistry.MANA_SLASH.get()
                                && events.get(1).getSpellDamageSource().getDirectEntity() == projectile
                                && events.get(1).getSpellDamageSource().getEntity() == caster,
                        "A delayed projectile must retain spell, projectile and caster identity");
            } finally {
                MinecraftForge.EVENT_BUS.unregister(listener);
            }
        } finally {
            victim.discard();
            caster.discard();
        }
        h.succeed();
    }

    private static Zombie target(GameTestHelper h, int x) {
        var target = h.spawn(EntityType.ZOMBIE, new BlockPos(x, 2, 1));
        target.setNoAi(true);
        target.setNoGravity(true);
        target.getAttribute(Attributes.MAX_HEALTH).setBaseValue(100);
        target.getAttribute(Attributes.ARMOR).setBaseValue(0);
        target.setHealth(100);
        return target;
    }
}
