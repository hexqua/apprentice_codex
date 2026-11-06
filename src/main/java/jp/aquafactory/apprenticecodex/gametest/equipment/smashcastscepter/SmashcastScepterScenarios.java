package jp.aquafactory.apprenticecodex.gametest.equipment.smashcastscepter;

import com.mojang.authlib.GameProfile;
import java.util.UUID;
import jp.aquafactory.apprenticecodex.item.smashcastscepter.SmashcastScepter;
import jp.aquafactory.apprenticecodex.item.smashcastscepter.SmashcastScepterAttackEvent;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.damagesource.DamageContainer;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;

final class SmashcastScepterScenarios {
    private SmashcastScepterScenarios() {}

    static void smashcastScepterWindBurstUsesVanillaPostAttackEffect(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var level = helper.getLevel();
            var player = new FakePlayer(level, new GameProfile(UUID.randomUUID(), "smashcast_wind_burst_test"));
            player.setPos(helper.absoluteVec(Vec3.atBottomCenterOf(new BlockPos(0, 3, 0))));
            player.setOnGround(false);
            player.fallDistance = 4.0F;
            player.setDeltaMovement(Vec3.ZERO);
            level.addFreshEntity(player);

            var target = EntityType.ARMOR_STAND.create(level);
            helper.assertTrue(target != null, "Armor Stand target could not be created for Smashcast Wind Burst test");
            target.setPos(helper.absoluteVec(Vec3.atBottomCenterOf(new BlockPos(0, 1, 0))));
            level.addFreshEntity(target);

            var secondTarget = EntityType.ARMOR_STAND.create(level);
            helper.assertTrue(secondTarget != null, "Second Armor Stand target could not be created for Smashcast Wind Burst test");
            secondTarget.setPos(helper.absoluteVec(Vec3.atBottomCenterOf(new BlockPos(1, 1, 0))));
            level.addFreshEntity(secondTarget);

            var stack = new ItemStack(ItemRegistry.SMASHCAST_SCEPTER.get());
            var windBurst = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT)
                    .getOrThrow(Enchantments.WIND_BURST);
            stack.enchant(windBurst, 1);

            EnchantmentHelper.doPostAttackEffectsWithItemSource(level, target, player.damageSources().playerAttack(player), stack);
            helper.assertTrue(player.getDeltaMovement().y > 0.1D,
                    "Smashcast Scepter Wind Burst should trigger vanilla wind burst upward motion but got " + player.getDeltaMovement());

            player.setItemInHand(InteractionHand.MAIN_HAND, stack);
            player.fallDistance = 0.0F;
            player.setDeltaMovement(Vec3.ZERO);
            var firstEpicFightBonus = SmashcastScepterAttackEvent.registerEpicFightSmashcastImpact(
                    player,
                    target,
                    player.damageSources().playerAttack(player),
                    4.0F
            );
            helper.assertTrue(firstEpicFightBonus > 0.0F,
                    "Epic Fight Smashcast should apply bonus damage to the first target");
            helper.assertTrue(player.fallDistance == 0.0F,
                    "Epic Fight Smashcast Wind Burst should restore the player's original fall distance");
            helper.assertTrue(player.getDeltaMovement().y > 0.1D,
                    "Epic Fight Smashcast Wind Burst should use the stored fall distance but got " + player.getDeltaMovement());

            var duplicateEpicFightBonus = SmashcastScepterAttackEvent.registerEpicFightSmashcastImpact(
                    player,
                    target,
                    player.damageSources().playerAttack(player),
                    4.0F
            );
            helper.assertTrue(duplicateEpicFightBonus == 0.0F,
                    "Epic Fight Smashcast should not apply duplicate bonus damage to the same target in one tick");

            var secondEpicFightBonus = SmashcastScepterAttackEvent.registerEpicFightSmashcastImpact(
                    player,
                    secondTarget,
                    player.damageSources().playerAttack(player),
                    4.0F
            );
            helper.assertTrue(secondEpicFightBonus > 0.0F,
                    "Epic Fight Smashcast should keep bonus damage for a different target in the same tick");
        });
    }

    static void smashcastScepterSmashSetsVanillaImpulseFallProtection(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var level = helper.getLevel();
            var player = new FakePlayer(level, new GameProfile(UUID.randomUUID(), "smashcast_impulse_fall_test"));
            player.setPos(helper.absoluteVec(Vec3.atBottomCenterOf(new BlockPos(0, 3, 0))));
            player.setOnGround(false);
            player.fallDistance = 6.0F;
            player.setDeltaMovement(0.12D, -0.8D, -0.08D);
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ItemRegistry.SMASHCAST_SCEPTER.get()));
            level.addFreshEntity(player);

            var target = EntityType.ARMOR_STAND.create(level);
            helper.assertTrue(target != null, "Armor Stand target could not be created for Smashcast impulse fall test");
            target.setPos(helper.absoluteVec(Vec3.atBottomCenterOf(new BlockPos(0, 1, 0))));
            level.addFreshEntity(target);

            SmashcastScepterAttackEvent.onAttackEntity(new AttackEntityEvent(player, target));
            var damage = new DamageContainer(player.damageSources().playerAttack(player), 1.0F);
            SmashcastScepterAttackEvent.onLivingDamage(new LivingDamageEvent.Post(target, damage));

            helper.assertTrue(player.fallDistance == 6.0F,
                    "Smashcast Scepter smash should preserve fall distance for vanilla impulse handling");
            helper.assertTrue(Math.abs(player.getDeltaMovement().y - SmashcastScepter.WIND_BURST_MOTION_EPSILON) < 1.0E-6D,
                    "Smashcast Scepter smash should apply vanilla Mace landing motion");
            helper.assertTrue(player.isIgnoringFallDamageFromCurrentImpulse(),
                    "Smashcast Scepter smash should enable vanilla impulse fall damage protection");
            helper.assertTrue(player.currentImpulseImpactPos != null
                            && player.currentImpulseImpactPos.distanceTo(player.position()) < 1.0E-6D,
                    "Smashcast Scepter smash should record the current impulse impact position");
        });
    }
}
