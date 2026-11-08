package jp.aquafactory.apprenticecodex.gametest.spell.totemofpermafrost;

import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.capabilities.magic.RecastResult;
import jp.aquafactory.apprenticecodex.registry.EntityRegistry;
import jp.aquafactory.apprenticecodex.registry.SpellRegistry;
import jp.aquafactory.apprenticecodex.spell.totemofpermafrost.TotemOfPermafrost;
import jp.aquafactory.apprenticecodex.spell.totemofpermafrost.TotemOfPermafrostTotemEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.FakePlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static jp.aquafactory.apprenticecodex.gametest.support.CurioTestSupport.equipGreaterConjurersTalisman;
import static jp.aquafactory.apprenticecodex.gametest.support.PlayerTestSupport.createEquipmentTestPlayer;

final class TotemOfPermafrostScenarios {
    private TotemOfPermafrostScenarios() {}

    static final UUID TOTEM_OF_PERMAFROST_SUMMON_DAMAGE_TEST_MODIFIER_ID =
            UUID.fromString("7543bb74-30b3-4e0e-9d73-6e4d71d1e218");

    static void totemOfPermafrostCanBePlacedOnSupportedSlab(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var owner = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "totem_permafrost_slab_test");
            owner.setYRot(45.0F);
            var anchorPos = new BlockPos(0, 2, 0);
            helper.setBlock(anchorPos.below(), Blocks.STONE_SLAB.defaultBlockState()
                    .setValue(SlabBlock.TYPE, SlabType.BOTTOM));

            castTotemOfPermafrost(helper, owner, 1, anchorPos);

            var totem = getSingleLivingTotemOfPermafrost(helper, owner);
            var expectedY = helper.absolutePos(anchorPos).below().getY() + 0.5D;
            helper.assertTrue(Math.abs(totem.getY() - expectedY) < 0.01D,
                    "TotemOfPermafrost should sit on the slab support top: " + totem.getY());
            helper.assertTrue(Mth.degreesDifferenceAbs(totem.getYRot(), owner.getYRot() + 180.0F) < 0.01F,
                    "TotemOfPermafrost should face back toward the caster: " + totem.getYRot());
        });
    }

    static void totemOfPermafrostFallsWhenSupportRemoved(GameTestHelper helper) {
        var owner = createEquipmentTestPlayer(helper, new BlockPos(0, 6, 0), "totem_permafrost_fall_test");
        var anchorPos = new BlockPos(0, 6, 0);
        helper.setBlock(anchorPos.below(), Blocks.STONE);
        helper.getLevel().addFreshEntity(owner);
        castTotemOfPermafrost(helper, owner, 1, anchorPos);
        var totem = getSingleLivingTotemOfPermafrost(helper, owner);
        var initialY = totem.getY();

        helper.runAtTickTime(1, () -> helper.setBlock(anchorPos.below(), Blocks.AIR));

        helper.succeedWhen(() -> {
            helper.assertTrue(totem.isAlive() && !totem.isRemoved(),
                    "TotemOfPermafrost should remain alive while falling after support removal");
            helper.assertTrue(totem.getY() < initialY - 0.05D,
                    "TotemOfPermafrost should fall below its anchored position after support removal: " + totem.getY());
        });
    }

    static void totemOfPermafrostInvalidPlacementCreatesNoRecast(GameTestHelper helper) {
        var owner = createEquipmentTestPlayer(helper, new BlockPos(0, 10, 0), "totem_permafrost_invalid_test");
        owner.setXRot(-90.0F);
        var magicData = MagicData.getPlayerMagicData(owner);
        var anchorPos = new BlockPos(0, 5, 0);
        helper.setBlock(anchorPos.below(), Blocks.AIR);

        castTotemOfPermafrost(helper, owner, 1, anchorPos);

        helper.assertTrue(getOwnedTotemOfPermafrost(helper, owner).isEmpty(),
                "TotemOfPermafrost should not spawn without support");
        helper.assertTrue(!magicData.getPlayerRecasts().hasRecastForSpell(SpellRegistry.TOTEM_OF_PERMAFROST.get()),
                "TotemOfPermafrost should not create recast data when placement fails");
        helper.succeed();
    }

    static void totemOfPermafrostRecastRemovesPlacedTotem(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var owner = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "totem_permafrost_recast_remove_test");
            var anchorPos = new BlockPos(0, 2, 0);
            helper.setBlock(anchorPos.below(), Blocks.STONE);
            castTotemOfPermafrost(helper, owner, 1, anchorPos);
            var magicData = MagicData.getPlayerMagicData(owner);
            helper.assertTrue(magicData.getPlayerRecasts().hasRecastForSpell(SpellRegistry.TOTEM_OF_PERMAFROST.get()),
                    "TotemOfPermafrost should create recast data after placement");

            ((TotemOfPermafrost) SpellRegistry.TOTEM_OF_PERMAFROST.get())
                    .onCast(helper.getLevel(), 1, owner, CastSource.SPELLBOOK, magicData);

            helper.assertTrue(getOwnedTotemOfPermafrost(helper, owner).isEmpty(),
                    "TotemOfPermafrost recast should remove the placed totem");
        });
    }

    static void totemOfPermafrostMissingTotemRecastIsNoop(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var owner = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "totem_permafrost_missing_recast_test");
            var anchorPos = new BlockPos(0, 2, 0);
            helper.setBlock(anchorPos.below(), Blocks.STONE);
            castTotemOfPermafrost(helper, owner, 1, anchorPos);
            var totem = getSingleLivingTotemOfPermafrost(helper, owner);
            totem.discard();

            var magicData = MagicData.getPlayerMagicData(owner);
            ((TotemOfPermafrost) SpellRegistry.TOTEM_OF_PERMAFROST.get())
                    .onCast(helper.getLevel(), 1, owner, CastSource.SPELLBOOK, magicData);

            helper.assertTrue(getOwnedTotemOfPermafrost(helper, owner).isEmpty(),
                    "TotemOfPermafrost missing recast should not recreate or require the old totem");
        });
    }

    static void totemOfPermafrostTimeoutRemovesPlacedTotem(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var owner = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "totem_permafrost_timeout_test");
            var anchorPos = new BlockPos(0, 2, 0);
            helper.setBlock(anchorPos.below(), Blocks.STONE);
            castTotemOfPermafrost(helper, owner, 1, anchorPos);

            var magicData = MagicData.getPlayerMagicData(owner);
            var recast = magicData.getPlayerRecasts().getRecastInstance(SpellRegistry.TOTEM_OF_PERMAFROST.get().getSpellId());
            helper.assertTrue(recast != null, "TotemOfPermafrost should have active recast data before timeout");

            magicData.getPlayerRecasts().removeRecast(recast, RecastResult.TIMEOUT);

            helper.assertTrue(getOwnedTotemOfPermafrost(helper, owner).isEmpty(),
                    "TotemOfPermafrost timeout should remove the placed totem");
        });
    }

    static void totemOfPermafrostGreaterConjurersTalismanKeepsTimeoutCooldown(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var owner = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "totem_permafrost_talisman_timeout_test");
            equipGreaterConjurersTalisman(owner);
            var anchorPos = new BlockPos(0, 2, 0);
            helper.setBlock(anchorPos.below(), Blocks.STONE);
            castTotemOfPermafrost(helper, owner, 1, anchorPos);

            var spell = SpellRegistry.TOTEM_OF_PERMAFROST.get();
            var magicData = MagicData.getPlayerMagicData(owner);
            var recast = magicData.getPlayerRecasts().getRecastInstance(spell.getSpellId());
            helper.assertTrue(recast != null, "TotemOfPermafrost should have active recast data before talisman timeout");

            magicData.getPlayerRecasts().removeRecast(recast, RecastResult.TIMEOUT);

            helper.assertTrue(getOwnedTotemOfPermafrost(helper, owner).isEmpty(),
                    "TotemOfPermafrost talisman timeout should still remove the placed totem");
            helper.assertFalse(magicData.getPlayerRecasts().hasRecastForSpell(spell),
                    "TotemOfPermafrost talisman timeout should remove the active recast");
            helper.assertTrue(magicData.getPlayerCooldowns().isOnCooldown(spell),
                    "TotemOfPermafrost timeout should apply cooldown even with Greater Conjurer's Talisman");
        });
    }

    static void totemOfPermafrostGreaterConjurersTalismanKeepsManualRecastCooldown(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var owner = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "totem_permafrost_talisman_manual_test");
            equipGreaterConjurersTalisman(owner);
            var anchorPos = new BlockPos(0, 2, 0);
            helper.setBlock(anchorPos.below(), Blocks.STONE);
            castTotemOfPermafrost(helper, owner, 1, anchorPos);

            var spell = (TotemOfPermafrost) SpellRegistry.TOTEM_OF_PERMAFROST.get();
            var magicData = MagicData.getPlayerMagicData(owner);
            spell.castSpell(helper.getLevel(), 1, owner, CastSource.SPELLBOOK, true);

            helper.assertTrue(getOwnedTotemOfPermafrost(helper, owner).isEmpty(),
                    "TotemOfPermafrost manual recast with talisman should remove the placed totem");
            helper.assertFalse(magicData.getPlayerRecasts().hasRecastForSpell(spell),
                    "TotemOfPermafrost manual recast with talisman should remove the active recast");
            helper.assertTrue(magicData.getPlayerCooldowns().isOnCooldown(spell),
                    "TotemOfPermafrost manual recast should apply cooldown even with Greater Conjurer's Talisman");
        });
    }

    static void totemOfPermafrostPulseUsesSummonDamageAttribute(GameTestHelper helper) {
        var level = helper.getLevel();
        var owner = createEquipmentTestPlayer(helper, new BlockPos(0, 2, -3), "totem_permafrost_summon_damage_test");
        level.addFreshEntity(owner);
        var anchorPos = new BlockPos(0, 2, 0);
        helper.setBlock(anchorPos.below(), Blocks.STONE);

        var baseDamageTotem = createTotemOfPermafrostTestEntity(level, owner, helper.absolutePos(anchorPos), 4.0F, 0);
        var baseTarget = helper.spawn(EntityType.ZOMBIE, new BlockPos(1, 2, 0));
        baseTarget.setNoAi(true);
        var baseTargetHealth = baseTarget.getHealth();
        pulseTotemOfPermafrostForGameTest(helper, baseDamageTotem, owner);
        var baseDamage = baseTargetHealth - baseTarget.getHealth();
        baseDamageTotem.discard();
        baseTarget.discard();

        owner.getAttribute(AttributeRegistry.SUMMON_DAMAGE.get()).addTransientModifier(new AttributeModifier(
                TOTEM_OF_PERMAFROST_SUMMON_DAMAGE_TEST_MODIFIER_ID,
                "TotemOfPermafrost Summon Damage test",
                0.5D,
                AttributeModifier.Operation.MULTIPLY_BASE
        ));

        var boostedDamageTotem = createTotemOfPermafrostTestEntity(level, owner, helper.absolutePos(anchorPos), 4.0F, 0);
        var boostedTarget = helper.spawn(EntityType.ZOMBIE, new BlockPos(1, 2, 0));
        boostedTarget.setNoAi(true);
        var boostedTargetHealth = boostedTarget.getHealth();
        pulseTotemOfPermafrostForGameTest(helper, boostedDamageTotem, owner);
        var boostedDamage = boostedTargetHealth - boostedTarget.getHealth();

        helper.assertTrue(baseDamage > 0.0F, "TotemOfPermafrost pulse should damage the baseline target");
        helper.assertTrue(boostedDamage > baseDamage + 0.1F,
                "TotemOfPermafrost pulse should scale with Summon Damage: " + boostedDamage + " <= " + baseDamage);
        helper.succeed();
    }

    static void totemOfPermafrostPulseHitsVisibleCombatTargetsOnly(GameTestHelper helper) {
        var level = helper.getLevel();
        var owner = createEquipmentTestPlayer(helper, new BlockPos(0, 2, -3), "totem_permafrost_pulse_test");
        var anchorPos = new BlockPos(0, 2, 0);
        helper.setBlock(anchorPos.below(), Blocks.STONE);
        level.addFreshEntity(owner);

        var totem = createTotemOfPermafrostTestEntity(level, owner, helper.absolutePos(anchorPos), 100.0F, 2);
        var visibleTarget = helper.spawn(EntityType.ZOMBIE, new BlockPos(2, 2, 0));
        var blockedTarget = helper.spawn(EntityType.ZOMBIE, new BlockPos(3, 2, 1));
        var outsideTarget = helper.spawn(EntityType.ZOMBIE, new BlockPos(6, 2, 0));
        visibleTarget.setNoAi(true);
        blockedTarget.setNoAi(true);
        outsideTarget.setNoAi(true);
        helper.setBlock(new BlockPos(2, 2, 1), Blocks.STONE);
        helper.setBlock(new BlockPos(2, 3, 1), Blocks.STONE);

        var visibleHealth = visibleTarget.getHealth();
        var blockedHealth = blockedTarget.getHealth();
        var outsideHealth = outsideTarget.getHealth();

        helper.runAtTickTime(1, () -> {
            pulseTotemOfPermafrostForGameTest(helper, totem, owner);

            helper.assertTrue(!totem.isRemoved(), "TotemOfPermafrost test totem should still exist before pulse assertions");
            helper.assertTrue(visibleTarget.getHealth() < visibleHealth,
                    "TotemOfPermafrost pulse should damage a visible target");
            helper.assertTrue(visibleTarget.getTicksFrozen() >= 40,
                    "TotemOfPermafrost pulse should increase frozen ticks after successful damage");
            var slowness = visibleTarget.getEffect(MobEffects.MOVEMENT_SLOWDOWN);
            helper.assertTrue(slowness != null && slowness.getAmplifier() == 2,
                    "TotemOfPermafrost pulse should apply configured slowness after successful damage");
            helper.assertTrue(Math.abs(blockedTarget.getHealth() - blockedHealth) < 1.0E-4F,
                    "TotemOfPermafrost pulse should not damage blocked targets");
            helper.assertTrue(blockedTarget.getEffect(MobEffects.MOVEMENT_SLOWDOWN) == null,
                    "TotemOfPermafrost pulse should not slow blocked targets");
            helper.assertTrue(Math.abs(outsideTarget.getHealth() - outsideHealth) < 1.0E-4F,
                    "TotemOfPermafrost pulse should not damage targets outside the AABB");
            helper.succeed();
        });
    }

    static void pulseTotemOfPermafrostForGameTest(
            GameTestHelper helper,
            TotemOfPermafrostTotemEntity totem,
            FakePlayer owner
    ) {
        try {
            var method = TotemOfPermafrostTotemEntity.class.getDeclaredMethod(
                    "pulse",
                    ServerLevel.class,
                    LivingEntity.class
            );
            method.setAccessible(true);
            method.invoke(totem, helper.getLevel(), owner);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Failed to invoke TotemOfPermafrost pulse for GameTest", exception);
        }
    }

    static TotemOfPermafrostTotemEntity createTotemOfPermafrostTestEntity(
            ServerLevel level,
            FakePlayer owner,
            BlockPos absoluteAnchorPos,
            float damage,
            int slownessAmplifier
    ) {
        var center = Vec3.atBottomCenterOf(absoluteAnchorPos);
        var totem = new TotemOfPermafrostTotemEntity(EntityRegistry.TOTEM_OF_PERMAFROST_TOTEM.get(), level);
        totem.setOwner(owner);
        totem.setAnchorPos(absoluteAnchorPos);
        totem.setDamage(damage);
        totem.setSlownessAmplifier(slownessAmplifier);
        totem.setRadius(3.0D);
        totem.moveTo(center.x, center.y, center.z, 0.0F, 0.0F);
        level.addFreshEntity(totem);
        return totem;
    }

    static void castTotemOfPermafrost(GameTestHelper helper, FakePlayer player, int spellLevel, BlockPos anchorPos) {
        var spell = (TotemOfPermafrost) SpellRegistry.TOTEM_OF_PERMAFROST.get();
        var castData = new TotemOfPermafrost.TotemOfPermafrostCastData();
        var absoluteAnchorPos = helper.absolutePos(anchorPos);
        var tag = new CompoundTag();
        tag.putInt("PositionX", absoluteAnchorPos.getX());
        tag.putInt("PositionY", absoluteAnchorPos.getY());
        tag.putInt("PositionZ", absoluteAnchorPos.getZ());
        castData.deserializeNBT(tag);
        var magicData = MagicData.getPlayerMagicData(player);
        magicData.setAdditionalCastData(castData);
        spell.onCast(helper.getLevel(), spellLevel, player, CastSource.SPELLBOOK, magicData);
    }

    static List<TotemOfPermafrostTotemEntity> getOwnedTotemOfPermafrost(GameTestHelper helper, FakePlayer owner) {
        var totems = new ArrayList<TotemOfPermafrostTotemEntity>();
        for (var entity : helper.getLevel().getAllEntities()) {
            if (entity instanceof TotemOfPermafrostTotemEntity totem
                    && totem.isAlive()
                    && totem.getOwner() != null
                    && owner.getUUID().equals(totem.getOwner().getUUID())) {
                totems.add(totem);
            }
        }
        return totems;
    }

    static TotemOfPermafrostTotemEntity getSingleLivingTotemOfPermafrost(GameTestHelper helper, FakePlayer owner) {
        var totems = getOwnedTotemOfPermafrost(helper, owner);
        helper.assertTrue(totems.size() == 1, "Expected exactly one living TotemOfPermafrost but found " + totems.size());
        return totems.get(0);
    }
}
