package jp.aquafactory.apprenticecodex.gametest.integration.bettercombat;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;

import java.io.IOException;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class BetterCombatNetworkGameTests {
    private static final String CLIENT_ADAPTER =
            "net/bettercombat/fabricmc/fabric/api/client/networking/v1/ClientPlayNetworking";

    private BetterCombatNetworkGameTests() {}

    @GameTest(template = "gametest/basic_floor")
    public static void scytheStopUsesRegisteredClientSendAdapter(GameTestHelper helper) throws IOException {
        // clientクラスをロードせず本番の呼出箇所を読む。wrapperだけをテストで作ると旧誤送信を検出できない。
        var node = readClass("jp/aquafactory/apprenticecodex/compat/bettercombat/BetterCombatScytheThrowClientCompat");
        var stop = node.methods.stream().filter(method -> method.name.equals("stopSwing")
                && method.desc.equals("()V")).findFirst().orElseThrow();
        int adapterCalls = 0;
        for (var instruction : stop.instructions) {
            if (!(instruction instanceof MethodInsnNode call)) continue;
            if (call.owner.equals(CLIENT_ADAPTER) && call.name.equals("send")) {
                adapterCalls++;
            }
            helper.assertFalse(call.owner.equals("net/minecraftforge/network/simple/SimpleChannel")
                            && call.name.equals("sendToServer"),
                    "Scythe stop must not send an unregistered AttackAnimation directly to SimpleChannel");
        }
        helper.assertTrue(adapterCalls == 1, "Scythe stop must use the dependency's registered send adapter exactly once");
        helper.succeed();
    }

    @GameTest(template = "gametest/basic_floor")
    public static void betterCombatStopWrapperEncodesThroughRegisteredForgeChannel(GameTestHelper helper) throws Exception {
        if (!ModList.get().isLoaded("bettercombat")) {
            helper.succeed();
            return;
        }
        // optional型は導入済み構成だけで読み込む。client送信APIそのものはserver上で実行しない。
        var node = readClass(CLIENT_ADAPTER);
        var send = node.methods.stream().filter(method -> method.name.equals("send")).findFirst().orElseThrow();
        boolean constructsWrapper = false;
        for (var instruction : send.instructions) {
            if (instruction instanceof MethodInsnNode call
                    && call.owner.equals("net/bettercombat/forge/network/PacketWrapper")
                    && call.name.equals("<init>")) {
                constructsWrapper = true;
            }
        }
        helper.assertTrue(constructsWrapper, "Better Combat's send adapter must construct the registered Forge wrapper");
        BetterCombatPacketGameTestHooks.assertStopWrapperUsesRegisteredChannel(helper);
        helper.succeed();
    }

    private static ClassNode readClass(String internalName) throws IOException {
        try (var input = BetterCombatNetworkGameTests.class.getClassLoader().getResourceAsStream(internalName + ".class")) {
            if (input == null) throw new IOException("Missing integration bytecode: " + internalName);
            var node = new ClassNode();
            new ClassReader(input).accept(node, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
            return node;
        }
    }
}
