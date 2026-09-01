package jp.aquafactory.apprenticecodex.gametest;

import io.redspace.ironsspellbooks.api.events.SpellHealEvent;
import io.redspace.ironsspellbooks.api.events.SpellOnCastEvent;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.capabilities.magic.SummonManager;
import io.redspace.ironsspellbooks.damage.SpellDamageSource;
import jp.aquafactory.apprenticecodex.item.curios.CuriosSlotConstants;
import jp.aquafactory.apprenticecodex.item.curios.monarchbondcharm.MonarchBondCharm;
import jp.aquafactory.apprenticecodex.item.curios.monarchbondcharm.MonarchBondHealingTarget;
import jp.aquafactory.apprenticecodex.item.curios.monarchbondcharm.MonarchBondHealingEvents;
import jp.aquafactory.apprenticecodex.registry.EntityRegistry;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import jp.aquafactory.apprenticecodex.spell.autoturret.AutoTurretEntity;
import jp.aquafactory.apprenticecodex.spell.companiontrunk.CompanionTrunkEntity;
import jp.aquafactory.apprenticecodex.spell.fieldoverseer.FieldOverseerStaffEntity;
import jp.aquafactory.apprenticecodex.spell.healingbloom.HealingBloomEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.damagesource.DamageContainer;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;


final class MonarchBondCharmGameTestScenarios extends ApprenticeCodexGameTestScenarios {
    private static final float EPSILON = 1.0E-4F;
    private MonarchBondCharmGameTestScenarios() {
    }

    static void spellHealOverflowIsReservedNearestFirst(GameTestHelper helper) {
        helper.succeedIf(() -> {
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
        });
    }

    static void spellHealRequiresEquippedWearerAndMatchingOwner(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var unequippedWearer = createEquipmentTestPlayer(
                    helper, new BlockPos(0, 2, 0), "monarch_bond_unequipped");
            helper.getLevel().addFreshEntity(unequippedWearer);
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
            var otherOwner = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 2), "monarch_bond_other_owner");
            helper.getLevel().addFreshEntity(otherOwner);
            var otherSummon = createManagedSummon(helper, otherOwner, new BlockPos(3, 2, 0), 5.0F);

            helper.assertTrue(MonarchBondCharm.isEquippedBy(wearer),
                    "Equipped wearer should expose Monarch Bond through Curios");
            NeoForge.EVENT_BUS.post(new SpellHealEvent(wearer, wearer, 5.0F, SchoolRegistry.HOLY.get()));
            assertHealth(helper, ownSummon, ownSummon.getMaxHealth(),
                    "Equipped wearer should heal their own summon");
            assertHealth(helper, otherSummon, otherSummon.getMaxHealth() - 5.0F,
                    "Equipped wearer should not heal another owner's summon");
        });
    }

    static void greaterHealFullyRestoresEveryOwnedTarget(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var wearer = createWearer(helper, "monarch_bond_greater_heal");
            var first = createManagedSummon(helper, wearer, new BlockPos(2, 2, 0), 3.0F);
            var second = createManagedSummon(helper, wearer, new BlockPos(4, 2, 0), 9.0F);
            var spell = io.redspace.ironsspellbooks.api.registry.SpellRegistry.GREATER_HEAL_SPELL.get();

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
        });
    }

    static void spellLifestealDoesNotDependOnSchool(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var wearer = createWearer(helper, "monarch_bond_spell_lifesteal");
            wearer.setHealth(wearer.getMaxHealth());
            var summon = createManagedSummon(helper, wearer, new BlockPos(2, 2, 0), 5.0F);
            var victim = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new BlockPos(4, 2, 0));
            var evocationSpell = io.redspace.ironsspellbooks.api.registry.SpellRegistry.MAGIC_ARROW_SPELL.get();
            var source = SpellDamageSource.source(wearer, evocationSpell).setLifestealPercent(0.5F);

            MonarchBondHealingEvents.onSpellLifesteal(
                    new LivingDamageEvent.Post(victim, new DamageContainer(source, 10.0F))
            );

            assertHealth(helper, summon, summon.getMaxHealth(),
                    "Positive spell lifesteal should distribute overflow regardless of school");
        });
    }

    static void codexManagedEntitiesExposeMonarchBondHealingTarget(GameTestHelper helper) {
        helper.succeedIf(() -> {
            helper.assertTrue(MonarchBondHealingTarget.class.isAssignableFrom(FieldOverseerStaffEntity.class),
                    "Field Overseer should be a Monarch Bond healing target");
            helper.assertTrue(MonarchBondHealingTarget.class.isAssignableFrom(AutoTurretEntity.class),
                    "Auto Turret should be a Monarch Bond healing target");
            helper.assertTrue(MonarchBondHealingTarget.class.isAssignableFrom(CompanionTrunkEntity.class),
                    "Companion Trunk should be a Monarch Bond healing target");
            helper.assertTrue(MonarchBondHealingTarget.class.isAssignableFrom(HealingBloomEntity.class),
                    "Healing Bloom should be a Monarch Bond healing target");
            helper.assertTrue(EntityRegistry.FIELD_OVERSEER_STAFF.get() != null,
                    "Field Overseer entity registration should remain available");
        });
    }

    private static FakePlayer createWearer(GameTestHelper helper, String profileName) {
        var wearer = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), profileName);
        equipCurio(wearer, CuriosSlotConstants.CHARM, new ItemStack(ItemRegistry.MONARCH_BOND_CHARM.get()));
        helper.getLevel().addFreshEntity(wearer);
        return wearer;
    }

    private static LivingEntity createManagedSummon(
            GameTestHelper helper,
            FakePlayer owner,
            BlockPos position,
            float missingHealth
    ) {
        var summon = helper.spawnWithNoFreeWill(EntityType.HUSK, position);
        summon.setHealth(Math.max(1.0F, summon.getMaxHealth() - missingHealth));
        SummonManager.setOwner(summon, owner);
        return summon;
    }

    private static void assertHealth(GameTestHelper helper, LivingEntity entity, float expected, String message) {
        helper.assertTrue(Math.abs(entity.getHealth() - expected) <= EPSILON,
                message + ": expected=" + expected + ", actual=" + entity.getHealth());
    }
}
