package jp.aquafactory.apprenticecodex.gametest;

import jp.aquafactory.apprenticecodex.item.modifier.manastabilizermodule.ManaStabilizerRecovery;
import jp.aquafactory.apprenticecodex.mixin.ManaStabilizerMixinPlugin;
import net.minecraft.gametest.framework.GameTestHelper;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

final class ManaStabilizerModuleGameTestScenariosCommon {
    private ManaStabilizerModuleGameTestScenariosCommon() {}
    static void math(GameTestHelper h) {
        var paid = ManaStabilizerRecovery.recover(3, 4, 2, true);
        h.assertTrue(Math.abs(paid.x - 1.8) < 0.001 && Math.abs(paid.y - 2.4) < 0.001, "Recovery must preserve direction");
        h.assertTrue(ManaStabilizerRecovery.recover(3, 4, 2, false).x == 3, "Unpaid recovery must not accelerate");
        h.assertTrue(ManaStabilizerRecovery.recover(0.3f, 0.4f, 1, false).lengthSquared() == 0, "Tail recovery must be free");
        h.assertTrue(ManaStabilizerRecovery.recover(0.3f, 0.4f, 0, true).lengthSquared() > 0, "Zero speed must disable all assistance");
        ClassNode node = new ClassNode();
        node.name = "io/redspace/irons_artifice/client/RecoilManager";
        h.assertTrue(!ManaStabilizerMixinPlugin.compatible(node), "Missing hook targets must disable the mixin");
        node.fields.add(new FieldNode(Opcodes.ACC_STATIC, "pitch", "F", null, null));
        node.fields.add(new FieldNode(Opcodes.ACC_STATIC, "yaw", "F", null, null));
        node.methods.add(new MethodNode(Opcodes.ACC_STATIC, "applyRecoil", "(Lio/redspace/irons_artifice/gun/ShotProfile;)V", null, null));
        MethodNode tick = new MethodNode(Opcodes.ACC_STATIC, "onClientTick", "(Lnet/minecraftforge/event/TickEvent$ClientTickEvent;)V", null, null);
        node.methods.add(tick);
        h.assertTrue(!ManaStabilizerMixinPlugin.compatible(node), "Missing injection point must disable the mixin");
        tick.instructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC, node.name, "updateCameraCursor", "()V", false));
        h.assertTrue(ManaStabilizerMixinPlugin.compatible(node), "Known hook shape must be accepted");
    }
}
