package jp.aquafactory.apprenticecodex.gametest;

import com.mojang.authlib.GameProfile;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import jp.aquafactory.apprenticecodex.damage.DamageTypes;
import jp.aquafactory.apprenticecodex.effect.BloodEngravedEffect;
import jp.aquafactory.apprenticecodex.registry.AttachmentRegistry;
import jp.aquafactory.apprenticecodex.registry.EffectRegistry;
import jp.aquafactory.apprenticecodex.registry.SpellRegistry;
import jp.aquafactory.apprenticecodex.spell.bloodbrand.BloodBrand;
import jp.aquafactory.apprenticecodex.spell.bloodbrand.BloodBrandEvents;
import jp.aquafactory.apprenticecodex.spell.bloodbrand.BloodBrandKunai;
import jp.aquafactory.apprenticecodex.spell.bloodbrand.BloodBrandState;
import jp.aquafactory.apprenticecodex.utility.CombatTools;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

import java.util.UUID;

final class BloodBrandGameTestScenarios {
    private static final float VALUE_EPSILON = 1.0E-3F;

    private BloodBrandGameTestScenarios() {
    }

    static void bloodBrandCastSpawnsOneGravityKunai(GameTestHelper helper) {
        var level = helper.getLevel();
        var owner = createPlayer(helper, "blood_brand_cast", testOrigin(helper));
        owner.setYRot(0.0F);
        owner.setXRot(0.0F);
        var spell = (BloodBrand) SpellRegistry.BLOOD_BRAND.get();

        spell.onCast(level, 1, owner, CastSource.SPELLBOOK, MagicData.getPlayerMagicData(owner));

        var projectiles = level.getEntitiesOfClass(BloodBrandKunai.class, owner.getBoundingBox().inflate(4.0D));
        helper.assertTrue(projectiles.size() == 1, "Blood Brand should spawn exactly one kunai");
        var projectile = projectiles.getFirst();
        helper.assertTrue(Math.abs(projectile.getDeltaMovement().length() - BloodBrandKunai.SPEED) < VALUE_EPSILON,
                "Blood Brand kunai should use its configured launch speed");
        helper.assertTrue(Math.abs(projectile.getDamageForGameTest() - spell.getDamage(1, owner)) < VALUE_EPSILON,
                "Blood Brand kunai should receive direct damage from the spell");
        helper.assertTrue(Math.abs(projectile.getBurstDamageForGameTest() - spell.getExplodeDamage(1, owner)) < VALUE_EPSILON,
                "Blood Brand kunai should receive burst damage from the spell");
        helper.assertTrue(Math.abs(projectile.getBurstRangeForGameTest() - spell.getExplodeRange()) < VALUE_EPSILON,
                "Blood Brand kunai should receive burst range from the spell");

        var initialVerticalSpeed = projectile.getDeltaMovement().y;
        projectile.tick();
        helper.assertTrue(projectile.getDeltaMovement().y < initialVerticalSpeed,
                "Blood Brand kunai should fall under gravity");

        discardAll(projectile, owner);
        helper.succeed();
    }

    static void bloodBrandKunaiMarksAndCleansState(GameTestHelper helper) {
        var level = helper.getLevel();
        var owner = createPlayer(helper, "blood_brand_mark", testOrigin(helper));
        owner.setYRot(0.0F);
        owner.setXRot(0.0F);
        var target = createZombie(level, owner.getEyePosition().add(owner.getLookAngle().scale(2.0D)).add(0.0D, -1.0D, 0.0D));
        var spell = (BloodBrand) SpellRegistry.BLOOD_BRAND.get();

        spell.onCast(level, 1, owner, CastSource.SPELLBOOK, MagicData.getPlayerMagicData(owner));
        var projectile = level.getEntitiesOfClass(BloodBrandKunai.class, owner.getBoundingBox().inflate(4.0D)).getFirst();
        for (var tick = 0; tick < 4 && !projectile.isRemoved(); ++tick) {
            projectile.tick();
        }

        helper.assertTrue(projectile.isRemoved(), "Blood Brand kunai should be consumed after hitting a mob");
        var engraved = target.getEffect(EffectRegistry.BLOOD_ENGRAVED);
        helper.assertTrue(engraved != null,
                "Blood Brand damage should apply Blood Engraved to a mob");
        helper.assertTrue(engraved != null && engraved.isVisible(),
                "Blood Engraved should expose potion particles and HUD integrations");
        var state = target.getExistingDataOrNull(AttachmentRegistry.BLOOD_BRAND_STATE);
        helper.assertTrue(state != null && state.casterUuid().equals(owner.getUUID()),
                "Blood Engraved should remember the last successful caster");
        helper.assertTrue(state != null && BloodBrandState.load(state.save()).equals(state),
                "Blood Brand state should survive its persistent NBT round trip");

        target.removeEffect(EffectRegistry.BLOOD_ENGRAVED);
        BloodBrandEvents.onEntityTick(new EntityTickEvent.Post(target));
        helper.assertTrue(target.getExistingDataOrNull(AttachmentRegistry.BLOOD_BRAND_STATE) == null,
                "Blood Brand state should be removed after Blood Engraved expires");

        discardAll(target, owner);
        helper.succeed();
    }

    static void bloodBrandBurstUsesSphereSightAndHalfHealing(GameTestHelper helper) {
        var level = helper.getLevel();
        var center = testOrigin(helper);
        var owner = createPlayer(helper, "blood_brand_burst", center.add(0.0D, 0.0D, -7.0D));
        owner.setHealth(5.0F);
        var origin = createCow(level, center);
        var visible = createCow(level, center.add(3.0D, 0.0D, 0.0D));
        var blocked = createCow(level, center.add(-3.0D, 0.0D, 0.0D));
        var cubeCorner = createCow(level, center.add(4.5D, 0.0D, 4.5D));
        var playerTarget = createPlayer(helper, "blood_brand_player_target", center.add(0.0D, 0.0D, 3.0D));
        var wallLower = BlockPos.containing(center.add(-1.5D, 0.5D, 0.0D));
        var wallUpper = wallLower.above();
        level.setBlockAndUpdate(wallLower, Blocks.STONE.defaultBlockState());
        level.setBlockAndUpdate(wallUpper, Blocks.STONE.defaultBlockState());
        mark(origin, owner.getUUID(), 6.0F, 5.0D);

        var visibleHealth = visible.getHealth();
        var blockedHealth = blocked.getHealth();
        var cornerHealth = cubeCorner.getHealth();
        var playerHealth = playerTarget.getHealth();
        var ownerHealth = owner.getHealth();
        origin.hurt(level.damageSources().generic(), origin.getHealth() + 1.0F);

        var visibleDamage = visibleHealth - visible.getHealth();
        helper.assertTrue(visibleDamage > 0.0F, "Blood Brand burst should damage a visible mob in range");
        helper.assertTrue(Math.abs(blocked.getHealth() - blockedHealth) < VALUE_EPSILON,
                "Blood Brand burst should not pass through a solid wall");
        helper.assertTrue(Math.abs(cubeCorner.getHealth() - cornerHealth) < VALUE_EPSILON,
                "Blood Brand burst should use a spherical range rather than its search cube");
        helper.assertTrue(Math.abs(playerTarget.getHealth() - playerHealth) < VALUE_EPSILON,
                "Blood Brand burst should never damage players");
        helper.assertTrue(Math.abs(owner.getHealth() - (ownerHealth + visibleDamage * 0.5F)) < VALUE_EPSILON,
                "Blood Brand burst should heal half of its total dealt damage");

        level.setBlockAndUpdate(wallLower, Blocks.AIR.defaultBlockState());
        level.setBlockAndUpdate(wallUpper, Blocks.AIR.defaultBlockState());
        discardAll(origin, visible, blocked, cubeCorner, playerTarget, owner);
        helper.succeed();
    }

    static void bloodBrandHiganbanaBurstEnhancesRangeDamageAndHealing(GameTestHelper helper) {
        var level = helper.getLevel();
        var center = testOrigin(helper);
        var owner = createPlayer(helper, "blood_brand_higanbana", center.add(0.0D, 0.0D, -6.0D));
        owner.setHealth(5.0F);
        var origin = createCow(level, center);
        origin.setHealth(1.0F);
        var enhancedOnlyTarget = createCow(level, center.add(2.6D, 0.0D, 0.0D));
        mark(origin, owner.getUUID(), 4.0F, 2.0D);
        var targetHealth = enhancedOnlyTarget.getHealth();
        var ownerHealth = owner.getHealth();
        var source = CombatTools.getDamageSource(level, owner, owner, DamageTypes.HIGANBANA);

        origin.hurt(source, 1.0F);

        var dealtDamage = targetHealth - enhancedOnlyTarget.getHealth();
        helper.assertTrue(Math.abs(dealtDamage - 6.0F) < VALUE_EPSILON,
                "Higanbana should increase Blood Brand burst damage by 50% and reach the expanded range");
        helper.assertTrue(Math.abs(owner.getHealth() - (ownerHealth + 0.5F + dealtDamage)) < VALUE_EPSILON,
                "Higanbana-enhanced Blood Brand should heal all burst damage in addition to Higanbana lifesteal");

        discardAll(origin, enhancedOnlyTarget, owner);
        helper.succeed();
    }

    static void bloodBrandWithoutCasterDoesNotBurst(GameTestHelper helper) {
        var level = helper.getLevel();
        var center = testOrigin(helper);
        var origin = createCow(level, center);
        var nearby = createCow(level, center.add(2.0D, 0.0D, 0.0D));
        mark(origin, UUID.randomUUID(), 8.0F, 5.0D);
        var nearbyHealth = nearby.getHealth();

        origin.hurt(level.damageSources().generic(), origin.getHealth() + 1.0F);

        helper.assertTrue(Math.abs(nearby.getHealth() - nearbyHealth) < VALUE_EPSILON,
                "Blood Brand should not burst when its caster cannot be resolved");
        helper.assertTrue(origin.getExistingDataOrNull(AttachmentRegistry.BLOOD_BRAND_STATE) == null,
                "An unresolved Blood Brand should still consume its stored state");

        discardAll(origin, nearby);
        helper.succeed();
    }

    private static void mark(LivingEntity target, UUID casterUuid, float damage, double range) {
        target.addEffect(new MobEffectInstance(
                EffectRegistry.BLOOD_ENGRAVED,
                BloodEngravedEffect.DURATION_TICKS,
                0,
                false,
                true,
                true
        ));
        target.setData(AttachmentRegistry.BLOOD_BRAND_STATE, new BloodBrandState(casterUuid, damage, range));
    }

    private static Vec3 testOrigin(GameTestHelper helper) {
        return helper.absoluteVec(new Vec3(2.5D, 20.0D, 2.5D));
    }

    private static FakePlayer createPlayer(GameTestHelper helper, String name, Vec3 position) {
        var player = new FakePlayer(helper.getLevel(), new GameProfile(UUID.randomUUID(), name));
        player.setPos(position.x, position.y, position.z);
        helper.getLevel().addFreshEntity(player);
        return player;
    }

    private static Zombie createZombie(ServerLevel level, Vec3 position) {
        var zombie = EntityType.ZOMBIE.create(level);
        if (zombie == null) {
            throw new IllegalStateException("Failed to create Blood Brand test zombie");
        }
        zombie.setPos(position.x, position.y, position.z);
        zombie.setNoAi(true);
        level.addFreshEntity(zombie);
        return zombie;
    }

    private static Cow createCow(ServerLevel level, Vec3 position) {
        var cow = EntityType.COW.create(level);
        if (cow == null) {
            throw new IllegalStateException("Failed to create Blood Brand test cow");
        }
        cow.setPos(position.x, position.y, position.z);
        cow.setNoAi(true);
        level.addFreshEntity(cow);
        return cow;
    }

    private static void discardAll(Entity... entities) {
        for (var entity : entities) {
            entity.discard();
        }
    }
}
