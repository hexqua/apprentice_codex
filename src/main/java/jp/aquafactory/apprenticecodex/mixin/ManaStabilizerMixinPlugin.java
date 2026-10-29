package jp.aquafactory.apprenticecodex.mixin;

import java.util.List;
import java.util.Set;
import java.util.stream.StreamSupport;
import net.minecraftforge.fml.loading.FMLLoader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;
import org.spongepowered.asm.service.MixinService;

public final class ManaStabilizerMixinPlugin implements IMixinConfigPlugin {
    public static boolean compatible(ClassNode node) {
        boolean pitch = node.fields.stream().anyMatch(f -> f.name.equals("pitch") && f.desc.equals("F") && (f.access & Opcodes.ACC_STATIC) != 0);
        boolean yaw = node.fields.stream().anyMatch(f -> f.name.equals("yaw") && f.desc.equals("F") && (f.access & Opcodes.ACC_STATIC) != 0);
        boolean shot = node.methods.stream().anyMatch(m -> m.name.equals("applyRecoil") && m.desc.equals("(Lio/redspace/irons_artifice/gun/ShotProfile;)V") && (m.access & Opcodes.ACC_STATIC) != 0);
        boolean tick = node.methods.stream().anyMatch(m -> m.name.equals("onClientTick")
                && m.desc.equals("(Lnet/minecraftforge/event/TickEvent$ClientTickEvent;)V")
                && (m.access & Opcodes.ACC_STATIC) != 0 && StreamSupport.stream(m.instructions.spliterator(), false)
                .anyMatch(i -> i instanceof MethodInsnNode call && call.owner.equals(node.name)
                        && call.name.equals("updateCameraCursor") && call.desc.equals("()V")));
        return pitch && yaw && shot && tick;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        var mods = FMLLoader.getLoadingModList();
        if (mods == null || mods.getModFileById("irons_artifice") == null) return false;
        try {
            var node = MixinService.getService().getBytecodeProvider().getClassNode(targetClassName);
            if (mixinClassName.endsWith("ManaStabilizerCrosshairMixin")) {
                return node.methods.stream().anyMatch(m -> m.name.equals("updateCrosshairCursor")
                        && m.desc.equals("()V") && (m.access & Opcodes.ACC_STATIC) != 0
                        && StreamSupport.stream(m.instructions.spliterator(), false).anyMatch(i ->
                        i instanceof MethodInsnNode call && call.owner.equals("net/minecraft/util/Mth")
                                // Forgeの配布環境はSRG名、開発環境はMojmap名を使う。
                                && (call.name.equals("lerp") || call.name.equals("m_14179_")) && call.desc.equals("(FFF)F")));
            }
            return compatible(node);
        } catch (Exception ignored) {
            // optionalな回復機能は上流構造が変わった場合に無言で停止する。
            return false;
        }
    }
    @Override public void onLoad(String mixinPackage) {}
    @Override public String getRefMapperConfig() { return null; }
    @Override public void acceptTargets(Set<String> mine, Set<String> others) {}
    @Override public List<String> getMixins() { return null; }
    @Override public void preApply(String name, ClassNode node, String mixin, IMixinInfo info) {}
    @Override public void postApply(String name, ClassNode node, String mixin, IMixinInfo info) {}
}
