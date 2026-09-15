package jp.aquafactory.apprenticecodex.gametest;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.damage.DamageTypes;
import jp.aquafactory.apprenticecodex.registry.EntityRegistry;
import jp.aquafactory.apprenticecodex.spell.precisionjack.PrecisionJackKnifeEntity;
import jp.aquafactory.apprenticecodex.spell.precisionjack.PrecisionJackLootingEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class PrecisionJackLootGameTests {
    private PrecisionJackLootGameTests() {
    }

    @GameTest(template = "gametest/basic_floor")
    public static void additionalRollPreservesAttackerDependentDrops(GameTestHelper helper) {
        var level = helper.getLevel();
        var target = helper.spawn(EntityType.CREEPER, new BlockPos(2, 2, 2));
        var attacker = helper.spawn(EntityType.SKELETON, new BlockPos(0, 2, 0));
        var knife = new PrecisionJackKnifeEntity(EntityRegistry.PRECISION_JACK_KNIFE.get(), level, attacker);
        knife.setLootingBonus(0);
        knife.setDuplicateDropChancePercent(30);
        var source = new DamageSource(level.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE)
                .getOrThrow(DamageTypes.PRECISION_JACK), knife, attacker);
        var drops = new ArrayList<ItemEntity>();

        // 追加抽選の発動を確定させ、攻撃者条件の成否だけを検証する。
        boolean triggered = false;
        for (long seed = 0; seed < 100; seed++) {
            target.getRandom().setSeed(seed);
            if (target.getRandom().nextInt(100) < 30) {
                target.getRandom().setSeed(seed);
                triggered = true;
                break;
            }
        }
        helper.assertTrue(triggered, "A deterministic duplicate-roll seed must be available");
        PrecisionJackLootingEvent.onLivingDrops(new LivingDropsEvent(target, source, drops, false));

        // vanillaのクリーパーはスケルトン撃破時のみレコードを落とす。攻撃者情報の欠落ではこの抽選が失敗する。
        helper.assertTrue(drops.stream().anyMatch(drop -> drop.getItem().has(DataComponents.JUKEBOX_PLAYABLE)),
                "Precision Jack's additional roll must preserve skeleton-dependent creeper music disc drops");
        helper.succeed();
    }
}
