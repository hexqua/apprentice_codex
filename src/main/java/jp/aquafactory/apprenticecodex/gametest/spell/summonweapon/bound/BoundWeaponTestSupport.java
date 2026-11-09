package jp.aquafactory.apprenticecodex.gametest.spell.summonweapon.bound;

import io.redspace.ironsspellbooks.compat.Curios;
import io.redspace.ironsspellbooks.registries.ItemRegistry;
import jp.aquafactory.apprenticecodex.registry.SpellRegistry;
import jp.aquafactory.apprenticecodex.spell.boundbow.BoundBow;
import jp.aquafactory.apprenticecodex.spell.boundsword.BoundSword;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.util.FakePlayer;
import top.theillusivec4.curios.api.CuriosApi;

import java.util.UUID;

final class BoundWeaponTestSupport {
    private BoundWeaponTestSupport() {}

    static int countArrows(GameTestHelper helper, FakePlayer player) {
        return helper.getLevel()
                .getEntitiesOfClass(AbstractArrow.class, new AABB(player.position(), player.position()).inflate(16.0D))
                .size();
    }

    static AbstractArrow getSingleArrow(GameTestHelper helper, FakePlayer player) {
        var arrows = helper.getLevel()
                .getEntitiesOfClass(AbstractArrow.class, new AABB(player.position(), player.position()).inflate(16.0D));
        helper.assertTrue(arrows.size() == 1, "Expected exactly one Bound Bow arrow but found " + arrows.size());
        return arrows.get(0);
    }

    static void equipGreaterConjurersTalisman(FakePlayer player) {
        var curiosInventory = CuriosApi.getCuriosInventory(player)
                .orElseThrow(() -> new IllegalStateException("Missing curios inventory for Bound Bow Greater Conjurer's Talisman test"));
        curiosInventory.setEquippedCurio(Curios.NECKLACE_SLOT, 0,
                new ItemStack(ItemRegistry.GREATER_CONJURERS_TALISMAN.get()));
    }

    static BoundBow boundBow() {
        return (BoundBow) SpellRegistry.BOUND_BOW.get();
    }

    static final UUID SUMMON_DAMAGE_TEST_MODIFIER_ID = UUID.fromString("ed5c2a3e-bf71-4c3f-893f-0715868e4c3a");

    static BoundSword boundSword() {
        return (BoundSword) SpellRegistry.BOUND_SWORD.get();
    }
}
