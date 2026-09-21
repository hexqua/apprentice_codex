package jp.aquafactory.apprenticecodex.gametest;

import com.mojang.authlib.GameProfile;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.compat.malum.MalumSpellReaperScytheBridge;
import jp.aquafactory.apprenticecodex.config.ApprenticeCodexServerConfig;
import jp.aquafactory.apprenticecodex.config.item.SpellReaperScytheServerConfig.Values;
import jp.aquafactory.apprenticecodex.item.spellreaperscythe.ScytheThrowEntity;
import jp.aquafactory.apprenticecodex.item.spellreaperscythe.ScytheThrowManager;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.UUID;
import java.util.function.Consumer;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class ScytheReboundGameTests {
    private static final String TEMPLATE = "gametest/basic_floor";

    static FakePlayer player(GameTestHelper h, float mana) {
        var p = new FakePlayer(h.getLevel(), new GameProfile(UUID.randomUUID(), "rebound_test"));
        p.gameMode.changeGameModeForPlayer(GameType.SURVIVAL);
        // 1.20.1 の構造配置が整地しない高さまで、最大 32 ブロックの縦経路を明示的に空ける。
        for (int y = 20; y <= 55; y++) h.setBlock(new BlockPos(2, y, 2), Blocks.AIR);
        p.setPos(h.absoluteVec(new Vec3(2.5, 20, 2.5)));
        p.setYRot(0); p.setXRot(-90);
        p.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(10);
        p.getAttribute(AttributeRegistry.MAX_MANA.get()).setBaseValue(10000);
        p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ItemRegistry.SPELL_REAPER_SCYTHE.get()));
        MagicData.getPlayerMagicData(p).setMana(mana);
        return p;
    }

    static void enchant(GameTestHelper h, ItemStack stack, String id, int level) {
        // 1.20.1 の ItemStack.enchant は同じ ID を追記するため、既存レベルを明示的に置換する。
        var enchantments = EnchantmentHelper.getEnchantments(stack);
        enchantments.put(h.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(
                ResourceKey.create(Registries.ENCHANTMENT, ResourceLocation.fromNamespaceAndPath("malum", id))).value(), level);
        EnchantmentHelper.setEnchantments(enchantments, stack);
    }

    static void use(GameTestHelper h, FakePlayer p) {
        // Entity・装備派生の共通テスト。Epic Fightの入力契約は専用GameTestで検証する。
        if (ScytheThrowManager.active(p) == null && MalumSpellReaperScytheBridge.tryTriggerAscension(
                h.getLevel(), p, InteractionHand.MAIN_HAND, p.getMainHandItem()) != InteractionResult.PASS) return;
        ScytheThrowManager.use(h.getLevel(), p, InteractionHand.MAIN_HAND);
    }

    @GameTest(template = TEMPLATE)
    public static void costClampsOverlevelsWithoutOverflow(GameTestHelper h) {
        var normal = new Values(200, 40, 10);
        h.assertTrue(normal.reboundManaCost(1) == 100 && normal.reboundManaCost(2) == 80
                && normal.reboundManaCost(3) == 60, "Rebound I/II/III must cost 100/80/60");
        h.assertTrue(normal.reboundManaCost(6) == 0 && normal.reboundManaCost(Integer.MAX_VALUE) == 0,
                "Overlevels must clamp to zero");
        var max = new Values(200, 40, 10, 100, 3, Integer.MAX_VALUE, Integer.MAX_VALUE);
        h.assertTrue(max.reboundManaCost(1) == Integer.MAX_VALUE && max.reboundManaCost(Integer.MAX_VALUE) == 0,
                "Maximum settings must not overflow");
        h.assertTrue(new Values(200, 40, 10, 100, 3, 17, 0).reboundManaCost(255) == 17,
                "Zero reduction must preserve the configured base cost");
        h.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void instantThrowUsesServerCostAndReturnsWithoutUpkeep(GameTestHelper h) {
        if (!MalumSpellReaperScytheBridge.isAvailable()) { h.succeed(); return; }
        for (int level = 1; level <= 3; level++) {
            var p = player(h, 1000);
            enchant(h, p.getMainHandItem(), "rebound", level);
            var origin = p.getEyePosition();
            use(h, p);
            var entity = ScytheThrowManager.active(p);
            h.assertTrue(entity != null && entity.getMode() == ScytheThrowEntity.Mode.REBOUND && !p.isUsingItem(),
                    "Rebound must launch on use without charging");
            try {
                var expected = 1000 - (100 - 20 * (level - 1));
                h.assertTrue(MagicData.getPlayerMagicData(p).getMana() == expected, "Server must charge once at launch");
                ScytheThrowManager.release(h.getLevel(), p, p.getMainHandItem());
                for (int tick = 0; tick < 20; tick++) {
                    entity.tick();
                    h.assertFalse(entity.isHovering(), "Rebound must never hover");
                }
                h.assertTrue(entity.isRemoved() && Math.abs(entity.position().distanceTo(origin) - 16) < 0.001,
                        "Rebound must reach sixteen blocks before recalling");
                h.assertTrue(MagicData.getPlayerMagicData(p).getMana() == expected, "Flight and recall must not charge upkeep");
                h.assertFalse(ScytheThrowManager.isThrown(p.getMainHandItem()), "Recall must restore the held item");
                h.assertFalse(p.getCooldowns().isOnCooldown(p.getMainHandItem().getItem()), "Rebound must not add a cooldown");
            } finally { entity.discard(); }
        }
        h.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void insufficientManaCreativeFreeCostAndOffhand(GameTestHelper h) {
        if (!MalumSpellReaperScytheBridge.isAvailable()) { h.succeed(); return; }
        var p = player(h, 59);
        enchant(h, p.getMainHandItem(), "rebound", 3);
        use(h, p);
        h.assertTrue(ScytheThrowManager.active(p) == null && MagicData.getPlayerMagicData(p).getMana() == 59,
                "Insufficient server balance must reject an instant throw");
        p.setItemInHand(InteractionHand.OFF_HAND, p.getMainHandItem().copy());
        ItemRegistry.SPELL_REAPER_SCYTHE.get().use(h.getLevel(), p, InteractionHand.OFF_HAND);
        h.assertTrue(ScytheThrowManager.active(p) == null, "Offhand Rebound must be rejected");
        p.gameMode.changeGameModeForPlayer(GameType.CREATIVE);
        use(h, p);
        h.assertTrue(ScytheThrowManager.active(p) != null && MagicData.getPlayerMagicData(p).getMana() == 59,
                "Creative must throw without spending mana");
        ScytheThrowManager.active(p).recall();
        p.gameMode.changeGameModeForPlayer(GameType.SURVIVAL);
        MagicData.getPlayerMagicData(p).setMana(0);
        try (var ignored = ApprenticeCodexServerConfig.useSpellReaperScytheConfigOverrideForGameTest(new Values(200, 40, 10, 100, 3, 0, 20))) {
            use(h, p);
            h.assertTrue(ScytheThrowManager.active(p) != null, "Configured free Rebound must work with zero mana");
            ScytheThrowManager.active(p).recall();
        }
        enchant(h, p.getMainHandItem(), "rebound", 7);
        use(h, p);
        h.assertTrue(ScytheThrowManager.active(p) != null, "Overlevel free Rebound must launch");
        ScytheThrowManager.active(p).recall();
        h.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void reUseAndSwapRecallWithoutAnotherCharge(GameTestHelper h) {
        if (!MalumSpellReaperScytheBridge.isAvailable()) { h.succeed(); return; }
        var p = player(h, 200);
        enchant(h, p.getMainHandItem(), "rebound", 1);
        use(h, p);
        var first = ScytheThrowManager.active(p);
        use(h, p);
        h.assertTrue(first != null && first.isRemoved() && MagicData.getPlayerMagicData(p).getMana() == 100,
                "Using an active scythe must recall without another mana charge");
        use(h, p);
        var second = ScytheThrowManager.active(p);
        var original = p.getMainHandItem();
        p.getInventory().selected = 1;
        second.checkOwner();
        h.assertTrue(second.isRemoved() && !ScytheThrowManager.isThrown(original), "Slot changes must recall immediately");
        h.assertTrue(MagicData.getPlayerMagicData(p).getMana() == 0, "Each actual launch must charge exactly once");
        h.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void ascensionKeepsPriorityWhenBothAreForced(GameTestHelper h) {
        if (!MalumSpellReaperScytheBridge.isAvailable()) { h.succeed(); return; }
        var p = player(h, 500);
        enchant(h, p.getMainHandItem(), "rebound", 1);
        enchant(h, p.getMainHandItem(), "ascension", 1);
        use(h, p);
        h.assertTrue(ScytheThrowManager.active(p) == null && MagicData.getPlayerMagicData(p).getMana() == 300,
                "Forced Ascension/Rebound coexistence must keep Ascension priority");
        h.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void canceledEntitySpawnDoesNotSpendManaOrLeaveToken(GameTestHelper h) {
        if (!MalumSpellReaperScytheBridge.isAvailable()) { h.succeed(); return; }
        var p = player(h, 100);
        enchant(h, p.getMainHandItem(), "rebound", 1);
        Consumer<EntityJoinLevelEvent> listener = event -> {
            if (event.getEntity() instanceof ScytheThrowEntity scythe && scythe.getOwner() == p) event.setCanceled(true);
        };
        MinecraftForge.EVENT_BUS.addListener(listener);
        try {
            use(h, p);
            h.assertTrue(ScytheThrowManager.active(p) == null && !ScytheThrowManager.isThrown(p.getMainHandItem())
                    && MagicData.getPlayerMagicData(p).getMana() == 100, "Rejected spawn must not spend mana or leave throw state");
        } finally { MinecraftForge.EVENT_BUS.unregister(listener); }
        h.succeed();
    }
}
