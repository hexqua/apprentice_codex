package jp.aquafactory.apprenticecodex.gametest;

import com.mojang.authlib.GameProfile;
import io.redspace.ironsspellbooks.compat.Curios;
import jp.aquafactory.apprenticecodex.compat.malum.MalumCompatibility;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.GameType;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.common.ItemAbilities;
import net.neoforged.neoforge.common.util.FakePlayer;
import top.theillusivec4.curios.api.CuriosApi;

import java.util.UUID;

final class SpellReaperScytheGameTestScenarios extends ApprenticeCodexGameTestScenarios {
    private static final ResourceKey<DamageType> MALUM_SCYTHE_SWEEP = ResourceKey.create(
            Registries.DAMAGE_TYPE,
            ResourceLocation.fromNamespaceAndPath(MalumCompatibility.MOD_ID, "scythe_sweep")
    );
    private static final float DAMAGE_TOLERANCE = 1.0E-4F;

    private SpellReaperScytheGameTestScenarios() {
    }

    static void spellReaperScytheUsesVanillaSweepWithoutMalum(GameTestHelper helper) {
        if (ModList.get().isLoaded(MalumCompatibility.MOD_ID)) {
            helper.succeed();
            return;
        }

        var context = prepareSweepAttack(helper, "spell_reaper_vanilla_sweep");
        performFullyChargedAttack(helper, context);

        assertDamageNear(helper, context.firstBystander(), context.firstBystanderHealth(), 1.0F,
                "Spell Reaper Scythe should retain vanilla sweep damage without Malum");
        helper.assertTrue(context.firstBystander().getLastDamageSource() != null
                        && !context.firstBystander().getLastDamageSource().is(MALUM_SCYTHE_SWEEP),
                "Vanilla sweep should not use Malum scythe sweep damage");
        helper.succeed();
    }

    static void spellReaperScytheUsesOneMalumSweepWithMalum(GameTestHelper helper) {
        if (!ModList.get().isLoaded(MalumCompatibility.MOD_ID)) {
            helper.succeed();
            return;
        }

        var context = prepareSweepAttack(helper, "spell_reaper_malum_sweep");
        var sweepingRatio = context.player().getAttributeValue(Attributes.SWEEPING_DAMAGE_RATIO);
        var expectedSweepDamage = (float) (context.player().getAttributeValue(Attributes.ATTACK_DAMAGE)
                * (0.5D + sweepingRatio * 0.33D));
        performFullyChargedAttack(helper, context);

        assertDamageNear(helper, context.firstBystander(), context.firstBystanderHealth(), expectedSweepDamage,
                "Spell Reaper Scythe should deal one Malum sweep hit to the first bystander");
        assertDamageNear(helper, context.secondBystander(), context.secondBystanderHealth(), expectedSweepDamage,
                "Spell Reaper Scythe should deal one Malum sweep hit to the second bystander");
        assertMalumSweepSource(helper, context.firstBystander());
        assertMalumSweepSource(helper, context.secondBystander());
        helper.succeed();
    }

    static void spellReaperScytheDoesNotSweepWithNarrowEdge(GameTestHelper helper) {
        assertMalumNecklacePreventsSweep(helper, "necklace_of_the_narrow_edge", "narrow_edge");
    }

    static void spellReaperScytheDoesNotSweepWithHiddenBlade(GameTestHelper helper) {
        assertMalumNecklacePreventsSweep(helper, "necklace_of_the_hidden_blade", "hidden_blade");
    }

    private static void assertMalumNecklacePreventsSweep(
            GameTestHelper helper,
            String necklaceId,
            String profileSuffix
    ) {
        if (!ModList.get().isLoaded(MalumCompatibility.MOD_ID)) {
            helper.succeed();
            return;
        }

        var context = prepareSweepAttack(helper, "spell_reaper_" + profileSuffix);
        equipMalumNecklace(helper, context.player(), necklaceId);
        performFullyChargedAttack(helper, context);

        helper.assertTrue(context.primaryTarget().getHealth() < context.primaryHealth(),
                "Spell Reaper Scythe should still damage its primary target with " + necklaceId);
        assertDamageNear(helper, context.firstBystander(), context.firstBystanderHealth(), 0.0F,
                "Spell Reaper Scythe should not sweep the first bystander with " + necklaceId);
        assertDamageNear(helper, context.secondBystander(), context.secondBystanderHealth(), 0.0F,
                "Spell Reaper Scythe should not sweep the second bystander with " + necklaceId);
        helper.succeed();
    }

    private static SweepAttackContext prepareSweepAttack(GameTestHelper helper, String profileName) {
        var player = new FullyChargedFakePlayer(
                helper.getLevel(),
                new GameProfile(UUID.randomUUID(), profileName)
        );
        player.gameMode.changeGameModeForPlayer(GameType.SURVIVAL);
        var playerPos = helper.absoluteVec(Vec3.atBottomCenterOf(new BlockPos(2, 2, 1)));
        player.setPos(playerPos.x, playerPos.y, playerPos.z);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ItemRegistry.SPELL_REAPER_SCYTHE.get()));
        player.setSprinting(false);
        player.setDeltaMovement(Vec3.ZERO);
        player.setOnGround(true);

        var primaryTarget = prepareTarget(helper, new BlockPos(2, 2, 3));
        var firstBystander = prepareTarget(helper, new BlockPos(3, 2, 3));
        var secondBystander = prepareTarget(helper, new BlockPos(1, 2, 3));
        return new SweepAttackContext(
                player,
                primaryTarget,
                firstBystander,
                secondBystander,
                primaryTarget.getHealth(),
                firstBystander.getHealth(),
                secondBystander.getHealth()
        );
    }

    private static LivingEntity prepareTarget(GameTestHelper helper, BlockPos pos) {
        var target = helper.spawn(EntityType.SHEEP, pos);
        target.setNoAi(true);
        var maxHealth = target.getAttribute(Attributes.MAX_HEALTH);
        if (maxHealth != null) {
            maxHealth.setBaseValue(100.0D);
        }
        target.setHealth(100.0F);
        return target;
    }

    private static void performFullyChargedAttack(GameTestHelper helper, SweepAttackContext context) {
        context.player().setSprinting(false);
        context.player().setDeltaMovement(Vec3.ZERO);
        context.player().setOnGround(true);
        context.player().fallDistance = 0.0F;
        context.player().walkDistO = context.player().walkDist;
        context.player().setSpeed(0.1F);
        helper.assertTrue(context.player().getAttackStrengthScale(0.5F) > 0.9F,
                "Spell Reaper Scythe sweep test requires a fully charged attack");
        helper.assertTrue(context.player().getMainHandItem().canPerformAction(ItemAbilities.SWORD_SWEEP)
                        == !ModList.get().isLoaded(MalumCompatibility.MOD_ID),
                "Spell Reaper Scythe sweep ability should match Malum availability");
        helper.assertTrue(context.player().getMainHandItem().getSweepHitBox(context.player(), context.primaryTarget())
                        .intersects(context.firstBystander().getBoundingBox()),
                "Spell Reaper Scythe sweep hit box should contain the first bystander");
        context.player().attack(context.primaryTarget());
    }

    private static void equipMalumNecklace(GameTestHelper helper, FakePlayer player, String necklaceId) {
        var necklace = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(
                MalumCompatibility.MOD_ID,
                necklaceId
        ));
        helper.assertTrue(necklace != Items.AIR, "Missing Malum necklace for GameTest: " + necklaceId);
        var curiosInventory = CuriosApi.getCuriosInventory(player)
                .orElseThrow(() -> new IllegalStateException("Missing Curios inventory for Malum necklace GameTest"));
        curiosInventory.setEquippedCurio(Curios.NECKLACE_SLOT, 0, new ItemStack(necklace));
    }

    private static void assertDamageNear(
            GameTestHelper helper,
            LivingEntity target,
            float initialHealth,
            float expectedDamage,
            String message
    ) {
        var actualDamage = initialHealth - target.getHealth();
        helper.assertTrue(Math.abs(actualDamage - expectedDamage) <= DAMAGE_TOLERANCE,
                message + ": got " + actualDamage + " / expected " + expectedDamage);
    }

    private static void assertMalumSweepSource(GameTestHelper helper, LivingEntity target) {
        var source = target.getLastDamageSource();
        helper.assertTrue(source != null && source.is(MALUM_SCYTHE_SWEEP),
                "Spell Reaper Scythe bystander damage should use malum:scythe_sweep");
    }

    private record SweepAttackContext(
            FakePlayer player,
            LivingEntity primaryTarget,
            LivingEntity firstBystander,
            LivingEntity secondBystander,
            float primaryHealth,
            float firstBystanderHealth,
            float secondBystanderHealth
    ) {
    }

    private static final class FullyChargedFakePlayer extends FakePlayer {
        private FullyChargedFakePlayer(ServerLevel level, GameProfile profile) {
            super(level, profile);
        }

        @Override
        public float getAttackStrengthScale(float adjustTicks) {
            // FakePlayer.tick() は空実装のため、攻撃処理そのものを検証できるよう充填済み状態を固定する。
            return 1.0F;
        }
    }
}
