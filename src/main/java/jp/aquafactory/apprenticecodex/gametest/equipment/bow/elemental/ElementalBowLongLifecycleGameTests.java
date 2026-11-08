package jp.aquafactory.apprenticecodex.gametest.equipment.bow.elemental;

import io.redspace.ironsspellbooks.api.events.SpellPreCastEvent;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.item.elementalbow.ElementalBowCasting;
import jp.aquafactory.apprenticecodex.item.elementalbow.ElementalBowPendingCast;
import jp.aquafactory.apprenticecodex.registry.SpellRegistry;
import jp.aquafactory.apprenticecodex.spell.lunaraim.LunarAimCastData;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.function.Consumer;

import static jp.aquafactory.apprenticecodex.gametest.equipment.bow.elemental.ElementalBowLongCastFixture.*;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class ElementalBowLongLifecycleGameTests {
    private ElementalBowLongLifecycleGameTests() {}
    private static final String TEMPLATE = "gametest/bow_casting";

    @GameTest(template = TEMPLATE)
    public static void changingUsedBowOrMainhandSlotCancelsWithoutConsumption(GameTestHelper h) {
        try (var config = ElementalBowTestSupport.useElementalBowSpellConfig(h)) {
            for (var hand : InteractionHand.values()) {
                var player = player(h, hand, SpellRegistry.LUNAR_AIM.get());
                var stack = player.getItemInHand(hand);
                try {
                    begin(h, player, hand);
                    player.setItemInHand(hand, stack.copy());
                    ElementalBowPendingCast.tick(player);
                    assertCleared(h, player);
                    assertUnspent(h, player, stack);
                } finally {
                    ElementalBowPendingCast.cancel(player);
                }
            }
            var player = player(h, InteractionHand.MAIN_HAND, SpellRegistry.LUNAR_AIM.get());
            var stack = player.getMainHandItem();
            try {
                begin(h, player, InteractionHand.MAIN_HAND);
                player.getInventory().selected = 3;
                ElementalBowPendingCast.tick(player);
                assertCleared(h, player);
                assertUnspent(h, player, stack);
            } finally {
                ElementalBowPendingCast.cancel(player);
            }
        }
        h.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void cancellationAndFailedReleaseNeverConsume(GameTestHelper h) {
        try (var config = ElementalBowTestSupport.useElementalBowSpellConfig(h)) {
            for (var reason : CancellationReason.values()) {
                var player = player(h, InteractionHand.MAIN_HAND, SpellRegistry.LUNAR_AIM.get());
                var stack = player.getMainHandItem();
                begin(h, player, InteractionHand.MAIN_HAND);
                switch (reason) {
                    case EARLY_RELEASE -> stack.getItem().releaseUsing(stack, h.getLevel(), player, stack.getUseDuration(player) - 1);
                    case SWAPPED_STACK -> player.setItemInHand(InteractionHand.MAIN_HAND, stack.copy());
                    case STOPPED_USE -> player.stopUsingItem();
                    case CHANGED_MODE -> CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.putString("ElementalBowShotMode", "normal"));
                    case EXPLICIT_CANCEL -> ElementalBowPendingCast.cancel(player); // 画面・切断・移動からの共通経路。
                    case EMPTY_MANA -> {
                        MagicData.getPlayerMagicData(player).setMana(0);
                        stack.getItem().releaseUsing(stack, h.getLevel(), player, stack.getUseDuration(player) - 100);
                        MagicData.getPlayerMagicData(player).setMana(1000);
                    }
                    case EMPTY_AMMO -> {
                        player.getInventory().setItem(2, ItemStack.EMPTY);
                        stack.getItem().releaseUsing(stack, h.getLevel(), player, stack.getUseDuration(player) - 100);
                        player.getInventory().setItem(2, new ItemStack(Items.ARROW, 3));
                    }
                }
                ElementalBowPendingCast.tick(player);
                h.assertFalse(ElementalBowPendingCast.isPending(player), "Cancellation must clear the pending cast: " + reason);
                assertUnspent(h, player, stack);
                assertCleared(h, player);
            }
        }
        h.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void externalCancellationClearsHoldAndAllowsRestart(GameTestHelper h) {
        try (var config = ElementalBowTestSupport.useElementalBowSpellConfig(h)) {
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
        try (var config = ElementalBowTestSupport.useElementalBowSpellConfig(h)) {
            var player = player(h, InteractionHand.MAIN_HAND, SpellRegistry.LUNAR_AIM.get());
            var magic = MagicData.getPlayerMagicData(player);
            Consumer<SpellPreCastEvent> veto = event -> {
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
            magic.initiateCast(existing, 1, 20, CastSource.SPELLBOOK, "mainhand");
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
        try (var config = ElementalBowTestSupport.useElementalBowSpellConfig(h)) {
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

    private enum CancellationReason { EARLY_RELEASE, SWAPPED_STACK, STOPPED_USE, CHANGED_MODE, EXPLICIT_CANCEL, EMPTY_MANA, EMPTY_AMMO }
}
