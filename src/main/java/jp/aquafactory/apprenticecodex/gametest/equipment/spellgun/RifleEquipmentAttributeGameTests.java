package jp.aquafactory.apprenticecodex.gametest.equipment.spellgun;

import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import java.util.List;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import net.minecraft.core.Holder;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import static jp.aquafactory.apprenticecodex.gametest.support.ConnectedPlayerTestSupport.createPlayer;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class RifleEquipmentAttributeGameTests {
    private RifleEquipmentAttributeGameTests() {}

    @GameTest(template = "gametest/basic_floor")
    public static void riflesPreserveAttributesThroughEquipmentChanges(GameTestHelper helper) {
        // modifier定義の読み返しではなく、通常の装備更新後の実値を比較する。
        try (var fixture = createPlayer(helper)) {
            var player = fixture.player();
            player.doTick();
            List<Holder<Attribute>> attributes = List.of(
                    Attributes.ATTACK_DAMAGE, Attributes.ATTACK_SPEED, AttributeRegistry.SPELL_POWER);
            var baseline = attributes.stream().mapToDouble(player::getAttributeValue).toArray();

            for (var item : List.of(ItemRegistry.MULTIPURPOSE_STAFFRIFLE.get(),
                    ItemRegistry.FULLAUTO_RAPIDCAST_SPELLRIFLE.get())) {
                player.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(item));
                player.doTick();
                assertAttributes(helper, player, attributes, baseline, "Equipping " + item);

                player.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(item));
                player.setItemSlot(EquipmentSlot.MAINHAND, player.getMainHandItem().copy());
                player.doTick();
                player.doTick();
                assertAttributes(helper, player, attributes, baseline, "Re-equipping and dual-wielding " + item);
            }

            player.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
            player.setItemSlot(EquipmentSlot.OFFHAND, ItemStack.EMPTY);
            player.doTick();
            assertAttributes(helper, player, attributes, baseline, "Removing rifles");
            helper.succeed();
        }
    }

    private static void assertAttributes(GameTestHelper helper, Player player,
                                         List<Holder<Attribute>> attributes, double[] baseline, String context) {
        for (int i = 0; i < attributes.size(); i++) {
            double actual = player.getAttributeValue(attributes.get(i));
            helper.assertTrue(Math.abs(actual - baseline[i]) < 1.0e-6,
                    context + " must preserve " + attributes.get(i) + ": expected=" + baseline[i] + ", actual=" + actual);
        }
    }
}
