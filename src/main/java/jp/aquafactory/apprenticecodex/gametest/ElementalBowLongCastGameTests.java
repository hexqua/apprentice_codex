package jp.aquafactory.apprenticecodex.gametest;

import io.redspace.ironsspellbooks.api.events.SpellOnCastEvent;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.item.elementalbow.*;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import jp.aquafactory.apprenticecodex.registry.SpellRegistry;
import jp.aquafactory.apprenticecodex.spell.lunaraim.LunarAimCastData;
import jp.aquafactory.apprenticecodex.spell.sacredarrow.SacredArrowCastData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.function.Consumer;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class ElementalBowLongCastGameTests {
    private static final String TEMPLATE = "gametest/basic_floor";

    @GameTest(template = TEMPLATE, timeoutTicks = 120)
    public static void lunarAimLocksAtDrawAndHoldsUntilRelease(GameTestHelper helper) {
        holdsTarget(helper, SpellRegistry.LUNAR_AIM.get(), InteractionHand.MAIN_HAND);
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 120)
    public static void sacredArrowLocksAtDrawInOffhand(GameTestHelper helper) {
        holdsTarget(helper, SpellRegistry.SACRED_ARROW.get(), InteractionHand.OFF_HAND);
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 120)
    public static void arrowVolleyPreservesPreconditionTarget(GameTestHelper helper) {
        holdsTarget(helper, io.redspace.ironsspellbooks.api.registry.SpellRegistry.ARROW_VOLLEY_SPELL.get(), InteractionHand.MAIN_HAND);
    }

    private static void holdsTarget(GameTestHelper h, AbstractSpell spell, InteractionHand hand) {
        var player = player(h, hand, spell);
        var stack = player.getItemInHand(hand);
        var magic = MagicData.getPlayerMagicData(player);
        var target = EntityType.ZOMBIE.create(h.getLevel());
        target.setNoAi(true);
        target.setNoGravity(true);
        target.setPos(player.position().add(0, 0, 5));
        target.addEffect(new MobEffectInstance(io.redspace.ironsspellbooks.registries.MobEffectRegistry.GUIDING_BOLT, 200));
        h.getLevel().addFreshEntity(target);
        begin(h, player, hand);
        var captured = magic.getAdditionalCastData();
        h.assertTrue(captured != null, "Drawing must create targeting data before release");
        if (captured instanceof LunarAimCastData lunar) h.assertTrue(target.getUUID().equals(lunar.targetId()), "Lunar Aim must lock at draw start");
        if (captured instanceof SacredArrowCastData sacred) h.assertTrue(target.getUUID().equals(sacred.targetId()), "Sacred Arrow must lock at draw start");
        player.setYRot(180);
        int ticks = ElementalBow.resolveMagicRequiredDrawTicks(stack) + 10;
        long startedAt = h.getLevel().getGameTime();
        // FakePlayer は world のプレイヤー一覧に入れず、実際の server tick ごとに弓の tick を駆動する。
        for (int i = 1; i < ticks; i++) h.runAfterDelay(i, () -> {
            try (var config = BowGameTestSupport.useElementalBowSpellConfig(h)) {
                ElementalBowPendingCast.tick(player);
                h.assertTrue(magic.isCasting() && magic.getAdditionalCastData() == captured,
                        "Holding must preserve the original cast without automatic completion");
                assertUnspent(h, player, stack);
            }
        });
        h.runAfterDelay(ticks, () -> {
            int[] casts = {0};
            Consumer<SpellOnCastEvent> listener = event -> {
                if (event.getEntity() != player) return;
                casts[0]++;
                h.assertTrue(magic.getAdditionalCastData() == captured, "Release must not reacquire the target");
            };
            NeoForge.EVENT_BUS.addListener(listener);
            try (var config = BowGameTestSupport.useElementalBowSpellConfig(h)) {
                int elapsed = (int) (h.getLevel().getGameTime() - startedAt);
                stack.getItem().releaseUsing(stack, h.getLevel(), player, stack.getUseDuration(player) - elapsed);
                stack.getItem().releaseUsing(stack, h.getLevel(), player, stack.getUseDuration(player) - elapsed);
                h.assertTrue(casts[0] == 1 && stack.getDamageValue() == 1, "Release must cast and damage exactly once");
                h.assertTrue(player.getInventory().getItem(2).getCount() == 2, "Release must consume exactly one arrow");
                h.assertTrue(magic.getMana() < 1000 && ElementalBowOverheatManager.getState(player).active(), "Successful release must consume mana and apply heat");
                assertCleared(h, player);
                h.assertFalse(magic.getPlayerCooldowns().isOnCooldown(spell), "Held casting must bypass normal cooldown");
                h.succeed();
            } finally {
                NeoForge.EVENT_BUS.unregister(listener);
                ElementalBowPendingCast.cancel(player);
                target.discard();
            }
        });
    }

    @GameTest(template = TEMPLATE)
    public static void cancellationAndFailedReleaseNeverConsume(GameTestHelper h) {
        try (var config = BowGameTestSupport.useElementalBowSpellConfig(h)) {
            for (int reason = 0; reason < 7; reason++) {
                var player = player(h, InteractionHand.MAIN_HAND, SpellRegistry.LUNAR_AIM.get());
                var stack = player.getMainHandItem();
                begin(h, player, InteractionHand.MAIN_HAND);
                switch (reason) {
                    case 0 -> stack.getItem().releaseUsing(stack, h.getLevel(), player, stack.getUseDuration(player) - 1);
                    case 1 -> player.setItemInHand(InteractionHand.MAIN_HAND, stack.copy());
                    case 2 -> player.stopUsingItem();
                    case 3 -> CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.putString("ElementalBowShotMode", "normal"));
                    case 4 -> ElementalBowPendingCast.cancel(player); // 画面・切断・移動からの共通経路。
                    case 5 -> {
                        MagicData.getPlayerMagicData(player).setMana(0);
                        stack.getItem().releaseUsing(stack, h.getLevel(), player, stack.getUseDuration(player) - 100);
                        MagicData.getPlayerMagicData(player).setMana(1000);
                    }
                    case 6 -> {
                        player.getInventory().setItem(2, ItemStack.EMPTY);
                        stack.getItem().releaseUsing(stack, h.getLevel(), player, stack.getUseDuration(player) - 100);
                        player.getInventory().setItem(2, new ItemStack(Items.ARROW, 3));
                    }
                }
                ElementalBowPendingCast.tick(player);
                ElementalBowPendingCast.cancel(player);
                assertUnspent(h, player, stack);
                assertCleared(h, player);
            }
        }
        h.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void externalCancellationClearsHoldAndAllowsRestart(GameTestHelper h) {
        try (var config = BowGameTestSupport.useElementalBowSpellConfig(h)) {
            var player = player(h, InteractionHand.MAIN_HAND, SpellRegistry.LUNAR_AIM.get());
            begin(h, player, InteractionHand.MAIN_HAND);
            var magic = MagicData.getPlayerMagicData(player);
            SpellRegistry.LUNAR_AIM.get().onServerCastComplete(h.getLevel(), 1, player, magic, true);
            assertCleared(h, player);
            begin(h, player, InteractionHand.MAIN_HAND);
            ElementalBowPendingCast.cancel(player);
            assertUnspent(h, player, player.getMainHandItem());
            assertCleared(h, player);
        }
        h.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void vetoedStartCleansTargetAndExistingCastIsPreserved(GameTestHelper h) {
        try (var config = BowGameTestSupport.useElementalBowSpellConfig(h)) {
            var player = player(h, InteractionHand.MAIN_HAND, SpellRegistry.LUNAR_AIM.get());
            var magic = MagicData.getPlayerMagicData(player);
            Consumer<io.redspace.ironsspellbooks.api.events.SpellPreCastEvent> veto = event -> {
                if (event.getEntity() == player) event.setCanceled(true);
            };
            NeoForge.EVENT_BUS.addListener(veto);
            try {
                h.assertFalse(player.getMainHandItem().getItem().use(h.getLevel(), player, InteractionHand.MAIN_HAND)
                        .getResult().consumesAction(), "A vetoed start must fail");
                assertCleared(h, player);
                assertUnspent(h, player, player.getMainHandItem());
            } finally { NeoForge.EVENT_BUS.unregister(veto); }
            var existing = io.redspace.ironsspellbooks.api.registry.SpellRegistry.FIRE_ARROW_SPELL.get();
            magic.initiateCast(existing, 1, 20, io.redspace.ironsspellbooks.api.spells.CastSource.SPELLBOOK, "mainhand");
            var data = new LunarAimCastData(null, h.getLevel().dimension());
            magic.setAdditionalCastData(data);
            h.assertFalse(player.getMainHandItem().getItem().use(h.getLevel(), player, InteractionHand.MAIN_HAND)
                    .getResult().consumesAction(), "Another active cast must reject bow drawing");
            h.assertTrue(magic.isCasting() && existing.getSpellId().equals(magic.getCastingSpellId())
                    && magic.getAdditionalCastData() == data, "Failed bow start must preserve another cast");
            magic.resetCastingState();
        }
        h.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void lifecycleCallbacksAreScopedAndCompleteOnce(GameTestHelper h) {
        try (var config = BowGameTestSupport.useElementalBowSpellConfig(h)) {
            var spell = new LifecycleSpell();
            var player = player(h, InteractionHand.MAIN_HAND, io.redspace.ironsspellbooks.api.registry.SpellRegistry.FIRE_ARROW_SPELL.get());
            var stack = player.getMainHandItem();
            h.assertTrue(ElementalBowPendingCast.begin(player, stack, InteractionHand.MAIN_HAND, spell, 1, 20), "Probe cast must start");
            h.assertTrue(ElementalBowPendingCast.shouldBypassMagicManager(MagicData.getPlayerMagicData(player)),
                    "Standard MagicManager must bypass the managed cast");
            for (int i = 0; i < 3; i++) ElementalBowPendingCast.tick(player);
            h.assertTrue(spell.starts == 1 && spell.ticks == 3 && spell.casts == 0, "Holding must call start once and each managed tick once");
            h.assertTrue(ElementalBowPendingCast.release(player, stack, spell, 1, 20), "Ready probe must release");
            ElementalBowPendingCast.cancel(player);
            h.assertTrue(spell.casts == 1 && spell.completions == 1 && spell.cancelled == 0 && spell.scoped,
                    "Successful release must complete once with the bow scope active");
            h.assertFalse(ElementalBowCasting.isActive(player, spell), "Callback scope must not escape into later work");
            h.assertTrue(ElementalBowPendingCast.begin(player, stack, InteractionHand.MAIN_HAND, spell, 1, 20), "Probe must restart");
            ElementalBowPendingCast.cancel(player);
            ElementalBowPendingCast.cancel(player);
            h.assertTrue(spell.completions == 2 && spell.cancelled == 1, "Repeated cancellation must notify once");
            assertCleared(h, player);
        }
        h.succeed();
    }

    private static final class LifecycleSpell extends io.redspace.ironsspellbooks.spells.fire.FireArrowSpell {
        int starts, ticks, casts, completions, cancelled;
        boolean scoped = true;

        @Override
        public void onServerPreCast(net.minecraft.world.level.Level level, int spellLevel,
                                    net.minecraft.world.entity.LivingEntity entity, MagicData magic) {
            starts++;
            scoped &= ElementalBowCasting.isActive((FakePlayer) entity, this);
        }

        @Override
        public void onServerCastTick(net.minecraft.world.level.Level level, int spellLevel,
                                     net.minecraft.world.entity.LivingEntity entity, MagicData magic) {
            ticks++;
            scoped &= ElementalBowCasting.isActive((FakePlayer) entity, this);
        }

        @Override
        public void onCast(net.minecraft.world.level.Level level, int spellLevel, net.minecraft.world.entity.LivingEntity entity,
                           io.redspace.ironsspellbooks.api.spells.CastSource source, MagicData magic) {
            casts++;
            scoped &= ElementalBowCasting.isActive((FakePlayer) entity, this);
        }

        @Override
        public void onServerCastComplete(net.minecraft.world.level.Level level, int spellLevel,
                                         net.minecraft.world.entity.LivingEntity entity, MagicData magic, boolean wasCancelled) {
            completions++;
            if (wasCancelled) cancelled++;
            scoped &= ElementalBowCasting.isActive((FakePlayer) entity, this);
            super.onServerCastComplete(level, spellLevel, entity, magic, wasCancelled);
        }
    }

    private static FakePlayer player(GameTestHelper h, InteractionHand hand, AbstractSpell spell) {
        var player = BowGameTestSupport.createEquipmentTestPlayer(h, new BlockPos(2, 80, 2), "bow_long");
        player.getAttribute(AttributeRegistry.MAX_MANA).setBaseValue(2000);
        MagicData.getPlayerMagicData(player).setMana(1000);
        var stack = new ItemStack(ItemRegistry.ELEMENTAL_BOW.get());
        ElementalBow.setCalibrationScroll(stack, 0, BowGameTestSupport.createSpellScroll(spell), h.getLevel().registryAccess());
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
            tag.putString("ElementalBowShotMode", "magic");
            tag.putString("ElementalBowMode", ElementalBow.selectionIdForSlot(0).toString());
        });
        player.setItemInHand(hand, stack);
        player.getInventory().setItem(2, new ItemStack(Items.ARROW, 3));
        return player;
    }

    private static void begin(GameTestHelper h, FakePlayer player, InteractionHand hand) {
        try (var config = BowGameTestSupport.useElementalBowSpellConfig(h)) {
            h.assertTrue(player.getItemInHand(hand).getItem().use(h.getLevel(), player, hand).getResult().consumesAction(),
                    "LONG bow draw must begin");
            h.assertTrue(ElementalBowPendingCast.isPending(player), "Drawing must own a pending cast");
        }
    }

    private static void assertUnspent(GameTestHelper h, FakePlayer player, ItemStack stack) {
        h.assertTrue(stack.getDamageValue() == 0 && player.getInventory().getItem(2).getCount() == 3
                && MagicData.getPlayerMagicData(player).getMana() == 1000
                && !ElementalBowOverheatManager.getState(player).active(), "Unfired casts must not spend resources or add heat");
    }

    private static void assertCleared(GameTestHelper h, FakePlayer player) {
        var magic = MagicData.getPlayerMagicData(player);
        h.assertTrue(!ElementalBowPendingCast.isPending(player) && !magic.isCasting()
                && magic.getAdditionalCastData() == null, "Completion must clear the hold and targeting data");
    }
}
