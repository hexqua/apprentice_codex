package jp.aquafactory.apprenticecodex.gametest.equipment.monarchbond;

import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.gametest.support.GameTestFixtureSupport;
import jp.aquafactory.apprenticecodex.item.curios.CuriosSlotConstants;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import static jp.aquafactory.apprenticecodex.gametest.equipment.EquipmentTestBatches.TEMPLATE;
import static jp.aquafactory.apprenticecodex.gametest.equipment.monarchbond.MonarchBondLegacyScenarios.createFieldOverseerTestEntity;
import static jp.aquafactory.apprenticecodex.gametest.equipment.monarchbond.MonarchBondTestSupport.EPSILON;
import static jp.aquafactory.apprenticecodex.gametest.support.CurioTestSupport.equipCurio;
import static jp.aquafactory.apprenticecodex.gametest.support.PlayerTestSupport.createEquipmentTestPlayer;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class MonarchBondFieldOverseerGameTests {
    private MonarchBondFieldOverseerGameTests() {}

    @GameTest(template = TEMPLATE)
    public static void monarchBondFieldOverseerManaRangeRemainsEightBlocksWithoutCharm(GameTestHelper helper) {
        MonarchBondTestSupport.fieldOverseerManaRangeRemainsEightBlocksWithoutCharm(helper);
    }

    static void verifyFieldOverseerManaRange(
            GameTestHelper helper,
            boolean equipCharm,
            float expectedStaffMana,
            float expectedOwnerMana
    ) {
        GameTestFixtureSupport.whenEntityChunksReady(helper, new BlockPos(-4, 0, -4), new BlockPos(20, 0, 8), () -> {
            var profileName = equipCharm
                    ? "monarch_bond_field_overseer_extended"
                    : "monarch_bond_field_overseer_base";
            var owner = createEquipmentTestPlayer(helper, new BlockPos(16, 2, 0), profileName);
            if (equipCharm) {
                equipCurio(owner, CuriosSlotConstants.CHARM, new ItemStack(ItemRegistry.MONARCH_BOND_CHARM.get()));
            }
            var manaRegen = owner.getAttribute(AttributeRegistry.MANA_REGEN.get());
            if (manaRegen != null) {
                manaRegen.setBaseValue(0.0D);
            }
            var ownerMagicData = MagicData.getPlayerMagicData(owner);
            ownerMagicData.setMana(20.0F);
            var anchorPos = helper.absolutePos(new BlockPos(0, 2, 0));
            helper.setBlock(new BlockPos(0, 1, 0), Blocks.STONE);
            var staff = createFieldOverseerTestEntity(helper, owner, anchorPos, 100.0F, 40);
            // 転送距離だけを測り、標的探索・攻撃成立・近隣テストの敵へ依存させない。
            var saved = staff.saveWithoutId(new CompoundTag());
            saved.putFloat("CurrentMana", 60.0F);
            saved.putDouble("TargetingRadius", 0.0D);
            staff.readAdditionalSaveData(saved);
            int startedTick = staff.tickCount;

            helper.runAfterDelay(55, () -> {
                helper.assertTrue(staff.isAlive() && helper.getLevel().getEntity(staff.getUUID()) == staff
                                && staff.tickCount - startedTick >= 5,
                        "Field Overseer transfer fixture must receive at least one five-tick transfer interval");
                helper.assertTrue(Math.abs(staff.getCurrentMana() - expectedStaffMana) <= EPSILON,
                        "Field Overseer mana transfer range should depend on Monarch Bond: expected="
                                + expectedStaffMana + ", actual=" + staff.getCurrentMana());
                helper.assertTrue(Math.abs(ownerMagicData.getMana() - expectedOwnerMana) <= EPSILON,
                        "Field Overseer should only drain mana within its effective transfer range: expected="
                                + expectedOwnerMana + ", actual=" + ownerMagicData.getMana());
                helper.succeed();
            });
        });
    }
}
