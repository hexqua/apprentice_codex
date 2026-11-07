package jp.aquafactory.apprenticecodex.gametest.spell.targeting.multipart;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.config.ApprenticeCodexServerConfig;
import jp.aquafactory.apprenticecodex.damage.DamageTypes;
import jp.aquafactory.apprenticecodex.item.multicastechostaff.MulticastEchoStaffAttackHandler;
import jp.aquafactory.apprenticecodex.item.multicastechostaff.MulticastEchoStaffAttackProfile;
import jp.aquafactory.apprenticecodex.item.multicastechostaff.MulticastEchoStaffAttackProfileManager;
import jp.aquafactory.apprenticecodex.item.spellreaperscythe.ScytheThrowDamage;
import jp.aquafactory.apprenticecodex.item.spellreaperscythe.ScytheThrowEntity;
import jp.aquafactory.apprenticecodex.mixin.LivingEntityDamageMemoryAccessor;
import jp.aquafactory.apprenticecodex.registry.EntityRegistry;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import jp.aquafactory.apprenticecodex.registry.SpellRegistry;
import jp.aquafactory.apprenticecodex.spell.moonlight.MoonLightChargeCutEntity;
import jp.aquafactory.apprenticecodex.utility.CombatTools;
import jp.aquafactory.apprenticecodex.utility.RaycastTools;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.Map;

import static jp.aquafactory.apprenticecodex.gametest.spell.targeting.multipart.DragonTargetFixture.*;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class DragonDamageMemoryGameTests {
    private DragonDamageMemoryGameTests() {
    }

    @GameTest(template = TEMPLATE)
    public static void scytheDamagePairUsesHeadDamageAndKeepsParentMemory(GameTestHelper helper) {
        if (skipDragonTest(helper)) return;
        try (var scene = new DragonTargetFixture(helper)) {
            scene.owner.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ItemRegistry.SPELL_REAPER_SCYTHE.get()));
            var projectile = new ScytheThrowEntity(EntityRegistry.SCYTHE_THROW.get(), helper.getLevel());
            projectile.setPos(scene.origin);
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
        try (var scene = new DragonTargetFixture(helper)) {
            var weapon = new ItemStack(ItemRegistry.SPELL_REAPER_SCYTHE.get());
            scene.owner.setItemInHand(InteractionHand.MAIN_HAND, weapon);
            var projectile = new ScytheThrowEntity(EntityRegistry.SCYTHE_THROW.get(), helper.getLevel());
            projectile.setPos(scene.origin);
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

    @GameTest(template = TEMPLATE, batch = "apprenticecodex.multicast_echo_staff_isolated")
    public static void repeatedPartDamageAdjustsParentIframe(GameTestHelper helper) {
        if (skipDragonTest(helper)) return;
        var spell = SpellRegistry.LETHAL_ASSAULT.get();
        var profile = new MulticastEchoStaffAttackProfile(0.25, true, true, true, 100, 2);
        try (var scene = new DragonTargetFixture(helper);
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
        try (var scene = new DragonTargetFixture(helper)) {
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
        try (var scene = new DragonTargetFixture(helper)) {
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
                    scene.scope.step(cut, true);
                }
            }
            assertDamage(helper, dragon, 20);
        }
        helper.succeed();
    }
}
