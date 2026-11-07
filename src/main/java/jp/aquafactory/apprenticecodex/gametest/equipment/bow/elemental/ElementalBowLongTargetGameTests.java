package jp.aquafactory.apprenticecodex.gametest.equipment.bow.elemental;

import io.redspace.ironsspellbooks.api.events.SpellOnCastEvent;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.capabilities.magic.TargetEntityCastData;
import io.redspace.ironsspellbooks.registries.MobEffectRegistry;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.gametest.equipment.bow.support.BowTestScope;
import jp.aquafactory.apprenticecodex.item.elementalbow.ElementalBow;
import jp.aquafactory.apprenticecodex.item.elementalbow.ElementalBowOverheatManager;
import jp.aquafactory.apprenticecodex.item.elementalbow.ElementalBowPendingCast;
import jp.aquafactory.apprenticecodex.registry.SpellRegistry;
import jp.aquafactory.apprenticecodex.spell.lunaraim.LunarAimCastData;
import jp.aquafactory.apprenticecodex.spell.sacredarrow.SacredArrowCastData;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.function.Consumer;

import static jp.aquafactory.apprenticecodex.gametest.equipment.bow.elemental.ElementalBowLongCastFixture.*;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class ElementalBowLongTargetGameTests {
    private ElementalBowLongTargetGameTests() {}
    private static final String TEMPLATE = "gametest/bow_casting";

    @GameTest(template = TEMPLATE, timeoutTicks = 120)
    public static void lunarAimLocksAtDrawAndHoldsUntilRelease(GameTestHelper helper) {
        holdsTarget(helper, SpellRegistry.LUNAR_AIM.get(), InteractionHand.MAIN_HAND);
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 120)
    public static void sacredArrowLocksAtDrawInOffhand(GameTestHelper helper) {
        holdsTarget(helper, SpellRegistry.SACRED_ARROW.get(), InteractionHand.OFF_HAND);
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 120)
    public static void offhandHotbarChangesPreserveTargetAndDrawTime(GameTestHelper helper) {
        holdsTarget(helper, SpellRegistry.SACRED_ARROW.get(), InteractionHand.OFF_HAND, true);
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 120)
    public static void arrowVolleyPreservesPreconditionTarget(GameTestHelper helper) {
        holdsTarget(helper, io.redspace.ironsspellbooks.api.registry.SpellRegistry.ARROW_VOLLEY_SPELL.get(), InteractionHand.MAIN_HAND);
    }

    private static void holdsTarget(GameTestHelper h, AbstractSpell spell, InteractionHand hand) {
        holdsTarget(h, spell, hand, false);
    }

    private static void holdsTarget(GameTestHelper h, AbstractSpell spell, InteractionHand hand, boolean changeHotbar) {
        var player = player(h, hand, spell);
        var stack = player.getItemInHand(hand);
        var magic = MagicData.getPlayerMagicData(player);
        var target = EntityType.ZOMBIE.create(h.getLevel());
        target.setNoAi(true);
        target.setNoGravity(true);
        // 水平位置を専用template内に保ち、配置先chunkによる検索対象の登録漏れを避ける。
        target.setPos(player.position().add(0, 0, 1.5));
        target.addEffect(new MobEffectInstance(MobEffectRegistry.GUIDING_BOLT, 200));
        BowTestScope.of(h).track(target);
        h.getLevel().addFreshEntity(target);
        BowTestScope.of(h).assertInside(target);
        h.assertTrue(h.getLevel().isPositionEntityTicking(target.blockPosition()),
                "Target lock fixture must remain in an entity-ticking chunk");
        begin(h, player, hand);
        var captured = magic.getAdditionalCastData();
        h.assertTrue(captured != null, "Drawing must create targeting data before release");
        if (captured instanceof LunarAimCastData lunar) h.assertTrue(target.getUUID().equals(lunar.targetId()), "Lunar Aim must lock at draw start");
        if (captured instanceof SacredArrowCastData sacred) h.assertTrue(target.getUUID().equals(sacred.targetId()), "Sacred Arrow must lock at draw start");
        if (captured instanceof TargetEntityCastData targeted) h.assertTrue(target.getUUID().equals(targeted.getTargetUUID()),
                "Precondition targeting must lock the fixture target at draw start");
        player.setYRot(180);
        int ticks = ElementalBow.resolveMagicRequiredDrawTicks(stack) + 10;
        long startedAt = h.getLevel().getGameTime();
        // FakePlayer は world のプレイヤー一覧に入れず、実際の server tick ごとに弓の tick を駆動する。
        for (int i = 1; i < ticks; i++) h.runAfterDelay(i, () -> {
            try (var config = ElementalBowTestSupport.useElementalBowSpellConfig(h)) {
                // 引き始めと異なるスロットのまま保持・発射し、詠唱時間や対象がリセットされないことを確認する。
                if (changeHotbar) player.getInventory().selected = 3 + (int) (h.getLevel().getGameTime() % 2);
                ElementalBowPendingCast.tick(player);
                h.assertTrue(magic.isCasting() && magic.getAdditionalCastData() == captured,
                        "Holding must preserve the original cast without automatic completion");
                h.assertTrue(magic.getCastDurationRemaining() == Math.max(0,
                                ElementalBow.resolveMagicRequiredDrawTicks(stack) - (h.getLevel().getGameTime() - startedAt)),
                        "Holding must preserve elapsed draw time across hotbar changes");
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
            try (var config = ElementalBowTestSupport.useElementalBowSpellConfig(h)) {
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
}
