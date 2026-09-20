package jp.aquafactory.apprenticecodex.gametest;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.GameType;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class MalumFishingGameTests {
    @GameTest(template = "gametest/basic_floor")
    public static void fishingWithoutGoodTidesPreservesEnchantments(GameTestHelper helper) throws ReflectiveOperationException {
        if (!isSupportedEnvironment()) { helper.succeed(); return; }
        // Malum未導入でも走らせ、通常の竿の経路とoptional参照の隔離を検証する。
        castAndCheck(helper, 0, -1, 0, 2).discard();
        castAndCheck(helper, 3, -1, 300, 2).discard();
        castAndCheck(helper, 6, -1, 600, 2).discard();
        helper.succeed();
    }

    @GameTest(template = "gametest/basic_floor")
    public static void goodTidesAddsTicksAndPreservesLuck(GameTestHelper helper) throws ReflectiveOperationException {
        if (!isPatchedMalum()) { helper.succeed(); return; }
        castAndCheck(helper, 0, 0, 100, 2).discard();
        castAndCheck(helper, 0, 2, 200, 3).discard();
        castAndCheck(helper, 3, 2, 500, 3).discard();
        castAndCheck(helper, 3, 4, 500, 4).discard();
        castAndCheck(helper, 6, 0, 500, 2).discard();
        helper.succeed();
    }

    @GameTest(template = "gametest/basic_floor")
    public static void cappedGoodTidesCanReachBite(GameTestHelper helper) throws ReflectiveOperationException {
        if (!isPatchedMalum()) { helper.succeed(); return; }
        var hook = castAndCheck(helper, 3, 4, 500, 4);
        try {
            // tickの物理移動から切り離し、実際の釣りタイマーが上限でも食いつきへ進むことを確認する。
            var randomField = Entity.class.getDeclaredField("random");
            randomField.setAccessible(true);
            ((RandomSource) randomField.get(hook)).setSeed(12345L);
            var catchingFish = FishingHook.class.getDeclaredMethod("catchingFish", BlockPos.class);
            catchingFish.setAccessible(true);
            for (int tick = 0; tick < 2000; tick++) {
                catchingFish.invoke(hook, helper.absolutePos(new BlockPos(1, 2, 1)));
                if (readInt(hook, "nibble") > 0) {
                    helper.succeed();
                    return;
                }
            }
            helper.fail("Capped Good Tides should allow the fishing timer to reach a bite");
        } finally {
            hook.discard();
        }
    }

    private static FishingHook castAndCheck(GameTestHelper helper, int lure, int amplifier,
                                            int expectedTicks, int expectedLuck) throws ReflectiveOperationException {
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        var rod = new ItemStack(Items.FISHING_ROD);
        var enchantments = helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        if (lure > 0) {
            rod.enchant(enchantments.getOrThrow(Enchantments.LURE), lure);
        }
        rod.enchant(enchantments.getOrThrow(Enchantments.LUCK_OF_THE_SEA), 2);
        player.setItemInHand(InteractionHand.MAIN_HAND, rod);
        if (amplifier >= 0) {
            // 登録時にMalum専用クラスを解決しないよう、効果はregistryから取得する。
            var effect = BuiltInRegistries.MOB_EFFECT.getHolder(ResourceLocation.fromNamespaceAndPath("malum", "good_tides"))
                    .orElseThrow();
            player.addEffect(new MobEffectInstance(effect, 1200, amplifier));
        }
        Items.FISHING_ROD.use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
        var hook = player.fishing;
        helper.assertTrue(hook != null, "Casting a vanilla fishing rod should create a hook");
        try {
            int actualTicks = readInt(hook, "lureSpeed");
            helper.assertTrue(actualTicks == expectedTicks,
                    "Unexpected fishing time reduction for lure=" + lure + ", amplifier=" + amplifier
                            + ": expected=" + expectedTicks + ", actual=" + actualTicks);
            helper.assertTrue(readInt(hook, "luck") == expectedLuck,
                    "Fishing luck should retain rod enchantments and add Good Tides only once");
        } catch (ReflectiveOperationException | RuntimeException exception) {
            hook.discard();
            throw exception;
        }
        return hook;
    }

    private static int readInt(FishingHook hook, String name) throws ReflectiveOperationException {
        var field = FishingHook.class.getDeclaredField(name);
        field.setAccessible(true);
        return field.getInt(hook);
    }

    private static boolean isSupportedEnvironment() {
        return !ModList.get().isLoaded("malum") || isPatchedMalum();
    }

    private static boolean isPatchedMalum() {
        return ModList.get().getModContainerById("malum")
                .map(container -> "1.8.2".equals(container.getModInfo().getVersion().toString()))
                .orElse(false);
    }
}
