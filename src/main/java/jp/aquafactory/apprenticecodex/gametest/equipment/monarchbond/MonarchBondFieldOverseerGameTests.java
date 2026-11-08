package jp.aquafactory.apprenticecodex.gametest.equipment.monarchbond;

import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.gametest.spell.fieldoverseer.FieldOverseerTestSupport;
import jp.aquafactory.apprenticecodex.gametest.support.DeployedSpellTestFixture;
import jp.aquafactory.apprenticecodex.item.curios.CuriosSlotConstants;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import static jp.aquafactory.apprenticecodex.gametest.equipment.monarchbond.MonarchBondTestSupport.EPSILON;
import static jp.aquafactory.apprenticecodex.gametest.support.CurioTestSupport.equipCurio;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class MonarchBondFieldOverseerGameTests {
    private MonarchBondFieldOverseerGameTests() {
    }

    @GameTest(template = DeployedSpellTestFixture.TEMPLATE)
    public static void monarchBondFieldOverseerManaRangeExtendsWhenCharmIsEquipped(GameTestHelper helper) {
        verifyFieldOverseerManaRange(helper, true, 80.0F, 0.0F);
    }

    @GameTest(template = DeployedSpellTestFixture.TEMPLATE)
    public static void monarchBondFieldOverseerManaRangeRemainsEightBlocksWithoutCharm(GameTestHelper helper) {
        verifyFieldOverseerManaRange(helper, false, 60.0F, 20.0F);
    }

    private static void verifyFieldOverseerManaRange(
            GameTestHelper helper,
            boolean equipCharm,
            float expectedStaffMana,
            float expectedOwnerMana
    ) {
        var profileName = equipCharm
                ? "monarch_bond_field_overseer_extended"
                : "monarch_bond_field_overseer_base";
        var owner = DeployedSpellTestFixture.of(helper).player(new BlockPos(16, 2, 0), profileName);
        if (equipCharm) {
            equipCurio(owner, CuriosSlotConstants.CHARM, new ItemStack(ItemRegistry.MONARCH_BOND_CHARM.get()));
        }
        var manaRegen = owner.getAttribute(AttributeRegistry.MANA_REGEN);
        if (manaRegen != null) {
            manaRegen.setBaseValue(0.0D);
        }
        var ownerMagicData = MagicData.getPlayerMagicData(owner);
        ownerMagicData.setMana(20.0F);
        var anchorPos = DeployedSpellTestFixture.of(helper).absolute(new BlockPos(0, 2, 0));
        DeployedSpellTestFixture.of(helper).setBlock(new BlockPos(0, 1, 0), Blocks.STONE);
        var staff = FieldOverseerTestSupport.create(helper, owner, anchorPos, 100.0F, 40);
        DeployedSpellTestFixture.of(helper).mob(EntityType.HUSK, new BlockPos(0, 2, 3));

        helper.runAtTickTime(55, () -> {
            helper.assertTrue(Math.abs(staff.getCurrentMana() - expectedStaffMana) <= EPSILON,
                    "Field Overseer mana transfer range should depend on Monarch Bond: expected="
                            + expectedStaffMana + ", actual=" + staff.getCurrentMana());
            helper.assertTrue(Math.abs(ownerMagicData.getMana() - expectedOwnerMana) <= EPSILON,
                    "Field Overseer should only drain mana within its effective transfer range: expected="
                            + expectedOwnerMana + ", actual=" + ownerMagicData.getMana());
            helper.succeed();
        });
    }
}
