package jp.aquafactory.apprenticecodex.gametest.equipment.multicastechostaff.lifecycle;

import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.entity.spells.target_area.TargetedAreaEntity;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.gametest.spell.targeting.support.TargetingTestFixture;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.function.Consumer;

import static jp.aquafactory.apprenticecodex.gametest.equipment.multicastechostaff.support.EchoStaffFixture.*;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class EchoStaffIndicatorGameTests {
    private EchoStaffIndicatorGameTests() {
    }

    @GameTest(template = TEMPLATE, batch = "apprenticecodex.multicast_echo_staff_isolated", timeoutTicks = 80)
    public static void multicastEchoStaffRepeatedFortifyClearsTargetAreaIndicator(GameTestHelper helper) {
        var player = manualPlayer(helper, "echo_fortify_indicator");
        var stack = new ItemStack(ItemRegistry.MULTICAST_ECHO_STAFF.get());
        var spell = SpellRegistry.FORTIFY_SPELL.get();
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        var initialMana = spell.getManaCost(1) * 3.0F;
        // setManaはMAX_MANAで丸められるため、通常分と追加分の両方を実行できる容量を用意する。
        var maxMana = player.getAttribute(AttributeRegistry.MAX_MANA);
        helper.assertTrue(maxMana != null, "Fortify fixture must have a max-mana attribute");
        maxMana.setBaseValue(initialMana);
        var scope = TargetingTestFixture.of(helper);
        var areas = new ArrayList<TargetedAreaEntity>();
        Consumer<EntityJoinLevelEvent> observer = event -> {
            if (event.getLevel() == helper.getLevel() && event.getEntity() instanceof TargetedAreaEntity area && area.getOwner() == player) {
                areas.add(area);
                scope.track(area);
            }
        };
        scope.onClose(() -> NeoForge.EVENT_BUS.unregister(observer));
        NeoForge.EVENT_BUS.addListener(observer);
        helper.runAtTickTime(1, () -> {
            beginCast(helper, player, stack, spell, 1, 0, initialMana);
            helper.assertTrue(Math.abs(MagicData.getPlayerMagicData(player).getMana() - initialMana) < 1.0e-4F,
                    "Fortify fixture must retain the full starting mana");
            helper.assertTrue(areas.size() == 1, "Normal pre-cast must create one owned target indicator: " + areas.size());
            scope.assertVisible(areas.getFirst());
            finishCast(helper, player, spell, 1);
            helper.assertTrue(Math.abs(MagicData.getPlayerMagicData(player).getMana() - (initialMana - spell.getManaCost(1))) < 1.0e-4F,
                    "Normal Fortify must leave enough mana for the repeated cast");
        });
        helper.runAtTickTime(4, () -> {
            // 近隣の無関係なindicatorが存在しても、自分の生成物だけを観測する。
            var unrelated = scope.track(TargetedAreaEntity.createTargetAreaEntity(helper.getLevel(), player.position().add(12, 0, 0), 3, 16239960));
            scope.assertVisible(unrelated);
            tickEcho(helper, player);
            helper.assertTrue(Math.abs(MagicData.getPlayerMagicData(player).getMana() - (initialMana - 2 * spell.getManaCost(1))) < 1.0e-4F,
                    "Repeated Fortify must actually consume its mana");
            helper.assertTrue(areas.size() == 2, "Repeated pre-cast must create its own indicator: " + areas.size());
            helper.assertTrue(areas.stream().allMatch(Entity::isRemoved),
                    "Both owned target indicators must be discarded by cast-data cleanup before fixture cleanup");
            helper.assertTrue(MagicData.getPlayerMagicData(player).getAdditionalCastData() == null,
                    "Repeated Fortify must clear temporary cast data");
            helper.assertTrue(!unrelated.isRemoved(), "Unrelated target indicator must remain untouched");
            helper.succeed();
        });
    }
}
