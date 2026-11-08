package jp.aquafactory.apprenticecodex.gametest.spell.fieldoverseer;

import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.gametest.support.DeployedSpellTestFixture;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class FieldOverseerTargetingGameTests {
    private FieldOverseerTargetingGameTests() {
    }

    @GameTest(template = DeployedSpellTestFixture.TEMPLATE, batch = "apprenticecodex.field_overseer_isolated", timeoutTicks = 100)
    public static void fieldOverseerPrioritizesHealthAndTransfersMana(GameTestHelper helper) {
        var owner = DeployedSpellTestFixture.of(helper).player(new BlockPos(2, 2, 0), "field_overseer_attack_test");
        var manaRegen = owner.getAttribute(AttributeRegistry.MANA_REGEN);
        if (manaRegen != null) {
            manaRegen.setBaseValue(0.0D);
        }
        var ownerMagicData = MagicData.getPlayerMagicData(owner);
        ownerMagicData.setMana(75.0F);
        var anchorPos = DeployedSpellTestFixture.of(helper).absolute(new BlockPos(2, 2, 2));
        DeployedSpellTestFixture.of(helper).setBlock(new BlockPos(2, 1, 2), Blocks.STONE);
        var staff = FieldOverseerTestSupport.create(helper, owner, anchorPos, 100.0F, 40);

        // 未攻撃対象の体力不変を検証するため、日光ダメージを受けないHuskを使う。
        var highHealthTarget = DeployedSpellTestFixture.of(helper).mob(EntityType.HUSK, new BlockPos(4, 2, 2));
        var mediumHealthTarget = DeployedSpellTestFixture.of(helper).mob(EntityType.HUSK, new BlockPos(2, 2, 4));
        var lowHealthTarget = DeployedSpellTestFixture.of(helper).mob(EntityType.HUSK, new BlockPos(0, 2, 2));
        highHealthTarget.setHealth(20.0F);
        mediumHealthTarget.setHealth(10.0F);
        lowHealthTarget.setHealth(5.0F);

        var expectedTargets = List.of(highHealthTarget, mediumHealthTarget, lowHealthTarget);
        var struckTargets = new ArrayList<LivingEntity>();
        var strikeTimes = new ArrayList<Long>();
        Consumer<LivingDamageEvent.Post> listener = event -> {
            if (event.getSource().getDirectEntity() == staff && event.getNewDamage() > 0
                    && expectedTargets.contains(event.getEntity())) {
                struckTargets.add(event.getEntity());
                strikeTimes.add(helper.getLevel().getGameTime());
            }
        };
        NeoForge.EVENT_BUS.addListener(listener);
        DeployedSpellTestFixture.of(helper).onClose(() -> NeoForge.EVENT_BUS.unregister(listener));

        helper.runAtTickTime(52, () -> {
            helper.assertTrue(highHealthTarget.getHealth() < 20.0F,
                    "FieldOverseer should strike the highest-current-health target first");
            helper.assertTrue(Math.abs(mediumHealthTarget.getHealth() - 10.0F) < 0.0001F,
                    "FieldOverseer second strike should wait three ticks");
            helper.assertTrue(Math.abs(lowHealthTarget.getHealth() - 5.0F) < 0.0001F,
                    "FieldOverseer should not strike the lower-health target before its sequence turn");
        });
        helper.runAtTickTime(58, () -> {
            helper.assertTrue(mediumHealthTarget.getHealth() < 10.0F,
                    "FieldOverseer should perform the second strike three ticks after the first");
        });
        helper.runAtTickTime(62, () -> {
            helper.assertTrue(lowHealthTarget.getHealth() < 5.0F,
                    "FieldOverseer should perform the third strike six ticks after the first");
            helper.assertTrue(Math.abs(staff.getCurrentMana() - 100.0F) < 0.0001F,
                    "FieldOverseer should receive two 20 mana transfers after spending one attack cost");
            helper.assertTrue(Math.abs(ownerMagicData.getMana() - 35.0F) < 0.0001F,
                    "FieldOverseer should drain the same amount from the owner: " + ownerMagicData.getMana());
            helper.assertTrue(struckTargets.equals(expectedTargets),
                    "Each selected target must be hit exactly once in descending health order: " + struckTargets);
            helper.assertTrue(strikeTimes.get(1) - strikeTimes.get(0) == 3
                            && strikeTimes.get(2) - strikeTimes.get(1) == 3,
                    "Consecutive strikes must occur exactly three server ticks apart: " + strikeTimes);
            helper.succeed();
        });
    }
}
