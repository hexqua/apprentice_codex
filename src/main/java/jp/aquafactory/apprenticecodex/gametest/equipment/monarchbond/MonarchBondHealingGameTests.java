package jp.aquafactory.apprenticecodex.gametest.equipment.monarchbond;

import io.redspace.ironsspellbooks.api.events.SpellHealEvent;
import io.redspace.ironsspellbooks.api.events.SpellOnCastEvent;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.capabilities.magic.SummonManager;
import io.redspace.ironsspellbooks.damage.SpellDamageSource;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.gametest.support.DeployedSpellTestFixture;
import jp.aquafactory.apprenticecodex.item.curios.monarchbondcharm.MonarchBondCharm;
import jp.aquafactory.apprenticecodex.item.curios.monarchbondcharm.MonarchBondHealingEvents;
import jp.aquafactory.apprenticecodex.registry.EntityRegistry;
import jp.aquafactory.apprenticecodex.spell.autoturret.AutoTurretEntity;
import jp.aquafactory.apprenticecodex.spell.companiontrunk.CompanionTrunkEntity;
import jp.aquafactory.apprenticecodex.spell.fieldoverseer.FieldOverseerStaffEntity;
import jp.aquafactory.apprenticecodex.spell.healingbloom.HealingBloomEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.damagesource.DamageContainer;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import static jp.aquafactory.apprenticecodex.gametest.equipment.monarchbond.MonarchBondTestSupport.*;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class MonarchBondHealingGameTests {
    private MonarchBondHealingGameTests() {
    }

    @GameTest(template = DeployedSpellTestFixture.TEMPLATE)
    public static void monarchBondSpellHealOverflowIsReservedNearestFirst(GameTestHelper helper) {
        var wearer = createWearer(helper, "monarch_bond_spell_heal");
        wearer.setHealth(wearer.getMaxHealth() - 2.0F);
        var nearest = createManagedSummon(helper, wearer, new BlockPos(2, 2, 0), 6.0F);
        var next = createManagedSummon(helper, wearer, new BlockPos(4, 2, 0), 10.0F);

        NeoForge.EVENT_BUS.post(new SpellHealEvent(wearer, wearer, 10.0F, SchoolRegistry.HOLY.get()));

        assertHealth(helper, nearest, nearest.getMaxHealth(),
                "Nearest summon should reserve its full missing health first");
        assertHealth(helper, next, next.getMaxHealth() - 8.0F,
                "Second summon should receive only the remaining overflow");

        var nextHealth = next.getHealth();
        NeoForge.EVENT_BUS.post(new SpellHealEvent(wearer, nearest, 100.0F, SchoolRegistry.HOLY.get()));
        assertHealth(helper, next, nextHealth,
                "Healing a summon directly should not distribute from the wearer");
        helper.succeed();
    }

    @GameTest(template = DeployedSpellTestFixture.TEMPLATE)
    public static void monarchBondSpellHealRequiresEquippedWearerAndMatchingOwner(GameTestHelper helper) {
        var unequippedWearer = DeployedSpellTestFixture.of(helper).player(new BlockPos(0, 2, 0), "monarch_bond_unequipped");
        unequippedWearer.setHealth(unequippedWearer.getMaxHealth());
        var unequippedSummon = createManagedSummon(
                helper, unequippedWearer, new BlockPos(2, 2, 0), 5.0F);

        NeoForge.EVENT_BUS.post(new SpellHealEvent(
                unequippedWearer, unequippedWearer, 5.0F, SchoolRegistry.HOLY.get()));
        assertHealth(helper, unequippedSummon, unequippedSummon.getMaxHealth() - 5.0F,
                "Unequipped wearer should not distribute overflow");

        unequippedSummon.discard();
        unequippedWearer.discard();
        var wearer = createWearer(helper, "monarch_bond_equipped_owner");
        var ownSummon = createManagedSummon(helper, wearer, new BlockPos(2, 2, 0), 5.0F);
        var otherOwner = DeployedSpellTestFixture.of(helper).player(new BlockPos(0, 2, 2), "monarch_bond_other_owner");
        var otherSummon = createManagedSummon(helper, otherOwner, new BlockPos(3, 2, 0), 5.0F);

        helper.assertTrue(MonarchBondCharm.isEquippedBy(wearer),
                "Equipped wearer should expose Monarch Bond through Curios");
        NeoForge.EVENT_BUS.post(new SpellHealEvent(wearer, wearer, 5.0F, SchoolRegistry.HOLY.get()));
        assertHealth(helper, ownSummon, ownSummon.getMaxHealth(),
                "Equipped wearer should heal their own summon");
        assertHealth(helper, otherSummon, otherSummon.getMaxHealth() - 5.0F,
                "Equipped wearer should not heal another owner's summon");
        helper.succeed();
    }

    @GameTest(template = DeployedSpellTestFixture.TEMPLATE)
    public static void monarchBondGreaterHealFullyRestoresEveryOwnedTarget(GameTestHelper helper) {
        var wearer = createWearer(helper, "monarch_bond_greater_heal");
        var first = createManagedSummon(helper, wearer, new BlockPos(2, 2, 0), 3.0F);
        var second = createManagedSummon(helper, wearer, new BlockPos(4, 2, 0), 9.0F);
        var spell = SpellRegistry.GREATER_HEAL_SPELL.get();

        NeoForge.EVENT_BUS.post(new SpellOnCastEvent(
                wearer,
                spell.getSpellId(),
                1,
                spell.getManaCost(1),
                spell.getSchoolType(),
                CastSource.SPELLBOOK
        ));

        assertHealth(helper, first, first.getMaxHealth(),
                "Greater Heal should fully restore the first summon");
        assertHealth(helper, second, second.getMaxHealth(),
                "Greater Heal should fully restore every summon without a finite budget");
        helper.succeed();
    }

    @GameTest(template = DeployedSpellTestFixture.TEMPLATE)
    public static void monarchBondSpellLifestealDoesNotDependOnSchool(GameTestHelper helper) {
        var wearer = createWearer(helper, "monarch_bond_spell_lifesteal");
        wearer.setHealth(wearer.getMaxHealth());
        var summon = createManagedSummon(helper, wearer, new BlockPos(2, 2, 0), 5.0F);
        var victim = DeployedSpellTestFixture.of(helper).mob(EntityType.HUSK, new BlockPos(4, 2, 0));
        var evocationSpell = SpellRegistry.MAGIC_ARROW_SPELL.get();
        var source = SpellDamageSource.source(wearer, evocationSpell).setLifestealPercent(0.5F);

        MonarchBondHealingEvents.onSpellLifesteal(
                new LivingDamageEvent.Post(victim, new DamageContainer(source, 10.0F))
        );

        assertHealth(helper, summon, summon.getMaxHealth(),
                "Positive spell lifesteal should distribute overflow regardless of school");
        helper.succeed();
    }

    @GameTest(template = DeployedSpellTestFixture.TEMPLATE)
    public static void monarchBondHealsCodexOwnedEntitiesWithoutSummonRegistration(GameTestHelper helper) {
        var fixture = DeployedSpellTestFixture.of(helper);
        for (var kind : HealingTarget.values()) {
            var wearer = createWearer(helper, "monarch_" + kind.name().toLowerCase());
            var foreignOwner = fixture.player(new BlockPos(0, 2, 2), "foreign_heal_owner");
            var owned = kind.create(helper, wearer, new BlockPos(2, 2, 0));
            var foreign = kind.create(helper, foreignOwner, new BlockPos(3, 2, 0));
            helper.assertFalse(SummonManager.getSummons(wearer).contains(owned.getUUID()),
                    "Codex healing target must not require SummonManager registration: " + kind);
            owned.setHealth(10);
            foreign.setHealth(10);
            NeoForge.EVENT_BUS.post(new SpellHealEvent(wearer, wearer, 3, SchoolRegistry.HOLY.get()));
            assertHealth(helper, owned, 13, "Owned Codex entity must receive overflow healing: " + kind);
            assertHealth(helper, foreign, 10, "Foreign Codex entity must not receive healing: " + kind);
            owned.discard();
            foreign.discard();
            wearer.discard();
            foreignOwner.discard();
        }
        helper.succeed();
    }

    private enum HealingTarget {
        FIELD_OVERSEER, AUTO_TURRET, COMPANION_TRUNK, HEALING_BLOOM;

        LivingEntity create(GameTestHelper helper, FakePlayer owner, BlockPos relative) {
            LivingEntity target = switch (this) {
                case FIELD_OVERSEER -> {
                    var entity = new FieldOverseerStaffEntity(EntityRegistry.FIELD_OVERSEER_STAFF.get(), helper.getLevel());
                    entity.setOwner(owner);
                    yield entity;
                }
                case AUTO_TURRET -> {
                    var entity = new AutoTurretEntity(EntityRegistry.AUTO_TURRET.get(), helper.getLevel());
                    entity.setOwner(owner);
                    yield entity;
                }
                case COMPANION_TRUNK -> {
                    var entity = new CompanionTrunkEntity(EntityRegistry.COMPANION_TRUNK.get(), helper.getLevel());
                    entity.setOwner(owner);
                    yield entity;
                }
                case HEALING_BLOOM -> {
                    var entity = new HealingBloomEntity(EntityRegistry.HEALING_BLOOM.get(), helper.getLevel());
                    entity.setOwner(owner);
                    yield entity;
                }
            };
            target.getAttribute(Attributes.MAX_HEALTH).setBaseValue(20);
            target.setHealth(20);
            target.setPos(DeployedSpellTestFixture.of(helper).absolute(Vec3.atBottomCenterOf(relative)));
            return DeployedSpellTestFixture.of(helper).add(target);
        }
    }
}
