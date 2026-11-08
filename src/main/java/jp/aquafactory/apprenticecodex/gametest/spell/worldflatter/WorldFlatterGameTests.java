package jp.aquafactory.apprenticecodex.gametest.spell.worldflatter;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.damage.DamageTypes;
import jp.aquafactory.apprenticecodex.datagen.DamageTypeTagGenerator;
import jp.aquafactory.apprenticecodex.gametest.support.MiningTestScope;
import jp.aquafactory.apprenticecodex.registry.EffectRegistry;
import jp.aquafactory.apprenticecodex.registry.EntityRegistry;
import jp.aquafactory.apprenticecodex.spell.worldflatter.WorldFlatterDrillEntity;
import jp.aquafactory.apprenticecodex.utility.CombatTools;
import jp.aquafactory.apprenticecodex.utility.RaycastTools;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import static jp.aquafactory.apprenticecodex.gametest.support.PlayerTestSupport.createEquipmentTestPlayer;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class WorldFlatterGameTests {
    private WorldFlatterGameTests() {}

    @GameTest(template = "gametest/casting_mining")
    public static void worldFlatterPenetratedArmorEffectAndDamageTags(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var scope = new MiningTestScope(helper);
            var player = scope.track(createEquipmentTestPlayer(helper, new BlockPos(4, 2, 4), "world_flatter_penetrated_armor_test"));
            var armor = player.getAttribute(Attributes.ARMOR);
            var toughness = player.getAttribute(Attributes.ARMOR_TOUGHNESS);
            helper.assertTrue(armor != null, "Player is missing armor attribute");
            helper.assertTrue(toughness != null, "Player is missing armor toughness attribute");

            armor.setBaseValue(10.0D);
            toughness.setBaseValue(8.0D);
            player.addEffect(new MobEffectInstance(EffectRegistry.PENETRATED_ARMOR, 100, 0));
            helper.assertTrue(Math.abs(player.getAttributeValue(Attributes.ARMOR) - 8.0D) < 1.0E-6D,
                    "Penetrated Armor I should reduce armor by 20%");
            helper.assertTrue(Math.abs(player.getAttributeValue(Attributes.ARMOR_TOUGHNESS)) < 1.0E-6D,
                    "Penetrated Armor should reduce armor toughness by 100%");

            player.removeEffect(EffectRegistry.PENETRATED_ARMOR);
            player.addEffect(new MobEffectInstance(EffectRegistry.PENETRATED_ARMOR, 100, 3));
            helper.assertTrue(Math.abs(player.getAttributeValue(Attributes.ARMOR) - 2.0D) < 1.0E-6D,
                    "Penetrated Armor IV should reduce armor by 80%");
            helper.assertTrue(Math.abs(player.getAttributeValue(Attributes.ARMOR_TOUGHNESS)) < 1.0E-6D,
                    "Penetrated Armor toughness reduction should not depend on amplifier");

            player.removeEffect(EffectRegistry.PENETRATED_ARMOR);
            helper.assertTrue(Math.abs(player.getAttributeValue(Attributes.ARMOR) - 10.0D) < 1.0e-6D
                            && Math.abs(player.getAttributeValue(Attributes.ARMOR_TOUGHNESS) - 8.0D) < 1.0e-6D,
                    "Removing Penetrated Armor must restore armor and toughness");

            var source = CombatTools.getDamageSource(
                    helper.getLevel(),
                    player,
                    DamageTypes.WORLD_FLATTER
            );
            helper.assertTrue(source.is(DamageTypes.WORLD_FLATTER),
                    "World Flatter damage source should use apprenticecodex:world_flatter");
            helper.assertTrue(!source.is(DamageTypeTagGenerator.BYPASSES_IFRAME),
                    "World Flatter should no longer use apprenticecodex:bypasses_iframe");
            helper.assertTrue(!source.is(DamageTypeTags.BYPASSES_COOLDOWN),
                    "World Flatter should no longer bypass vanilla cooldown i-frame");
        });
    }

    @GameTest(template = "gametest/casting_mining", batch = "apprenticecodex.mining_spell_isolated")
    public static void worldFlatterBlockTargetFilterMatchesPickaxeOrShovel(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var scope = new MiningTestScope(helper);
            var level = helper.getLevel();
            var player = scope.track(createEquipmentTestPlayer(helper, new BlockPos(4, 2, 4), "world_flatter_block_filter_test"));
            var pos = helper.absolutePos(new BlockPos(4, 2, 4));

            helper.assertTrue(WorldFlatterDrillEntity.canBreakTarget(
                            level, player, pos, Blocks.STONE.defaultBlockState(), Blocks.STONE.defaultBlockState()),
                    "World Flatter should target pickaxe-mineable stone");
            helper.assertTrue(WorldFlatterDrillEntity.canBreakTarget(
                            level, player, pos, Blocks.DIRT.defaultBlockState(), Blocks.DIRT.defaultBlockState()),
                    "World Flatter should target shovel-mineable dirt");
            helper.assertFalse(WorldFlatterDrillEntity.canBreakTarget(
                            level, player, pos, Blocks.GLASS.defaultBlockState(), Blocks.GLASS.defaultBlockState()),
                    "World Flatter should reject glass because it has no specific pickaxe/shovel tool tag");
            helper.assertFalse(WorldFlatterDrillEntity.canBreakTarget(
                            level, player, pos, Blocks.OAK_LOG.defaultBlockState(), Blocks.OAK_LOG.defaultBlockState()),
                    "World Flatter should reject axe-mineable logs");
            helper.assertFalse(WorldFlatterDrillEntity.canBreakTarget(
                            level, player, pos, Blocks.BEDROCK.defaultBlockState(), Blocks.BEDROCK.defaultBlockState()),
                    "World Flatter should reject unbreakable blocks");
            helper.assertFalse(WorldFlatterDrillEntity.canBreakTarget(
                            level, player, pos, Blocks.DIAMOND_ORE.defaultBlockState(), Blocks.STONE.defaultBlockState()),
                    "World Flatter should not splash unrelated ore blocks from a non-ore center");
        });
    }

    @GameTest(template = "gametest/casting_mining")
    public static void worldFlatterEntityAttackRequiresArrivalAndHitsSingleTarget(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var scope = new MiningTestScope(helper);
            var level = helper.getLevel();
            var owner = scope.track(createEquipmentTestPlayer(helper, new BlockPos(4, 2, 4), "world_flatter_single_target_owner"));
            owner.setYRot(0.0F);
            owner.setXRot(0.0F);

            var target = scope.capture(() -> helper.spawn(EntityType.SHEEP, new BlockPos(4, 2, 8)));
            var bystander = scope.capture(() -> helper.spawn(EntityType.SHEEP, new BlockPos(5, 2, 8)));
            target.setNoAi(true);
            bystander.setNoAi(true);
            var targetHealth = target.getHealth();
            var bystanderHealth = bystander.getHealth();

            var weapon = scope.track(new WorldFlatterDrillEntity(EntityRegistry.WORLD_FLATTER_DRILL.get(), level, owner));
            weapon.setDamage(4.0F);
            weapon.setPenetratedArmorAmplifier(1);
            weapon.setToolSpeed(4.0F);
            weapon.updateOwnerTarget(level, new RaycastTools.TargetResult(
                    RaycastTools.TargetType.LIVING_ENTITY,
                    target.getBoundingBox().getCenter(),
                    target,
                    null
            ));

            for (var i = 0; i < 14; ++i) {
                target.setPos(target.getX() + 0.08D, target.getY(), target.getZ());
                scope.run(() -> weapon.tickOnServer(level));
            }
            helper.assertTrue(Math.abs(target.getHealth() - targetHealth) < 1.0E-6F,
                    "World Flatter should not damage an entity before the 15 tick attach completes");

            target.setPos(target.getX() + 0.08D, target.getY(), target.getZ());
            scope.run(() -> weapon.tickOnServer(level));
            helper.assertTrue(target.getHealth() < targetHealth,
                    "World Flatter should damage the attached moving target after 15 ticks");
            helper.assertTrue(target.hasEffect(EffectRegistry.PENETRATED_ARMOR),
                    "World Flatter should apply Penetrated Armor after successful damage");
            helper.assertTrue(Math.abs(bystander.getHealth() - bystanderHealth) < 1.0E-6F,
                    "World Flatter should not damage nearby non-target entities");
        });
    }
}
