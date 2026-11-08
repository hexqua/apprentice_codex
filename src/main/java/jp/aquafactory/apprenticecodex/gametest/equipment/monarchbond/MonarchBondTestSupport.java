package jp.aquafactory.apprenticecodex.gametest.equipment.monarchbond;

import io.redspace.ironsspellbooks.capabilities.magic.SummonManager;
import jp.aquafactory.apprenticecodex.gametest.support.DeployedSpellTestFixture;
import jp.aquafactory.apprenticecodex.item.curios.CuriosSlotConstants;
import jp.aquafactory.apprenticecodex.item.curios.monarchbondcharm.MonarchBondCharm;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import jp.aquafactory.apprenticecodex.spell.autoturret.AutoTurretEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.util.FakePlayer;
import top.theillusivec4.curios.api.CuriosApi;

import static jp.aquafactory.apprenticecodex.gametest.support.CurioTestSupport.equipCurio;

final class MonarchBondTestSupport {
    static final float EPSILON = 1.0E-4F;

    static FakePlayer createWearer(GameTestHelper helper, String profileName) {
        var wearer = DeployedSpellTestFixture.of(helper).player(new BlockPos(0, 2, 0), profileName);
        equipCurio(wearer, CuriosSlotConstants.CHARM, new ItemStack(ItemRegistry.MONARCH_BOND_CHARM.get()));
        return wearer;
    }

    static LivingEntity createManagedSummon(
            GameTestHelper helper,
            FakePlayer owner,
            BlockPos position,
            float missingHealth
    ) {
        var summon = DeployedSpellTestFixture.of(helper).mob(EntityType.HUSK, position);
        summon.setHealth(Math.max(1.0F, summon.getMaxHealth() - missingHealth));
        SummonManager.setOwner(summon, owner);
        return summon;
    }

    static AutoTurretEntity createAutoTurret(GameTestHelper helper, FakePlayer owner, BlockPos localPos, int bullets, int manaCost) {
        return DeployedSpellTestFixture.of(helper).turret(owner, localPos, bullets, manaCost);
    }

    static void tickCharmAtRestockInterval(FakePlayer wearer) {
        var slotResult = CuriosApi.getCuriosInventory(wearer)
                .flatMap(inventory -> inventory.findFirstCurio(ItemRegistry.MONARCH_BOND_CHARM.get()))
                .orElseThrow(() -> new IllegalStateException("Missing equipped Monarch Bond Charm for GameTest"));
        wearer.tickCount = 20;
        ((MonarchBondCharm) slotResult.stack().getItem()).curioTick(slotResult.slotContext(), slotResult.stack());
    }

    static void assertHealth(GameTestHelper helper, LivingEntity entity, float expected, String message) {
        helper.assertTrue(Math.abs(entity.getHealth() - expected) <= EPSILON,
                message + ": expected=" + expected + ", actual=" + entity.getHealth());
    }
}
