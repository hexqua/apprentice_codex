package jp.aquafactory.apprenticecodex.gametest.equipment.armor.calibration;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.gametest.crafting.calibration.SpellCalibrationAdjustmentGameTestSupport;
import jp.aquafactory.apprenticecodex.item.armor.EndgameArmorExplosionKnockbackEvent;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.level.ExplosionKnockbackEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import static jp.aquafactory.apprenticecodex.gametest.support.PlayerTestSupport.createEquipmentTestPlayer;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class ArmorExplosionKnockbackGameTests {
    private ArmorExplosionKnockbackGameTests() {}

    @GameTest(template = "gametest/basic_floor")
    public static void negativeExplosionResistanceAmplifiesKnockback(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "wind_event");
            var boots = new ItemStack(ItemRegistry.MAGI_AGENT_SUIT_BOOTS.get());
            helper.assertTrue(SpellCalibrationAdjustmentGameTestSupport.setCalibrationAdjustment(boots, 0,
                            new ItemStack(ItemRegistry.WIND_ACCUMULATION_WEAVE.get())), "Wind fixture should be accepted");
            player.setItemSlot(EquipmentSlot.FEET, boots);
            var resistance = player.getAttribute(Attributes.EXPLOSION_KNOCKBACK_RESISTANCE);
            var id = ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "gametest_wind_event");
            // この検証はeventの負値処理。装備更新によるmodifier反映は別の実装備テストで確認する。
            resistance.setBaseValue(0);
            resistance.addTransientModifier(new AttributeModifier(id, -0.2, AttributeModifier.Operation.ADD_VALUE));
            try {
                var event = new ExplosionKnockbackEvent(helper.getLevel(), null, player, new Vec3(1, 0, 0));
                EndgameArmorExplosionKnockbackEvent.onExplosionKnockback(event);
                helper.assertTrue(Math.abs(event.getKnockbackVelocity().x - 1.2) < 1.0e-9,
                        "Negative resistance should amplify explosion knockback by 20%");
            } finally {
                resistance.removeModifier(id);
                player.setItemSlot(EquipmentSlot.FEET, ItemStack.EMPTY);
            }
        });
    }
}
