package jp.aquafactory.apprenticecodex.gametest.spell.targeting.multipart;

import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.compat.malum.MalumSpellReaperScytheBridge;
import jp.aquafactory.apprenticecodex.gametest.equipment.scythe.ScytheNarrowGameTests;
import jp.aquafactory.apprenticecodex.gametest.equipment.scythe.ScytheReboundGameTests;
import jp.aquafactory.apprenticecodex.item.spellreaperscythe.ScytheThrowManager;
import jp.aquafactory.apprenticecodex.registry.EntityRegistry;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import jp.aquafactory.apprenticecodex.spell.moonlight.MoonLightKatanaEntity;
import jp.aquafactory.apprenticecodex.spell.quickarms.QuickArmsHandgunEntity;
import jp.aquafactory.apprenticecodex.spell.skyedge.SkyEdgeProjectileEntity;
import jp.aquafactory.apprenticecodex.utility.CombatTools;
import jp.aquafactory.apprenticecodex.utility.RaycastTools;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.Objects;

import static jp.aquafactory.apprenticecodex.gametest.spell.targeting.multipart.DragonDamageGameTests.*;
import static jp.aquafactory.apprenticecodex.gametest.spell.targeting.multipart.DragonTargetFixture.skipDragonTest;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class DragonWeaponContactGameTests {
    private DragonWeaponContactGameTests() {}

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
}
