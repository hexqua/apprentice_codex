package jp.aquafactory.apprenticecodex.gametest;

import io.redspace.ironsspellbooks.api.events.SpellHealEvent;
import io.redspace.ironsspellbooks.api.events.SpellOnCastEvent;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.capabilities.magic.SummonManager;
import io.redspace.ironsspellbooks.damage.SpellDamageSource;
import jp.aquafactory.apprenticecodex.item.curios.CuriosSlotConstants;
import jp.aquafactory.apprenticecodex.item.curios.monarchbondcharm.MonarchBondCharm;
import jp.aquafactory.apprenticecodex.item.curios.monarchbondcharm.MonarchBondHealingTarget;
import jp.aquafactory.apprenticecodex.item.curios.monarchbondcharm.MonarchBondHealingEvents;
import jp.aquafactory.apprenticecodex.particle.TransferParticleEffect;
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
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
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

    static void autoRestockRefillsEmptyOwnedTurretAtRangeBoundary(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var wearer = createWearer(helper, "monarch_bond_auto_restock_boundary");
            var magicData = MagicData.getPlayerMagicData(wearer);
            magicData.setMana(17.0F);
            var turret = createAutoTurret(
                    helper, wearer, new BlockPos((int) MonarchBondCharm.RANGE, 2, 0), 5, 17);
            turret.setRestBulletCount(0);

            tickCharmAtRestockInterval(wearer);

            helper.assertTrue(turret.getRestBulletCount() == 5,
                    "Monarch Bond should refill an empty owned AutoTurret at the range boundary");
            helper.assertTrue(Math.abs(magicData.getMana()) <= EPSILON,
                    "Monarch Bond should spend the exact AutoTurret restock mana cost");
        });
    }

    static void autoRestockUsesNearestDeterministicPriority(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var wearer = createWearer(helper, "monarch_bond_auto_restock_priority");
            var magicData = MagicData.getPlayerMagicData(wearer);
            magicData.setMana(10.0F);
            var farther = createAutoTurret(helper, wearer, new BlockPos(6, 2, 0), 5, 10);
            var nearer = createAutoTurret(helper, wearer, new BlockPos(2, 2, 0), 5, 10);
            farther.setRestBulletCount(0);
            nearer.setRestBulletCount(0);

            tickCharmAtRestockInterval(wearer);

            helper.assertTrue(nearer.getRestBulletCount() == 5,
                    "Monarch Bond should prioritize the nearest empty AutoTurret");
            helper.assertTrue(farther.getRestBulletCount() == 0,
                    "Monarch Bond should leave the farther AutoTurret empty after mana is exhausted");
        });
    }

    static void autoRestockSkipsIneligibleAndUnaffordableTurrets(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var wearer = createWearer(helper, "monarch_bond_auto_restock_filter");
            var magicData = MagicData.getPlayerMagicData(wearer);
            magicData.setMana(10.0F);
            var expensive = createAutoTurret(helper, wearer, new BlockPos(2, 2, 0), 5, 20);
            var partial = createAutoTurret(helper, wearer, new BlockPos(3, 2, 0), 5, 10);
            var affordable = createAutoTurret(helper, wearer, new BlockPos(4, 2, 0), 5, 10);
            var outside = createAutoTurret(helper, wearer, new BlockPos(33, 2, 0), 5, 0);
            var otherOwner = createEquipmentTestPlayer(
                    helper, new BlockPos(0, 2, 2), "monarch_bond_auto_restock_other_owner");
            helper.getLevel().addFreshEntity(otherOwner);
            var foreign = createAutoTurret(helper, otherOwner, new BlockPos(5, 2, 0), 5, 0);
            expensive.setRestBulletCount(0);
            partial.setRestBulletCount(1);
            affordable.setRestBulletCount(0);
            outside.setRestBulletCount(0);
            foreign.setRestBulletCount(0);

            tickCharmAtRestockInterval(wearer);

            helper.assertTrue(expensive.getRestBulletCount() == 0,
                    "Monarch Bond should skip an AutoTurret whose restock cost cannot be paid");
            helper.assertTrue(partial.getRestBulletCount() == 1,
                    "Monarch Bond should not refill an AutoTurret that still has ammunition");
            helper.assertTrue(affordable.getRestBulletCount() == 5,
                    "Monarch Bond should continue to a later affordable AutoTurret");
            helper.assertTrue(outside.getRestBulletCount() == 0,
                    "Monarch Bond should not refill an AutoTurret outside its range");
            helper.assertTrue(foreign.getRestBulletCount() == 0,
                    "Monarch Bond should not refill another owner's AutoTurret");
        });
    }

    static void transferParticleCountScalesAndCaps(GameTestHelper helper) {
        helper.succeedIf(() -> {
            helper.assertTrue(TransferParticleEffect.calculateParticleCount(1.0D) == 4,
                    "Transfer particles should keep the minimum count at short range");
            helper.assertTrue(TransferParticleEffect.calculateParticleCount(10.0D) == 5,
                    "Transfer particles should increase with travel distance");
            helper.assertTrue(TransferParticleEffect.calculateParticleCount(64.0D) == 16,
                    "Transfer particles should cap the count at long range");
        });
    }

    static void fieldOverseerManaRangeExtendsWhenCharmIsEquipped(GameTestHelper helper) {
        verifyFieldOverseerManaRange(helper, true, 80.0F, 0.0F);
    }

    static void fieldOverseerManaRangeRemainsEightBlocksWithoutCharm(GameTestHelper helper) {
        verifyFieldOverseerManaRange(helper, false, 60.0F, 20.0F);
    }

    private static void verifyFieldOverseerManaRange(
            GameTestHelper helper,
            boolean equipCharm,
            float expectedStaffMana,
            float expectedOwnerMana
    ) {
        var profileName = equipCharm
                ? "monarch_bond_field_overseer_extended"
                : "monarch_bond_field_overseer_base";
        var owner = createEquipmentTestPlayer(helper, new BlockPos(16, 2, 0), profileName);
        if (equipCharm) {
            equipCurio(owner, CuriosSlotConstants.CHARM, new ItemStack(ItemRegistry.MONARCH_BOND_CHARM.get()));
        }
        var manaRegen = owner.getAttribute(io.redspace.ironsspellbooks.api.registry.AttributeRegistry.MANA_REGEN);
        if (manaRegen != null) {
            manaRegen.setBaseValue(0.0D);
        }
        var ownerMagicData = MagicData.getPlayerMagicData(owner);
        ownerMagicData.setMana(20.0F);
        var anchorPos = helper.absolutePos(new BlockPos(0, 2, 0));
        helper.setBlock(new BlockPos(0, 1, 0), Blocks.STONE);
        var staff = createFieldOverseerTestEntity(helper, owner, anchorPos, 100.0F, 40);
        helper.spawnWithNoFreeWill(EntityType.HUSK, new BlockPos(0, 2, 3));

        helper.runAtTickTime(55, () -> {
            helper.assertTrue(Math.abs(staff.getCurrentMana() - expectedStaffMana) <= EPSILON,
                    "Field Overseer mana transfer range should depend on Monarch Bond: expected="
                            + expectedStaffMana + ", actual=" + staff.getCurrentMana());
            helper.assertTrue(Math.abs(ownerMagicData.getMana() - expectedOwnerMana) <= EPSILON,
                    "Field Overseer should only drain mana within its effective transfer range: expected="
                            + expectedOwnerMana + ", actual=" + ownerMagicData.getMana());
            helper.succeed();
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

    private static AutoTurretEntity createAutoTurret(
            GameTestHelper helper,
            FakePlayer owner,
            BlockPos localPos,
            int initialBulletCount,
            int restockManaCost
    ) {
        var level = helper.getLevel();
        var anchorPos = helper.absolutePos(localPos);
        var center = helper.absoluteVec(Vec3.atBottomCenterOf(localPos));
        var turret = new AutoTurretEntity(EntityRegistry.AUTO_TURRET.get(), level);
        turret.setOwner(owner);
        turret.setAnchorPos(anchorPos);
        turret.setDamage(4.0F);
        turret.setRestockData(initialBulletCount, restockManaCost);
        turret.setTurretMaxHealth(20.0F);
        turret.moveTo(center.x, center.y, center.z, 0.0F, 0.0F);
        level.addFreshEntity(turret);
        return turret;
    }

    private static void tickCharmAtRestockInterval(FakePlayer wearer) {
        var slotResult = top.theillusivec4.curios.api.CuriosApi.getCuriosInventory(wearer)
                .flatMap(inventory -> inventory.findFirstCurio(ItemRegistry.MONARCH_BOND_CHARM.get()))
                .orElseThrow(() -> new IllegalStateException("Missing equipped Monarch Bond Charm for GameTest"));
        wearer.tickCount = 20;
        ((MonarchBondCharm) slotResult.stack().getItem()).curioTick(slotResult.slotContext(), slotResult.stack());
    }

    private static void assertHealth(GameTestHelper helper, LivingEntity entity, float expected, String message) {
        helper.assertTrue(Math.abs(entity.getHealth() - expected) <= EPSILON,
                message + ": expected=" + expected + ", actual=" + entity.getHealth());
    }
}
