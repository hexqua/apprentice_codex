package jp.aquafactory.apprenticecodex.compat.accessories;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.service.MixinService;

import java.io.IOException;
import java.util.stream.StreamSupport;
import java.util.concurrent.atomic.AtomicBoolean;

public final class AccessoriesCompatWorkaroundSupport {
    public static final String METHOD_DESCRIPTOR = "(Lnet/minecraft/world/level/Level;Lio/wispforest/accessories/api/slot/SlotType;ILnet/minecraft/world/item/ItemStack;)Lnet/fabricmc/fabric/api/util/TriState;";
    private static final AtomicBoolean WARNED = new AtomicBoolean();
    private static String version = "unknown";

    private AccessoriesCompatWorkaroundSupport() {
    }

    public static boolean canApply(String target, String installedVersion) {
        version = installedVersion;
        // ModLauncherは未変換bytecodeの取得を提供しないため、対応する取得経路を使う。
        try {
            if (isCompatible(MixinService.getService().getBytecodeProvider().getClassNode(target, true))) {
                return true;
            }
            warnDisabled("Incompatible target structure");
        } catch (IOException | ClassNotFoundException | RuntimeException exception) {
            warnDisabled("Cannot inspect target: " + exception);
        }
        return false;
    }

    public static boolean isCompatible(ClassNode target) {
        if ((target.access & (Opcodes.ACC_INTERFACE | Opcodes.ACC_ABSTRACT | Opcodes.ACC_ENUM)) != 0) {
            return false;
        }
        boolean fieldMatches = target.fields.stream().anyMatch(field ->
                "location".equals(field.name) && "Lnet/minecraft/resources/ResourceLocation;".equals(field.desc)
                        && (field.access & Opcodes.ACC_STATIC) == 0 && (field.access & Opcodes.ACC_FINAL) != 0);
        return fieldMatches && target.methods.stream().anyMatch(method ->
                "isValid".equals(method.name) && METHOD_DESCRIPTOR.equals(method.desc)
                        && (method.access & (Opcodes.ACC_STATIC | Opcodes.ACC_ABSTRACT | Opcodes.ACC_NATIVE)) == 0
                        && StreamSupport.stream(method.instructions.spliterator(), false)
                        .anyMatch(instruction -> instruction.getOpcode() == Opcodes.ARETURN));
    }

    public static boolean hasInjection(ClassNode target) {
        return target.methods.stream().filter(method ->
                "isValid".equals(method.name) && METHOD_DESCRIPTOR.equals(method.desc))
                .anyMatch(method -> StreamSupport.stream(method.instructions.spliterator(), false)
                        .anyMatch(instruction -> instruction instanceof MethodInsnNode call
                                && target.name.equals(call.owner)
                                && call.name.contains("apprenticecodex$keepUnrelatedBackItemsValid")));
    }

    public static void warnDisabled(String reason) {
        if (WARNED.compareAndSet(false, true)) {
            LoggerFactory.getLogger("apprenticecodex").warn(
                    """
                    ACCESSORIES COMPATIBILITY WORKAROUND DISABLED: UPSTREAM IMPLEMENTATION HAS CHANGED.
                    Curios custom slot validators may reject unrelated items.
                    Report compatibility issues to Accessories Compatibility Layer:
                    https://github.com/wisp-forest/accessories-compat-layer/issues/51
                    Installed version: {}; reason: {}""", version, reason);
        }
    }
}
