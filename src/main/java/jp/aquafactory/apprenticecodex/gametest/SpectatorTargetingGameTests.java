package jp.aquafactory.apprenticecodex.gametest;

import com.mojang.authlib.GameProfile;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.item.manaforcereleasedevice.ManaForceReleaseDevice;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import jp.aquafactory.apprenticecodex.registry.SpellRegistry;
import jp.aquafactory.apprenticecodex.spell.sacredarrow.SacredArrowEntity;
import jp.aquafactory.apprenticecodex.spell.tirovolley.TiroVolley;
import jp.aquafactory.apprenticecodex.utility.CombatTools;
import jp.aquafactory.apprenticecodex.utility.RaycastTools;
import jp.aquafactory.apprenticecodex.utility.SummonedFirearmTools;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Predicate;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
@SuppressWarnings("unused")
public final class SpectatorTargetingGameTests {
    private static final String TEMPLATE = "gametest/basic_floor";

    private SpectatorTargetingGameTests() {}

    private static final class Scene implements AutoCloseable {
        private final GameTestHelper helper;
        private final List<Entity> entities = new ArrayList<>();
        private final FakePlayer caster;
        private final FakePlayer spectator;

        private Scene(GameTestHelper helper) {
            this.helper = helper;
            caster = new FakePlayer(helper.getLevel(), new GameProfile(UUID.randomUUID(), "spectator_caster"));
            caster.gameMode.changeGameModeForPlayer(GameType.SURVIVAL);
            caster.setPos(helper.absoluteVec(new BlockPos(4, 18, 1).getCenter()));
            caster.setYRot(0);
            caster.setXRot(0);
            spectator = new FakePlayer(helper.getLevel(), new GameProfile(UUID.randomUUID(), "spectator_target"));
            spectator.gameMode.changeGameModeForPlayer(GameType.SPECTATOR);
            spectator.setPos(caster.position().add(0, 0, 2));
            add(spectator);
        }

        private void add(Entity entity) {
            entities.add(entity);
            helper.getLevel().addFreshEntity(entity);
        }

        private Zombie target(Vec3 offset, boolean pickable) {
            var target = new Zombie(EntityType.ZOMBIE, helper.getLevel()) {
                @Override
                public boolean isPickable() { return pickable; }
            };
            target.setNoAi(true);
            target.setNoGravity(true);
            target.setPos(caster.position().add(offset));
            add(target);
            return target;
        }

        @Override
        public void close() {
            entities.forEach(Entity::discard);
            caster.discard();
        }
    }

    @GameTest(template = TEMPLATE)
    public static void commonCombatAndHostilityRejectSpectatorTransitions(GameTestHelper helper) {
        try (var scene = new Scene(helper)) {
            var owner = scene.target(Vec3.ZERO, true);
            var target = scene.spectator;
            target.gameMode.changeGameModeForPlayer(GameType.SURVIVAL);
            owner.setLastHurtByMob(target);
            helper.assertTrue(CombatTools.isValidCombatTarget(target, owner)
                            && CombatTools.canBeHostileToMe(target, owner)
                            && SacredArrowEntity.isLiveTarget(target, owner),
                    "A living attacker must initially remain a valid combat and homing target");
            target.gameMode.changeGameModeForPlayer(GameType.SPECTATOR);
            helper.assertTrue(target.isAlive(), "Entering spectator mode must leave the target alive");
            for (var policy : CombatTools.CombatTargetPolicy.values()) {
                helper.assertFalse(CombatTools.isValidCombatTarget(target, owner, policy)
                                || CombatTools.isValidCombatTarget(target, null, policy),
                        "Spectators must be rejected for every combat policy, including ownerless targeting");
            }
            helper.assertFalse(CombatTools.canBeHostileToMe(target, owner)
                            || SacredArrowEntity.isLiveTarget(target, owner),
                    "A former attacker entering spectator mode must no longer be hostile or a homing target");
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void commonAimQueriesSkipSpectatorsWithPermissivePredicates(GameTestHelper helper) {
        try (var scene = new Scene(helper)) {
            var target = scene.target(new Vec3(0, 0, 5), true);
            Predicate<Entity> candidates = e -> e == scene.spectator || e == target;
            helper.assertTrue(RaycastTools.raycastFromEye(scene.caster, 8, 1, candidates).hitEntity() == target,
                    "Legacy rays must pass through spectators even without a combat predicate");
            helper.assertTrue(RaycastTools.raycastFromEyeWithAimAssist(scene.caster, 8, 1, candidates).hitEntity() == target,
                    "Assisted rays must pass through spectators even without a combat predicate");
            helper.assertTrue(SummonedFirearmTools.resolveAssistedAim(scene.caster, 8, candidates).hitEntity() == target,
                    "Summoned firearm aim must pass through spectators");
            helper.assertTrue(RaycastTools.findNearestEntityInForwardBox(helper.getLevel(), scene.caster,
                            new Vec3(0, 0, 1), 8, 2, 2, candidates, true).orElse(null) == target,
                    "Forward-box selection must skip a nearer spectator");
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void assistedAimSkipsNonPickableLivingEntities(GameTestHelper helper) {
        try (var scene = new Scene(helper)) {
            var unpickable = scene.target(new Vec3(0, 0, 3), false);
            var target = scene.target(new Vec3(0, 0, 6), true);
            Predicate<Entity> candidates = e -> e == unpickable || e == target;
            helper.assertTrue(RaycastTools.raycastFromEyeWithAimAssist(scene.caster, 8, 1, candidates).hitEntity() == target
                            && SummonedFirearmTools.resolveAssistedAim(scene.caster, 8, candidates).hitEntity() == target,
                    "Assisted aim must not let non-pickable living entities hide a farther pickable target");
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void legacyRayRetainsDroppedItemTargeting(GameTestHelper helper) {
        try (var scene = new Scene(helper)) {
            var position = scene.caster.getEyePosition().add(0, 0, 4);
            var item = new ItemEntity(helper.getLevel(), position.x, position.y, position.z, new ItemStack(Items.IRON_ORE));
            item.setNoGravity(true);
            scene.add(item);
            helper.assertFalse(item.isPickable(), "The dropped-item regression case must be non-pickable");
            // Thermal Processはドロップアイテムも照射するため、互換APIにpickable制限を加えない。
            helper.assertTrue(RaycastTools.raycastFromEye(scene.caster, 8, 0.5, e -> e == item).hitEntity() == item,
                    "Legacy aim must retain explicit targeting of non-pickable dropped items");
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void fallbackSelectionSkipsSpectators(GameTestHelper helper) {
        try (var scene = new Scene(helper)) {
            var target = scene.target(new Vec3(0, 0, 5), true);
            helper.assertTrue(TiroVolley.findFallbackTarget(helper.getLevel(), scene.caster).orElse(null) == target
                            && TiroVolley.findRandomForwardTarget(helper.getLevel(), scene.caster).orElse(null) == target,
                    "Nearby and random-forward fallbacks must not select a spectator");
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void shockAndDeviceReachTargetsBehindSpectators(GameTestHelper helper) {
        try (var scene = new Scene(helper)) {
            var target = scene.target(new Vec3(0, 0, 5), true);
            var device = (ManaForceReleaseDevice) ItemRegistry.NOVICE_MANA_FORCE_RELEASE_DEVICE.get();
            scene.caster.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(device));
            var data = MagicData.getPlayerMagicData(scene.caster);
            // 中心線の敵と広い補助だけで拾う敵の両方を、手前のspectator越しに攻撃する。
            for (double offset : new double[]{0, 1}) {
                target.setPos(scene.caster.position().add(offset, 0, 5));
                target.setHealth(target.getMaxHealth());
                target.invulnerableTime = 0;
                SpellRegistry.SHOCK.get().onCast(helper.getLevel(), 1, scene.caster, CastSource.SPELLBOOK, data);
                helper.assertTrue(target.getHealth() < target.getMaxHealth(),
                        "Shock must damage direct and wide-only targets behind a spectator");
                target.setHealth(target.getMaxHealth());
                target.invulnerableTime = 0;
                data.setMana(100);
                scene.caster.getCooldowns().removeCooldown(device);
                device.finishUsingItem(scene.caster.getMainHandItem(), helper.getLevel(), scene.caster);
                helper.assertTrue(target.getHealth() < target.getMaxHealth() && data.getMana() == 0,
                        "A device discharge must reach direct and wide-only targets behind a spectator");
            }
            helper.assertTrue(scene.spectator.getHealth() == scene.spectator.getMaxHealth(),
                    "Spectator health must remain unchanged");
        }
        helper.succeed();
    }
}
