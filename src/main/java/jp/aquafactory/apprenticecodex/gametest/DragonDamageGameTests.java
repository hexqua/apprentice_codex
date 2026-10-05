package jp.aquafactory.apprenticecodex.gametest;

import com.mojang.authlib.GameProfile;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import io.redspace.ironsspellbooks.api.util.Utils;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.compat.malum.MalumSpellReaperScytheBridge;
import jp.aquafactory.apprenticecodex.config.ApprenticeCodexServerConfig;
import jp.aquafactory.apprenticecodex.damage.DamageTypes;
import jp.aquafactory.apprenticecodex.item.multicastechostaff.MulticastEchoStaffAttackHandler;
import jp.aquafactory.apprenticecodex.item.multicastechostaff.MulticastEchoStaffAttackProfile;
import jp.aquafactory.apprenticecodex.item.multicastechostaff.MulticastEchoStaffAttackProfileManager;
import jp.aquafactory.apprenticecodex.item.spellreaperscythe.ScytheThrowDamage;
import jp.aquafactory.apprenticecodex.item.spellreaperscythe.ScytheThrowEntity;
import jp.aquafactory.apprenticecodex.item.spellreaperscythe.ScytheThrowManager;
import jp.aquafactory.apprenticecodex.mixin.LivingEntityDamageMemoryAccessor;
import jp.aquafactory.apprenticecodex.registry.EntityRegistry;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import jp.aquafactory.apprenticecodex.registry.SpellRegistry;
import jp.aquafactory.apprenticecodex.spell.moonlight.MoonLightChargeCutEntity;
import jp.aquafactory.apprenticecodex.spell.moonlight.MoonLightKatanaEntity;
import jp.aquafactory.apprenticecodex.spell.quickarms.QuickArmsHandgunEntity;
import jp.aquafactory.apprenticecodex.spell.skyedge.SkyEdgeProjectileEntity;
import jp.aquafactory.apprenticecodex.utility.CombatTools;
import jp.aquafactory.apprenticecodex.utility.RaycastTools;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.enderdragon.phases.EnderDragonPhase;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
@SuppressWarnings("unused")
public final class DragonDamageGameTests {
    private static final String TEMPLATE = "gametest/basic_floor";
    private static int sceneSequence;

    private DragonDamageGameTests() {}

    @GameTest(template = TEMPLATE)
    public static void spellDamageUsesHeadDamageForEveryPartAndParent(GameTestHelper helper) {
        if (skipDragonTest(helper)) return;
        try (var scene = new Scene(helper)) {
            assertDragonDamage(helper, scene, true);
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void unscaledDamageUsesHeadDamageForEveryPartAndParent(GameTestHelper helper) {
        if (skipDragonTest(helper)) return;
        try (var scene = new Scene(helper)) {
            assertDragonDamage(helper, scene, false);
        }
        helper.succeed();
    }

    private static void assertDragonDamage(GameTestHelper helper, Scene scene, boolean spell) {
        // 比較条件ごとに別個体を使い、部位間で共有する無敵時間やフェーズ遷移を混ぜない。
        var dragon = scene.dragon();
        for (int index = 0; index <= dragon.getSubEntities().length; index++) {
            if (index > 0) dragon = scene.dragon();
            Entity target = index < dragon.getSubEntities().length ? dragon.getSubEntities()[index] : dragon;
            var source = helper.getLevel().damageSources().playerAttack(scene.owner);
            boolean applied = spell
                    ? CombatTools.applyDamage(target, 20, source, null, CombatTools.KnockbackTypes.NO_KNOCKBACK)
                    : CombatTools.applyUnscaledDamage(target, 20, source, CombatTools.KnockbackTypes.NO_KNOCKBACK);
            helper.assertTrue(applied, "Dragon damage should be accepted for target " + index);
            assertDamage(helper, dragon, 20);
        }
    }

    @GameTest(template = TEMPLATE)
    public static void partDamageUsesParentResistanceAndProtection(GameTestHelper helper) {
        if (skipDragonTest(helper)) return;
        try (var scene = new Scene(helper)) {
            var dragon = scene.dragon();
            Objects.requireNonNull(dragon.getAttribute(AttributeRegistry.SPELL_RESIST.get())).setBaseValue(2);
            var source = helper.getLevel().damageSources().playerAttack(scene.owner);
            CombatTools.applyDamage(dragon.getSubEntities()[2], 20, source, null, CombatTools.KnockbackTypes.DEFAULT);
            assertDamage(helper, dragon, 20 * (2 - (float) Utils.softCapFormula(2)));

            var unscaled = scene.dragon();
            Objects.requireNonNull(unscaled.getAttribute(AttributeRegistry.SPELL_RESIST.get())).setBaseValue(2);
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
    public static void quickArmsParentRayUsesHeadDamage(GameTestHelper helper) {
        if (skipDragonTest(helper)) return;
        try (var scene = new Scene(helper)) {
            var dragon = scene.dragon();
            // 頭と本体の外枠が重なり、照準が手前の本体を選ぶ場合も頭部相当のダメージにする。
            dragon.setPos(scene.origin.add(11, 0, 0));
            scene.exposePart(dragon.head, new Vec3(4.5, scene.owner.getEyeHeight() - 0.5, 0));
            var selected = RaycastTools.raycastFromEye(scene.owner, 16, 0.5,
                    entity -> CombatTools.isValidCombatTarget(entity, scene.owner));
            helper.assertTrue(selected.hitEntity() == dragon,
                    "The ray must select the parent's envelope: selected=" + selected.hitEntity()
                            + ", eye=" + scene.owner.getEyePosition() + ", view=" + scene.owner.getViewVector(1)
                            + ", head=" + dragon.head.getBoundingBox());
            var weapon = new QuickArmsHandgunEntity(EntityRegistry.QUICK_ARMS_HANDGUN.get(),
                    helper.getLevel(), scene.owner);
            weapon.setRange(16);
            scene.add(weapon);
            weapon.fire(helper.getLevel(), 20);
            assertDamage(helper, dragon, 20);
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void areaSlashUsesHeadDamage(GameTestHelper helper) {
        if (skipDragonTest(helper)) return;
        try (var scene = new Scene(helper)) {
            var dragon = scene.dragon();
            scene.exposePart(dragon.getSubEntities()[1], new Vec3(2.5, scene.owner.getEyeHeight() - 0.5, 0));
            var weapon = new MoonLightKatanaEntity(EntityRegistry.MOON_LIGHT_KATANA.get(),
                    helper.getLevel(), scene.owner);
            weapon.setPos(scene.owner.getEyePosition());
            weapon.setYRot(-90);
            weapon.setDamage(20);
            scene.add(weapon);
            weapon.slash(helper.getLevel());
            assertDamage(helper, dragon, 20);
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void skyEdgeNonHeadCollisionUsesHeadDamage(GameTestHelper helper) {
        if (skipDragonTest(helper)) return;
        try (var scene = new Scene(helper)) {
            var dragon = scene.dragon();
            scene.exposePart(dragon.getSubEntities()[1], new Vec3(2.5, scene.owner.getEyeHeight() - 0.5, 0));
            // Lv1の表示値と同じ5ダメージ4発を、頭以外へ当てても合計20にする。
            for (int index = 0; index < 4; index++) {
                var projectile = new SkyEdgeProjectileEntity(EntityRegistry.SKY_EDGE_PROJECTILE.get(),
                        helper.getLevel(), scene.owner);
                projectile.setPos(scene.owner.getEyePosition());
                projectile.setDamage(5);
                projectile.setProjectileVelocity(new Vec3(1, 0, 0), 5);
                scene.add(projectile);
                projectile.tick();
                helper.assertTrue(projectile.isRemoved(), "Sky Edge must collide with the exposed non-head part");
            }
            assertDamage(helper, dragon, 20);
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void scytheDamagePairUsesHeadDamageAndKeepsParentMemory(GameTestHelper helper) {
        if (skipDragonTest(helper)) return;
        try (var scene = new Scene(helper)) {
            scene.owner.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ItemRegistry.SPELL_REAPER_SCYTHE.get()));
            var projectile = new ScytheThrowEntity(EntityRegistry.SCYTHE_THROW.get(), helper.getLevel());
            scene.add(projectile);
            var dragon = scene.dragon();
            for (int index = 0; index <= dragon.getSubEntities().length; index++) {
                if (index > 0) dragon = scene.dragon();
                Entity target = index < dragon.getSubEntities().length ? dragon.getSubEntities()[index] : dragon;
                ScytheThrowDamage.hit(helper.getLevel(), projectile, scene.owner, target,
                        scene.owner.getMainHandItem(), 10, 2, false);
                assertDamage(helper, dragon, 12);
                var memory = (LivingEntityDamageMemoryAccessor) dragon;
                helper.assertTrue(Math.abs(memory.apprenticecodex$getLastHurt() - 10) < 0.01F,
                        "Additional magic must preserve the parent's physical damage memory");
                helper.assertTrue(dragon.invulnerableTime > 0, "Physical damage must retain the parent's iframe");
            }
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void continuousScytheDamageUsesHeadDamageAndRestoresParentMemory(GameTestHelper helper) {
        if (skipDragonTest(helper)) return;
        try (var scene = new Scene(helper)) {
            var weapon = new ItemStack(ItemRegistry.SPELL_REAPER_SCYTHE.get());
            scene.owner.setItemInHand(InteractionHand.MAIN_HAND, weapon);
            var projectile = new ScytheThrowEntity(EntityRegistry.SCYTHE_THROW.get(), helper.getLevel());
            scene.add(projectile);
            var dragon = scene.dragon();
            for (int index = 0; index <= dragon.getSubEntities().length; index++) {
                if (index > 0) dragon = scene.dragon();
                Entity target = index < dragon.getSubEntities().length ? dragon.getSubEntities()[index] : dragon;
                dragon.invulnerableTime = 7;
                var memory = (LivingEntityDamageMemoryAccessor) dragon;
                memory.apprenticecodex$setLastHurt(50);
                ScytheThrowDamage.hit(helper.getLevel(), projectile, scene.owner, target, weapon, 10, 2, true);
                assertDamage(helper, dragon, 1.2F);
                helper.assertTrue(dragon.invulnerableTime == 7 && memory.apprenticecodex$getLastHurt() == 50,
                        "Continuous damage must restore the parent's iframe and damage memory");
            }
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void narrowScytheNonHeadContactUsesHeadDamage(GameTestHelper helper) {
        if (skipDragonTest(helper)) return;
        if (!MalumSpellReaperScytheBridge.isAvailable()) {
            helper.succeed();
            return;
        }
        try (var scene = new Scene(helper)) {
            var owner = scene.owner;
            owner.setYRot(0);
            owner.setYHeadRot(0);
            Objects.requireNonNull(owner.getAttribute(Attributes.ATTACK_DAMAGE)).setBaseValue(10);
            Objects.requireNonNull(owner.getAttribute(AttributeRegistry.MAX_MANA.get())).setBaseValue(10000);
            MagicData.getPlayerMagicData(owner).setMana(1000);
            owner.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ItemRegistry.SPELL_REAPER_SCYTHE.get()));
            ScytheNarrowGameTests.equip(owner, "necklace_of_the_narrow_edge");
            ScytheReboundGameTests.enchant(helper, owner.getMainHandItem(), "rebound", 1);
            var dragon = scene.dragon();
            scene.exposePart(dragon.getSubEntities()[1], new Vec3(0, owner.getEyeHeight() - 0.5, 2.5));
            ScytheReboundGameTests.use(helper, owner);
            var projectile = ScytheThrowManager.active(owner);
            helper.assertTrue(projectile != null && projectile.isNarrow(), "Narrow Edge must launch a narrow scythe");
            Objects.requireNonNull(projectile);
            scene.entities.add(projectile);
            float expected = projectile.getPhysicalDamage() + projectile.getMagicDamage();
            projectile.tick();
            helper.assertTrue(projectile.isRemoved(), "The first non-head contact must recall the narrow scythe");
            assertDamage(helper, dragon, expected);
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void ordinaryLivingTargetsAndCrystalsStillTakeDamage(GameTestHelper helper) {
        try (var scene = new Scene(helper)) {
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

    @GameTest(template = TEMPLATE, batch = "apprenticecodex.multicast_echo_staff_isolated")
    public static void repeatedPartDamageAdjustsParentIframe(GameTestHelper helper) {
        if (skipDragonTest(helper)) return;
        var spell = SpellRegistry.LETHAL_ASSAULT.get();
        var profile = new MulticastEchoStaffAttackProfile(0.25, true, true, true, 100, 2);
        try (var scene = new Scene(helper);
             var config = ApprenticeCodexServerConfig.useMulticastEchoStaffAttackConfigOverrideForGameTest(true, 1);
             var profiles = MulticastEchoStaffAttackProfileManager.useProfilesForGameTest(
                     Map.of(spell.getSpellResource(), profile))) {
            var dragon = scene.dragon();
            dragon.invulnerableTime = 20;
            ((LivingEntityDamageMemoryAccessor) dragon).apprenticecodex$setLastHurt(100);
            var source = helper.getLevel().damageSources().playerAttack(scene.owner);
            MulticastEchoStaffAttackHandler.runRepeatedCast(scene.owner, spell, () ->
                    helper.assertTrue(CombatTools.applyDamage(dragon.getSubEntities()[2], 20, source, null,
                            CombatTools.KnockbackTypes.NO_KNOCKBACK), "Repeated part damage must bypass the parent's iframe"));
            assertDamage(helper, dragon, 5);
            helper.assertTrue(dragon.invulnerableTime == 2,
                    "Repeated part damage must restore the configured iframe on the parent");
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void sampledBeamHitsNonHeadPartsOncePerDamagePass(GameTestHelper helper) {
        if (skipDragonTest(helper)) return;
        try (var scene = new Scene(helper)) {
            var dragon = scene.dragon();
            scene.exposePart(dragon.getSubEntities()[6], new Vec3(3, 1, 0));
            scene.exposePart(dragon.getSubEntities()[7], new Vec3(5, 1, 0));
            var source = CombatTools.getDamageSource(helper.getLevel(), scene.owner, DamageTypes.GRIND_RUNNER);
            // 複数部位を拾っても一回分。次の判定では再び命中し、継続攻撃を妨げない。
            for (int pass = 1; pass <= 2; pass++) {
                var hits = RaycastTools.sampleBeamHits(helper.getLevel(), scene.origin.add(0, 1, 0),
                        scene.origin.add(8, 1, 0), 0.3, 0.2,
                        target -> CombatTools.isValidCombatTarget(target, scene.owner));
                helper.assertTrue(hits.size() == 1 && hits.contains(dragon),
                        "Beam must merge exposed non-head parts into one parent hit");
                hits.forEach(target -> CombatTools.applyDamage(target, 20, source, null,
                        CombatTools.KnockbackTypes.NO_KNOCKBACK));
                assertDamage(helper, dragon, 20 * pass);
            }
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void moonLightChargeCutHitsNonHeadPartsOnceAcrossSegments(GameTestHelper helper) {
        if (skipDragonTest(helper)) return;
        try (var scene = new Scene(helper)) {
            var dragon = scene.dragon();
            scene.exposePart(dragon.getSubEntities()[6], new Vec3(3, 1, 0));
            scene.exposePart(dragon.getSubEntities()[7], new Vec3(6, 1, 0));
            var cut = new MoonLightChargeCutEntity(EntityRegistry.MOON_LIGHT_CHARGE_CUT.get(),
                    helper.getLevel(), scene.owner);
            cut.setPos(scene.origin);
            cut.setYRot(-90);
            cut.setXRot(0);
            cut.setup(8, 20);
            scene.add(cut);
            // 部位が別の進行区間で命中しても、斬撃全体では本体へ一回だけ適用する。
            for (int tick = 0; tick < MoonLightChargeCutEntity.PROCESS_START_DELAY_TICKS
                    + MoonLightChargeCutEntity.PROCESS_DURATION_TICKS + 1; tick++) {
                if (!cut.isRemoved()) {
                    ++cut.tickCount;
                    cut.tick();
                }
            }
            assertDamage(helper, dragon, 20);
        }
        helper.succeed();
    }

    private static boolean skipDragonTest(GameTestHelper helper) {
        // Epic Fight系はドラゴンの部位・ダメージ処理を変更するため、バニラ比較の対象外にする。
        if (ModList.get().isLoaded("epicfight") || ModList.get().isLoaded("efn")) {
            helper.succeed();
            return true;
        }
        return false;
    }

    private static void assertDamage(GameTestHelper helper, EnderDragon dragon, float expected) {
        float actual = dragon.getMaxHealth() - dragon.getHealth();
        helper.assertTrue(Math.abs(actual - expected) < 0.01F,
                "Unexpected dragon damage: expected=" + expected + ", actual=" + actual);
    }

    private static final class Scene implements AutoCloseable {
        private final GameTestHelper helper;
        private final Vec3 origin;
        private final FakePlayer owner;
        private final List<Entity> entities = new ArrayList<>();

        private Scene(GameTestHelper helper) {
            this.helper = helper;
            // 同時実行される隣のテストの大型部位をレイや接触判定に拾わないよう、高度を分離する。
            origin = helper.absoluteVec(new Vec3(2.5, 260 + 16 * sceneSequence++, 2.5));
            owner = new FakePlayer(helper.getLevel(), new GameProfile(UUID.randomUUID(), "part_hit_test"));
            owner.gameMode.changeGameModeForPlayer(GameType.SURVIVAL);
            owner.setPos(origin);
            owner.setYRot(-90);
            owner.setYHeadRot(-90);
            owner.setXRot(0);
            owner.setNoGravity(true);
            add(owner);
        }

        private void add(Entity entity) {
            entities.add(entity);
            helper.getLevel().addFreshEntity(entity);
        }

        private EnderDragon dragon() {
            var dragon = Objects.requireNonNull(EntityType.ENDER_DRAGON.create(helper.getLevel()));
            // 本体の大きなAABBを経路から離し、露出させた部位だけへの命中を検証する。
            dragon.setPos(origin.add(8, 20, 8));
            dragon.setNoAi(true);
            dragon.getPhaseManager().setPhase(EnderDragonPhase.HOLDING_PATTERN);
            Objects.requireNonNull(dragon.getAttribute(AttributeRegistry.SPELL_RESIST.get())).setBaseValue(1);
            add(dragon);
            for (var part : dragon.getSubEntities()) part.setPos(origin.add(8, 20, 8));
            return dragon;
        }

        private void exposePart(Entity part, Vec3 offset) {
            part.setPos(origin.add(offset));
        }

        @Override
        public void close() {
            entities.forEach(Entity::discard);
        }
    }
}
