package jp.aquafactory.apprenticecodex.gametest.equipment.bow.arrows.bloody;

import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.registry.SpellRegistry;
import jp.aquafactory.apprenticecodex.spell.bloodyarrow.BloodyArrowEntity;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.List;

import static jp.aquafactory.apprenticecodex.gametest.equipment.bow.arrows.bloody.BloodyArrowOrbsGameTests.*;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class BloodyArrowCombatGameTests {
    private BloodyArrowCombatGameTests() {}

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void successfulHitsDropOrbsAndKillsDoubleThem(GameTestHelper helper) {
        try (var scene = new Scene(helper)) {
            for (int level : List.of(1, 2, 9, 10)) {
                int count = 3 + level / 2;
                var target = scene.zombie(new Vec3(3, 240, 3));
                var arrow = scene.arrow(helper.absoluteVec(new Vec3(0.8, 240.8, 3)), new Vec3(1, 0, 0), 2, count);
                step(arrow);
                helper.assertTrue(target.getHealth() < 20 && arrow.isRemoved() && scene.orbs().size() == count,
                        "A successful living hit must drop the configured count exactly once");
                step(arrow);
                helper.assertTrue(scene.orbs().size() == count, "Removed arrows must not repeat their hit");
                scene.clearOrbs();
                target.discard();

                target = scene.zombie(new Vec3(3, 240, 3));
                target.setHealth(1);
                arrow = scene.arrow(helper.absoluteVec(new Vec3(0.8, 240.8, 3)), new Vec3(1, 0, 0), 10, count);
                step(arrow);
                helper.assertTrue(!target.isAlive() && scene.orbs().size() == count * 2,
                        "A lethal hit must double the configured count");
                scene.clearOrbs();
                target.discard();
            }
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void invulnerabilityTotemAndCrystalHaveDistinctOutcomes(GameTestHelper helper) {
        try (var scene = new Scene(helper)) {
            var target = scene.zombie(new Vec3(3, 240, 3));
            target.setInvulnerable(true);
            var arrow = scene.arrow(helper.absoluteVec(new Vec3(0.8, 240.8, 3)), new Vec3(1, 0, 0), 10, 3);
            step(arrow);
            helper.assertTrue(arrow.isRemoved() && scene.orbs().isEmpty(), "Rejected damage must not create orbs");
            target.discard();
            target = scene.zombie(new Vec3(3, 240, 3));
            target.setHealth(1);
            target.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(Items.TOTEM_OF_UNDYING));
            arrow = scene.arrow(helper.absoluteVec(new Vec3(0.8, 240.8, 3)), new Vec3(1, 0, 0), 10, 3);
            step(arrow);
            helper.assertTrue(target.isAlive() && target.getOffhandItem().isEmpty() && scene.orbs().size() == 3,
                    "Totem survival must keep the normal orb count");
            scene.clearOrbs();
            target.discard();
            var crystal = EntityType.END_CRYSTAL.create(helper.getLevel());
            crystal.setPos(helper.absoluteVec(new Vec3(3, 240, 3)));
            scene.add(crystal);
            // 通常の地形から離れた高度で、実際の破壊経路を確認する。
            crystal.setInvulnerable(false);
            arrow = scene.arrow(helper.absoluteVec(new Vec3(0.8, 240.8, 3)), new Vec3(1, 0, 0), 2, 3);
            step(arrow);
            helper.assertTrue(crystal.isRemoved() && scene.orbs().isEmpty(),
                    "End crystals must remain damageable without producing blood orbs");
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void castingUsesExistingSpellValues(GameTestHelper helper) {
        try (var scene = new Scene(helper)) {
            scene.owner.setPos(helper.absoluteVec(new Vec3(0.8, 240, 3)));
            scene.owner.setYRot(-90);
            scene.owner.setXRot(0);
            var spell = SpellRegistry.BLOODY_ARROW.get();
            var info = spell.getUniqueInfo(10, scene.owner);
            float damage = Float.parseFloat(((TranslatableContents) info.get(0).getContents()).getArgs()[0].toString());
            float healing = Float.parseFloat(((TranslatableContents) info.get(2).getContents()).getArgs()[0].toString());
            var target = scene.zombie(new Vec3(3, 240, 3));
            target.getAttribute(Attributes.MAX_HEALTH).setBaseValue(200);
            target.setHealth(200);
            spell.onCast(helper.getLevel(), 10, scene.owner, CastSource.SPELLBOOK, MagicData.getPlayerMagicData(scene.owner));
            var arrows = helper.getLevel().getEntitiesOfClass(BloodyArrowEntity.class, scene.owner.getBoundingBox().inflate(1));
            helper.assertTrue(arrows.size() == 1, "Casting must spawn exactly one arrow");
            var arrow = arrows.get(0);
            scene.entities.add(arrow);
            helper.assertTrue(arrow.position().distanceTo(scene.owner.getEyePosition().add(1, -0.4, 0)) < 1.0e-6,
                    "Casting must start one block forward and 0.4 blocks below the eye");
            step(arrow);
            helper.assertTrue(Math.abs(200 - target.getHealth() - damage) < 0.02 && scene.orbs().size() == 8,
                    "Casting must use existing damage and level-dependent count");
            var orbs = scene.orbs();
            var orb = orbs.get(0);
            orbs.stream().skip(1).forEach(Entity::discard);
            scene.owner.setHealth(5);
            orb.setPos(scene.owner.position());
            orb.scatter(scene.owner.position(), scene.owner.position());
            orb.setNoGravity(true);
            orb.setDeltaMovement(Vec3.ZERO);
            step(orb);
            helper.assertTrue(Math.abs(scene.owner.getHealth() - 5 - healing) < 0.02,
                    "Orbs must use the healing amount advertised by the spell");
        }
        helper.succeed();
    }
}
