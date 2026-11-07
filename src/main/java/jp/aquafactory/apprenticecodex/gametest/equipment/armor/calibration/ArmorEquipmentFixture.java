package jp.aquafactory.apprenticecodex.gametest.equipment.armor.calibration;

import jp.aquafactory.apprenticecodex.gametest.support.ConnectedPlayerTestSupport;
import net.minecraft.core.Holder;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

final class ArmorEquipmentFixture implements AutoCloseable {
    private final GameTestHelper helper;
    private final ConnectedPlayerTestSupport.PlayerFixture connection;

    ArmorEquipmentFixture(GameTestHelper helper) {
        this.helper = helper;
        connection = ConnectedPlayerTestSupport.createPlayer(helper);
    }

    ServerPlayer player() {
        return connection.player();
    }

    void equip(ItemStack stack, EquipmentSlot slot) {
        player().setItemSlot(slot, stack.copy());
        update();
    }

    void update() {
        // world未登録playerでも、loaderとCuriosが使用する装備更新の前後を通す。
        var before = new EntityTickEvent.Pre(player());
        NeoForge.EVENT_BUS.post(before);
        helper.assertFalse(before.isCanceled(), "Armor fixture entity tick should complete");
        player().doTick();
        NeoForge.EVENT_BUS.post(new EntityTickEvent.Post(player()));
    }

    void assertAttribute(Holder<Attribute> attribute, double expected, String context) {
        var actual = player().getAttributeValue(attribute);
        helper.assertTrue(Math.abs(actual - expected) < 1.0e-7,
                context + " attribute=" + attribute + " expected=" + expected + " actual=" + actual);
    }

    @Override
    public void close() {
        connection.close();
    }
}
