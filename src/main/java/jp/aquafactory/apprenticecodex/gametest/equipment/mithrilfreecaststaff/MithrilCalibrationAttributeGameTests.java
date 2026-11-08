package jp.aquafactory.apprenticecodex.gametest.equipment.mithrilfreecaststaff;

import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import java.util.List;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.gametest.crafting.calibration.SpellCalibrationAdjustmentGameTestSupport;
import jp.aquafactory.apprenticecodex.gametest.support.ConnectedPlayerTestSupport;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import net.minecraft.core.Holder;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class MithrilCalibrationAttributeGameTests {
    private MithrilCalibrationAttributeGameTests() {}

    @GameTest(template = "gametest/basic_floor")
    public static void schoolRuneChangesEquippedStaffAttributesWithoutDuplication(GameTestHelper helper) {
        helper.succeedIf(() -> {
            try (var connection = ConnectedPlayerTestSupport.createPlayer(helper)) {
                var player = connection.player();
                var watched = List.of(AttributeRegistry.SPELL_POWER, AttributeRegistry.FIRE_SPELL_POWER,
                        AttributeRegistry.ICE_SPELL_POWER);
                var id = ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "gametest_mithril_baseline");
                for (var attribute : watched) {
                    var instance = player.getAttribute(attribute);
                    instance.setBaseValue(1);
                    // 既存の基礎乗算と合成し、最終乗算への取り違えも区別する。
                    instance.addTransientModifier(new AttributeModifier(id, 0.2, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
                }
                try {
                    var plain = new ItemStack(ItemRegistry.MITHRIL_FREECAST_STAFF.get());
                    var tuned = plain.copy();
                    helper.assertTrue(SpellCalibrationAdjustmentGameTestSupport.setCalibrationAdjustment(tuned, 0,
                                    new ItemStack(io.redspace.ironsspellbooks.registries.ItemRegistry.FIRE_RUNE.get())),
                            "Fire Rune should be accepted by Mithril Staff");
                    for (var stage : new int[]{0, 1, 1, 0, 2}) {
                        player.setItemSlot(EquipmentSlot.MAINHAND, stage == 0 ? plain : stage == 1 ? tuned : ItemStack.EMPTY);
                        var before = new EntityTickEvent.Pre(player);
                        NeoForge.EVENT_BUS.post(before);
                        helper.assertFalse(before.isCanceled(), "Staff fixture entity tick should complete");
                        player.doTick();
                        NeoForge.EVENT_BUS.post(new EntityTickEvent.Post(player));
                        assertValue(helper, player, AttributeRegistry.SPELL_POWER, stage == 0 ? 1.3 : stage == 1 ? 1.25 : 1.2);
                        assertValue(helper, player, AttributeRegistry.FIRE_SPELL_POWER, stage == 1 ? 1.35 : 1.2);
                        assertValue(helper, player, AttributeRegistry.ICE_SPELL_POWER, 1.2);
                    }
                } finally {
                    for (var attribute : watched) {
                        player.getAttribute(attribute).removeModifier(id);
                    }
                }
            }
        });
    }

    private static void assertValue(GameTestHelper helper, Player player, Holder<Attribute> attribute, double expected) {
        var actual = player.getAttributeValue(attribute);
        helper.assertTrue(Math.abs(actual - expected) < 1.0e-7,
                "Equipped staff attribute=" + attribute + " expected=" + expected + " actual=" + actual);
    }
}
