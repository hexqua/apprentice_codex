package jp.aquafactory.apprenticecodex.gametest;

import java.util.function.Predicate;
import java.lang.reflect.Proxy;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.compat.accessories.AccessoriesCompatWorkaroundSupport;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class AccessoriesCompatWorkaroundGameTests {
    private AccessoriesCompatWorkaroundGameTests() {
    }

    @GameTest(template = "gametest/basic_floor")
    public static void rejectsIncompatibleUpstreamStructures(GameTestHelper helper) {
        helper.assertTrue(AccessoriesCompatWorkaroundSupport.isCompatible(fixture()), "Compatible structure must be accepted");
        var missingField = fixture();
        missingField.fields.clear();
        helper.assertTrue(!AccessoriesCompatWorkaroundSupport.isCompatible(missingField), "Missing shadow must be rejected");
        var wrongType = fixture();
        wrongType.fields.getFirst().desc = "Ljava/lang/String;";
        helper.assertTrue(!AccessoriesCompatWorkaroundSupport.isCompatible(wrongType), "Wrong field type must be rejected");
        for (int access : new int[]{Opcodes.ACC_STATIC | Opcodes.ACC_FINAL, 0}) {
            var changedField = fixture();
            changedField.fields.getFirst().access = access;
            helper.assertTrue(!AccessoriesCompatWorkaroundSupport.isCompatible(changedField), "Wrong field modifiers must be rejected");
        }
        var missingMethod = fixture();
        missingMethod.methods.clear();
        helper.assertTrue(!AccessoriesCompatWorkaroundSupport.isCompatible(missingMethod), "Missing method must be rejected");
        var wrongDescriptor = fixture();
        wrongDescriptor.methods.getFirst().desc = "()V";
        helper.assertTrue(!AccessoriesCompatWorkaroundSupport.isCompatible(wrongDescriptor), "Changed descriptor must be rejected");
        for (int access : new int[]{Opcodes.ACC_STATIC, Opcodes.ACC_ABSTRACT, Opcodes.ACC_NATIVE}) {
            var changedMethod = fixture();
            changedMethod.methods.getFirst().access = access;
            helper.assertTrue(!AccessoriesCompatWorkaroundSupport.isCompatible(changedMethod), "Unsupported method modifiers must be rejected");
        }
        var noReturn = fixture();
        noReturn.methods.getFirst().instructions.clear();
        helper.assertTrue(!AccessoriesCompatWorkaroundSupport.isCompatible(noReturn), "Missing return must be rejected");
        helper.succeed();
    }

    @GameTest(template = "gametest/basic_floor")
    public static void detectsActualHandlerInvocation(GameTestHelper helper) {
        var target = fixture();
        helper.assertTrue(!AccessoriesCompatWorkaroundSupport.hasInjection(target), "Unmodified method must not count as injected");
        target.methods.getFirst().instructions.insert(new MethodInsnNode(Opcodes.INVOKESPECIAL, target.name,
                "handler$test$apprenticecodex$keepUnrelatedBackItemsValid",
                "(Lorg/spongepowered/asm/mixin/injection/callback/CallbackInfoReturnable;)V", false));
        helper.assertTrue(AccessoriesCompatWorkaroundSupport.hasInjection(target), "Handler call must be detected");
        helper.succeed();
    }

    @GameTest(template = "gametest/basic_floor")
    public static void installedCompatibilityLayerPreservesValidatorResults(GameTestHelper helper) throws ReflectiveOperationException {
        // 任意依存の実クラスを通し、起動時のmixin注入も含めて検証する。
        if (!ModList.get().isLoaded("accessories_compat_layer")) {
            helper.succeed();
            return;
        }
        var wrapper = Class.forName("io.wispforest.accessories_compat.curios.wrapper.CuriosSlotBasedPredicate");
        var slotType = Class.forName("io.wispforest.accessories.api.slot.SlotType");
        var backSlot = Proxy.newProxyInstance(slotType.getClassLoader(), new Class<?>[]{slotType},
                (proxy, invoked, arguments) -> switch (invoked.getName()) {
                    case "name", "toString" -> "back";
                    case "equals" -> proxy == arguments[0];
                    case "hashCode" -> System.identityHashCode(proxy);
                    default -> throw new UnsupportedOperationException(invoked.getName());
                });
        var constructor = wrapper.getConstructor(ResourceLocation.class, Predicate.class);
        var method = wrapper.getMethod("isValid", Level.class, slotType, int.class, ItemStack.class);
        var calibrated = ResourceLocation.parse("apprenticecodex:calibrated_broom");
        var unrelated = ResourceLocation.parse("example:unrelated");
        Predicate<Object> reject = ignored -> false;
        Predicate<Object> accept = ignored -> true;
        var stack = new ItemStack(Items.CHEST);
        helper.assertTrue("DEFAULT".equals(((Enum<?>) method.invoke(constructor.newInstance(calibrated, reject),
                helper.getLevel(), backSlot, 0, stack)).name()), "Unrelated items must fall through calibrated broom validator");
        helper.assertTrue("FALSE".equals(((Enum<?>) method.invoke(constructor.newInstance(unrelated, reject),
                helper.getLevel(), backSlot, 0, stack)).name()), "Other validators must retain false");
        helper.assertTrue("TRUE".equals(((Enum<?>) method.invoke(constructor.newInstance(calibrated, accept),
                helper.getLevel(), backSlot, 0, stack)).name()), "Accepted broom validator must retain true");
        helper.succeed();
    }

    private static ClassNode fixture() {
        var target = new ClassNode();
        target.name = "example/Predicate";
        target.fields.add(new FieldNode(Opcodes.ACC_PRIVATE | Opcodes.ACC_FINAL, "location",
                "Lnet/minecraft/resources/ResourceLocation;", null, null));
        var method = new MethodNode(Opcodes.ACC_PUBLIC, "isValid",
                AccessoriesCompatWorkaroundSupport.METHOD_DESCRIPTOR, null, null);
        method.instructions.add(new InsnNode(Opcodes.ARETURN));
        target.methods.add(method);
        return target;
    }
}
